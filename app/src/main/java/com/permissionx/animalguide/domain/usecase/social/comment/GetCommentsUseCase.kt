package com.permissionx.animalguide.domain.usecase.social.comment

import com.permissionx.animalguide.data.repository.CommentRepository
import com.permissionx.animalguide.domain.model.social.Comment
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(
        postId: String,
        pageNumber: Int = 1
    ): Result<Pair<List<Comment>, Boolean>> =
        commentRepository.getComments(postId, pageNumber)
}