package com.permissionx.animalguide.ui.auth

sealed class CompleteProfileUiState {
    object Idle : CompleteProfileUiState()
    object Uploading : CompleteProfileUiState()
    object Success : CompleteProfileUiState()
    data class Error(val message: String) : CompleteProfileUiState()
}