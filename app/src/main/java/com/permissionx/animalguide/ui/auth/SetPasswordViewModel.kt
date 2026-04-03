package com.permissionx.animalguide.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.remote.cloudbase.AuthValidator
import com.permissionx.animalguide.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SetPasswordUiState {
    object Idle : SetPasswordUiState()
    object Loading : SetPasswordUiState()
    object Success : SetPasswordUiState()
    data class Error(val message: String) : SetPasswordUiState()
}

@HiltViewModel
class SetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SetPasswordUiState>(SetPasswordUiState.Idle)
    val state = _state.asStateFlow()

    fun setPassword(
        phone: String,
        verificationToken: String,
        password: String
    ) {
        val validation = AuthValidator.validatePassword(password)
        if (validation.isFailure) {
            _state.value = SetPasswordUiState.Error(
                validation.exceptionOrNull()?.message ?: "密码格式不正确"
            )
            return
        }
        viewModelScope.launch {
            _state.value = SetPasswordUiState.Loading
            val result = authRepository.loginOrRegister(
                phone = phone,
                verificationToken = verificationToken,
                password = password
            )
            result.fold(
                onSuccess = {
                    // 无论新老用户都直接进入社区
                    _state.value = SetPasswordUiState.Success
                },
                onFailure = {
                    _state.value = SetPasswordUiState.Error(
                        it.message ?: "设置失败，请重试"
                    )
                }
            )
        }
    }
}