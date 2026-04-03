package com.permissionx.animalguide.ui.social

sealed class SocialUiState {
    object Guest : SocialUiState()
    object LoggedIn : SocialUiState()
}