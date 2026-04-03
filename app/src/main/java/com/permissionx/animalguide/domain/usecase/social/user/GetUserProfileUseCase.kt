package com.permissionx.animalguide.domain.usecase.social.user

import com.permissionx.animalguide.data.repository.UserRepository
import com.permissionx.animalguide.domain.model.social.User
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String): Result<User> =
        userRepository.getUserProfile(uid)
}