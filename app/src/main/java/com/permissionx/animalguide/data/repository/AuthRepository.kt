package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.local.CachedUserDao
import com.permissionx.animalguide.data.local.entity.CachedUserEntity
import com.permissionx.animalguide.data.remote.cloudbase.AuthDataSource
import com.permissionx.animalguide.data.remote.cloudbase.CollectDataSource
import com.permissionx.animalguide.data.remote.cloudbase.CommentDataSource
import com.permissionx.animalguide.data.remote.cloudbase.DefaultImageHelper
import com.permissionx.animalguide.data.remote.cloudbase.FollowDataSource
import com.permissionx.animalguide.data.remote.cloudbase.LikeDataSource
import com.permissionx.animalguide.data.remote.cloudbase.PostDataSource
import com.permissionx.animalguide.data.remote.cloudbase.StorageDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource,
    private val postDataSource: PostDataSource,
    private val commentDataSource: CommentDataSource,
    private val likeDataSource: LikeDataSource,
    private val collectDataSource: CollectDataSource,
    private val followDataSource: FollowDataSource,
    private val userSessionManager: UserSessionManager,
    private val storageDataSource: StorageDataSource,
    private val defaultImageHelper: DefaultImageHelper,
    private val cachedUserDao: CachedUserDao
) {
    // 发送验证码，返回 verificationId
    suspend fun sendSmsCode(phone: String): Result<String> =
        authDataSource.sendSmsCode(phone)

    // 验证验证码，返回 verificationToken
    suspend fun verifySmsCode(
        verificationId: String,
        code: String
    ): Result<String> = authDataSource.verifySmsCode(verificationId, code)

    // 登录或注册
    suspend fun loginOrRegister(
        phone: String,
        verificationToken: String,
        password: String? = null
    ): Result<Boolean> {
        val authResult = authDataSource.loginOrRegister(phone, verificationToken, password)
        authResult.onFailure { return Result.failure(it) }

        val (accessToken, uid, refreshToken) = authResult.getOrNull()!!

        userSessionManager.onLoginSuccess(
            uid = uid,
            phone = phone,
            nickname = "",
            avatarUrl = "",
            accessToken = accessToken,
            refreshToken = refreshToken

        )

        val userResult = userDataSource.getUserByUid(uid)
        userResult.onFailure { return Result.failure(it) }

        val userMap = userResult.getOrNull()
        val isNewUser = userMap == null

        if (isNewUser) {
            val defaultNickname = "探险家${phone.takeLast(4)}"

            // 上传随机默认头像
            val avatarUri = defaultImageHelper.getRandomAvatarUri()
            val defaultAvatarUrl = storageDataSource.uploadImage(
                uri = avatarUri,
                path = "avatars/${uid}_default.jpg"
            ).getOrNull() ?: ""

            // 上传随机默认背景图
            val bgUri = defaultImageHelper.getRandomBackgroundUri()
            val defaultBgUrl = storageDataSource.uploadImage(
                uri = bgUri,
                path = "backgrounds/${uid}_default.jpg"
            ).getOrNull() ?: ""

            userDataSource.createUser(
                uid = uid,
                phone = phone,
                nickname = defaultNickname,
                avatarUrl = defaultAvatarUrl,
                bio = "",
                gender = "SECRET"
            )

            // 写入背景图
            if (defaultBgUrl.isNotEmpty()) {
                userDataSource.updateBackground(uid, defaultBgUrl)
            }

            userSessionManager.updateUserInfo(defaultNickname, defaultAvatarUrl)
            cachedUserDao.insertUser(
                CachedUserEntity(
                    uid = uid, nickname = defaultNickname, avatarUrl = defaultAvatarUrl,
                    backgroundUrl = "", bio = "", phone = phone, gender = "SECRET",
                    postCount = 0, followCount = 0, followerCount = 0, likeCount = 0,
                    cachedAt = System.currentTimeMillis()
                )
            )
        } else {
            val nickname = userMap!!["nickname"] as? String ?: ""
            val avatarUrl = userMap["avatarUrl"] as? String ?: ""
            userSessionManager.updateUserInfo(nickname, avatarUrl)
            cachedUserDao.insertUser(
                CachedUserEntity(
                    uid = uid, nickname = nickname, avatarUrl = avatarUrl,
                    backgroundUrl = userMap["backgroundUrl"] as? String ?: "",
                    bio = userMap["bio"] as? String ?: "",
                    phone = phone, gender = userMap["gender"] as? String ?: "SECRET",
                    postCount = (userMap["postCount"] as? Double)?.toInt() ?: 0,
                    followCount = (userMap["followCount"] as? Double)?.toInt() ?: 0,
                    followerCount = (userMap["followerCount"] as? Double)?.toInt() ?: 0,
                    likeCount = (userMap["likeCount"] as? Double)?.toInt() ?: 0,
                    cachedAt = System.currentTimeMillis()
                )
            )
        }

        return Result.success(isNewUser)
    }

    fun logout() = userSessionManager.onLogout()

    val currentUser = userSessionManager.currentUser
    val isLoggedIn get() = userSessionManager.isLoggedIn

    suspend fun loginWithPassword(
        phone: String,
        password: String
    ): Result<Boolean> {
        val authResult = authDataSource.loginWithPassword(phone, password)
        authResult.onFailure { return Result.failure(it) }

        val (accessToken, uid, refreshToken) = authResult.getOrNull()!!

        userSessionManager.onLoginSuccess(
            uid = uid,
            phone = phone,
            nickname = "",
            avatarUrl = "",
            accessToken = accessToken,
            refreshToken = refreshToken
        )

        val userResult = userDataSource.getUserByUid(uid)
        userResult.onFailure { return Result.failure(it) }

        val userMap = userResult.getOrNull()
        val isNewUser = userMap == null

        if (!isNewUser && userMap != null) {
            val nickname = userMap["nickname"] as? String ?: ""
            val avatarUrl = userMap["avatarUrl"] as? String ?: ""
            userSessionManager.updateUserInfo(nickname, avatarUrl)
            cachedUserDao.insertUser(
                CachedUserEntity(
                    uid = uid, nickname = nickname, avatarUrl = avatarUrl,
                    backgroundUrl = userMap["backgroundUrl"] as? String ?: "",
                    bio = userMap["bio"] as? String ?: "",
                    phone = phone, gender = userMap["gender"] as? String ?: "SECRET",
                    postCount = (userMap["postCount"] as? Double)?.toInt() ?: 0,
                    followCount = (userMap["followCount"] as? Double)?.toInt() ?: 0,
                    followerCount = (userMap["followerCount"] as? Double)?.toInt() ?: 0,
                    likeCount = (userMap["likeCount"] as? Double)?.toInt() ?: 0,
                    cachedAt = System.currentTimeMillis()
                )
            )
        } else if (isNewUser) {
            // 密码登录的新用户（理论上不常见，但保持一致）
            val defaultNickname = "探险家${phone.takeLast(4)}"

            val avatarUri = defaultImageHelper.getRandomAvatarUri()
            val defaultAvatarUrl = storageDataSource.uploadImage(
                uri = avatarUri,
                path = "avatars/${uid}_default.jpg"
            ).getOrNull() ?: ""

            val bgUri = defaultImageHelper.getRandomBackgroundUri()
            val defaultBgUrl = storageDataSource.uploadImage(
                uri = bgUri,
                path = "backgrounds/${uid}_default.jpg"
            ).getOrNull() ?: ""

            userDataSource.createUser(
                uid = uid,
                phone = phone,
                nickname = defaultNickname,
                avatarUrl = defaultAvatarUrl,
                bio = "",
                gender = "SECRET"
            )

            if (defaultBgUrl.isNotEmpty()) {
                userDataSource.updateBackground(uid, defaultBgUrl)
            }

            userSessionManager.updateUserInfo(defaultNickname, defaultAvatarUrl)
            cachedUserDao.insertUser(
                CachedUserEntity(
                    uid = uid, nickname = defaultNickname, avatarUrl = defaultAvatarUrl,
                    backgroundUrl = "", bio = "", phone = phone, gender = "SECRET",
                    postCount = 0, followCount = 0, followerCount = 0, likeCount = 0,
                    cachedAt = System.currentTimeMillis()
                )
            )
        }

        return Result.success(isNewUser)
    }

    suspend fun checkUserExists(phone: String, verificationToken: String): Boolean {
        val result = authDataSource.tryLogin(phone, verificationToken)
        return result.isSuccess
    }

    suspend fun deleteAccount(): Result<Unit> {
        val uid = userSessionManager.currentUser.value?.uid
            ?: return Result.failure(Exception("未登录"))

        // 1. 修复点赞计数 → 同步帖子/评论作者获赞数 → 逐条删除点赞记录（非致命）
        val likeRecords = likeDataSource.getUserLikeRecords(uid).getOrNull() ?: emptyList()
        likeRecords.forEach { like ->
            val targetId = like["targetId"] as? String ?: return@forEach
            val targetType = like["targetType"] as? String ?: return@forEach
            when (targetType) {
                "POST" -> {
                    val authorUid = postDataSource.decrementPostField(targetId, "likeCount").getOrNull()
                    authorUid?.let { userDataSource.updateUserCount(it, "likeCount", -1) }
                }
                "COMMENT" -> commentDataSource.decrementCommentLikeCount(targetId)
            }
        }
        likeRecords.forEach { like ->
            (like["_id"] as? String)?.let { likeDataSource.deleteLikeById(it) }
        }

        // 2. 修复收藏计数 → 逐条删除收藏记录（非致命）
        val collectRecords = collectDataSource.getUserCollectRecords(uid).getOrNull() ?: emptyList()
        collectRecords.forEach { collect ->
            val postId = collect["postId"] as? String ?: return@forEach
            postDataSource.decrementPostField(postId, "collectCount")
        }
        collectRecords.forEach { collect ->
            (collect["_id"] as? String)?.let { collectDataSource.deleteCollectById(it) }
        }

        // 3. 修复关注计数：我关注的人 followerCount -1（非致命）
        followDataSource.getAllFollowingUids(uid).getOrNull()?.forEach { toUid ->
            userDataSource.updateUserCount(toUid, "followerCount", -1)
        }

        // 4. 修复粉丝计数：关注我的人 followCount -1（非致命）
        followDataSource.getAllFollowerUids(uid).getOrNull()?.forEach { fromUid ->
            userDataSource.updateUserCount(fromUid, "followCount", -1)
        }

        // 5. 删除关注/粉丝记录（非致命）
        followDataSource.deleteUserFollows(uid)

        // 6. 删除评论（非致命）
        val commentPostCounts = commentDataSource.getUserCommentPostCounts(uid).getOrNull()
        commentDataSource.deleteUserComments(uid)
        commentPostCounts?.forEach { (postId, count) ->
            postDataSource.decrementPostCommentCount(postId, count)
        }

        // 7. 删除帖子（致命：先查出所有 _id，再逐条删除）
        postDataSource.deleteUserPosts(uid).onFailure {
            return Result.failure(Exception("注销失败：删除帖子时出错"))
        }

        // 8. 删除用户文档（致命）
        userDataSource.deleteUser(uid).onFailure {
            return Result.failure(Exception("注销失败：删除用户数据时出错"))
        }

        // 9. 删除 Auth 账号（非致命）
        authDataSource.deleteAuthAccount()

        // 10. 清除本地 Session
        userSessionManager.onLogout()
        return Result.success(Unit)
    }
}