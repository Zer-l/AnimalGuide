package com.permissionx.animalguide.domain.usecase.social.auth

import com.permissionx.animalguide.data.remote.cloudbase.AuthValidator
import com.permissionx.animalguide.data.repository.AuthRepository
import javax.inject.Inject

class SendSmsCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phone: String): Result<String> {
        val validation = AuthValidator.validatePhone(phone)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }
        return authRepository.sendSmsCode(phone)
    }
}