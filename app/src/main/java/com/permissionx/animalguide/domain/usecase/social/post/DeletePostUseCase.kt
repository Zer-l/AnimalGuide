package com.permissionx.animalguide.domain.usecase.social.post

import com.permissionx.animalguide.data.repository.PostRepository
import javax.inject.Inject

class DeletePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): Result<Boolean> =
        postRepository.deletePost(postId)
}