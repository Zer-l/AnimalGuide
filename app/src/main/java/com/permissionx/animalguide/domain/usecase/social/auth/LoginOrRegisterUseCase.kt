package com.permissionx.animalguide.domain.usecase.social.auth

import com.permissionx.animalguide.data.repository.AuthRepository
import javax.inject.Inject

class LoginOrRegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    // 返回 isNewUser
    suspend operator fun invoke(
        phone: String,
        verificationToken: String
    ): Result<Boolean> = authRepository.loginOrRegister(phone, verificationToken)
}