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
                "orderBy" to listOf(mapOf("createdAt" to "asc")),
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
}