package com.permissionx.animalguide.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.AuthRepository
import com.permissionx.animalguide.domain.usecase.social.auth.LoginOrRegisterUseCase
import com.permissionx.animalguide.domain.usecase.social.auth.SendSmsCodeUseCase
import com.permissionx.animalguide.domain.usecase.social.auth.VerifySmsCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sendSmsCodeUseCase: SendSmsCodeUseCase,
    private val verifySmsCodeUseCase: VerifySmsCodeUseCase,
    private val loginOrRegisterUseCase: LoginOrRegisterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state = _state.asStateFlow()

    private val _countdown = MutableStateFlow(0)
    val countdown = _countdown.asStateFlow()

    private var verificationId: String = ""
    private var countdownJob: Job? = null

    fun sendCode(phone: String) {
        viewModelScope.launch {
            _state.value = LoginUiState.SendingCode
            val result = sendSmsCodeUseCase(phone)
            result.fold(
                onSuccess = { id ->
                    verificationId = id
                    _state.value = LoginUiState.CodeSent
                    startCountdown()
                },
                onFailure = {
                    _state.value = LoginUiState.Error(
                        it.message ?: "发送验证码失败，请重试"
                    )
                }
            )
        }
    }

    fun loginWithCode(phone: String, code: String) {
        viewModelScope.launch {
            _state.value = LoginUiState.Verifying

            val verifyResult = verifySmsCodeUseCase(verificationId, code)
            verifyResult.onFailure {
                _state.value = LoginUiState.Error(
                    it.message ?: "验证码错误，请重试"
                )
                return@launch
            }

            val verificationToken = verifyResult.getOrNull()!!
            val isExistingUser = authRepository.checkUserExists(phone, verificationToken)

            if (isExistingUser) {
                val loginResult = loginOrRegisterUseCase(phone, verificationToken)
                loginResult.fold(
                    onSuccess = { _state.value = LoginUiState.Success },
                    onFailure = {
                        _state.value = LoginUiState.Error(
                            it.message ?: "登录失败，请重试"
                        )
                    }
                )
            } else {
                _state.value = LoginUiState.CodeVerified(phone, verificationToken)
            }
        }
    }

    fun loginWithPassword(phone: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginUiState.LoggingIn
            val result = authRepository.loginWithPassword(phone, password)
            result.fold(
                onSuccess = { isNewUser ->
                    if (isNewUser) {
                        _state.value = LoginUiState.NewUser(phone, "")
                    } else {
                        _state.value = LoginUiState.Success
                    }
                },
                onFailure = {
                    _state.value = LoginUiState.Error(
                        it.message ?: "手机号或密码错误"
                    )
                }
            )
        }
    }

    fun resetError() {
        _state.value = LoginUiState.Idle
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _countdown.value = 60
            while (_countdown.value > 0) {
                delay(1000)
                _countdown.value -= 1
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    fun resetState() {
        _state.value = LoginUiState.Idle
        _countdown.value = 0
        verificationId = ""
        countdownJob?.cancel()
    }
}