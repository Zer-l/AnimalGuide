package com.permissionx.animalguide.data.remote.cloudbase

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class CurrentUser(
    val uid: String,
    val phone: String,
    val nickname: String = "",
    val avatarUrl: String = "",
    val accessToken: String,
    val refreshToken: String = ""  // 新增
)

@Singleton
open class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudBaseClient: CloudBaseClient
) {
    // Token 等敏感信息使用加密存储，防止 root 设备或 adb backup 泄露
    private val prefs by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "user_session_enc",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _currentUser = MutableStateFlow<CurrentUser?>(restoreSession())
    val currentUser: StateFlow<CurrentUser?> = _currentUser.asStateFlow()

    val isLoggedIn: Boolean get() = _currentUser.value != null

    // 刷新锁，防止多个请求同时触发刷新
    private val refreshMutex = kotlinx.coroutines.sync.Mutex()

    fun onLoginSuccess(
        uid: String,
        phone: String,
        nickname: String,
        avatarUrl: String,
        accessToken: String,
        refreshToken: String  // 新增参数
    ) {
        val user = CurrentUser(
            uid = uid,
            phone = phone,
            nickname = nickname,
            avatarUrl = avatarUrl,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
        _currentUser.value = user
        cloudBaseClient.updateAccessToken(accessToken)
        saveSession(user)
    }

    fun onLogout() {
        _currentUser.value = null
        cloudBaseClient.clearAccessToken()
        clearSession()
    }

    fun updateUserInfo(nickname: String, avatarUrl: String) {
        val user = _currentUser.value ?: return
        _currentUser.value = user.copy(nickname = nickname, avatarUrl = avatarUrl)
        saveSession(_currentUser.value!!)
    }

    /**
     * 刷新 access_token，返回新 token；失败返回 null 并清除登录态
     * 使用 Mutex 保证并发安全：多个 401 同时触发时只刷新一次
     */
    suspend fun refreshAccessToken(): String? {
        refreshMutex.withLock {
            val currentRefreshToken = _currentUser.value?.refreshToken
                ?: return null

            return try {
                val result = cloudBaseClient.requestWithoutAuth<Map<String, Any>>(
                    method = "POST",
                    path = "/auth/v1/token",
                    body = mapOf(
                        "grant_type" to "refresh_token",
                        "refresh_token" to currentRefreshToken
                    ),
                    typeToken = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}
                )

                result.getOrNull()?.let { map ->
                    val newAccessToken = map["access_token"] as? String
                    val newRefreshToken = map["refresh_token"] as? String
                    if (newAccessToken != null && newRefreshToken != null) {
                        val user = _currentUser.value!!.copy(
                            accessToken = newAccessToken,
                            refreshToken = newRefreshToken
                        )
                        _currentUser.value = user
                        cloudBaseClient.updateAccessToken(newAccessToken)
                        saveSession(user)
                        newAccessToken
                    } else null
                }
            } catch (e: Exception) {
                onLogout()
                null
            }
        }
    }

    private fun saveSession(user: CurrentUser) {
        prefs.edit()
            .putString("uid", user.uid)
            .putString("phone", user.phone)
            .putString("nickname", user.nickname)
            .putString("avatarUrl", user.avatarUrl)
            .putString("accessToken", user.accessToken)
            .putString("refreshToken", user.refreshToken)  // 新增
            .apply()
        cloudBaseClient.updateAccessToken(user.accessToken)
    }

    private fun restoreSession(): CurrentUser? {
        val uid = prefs.getString("uid", null) ?: return null
        val phone = prefs.getString("phone", null) ?: return null
        val accessToken = prefs.getString("accessToken", null) ?: return null
        val nickname = prefs.getString("nickname", "") ?: ""
        val avatarUrl = prefs.getString("avatarUrl", "") ?: ""
        val refreshToken = prefs.getString("refreshToken", "") ?: ""  // 新增
        cloudBaseClient.updateAccessToken(accessToken)
        return CurrentUser(
            uid = uid,
            phone = phone,
            nickname = nickname,
            avatarUrl = avatarUrl,
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }
}