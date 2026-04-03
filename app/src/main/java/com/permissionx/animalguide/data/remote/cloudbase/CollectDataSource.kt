package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class CollectDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val MODEL = "collects"
        private const val ENV_TYPE = "prod"
    }

    suspend fun isCollected(uid: String, postId: String): Result<Boolean> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to 1,
                "pageNumber" to 1,
                "getCount" to true,
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("uid" to mapOf("\$eq" to uid)),
                            mapOf("postId" to mapOf("\$eq" to postId))
                        )
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val total = (data?.get("total") as? Double)?.toInt() ?: 0
                Result.success(total > 0)
            },
            onFailure = { Result.success(false) }
        )
    }

    suspend fun collect(uid: String, postId: String): Result<Boolean> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/create",
            body = mapOf(
                "data" to mapOf(
                    "uid" to uid,
                    "postId" to postId
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("操作失败，请稍后重试")) }
        )
    }

    suspend fun uncollect(uid: String, postId: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("uid" to mapOf("\$eq" to uid)),
                            mapOf("postId" to mapOf("\$eq" to postId))
                        )
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("操作失败，请稍后重试")) }
        )
    }

    suspend fun getUserCollectPostIds(
        uid: String,
        pageSize: Int = 10,
        pageNumber: Int = 1
    ): Result<Pair<List<String>, Boolean>> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to pageSize,
                "pageNumber" to pageNumber,
                "getCount" to true,
                "filter" to mapOf(
                    "where" to mapOf(
                        "uid" to mapOf("\$eq" to uid)
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val records = (data?.get("records") as? List<*>)?.filterIsInstance<Map<*, *>>() ?: emptyList()
                val total = (data?.get("total") as? Double)?.toInt() ?: 0
                val postIds = records.mapNotNull { it["postId"] as? String }
                val hasMore = pageNumber * pageSize < total
                Result.success(Pair(postIds, hasMore))
            },
            onFailure = { Result.failure(it) }
        )
    }
}