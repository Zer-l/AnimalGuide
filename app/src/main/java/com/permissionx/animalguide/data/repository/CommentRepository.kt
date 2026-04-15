package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.local.CachedCommentDao
import com.permissionx.animalguide.data.local.entity.toCacheEntity
import com.permissionx.animalguide.data.remote.cloudbase.CommentDataSource
import com.permissionx.animalguide.data.remote.cloudbase.LikeDataSource
import com.permissionx.animalguide.data.remote.cloudbase.PostDataSource
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.domain.model.social.Comment
import com.permissionx.animalguide.domain.model.social.CommentStatus
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class CommentRepository @Inject constructor(
    private val commentDataSource: CommentDataSource,
    private val likeDataSource: LikeDataSource,
    private val postDataSource: PostDataSource,
    private val userSessionManager: UserSessionManager,
    private val cachedCommentDao: CachedCommentDao
) {
    // 防止并发操作评论计数
    private val commentCountMutex = kotlinx.coroutines.sync.Mutex()

    // 获取评论列表
    suspend fun getComments(
        postId: String,
        pageNumber: Int = 1
    ): Result<Pair<List<Comment>, Boolean>> {
        val result = commentDataSource.getComments(postId, pageNumber = pageNumber)
        return result.fold(
            onSuccess = { (records, hasMore) ->
                val uid = userSessionManager.currentUser.value?.uid
                val comments = records.map { it.toComment() }.map { comment ->
                    val isLiked = if (uid != null) {
                        likeDataSource.isLiked(uid, comment.id, "COMMENT").getOrNull() ?: false
                    } else false
                    comment.copy(isLiked = isLiked)
                }
                // 首页成功后更新本地缓存
                if (pageNumber == 1) cacheComments(postId, comments)
                Result.success(Pair(comments, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun getCachedComments(postId: String): List<Comment> {
        return cachedCommentDao.getCommentsByPostId(postId).map { it.toComment() }
    }

    private suspend fun cacheComments(postId: String, comments: List<Comment>) {
        cachedCommentDao.deleteByPostId(postId)
        cachedCommentDao.insertAll(comments.mapIndexed { index, comment -> comment.toCacheEntity(index) })
    }

    // 获取回复列表
    suspend fun getReplies(parentId: String): Result<List<Comment>> {
        val result = commentDataSource.getReplies(parentId)
        return result.fold(
            onSuccess = { records ->
                Result.success(records.map { it.toComment() })
            },
            onFailure = { Result.failure(it) }
        )
    }

    // 发表评论
    suspend fun createComment(
        postId: String,
        content: String,
        parentId: String? = null,
        replyToUid: String? = null,
        replyToNickname: String? = null
    ): Result<Comment> {
        val user = userSessionManager.currentUser.value
            ?: return Result.failure(Exception("请先登录"))

        val result = commentDataSource.createComment(
            postId = postId,
            uid = user.uid,
            nickname = user.nickname,
            avatarUrl = user.avatarUrl,
            content = content,
            parentId = parentId,
            replyToUid = replyToUid,
            replyToNickname = replyToNickname
        )
        result.onFailure { return Result.failure(it) }

        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            commentCountMutex.withLock {
                val postResult = postDataSource.getPostById(postId)
                val currentCommentCount = postResult.getOrNull()
                    ?.let { (it["commentCount"] as? Double)?.toInt() } ?: 0
                postDataSource.updatePostCount(postId, "commentCount", currentCommentCount + 1)
            }

            if (parentId != null) {
                commentCountMutex.withLock {
                    val repliesResult = commentDataSource.getReplies(parentId)
                    val currentReplyCount = repliesResult.getOrNull()?.size ?: 0
                    commentDataSource.updateCommentReplyCount(parentId, currentReplyCount)
                }
            }
        }

        return Result.success(
            Comment(
                id = result.getOrNull() ?: "",
                postId = postId,
                uid = user.uid,
                nickname = user.nickname,
                avatarUrl = user.avatarUrl,
                content = content,
                parentId = parentId,
                replyToUid = replyToUid,
                replyToNickname = replyToNickname,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    // 删除主评论及其所有子评论
    suspend fun deleteCommentWithReplies(
        commentId: String,
        postId: String,
        replyCount: Int
    ): Result<Boolean> {
        val repliesResult = commentDataSource.getReplies(commentId)
        repliesResult.onSuccess { replies ->
            replies.forEach { reply ->
                val replyId = reply["_id"] as? String ?: return@forEach
                commentDataSource.deleteComment(replyId)
            }
        }

        val result = commentDataSource.deleteComment(commentId)
        result.onSuccess {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                commentCountMutex.withLock {
                    val postResult = postDataSource.getPostById(postId)
                    val currentCount = postResult.getOrNull()
                        ?.let { (it["commentCount"] as? Double)?.toInt() } ?: 0
                    val actualReplyCount = repliesResult.getOrNull()?.size ?: replyCount
                    val newCount = (currentCount - 1 - actualReplyCount).coerceAtLeast(0)
                    postDataSource.updatePostCount(postId, "commentCount", newCount)
                }
            }
        }
        return result
    }

    // 删除单条评论（子评论）
    suspend fun deleteComment(commentId: String, postId: String): Result<Boolean> {
        val result = commentDataSource.deleteComment(commentId)
        result.onSuccess {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                commentCountMutex.withLock {
                    val postResult = postDataSource.getPostById(postId)
                    val currentCount = postResult.getOrNull()
                        ?.let { (it["commentCount"] as? Double)?.toInt() } ?: 1
                    postDataSource.updatePostCount(
                        postId,
                        "commentCount",
                        (currentCount - 1).coerceAtLeast(0)
                    )
                }
            }
        }
        return result
    }

    // 点赞/取消点赞评论
    private val likingCommentIds = mutableSetOf<String>()

    suspend fun toggleLikeComment(comment: Comment): Result<Comment> {
        if (likingCommentIds.contains(comment.id)) return Result.failure(Exception("操作中"))
        likingCommentIds.add(comment.id)

        try {
            val uid = userSessionManager.currentUser.value?.uid
                ?: return Result.failure(Exception("请先登录"))

            return if (comment.isLiked) {
                val result = likeDataSource.unlike(uid, comment.id, "COMMENT")
                result.fold(
                    onSuccess = {
                        val newCount = (comment.likeCount - 1).coerceAtLeast(0)
                        commentDataSource.updateCommentLikeCount(comment.id, newCount)
                        Result.success(comment.copy(isLiked = false, likeCount = newCount))
                    },
                    onFailure = { Result.failure(it) }
                )
            } else {
                val result = likeDataSource.like(uid, comment.id, "COMMENT")
                result.fold(
                    onSuccess = {
                        val newCount = comment.likeCount + 1
                        commentDataSource.updateCommentLikeCount(comment.id, newCount)
                        Result.success(comment.copy(isLiked = true, likeCount = newCount))
                    },
                    onFailure = { Result.failure(it) }
                )
            }
        } finally {
            likingCommentIds.remove(comment.id)
        }
    }

    private fun Map<String, Any>.toComment() = Comment(
        id = this["_id"] as? String ?: "",
        postId = this["postId"] as? String ?: "",
        uid = this["uid"] as? String ?: "",
        nickname = this["nickname"] as? String ?: "",
        avatarUrl = this["avatarUrl"] as? String ?: "",
        content = this["content"] as? String ?: "",
        parentId = this["parentId"] as? String,
        replyToUid = this["replyToUid"] as? String,
        replyToNickname = this["replyToNickname"] as? String,
        likeCount = (this["likeCount"] as? Double)?.toInt() ?: 0,
        replyCount = (this["replyCount"] as? Double)?.toInt() ?: 0,
        status = when (this["status"] as? String) {
            "DELETED" -> CommentStatus.DELETED
            else -> CommentStatus.NORMAL
        },
        createdAt = (this["createdAt"] as? Double)?.toLong() ?: 0L
    )
}