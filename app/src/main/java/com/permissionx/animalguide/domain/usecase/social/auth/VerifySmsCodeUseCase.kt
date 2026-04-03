package com.permissionx.animalguide.domain.usecase.social.auth

import com.permissionx.animalguide.data.repository.AuthRepository
import javax.inject.Inject

class VerifySmsCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        verificationId: String,
        code: String
    ): Result<String> {
        if (code.length != 6) {
            return Result.failure(Exception("请输入6位验证码"))
        }
        return authRepository.verifySmsCode(verificationId, code)
    }
}