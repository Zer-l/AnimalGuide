package com.permissionx.animalguide.data.repository

import android.net.Uri
import com.permissionx.animalguide.data.local.CachedUserDao
import com.permissionx.animalguide.data.local.entity.toCacheEntity
import com.permissionx.animalguide.data.remote.cloudbase.DefaultImageHelper
import com.permissionx.animalguide.data.remote.cloudbase.StorageDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.domain.model.social.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDataSource: UserDataSource,
    private val storageDataSource: StorageDataSource,
    private val userSessionManager: UserSessionManager,
    private val cachedUserDao: CachedUserDao
) {
    // 获取用户资料：网络优先，失败时回退本地缓存
    suspend fun getUserProfile(uid: String): Result<User> {
        val result = userDataSource.getUserByUid(uid)
        return result.fold(
            onSuccess = { map ->
                if (map != null) {
                    val user = map.toUser()
                    cachedUserDao.insertUser(user.toCacheEntity())
                    Result.success(user)
                } else Result.failure(Exception("用户不存在"))
            },
            onFailure = {
                val cached = cachedUserDao.getUserByUid(uid)
                if (cached != null) Result.success(cached.toUser())
                else Result.failure(it)
            }
        )
    }

    // 更新用户资料
    suspend fun updateUserProfile(
        nickname: String,
        avatarUri: Uri?,
        bio: String,
        gender: String
    ): Result<Boolean> {
        val user = userSessionManager.currentUser.value
            ?: return Result.failure(Exception("未登录"))

        val avatarUrl = if (avatarUri != null) {
            val uploadResult = storageDataSource.uploadImage(
                uri = avatarUri,
                path = "avatars/${user.uid}_${System.currentTimeMillis()}.jpg"
            )
            uploadResult.getOrNull() ?: user.avatarUrl
        } else user.avatarUrl

        val updateResult = userDataSource.updateUser(
            uid = user.uid,
            nickname = nickname,
            avatarUrl = avatarUrl,
            bio = bio,
            gender = gender
        )
        updateResult.onFailure { return Result.failure(it) }

        userSessionManager.updateUserInfo(nickname, avatarUrl)
        return Result.success(true)
    }

    private fun Map<String, Any>.toUser() = User(
        id = this["_openid"] as? String ?: this["_id"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "",
        avatarUrl = this["avatarUrl"] as? String ?: "",
        backgroundUrl = this["backgroundUrl"] as? String ?: "",  // 新增
        bio = this["bio"] as? String ?: "",
        phone = this["phone"] as? String ?: "",
        gender = this["gender"] as? String ?: "SECRET",
        postCount = (this["postCount"] as? Double)?.toInt() ?: 0,
        followCount = (this["followCount"] as? Double)?.toInt() ?: 0,
        followerCount = (this["followerCount"] as? Double)?.toInt() ?: 0,
        likeCount = (this["likeCount"] as? Double)?.toInt() ?: 0
    )

    // 更新用户计数字段（postCount、followCount、followerCount、likeCount等）
    suspend fun updateUserCount(
        uid: String,
        field: String,
        increment: Int = 1
    ): Result<Boolean> = userDataSource.updateUserCount(uid, field, increment)

    suspend fun updateBackground(backgroundUri: Uri): Result<String> {
        val user = userSessionManager.currentUser.value
            ?: return Result.failure(Exception("未登录"))

        val uploadResult = storageDataSource.uploadImage(
            uri = backgroundUri,
            path = "backgrounds/${user.uid}_${System.currentTimeMillis()}.jpg"
        )
        val backgroundUrl = uploadResult.getOrNull()
            ?: return Result.failure(Exception("上传失败"))

        val updateResult = userDataSource.updateBackground(user.uid, backgroundUrl)
        updateResult.onFailure { return Result.failure(it) }

        return Result.success(backgroundUrl)
    }

    suspend fun updateAvatar(avatarUri: Uri): Result<String> {
        val user = userSessionManager.currentUser.value
            ?: return Result.failure(Exception("未登录"))

        val uploadResult = storageDataSource.uploadImage(
            uri = avatarUri,
            path = "avatars/${user.uid}_${System.currentTimeMillis()}.jpg"
        )
        val avatarUrl = uploadResult.getOrNull()
            ?: return Result.failure(Exception("上传失败"))

        val updateResult = userDataSource.updateAvatar(user.uid, avatarUrl)
        updateResult.onFailure { return Result.failure(it) }

        userSessionManager.updateUserInfo(user.nickname, avatarUrl)
        return Result.success(avatarUrl)
    }
}