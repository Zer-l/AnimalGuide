package com.permissionx.animalguide.ui.social.publish

sealed class PublishUiState {
    object Idle : PublishUiState()
    object Publishing : PublishUiState()
    object Success : PublishUiState()
    data class Error(val message: String) : PublishUiState()
}