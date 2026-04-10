package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.remote.cloudbase.AuthDataSource
import com.permissionx.animalguide.data.remote.cloudbase.DefaultImageHelper
import com.permissionx.animalguide.data.remote.cloudbase.StorageDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource,
    private val userSessionManager: UserSessionManager,
    private val storageDataSource: StorageDataSource,  // 新增
    private val defaultImageHelper: DefaultImageHelper  // 新增
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
        } else {
            val nickname = userMap!!["nickname"] as? String ?: ""
            val avatarUrl = userMap["avatarUrl"] as? String ?: ""
            userSessionManager.updateUserInfo(nickname, avatarUrl)
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
        }

        return Result.success(isNewUser)
    }

    suspend fun checkUserExists(phone: String, verificationToken: String): Boolean {
        val result = authDataSource.tryLogin(phone, verificationToken)
        return result.isSuccess
    }
}