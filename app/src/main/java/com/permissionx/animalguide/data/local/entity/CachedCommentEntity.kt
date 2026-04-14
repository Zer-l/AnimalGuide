package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.permissionx.animalguide.domain.model.social.Comment
import com.permissionx.animalguide.domain.model.social.CommentStatus

@Entity(tableName = "cached_comments")
data class CachedCommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val uid: String,
    val nickname: String,
    val avatarUrl: String,
    val content: String,
    val parentId: String,
    val replyToUid: String,
    val replyToNickname: String,
    val likeCount: Int,
    val replyCount: Int,
    val status: String,
    val createdAt: Long,
    val position: Int
) {
    fun toComment() = Comment(
        id = id,
        postId = postId,
        uid = uid,
        nickname = nickname,
        avatarUrl = avatarUrl,
        content = content,
        parentId = parentId.ifEmpty { null },
        replyToUid = replyToUid.ifEmpty { null },
        replyToNickname = replyToNickname.ifEmpty { null },
        likeCount = likeCount,
        replyCount = replyCount,
        status = if (status == "DELETED") CommentStatus.DELETED else CommentStatus.NORMAL,
        createdAt = createdAt
    )
}

fun Comment.toCacheEntity(position: Int) = CachedCommentEntity(
    id = id,
    postId = postId,
    uid = uid,
    nickname = nickname,
    avatarUrl = avatarUrl,
    content = content,
    parentId = parentId ?: "",
    replyToUid = replyToUid ?: "",
    replyToNickname = replyToNickname ?: "",
    likeCount = likeCount,
    replyCount = replyCount,
    status = status.name,
    createdAt = createdAt,
    position = position
)
