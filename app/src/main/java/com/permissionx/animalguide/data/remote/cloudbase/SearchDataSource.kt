package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class SearchDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val ENV_TYPE = "prod"
    }

    // 搜索帖子：匹配 title
    // 搜索帖子：匹配 title 或 content 或 tags
    suspend fun searchPosts(
        keyword: String,
        pageSize: Int = 10,
        pageNumber: Int = 1
    ): Result<Pair<List<Map<String, Any>>, Boolean>> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/posts/list",
            body = mapOf(
                "pageSize" to pageSize,
                "pageNumber" to pageNumber,
                "orderBy" to listOf(mapOf("createdAt" to "desc")),
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("status" to mapOf("\$eq" to "NORMAL")),
                            mapOf(
                                "\$or" to listOf(
                                    mapOf("title" to mapOf("\$search" to keyword)),
                                    mapOf("content" to mapOf("\$search" to keyword))
                                )
                            )
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
                val posts = records.filterIsInstance<Map<String, Any>>()
                val hasMore = posts.size >= pageSize
                Result.success(Pair(posts, hasMore))
            },
            onFailure = { Result.failure(Exception("搜索失败，请稍后重试")) }
        )
    }

    // 搜索用户：匹配 nickname
    suspend fun searchUsers(
        keyword: String,
        pageSize: Int = 20,
        pageNumber: Int = 1
    ): Result<Pair<List<Map<String, Any>>, Boolean>> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/users/list",
            body = mapOf(
                "pageSize" to pageSize,
                "pageNumber" to pageNumber,
                "filter" to mapOf(
                    "where" to mapOf(
                        "nickname" to mapOf("\$search" to keyword)
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )

        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val records = data?.get("records") as? List<*> ?: emptyList<Any>()
                // ===== 临时日志 =====
                records.filterIsInstance<Map<String, Any>>().forEach {
                    android.util.Log.d("SearchDebug", "用户: nickname=${it["nickname"]}, avatarUrl=${it["avatarUrl"]}, keys=${it.keys}")
                }
                // ====================
                val users = records.filterIsInstance<Map<String, Any>>()
                val hasMore = users.size >= pageSize
                Result.success(Pair(users, hasMore))
            },
            onFailure = { Result.failure(Exception("搜索失败，请稍后重试")) }
        )
    }
}