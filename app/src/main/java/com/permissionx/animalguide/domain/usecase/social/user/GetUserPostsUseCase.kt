package com.permissionx.animalguide.domain.usecase.social.user

import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.domain.model.social.Post
import javax.inject.Inject

class GetUserPostsUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        uid: String,
        pageNumber: Int = 1
    ): Result<Pair<List<Post>, Boolean>> =
        postRepository.getUserPosts(uid, pageNumber)
}