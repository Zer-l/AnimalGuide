package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import javax.inject.Inject

class CommentDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val MODEL = "comments"
        private const val ENV_TYPE = "prod"
    }

    // 获取评论列表
    suspend fun getComments(
        postId: String,
        pageSize: Int = 20,
        pageNumber: Int = 1
    ): Result<Pair<List<Map<String, Any>>, Boolean>> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to pageSize,
                "pageNumber" to pageNumber,
                "getCount" to true,
                "orderBy" to listOf(mapOf("likeCount" to "desc"), mapOf("createdAt" to "asc")),
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("postId" to mapOf("\$eq" to postId)),
                            mapOf("parentId" to mapOf("\$eq" to "")),
                            mapOf("status" to mapOf("\$eq" to "NORMAL"))
                        )
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val records = data?.get("records") as? List<*> ?: emptyList<Any>()
                val total = (data?.get("total") as? Double)?.toInt() ?: 0
                val comments = records.filterIsInstance<Map<String, Any>>()
                val hasMore = pageNumber * pageSize < total
                Result.success(Pair(comments, hasMore))
            },
            onFailure = { Result.failure(Exception("加载评论失败，请稍后重试")) }
        )
    }

    // 获取回复列表
    suspend fun getReplies(
        parentId: String
    ): Result<List<Map<String, Any>>> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to 50,
                "pageNumber" to 1,
                "orderBy" to listOf(mapOf("createdAt" to "asc")),
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("parentId" to mapOf("\$eq" to parentId)),
                            mapOf("status" to mapOf("\$eq" to "NORMAL"))
                        )
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val records = data?.get("records") as? List<*> ?: emptyList<Any>()
                Result.success(records.filterIsInstance<Map<String, Any>>())
            },
            onFailure = { Result.failure(it) }
        )
    }

    // 发表评论
    suspend fun createComment(
        postId: String,
        uid: String,
        nickname: String,
        avatarUrl: String,
        content: String,
        parentId: String? = null,
        replyToUid: String? = null,
        replyToNickname: String? = null
    ): Result<String> {
        val data = mutableMapOf<String, Any>(
            "postId" to postId,
            "uid" to uid,
            "nickname" to nickname,
            "avatarUrl" to avatarUrl,
            "content" to content,
            "parentId" to (parentId ?: ""),  // 主评论写空字符串
            "likeCount" to 0,
            "replyCount" to 0,
            "status" to "NORMAL"
        )
        replyToUid?.let { data["replyToUid"] = it }
        replyToNickname?.let { data["replyToNickname"] = it }

        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/create",
            body = mapOf("data" to data),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val id = (map["data"] as? Map<*, *>)?.get("id") as? String
                if (id != null) Result.success(id)
                else Result.failure(Exception("评论失败，请重试"))
            },
            onFailure = { Result.failure(Exception("评论失败，请稍后重试")) }
        )
    }

    // 删除评论
    suspend fun deleteComment(commentId: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "_id" to mapOf("\$eq" to commentId)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("删除失败，请稍后重试")) }
        )
    }

    // 更新评论点赞数
    suspend fun updateCommentLikeCount(commentId: String, newValue: Int): Result<Boolean> {
        val result = client.request<Any>(
            method = "PUT",
            path = "/v1/model/$ENV_TYPE/$MODEL/update",
            body = mapOf(
                "data" to mapOf("likeCount" to newValue),
                "filter" to mapOf(
                    "where" to mapOf(
                        "_id" to mapOf("\$eq" to commentId)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(it) }
        )
    }

    // 注销时用：统计该用户在各帖子下的评论数（含回复），返回 {postId → count}
    suspend fun getUserCommentPostCounts(uid: String): Result<Map<String, Int>> {
        var pageNumber = 1
        val postCounts = mutableMapOf<String, Int>()
        var total = Int.MAX_VALUE

        while (postCounts.values.sum() < total) {
            val result = client.request<Map<String, Any>>(
                method = "POST",
                path = "/v1/model/$ENV_TYPE/$MODEL/list",
                body = mapOf(
                    "pageSize" to 50,
                    "pageNumber" to pageNumber,
                    "filter" to mapOf("where" to mapOf("uid" to mapOf("\$eq" to uid)))
                ),
                typeToken = object : TypeToken<Map<String, Any>>() {}
            )
            result.onFailure { return Result.failure(it) }
            val data = (result.getOrNull()?.get("data") as? Map<*, *>) ?: break
            val records = (data["records"] as? List<*>) ?: break
            if (records.isEmpty()) break
            total = (data["total"] as? Double)?.toInt() ?: 0
            records.filterIsInstance<Map<String, Any>>().forEach { record ->
                val postId = record["postId"] as? String ?: return@forEach
                postCounts[postId] = (postCounts[postId] ?: 0) + 1
            }
            pageNumber++
        }

        return Result.success(postCounts)
    }

    // 删除该用户的所有评论（注销账号用）
    // 同时对回复类评论（parentId != ""）减少父评论的 replyCount
    suspend fun deleteUserComments(uid: String): Result<Unit> {
        var pageNumber = 1
        val commentIds = mutableListOf<String>()
        val parentIds = mutableListOf<String>()  // 回复评论的父评论 id

        while (true) {
            val result = client.request<Map<String, Any>>(
                method = "POST",
                path = "/v1/model/$ENV_TYPE/$MODEL/list",
                body = mapOf(
                    "pageSize" to 50,
                    "pageNumber" to pageNumber,
                    "filter" to mapOf(
                        "where" to mapOf("uid" to mapOf("\$eq" to uid))
                    )
                ),
                typeToken = object : TypeToken<Map<String, Any>>() {}
            )
            result.onFailure { return Result.failure(it) }

            val data = (result.getOrNull()?.get("data") as? Map<*, *>) ?: break
            val records = (data["records"] as? List<*>) ?: break
            if (records.isEmpty()) break

            records.filterIsInstance<Map<String, Any>>().forEach { record ->
                val id = record["_id"] as? String ?: return@forEach
                commentIds.add(id)
                val parentId = record["parentId"] as? String
                if (!parentId.isNullOrEmpty()) parentIds.add(parentId)
            }

            val total = (data["total"] as? Double)?.toInt() ?: 0
            if (commentIds.size >= total) break
            pageNumber++
        }

        // 先删评论，再精确修正父评论的 replyCount（删后统计实际剩余数量，与 createComment 逻辑一致）
        for (commentId in commentIds) {
            deleteComment(commentId)
        }
        for (parentId in parentIds.distinct()) {
            val remaining = getReplies(parentId).getOrNull()?.size ?: continue
            updateCommentReplyCount(parentId, remaining)
        }

        return Result.success(Unit)
    }

    // 注销时用：先读当前 likeCount 再写 max(0, current-1)，同时返回评论作者 uid
    suspend fun decrementCommentLikeCount(commentId: String): Result<String?> {
        val listResult = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to 1,
                "pageNumber" to 1,
                "filter" to mapOf("where" to mapOf("_id" to mapOf("\$eq" to commentId)))
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        listResult.onFailure { return Result.success(null) }
        val records = ((listResult.getOrNull()?.get("data") as? Map<*, *>)
            ?.get("records") as? List<*>) ?: return Result.success(null)
        val comment = records.firstOrNull() as? Map<*, *> ?: return Result.success(null)
        val authorUid = comment["uid"] as? String
        val current = (comment["likeCount"] as? Double)?.toInt() ?: 0
        updateCommentLikeCount(commentId, maxOf(0, current - 1))
        return Result.success(authorUid)
    }

    // 更新评论回复数
    suspend fun updateCommentReplyCount(commentId: String, newValue: Int): Result<Boolean> {
        val result = client.request<Any>(
            method = "PUT",
            path = "/v1/model/$ENV_TYPE/$MODEL/update",
            body = mapOf(
                "data" to mapOf("replyCount" to newValue),
                "filter" to mapOf(
                    "where" to mapOf(
                        "_id" to mapOf("\$eq" to commentId)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(it) }
        )
    }

    // 注销时用：先读当前 replyCount 再写 max(0, current-1)
    private suspend fun decrementCommentReplyCount(commentId: String) {
        val listResult = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to 1,
                "pageNumber" to 1,
                "filter" to mapOf("where" to mapOf("_id" to mapOf("\$eq" to commentId)))
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        listResult.onFailure { return }
        val records = ((listResult.getOrNull()?.get("data") as? Map<*, *>)
            ?.get("records") as? List<*>) ?: return
        val comment = records.firstOrNull() as? Map<*, *> ?: return
        val current = (comment["replyCount"] as? Double)?.toInt() ?: 0
        updateCommentReplyCount(commentId, maxOf(0, current - 1))
    }
}