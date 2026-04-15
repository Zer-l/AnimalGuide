package com.permissionx.animalguide.domain.usecase.social.post

import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.domain.model.social.Post
import javax.inject.Inject

open class GetFeedUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(
        pageSize: Int = 10,
        pageNumber: Int = 1,
        sortByHot: Boolean = false
    ): Result<Pair<List<Post>, Boolean>> =
        postRepository.getPosts(pageSize, pageNumber, sortByHot)
}