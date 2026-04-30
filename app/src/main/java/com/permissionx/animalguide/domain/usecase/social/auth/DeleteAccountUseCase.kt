package com.permissionx.animalguide.domain.usecase.social.auth

import com.permissionx.animalguide.data.repository.AuthRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.deleteAccount()
}
