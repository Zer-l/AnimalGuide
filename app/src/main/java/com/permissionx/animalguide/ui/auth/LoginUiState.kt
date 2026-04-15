package com.permissionx.animalguide.ui.auth

sealed class LoginUiState {
    object Idle : LoginUiState()
    object SendingCode : LoginUiState()
    object CodeSent : LoginUiState()
    object Verifying : LoginUiState()
    object LoggingIn : LoginUiState()
    data class NewUser(
        val phone: String,
        val verificationToken: String
    ) : LoginUiState()

    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
    data class CodeVerified(
        val phone: String,
        val verificationToken: String
    ) : LoginUiState()
}