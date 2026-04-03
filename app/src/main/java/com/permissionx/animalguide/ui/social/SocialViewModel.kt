package com.permissionx.animalguide.ui.social

import androidx.lifecycle.ViewModel
import com.permissionx.animalguide.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState = authRepository.currentUser.map { user ->
        if (user != null) SocialUiState.LoggedIn
        else SocialUiState.Guest
    }

    val currentUser = authRepository.currentUser
}