package com.permissionx.animalguide.ui.me

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import coil.Coil
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class EditProfileUiState {
    object Idle : EditProfileUiState()
    object Saving : EditProfileUiState()
    object Success : EditProfileUiState()
    data class Error(val message: String) : EditProfileUiState()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val state = _state.asStateFlow()

    val currentUser = userSessionManager.currentUser

    fun saveProfile(
        nickname: String,
        avatarUri: Uri?,
        bio: String,
        gender: String
    ) {
        viewModelScope.launch {
            _state.value = EditProfileUiState.Saving
            val result = userRepository.updateUserProfile(
                nickname = nickname,
                avatarUri = avatarUri,
                bio = bio,
                gender = gender
            )
            result.fold(
                onSuccess = {
                    // 清除 Coil 图片缓存
                    if (avatarUri != null) {
                        Coil.imageLoader(context).memoryCache?.clear()
                        Coil.imageLoader(context).diskCache?.clear()
                    }
                    _state.value = EditProfileUiState.Success
                },
                onFailure = {
                    _state.value = EditProfileUiState.Error(
                        it.message ?: "保存失败，请重试"
                    )
                }
            )
        }
    }
}