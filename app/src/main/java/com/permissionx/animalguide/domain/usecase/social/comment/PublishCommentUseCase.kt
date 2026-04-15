package com.permissionx.animalguide.domain.usecase.social.comment

import com.permissionx.animalguide.data.repository.CommentRepository
import com.permissionx.animalguide.domain.model.social.Comment
import javax.inject.Inject

open class PublishCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(
        postId: String,
        content: String,
        parentId: String? = null,
        replyToUid: String? = null,
        replyToNickname: String? = null
    ): Result<Comment> {
        if (content.isBlank()) return Result.failure(Exception("评论内容不能为空"))
        return commentRepository.createComment(
            postId = postId,
            content = content,
            parentId = parentId,
            replyToUid = replyToUid,
            replyToNickname = replyToNickname
        )
    }
}