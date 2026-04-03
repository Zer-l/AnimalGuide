package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.remote.cloudbase.AuthDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authDataSource: AuthDataSource,
    private val userDataSource: UserDataSource,
    private val userSessionManager: UserSessionManager
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

        val (accessToken, uid) = authResult.getOrNull()!!

        userSessionManager.onLoginSuccess(
            uid = uid,
            phone = phone,
            nickname = "",
            avatarUrl = "",
            accessToken = accessToken
        )

        val userResult = userDataSource.getUserByUid(uid)
        userResult.onFailure { return Result.failure(it) }

        val userMap = userResult.getOrNull()
        val isNewUser = userMap == null

        if (isNewUser) {
            // 自动生成默认昵称和头像
            val defaultNickname = "探险家${phone.takeLast(4)}"
            val defaultAvatarUrl = ""
            userDataSource.createUser(
                uid = uid,
                phone = phone,
                nickname = defaultNickname,
                avatarUrl = defaultAvatarUrl,
                bio = "",
                gender = "SECRET"
            )
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

        val (accessToken, uid) = authResult.getOrNull()!!

        userSessionManager.onLoginSuccess(
            uid = uid,
            phone = phone,
            nickname = "",
            avatarUrl = "",
            accessToken = accessToken
        )

        val userResult = userDataSource.getUserByUid(uid)
        userResult.onFailure { return Result.failure(it) }

        val userMap = userResult.getOrNull()
        val isNewUser = userMap == null

        if (!isNewUser && userMap != null) {
            val nickname = userMap["nickname"] as? String ?: ""
            val avatarUrl = userMap["avatarUrl"] as? String ?: ""
            userSessionManager.updateUserInfo(nickname, avatarUrl)
        }

        return Result.success(isNewUser)
    }

    suspend fun checkUserExists(phone: String, verificationToken: String): Boolean {
        val result = authDataSource.tryLogin(phone, verificationToken)
        return result.isSuccess
    }
}