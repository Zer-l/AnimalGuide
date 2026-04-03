package com.permissionx.animalguide.domain.model.social

data class Comment(
    val id: String = "",
    val postId: String = "",
    val uid: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val parentId: String? = null,
    val replyToUid: String? = null,
    val replyToNickname: String? = null,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val status: CommentStatus = CommentStatus.NORMAL,
    val createdAt: Long = 0L,
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList()
)

enum class CommentStatus { NORMAL, DELETED }