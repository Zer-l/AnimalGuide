package com.permissionx.animalguide.data.remote.cloudbase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class CurrentUser(
    val uid: String,
    val phone: String,
    val nickname: String = "",
    val avatarUrl: String = "",
    val accessToken: String
)

@Singleton
class UserSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cloudBaseClient: CloudBaseClient
) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<CurrentUser?>(restoreSession())
    val currentUser: StateFlow<CurrentUser?> = _currentUser.asStateFlow()

    val isLoggedIn: Boolean get() = _currentUser.value != null

    fun onLoginSuccess(
        uid: String,
        phone: String,
        nickname: String,
        avatarUrl: String,
        accessToken: String
    ) {
        val user = CurrentUser(
            uid = uid,
            phone = phone,
            nickname = nickname,
            avatarUrl = avatarUrl,
            accessToken = accessToken
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

    private fun saveSession(user: CurrentUser) {
        prefs.edit()
            .putString("uid", user.uid)
            .putString("phone", user.phone)
            .putString("nickname", user.nickname)
            .putString("avatarUrl", user.avatarUrl)
            .putString("accessToken", user.accessToken)
            .apply()
        cloudBaseClient.updateAccessToken(user.accessToken)
    }

    private fun restoreSession(): CurrentUser? {
        val uid = prefs.getString("uid", null) ?: return null
        val phone = prefs.getString("phone", null) ?: return null
        val accessToken = prefs.getString("accessToken", null) ?: return null
        val nickname = prefs.getString("nickname", "") ?: ""
        val avatarUrl = prefs.getString("avatarUrl", "") ?: ""
        cloudBaseClient.updateAccessToken(accessToken)
        return CurrentUser(
            uid = uid,
            phone = phone,
            nickname = nickname,
            avatarUrl = avatarUrl,
            accessToken = accessToken
        )
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }
}