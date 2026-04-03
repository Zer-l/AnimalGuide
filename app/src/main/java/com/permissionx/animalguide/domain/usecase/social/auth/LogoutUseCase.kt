package com.permissionx.animalguide.domain.usecase.social.auth

import com.permissionx.animalguide.data.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke() = authRepository.logout()
}