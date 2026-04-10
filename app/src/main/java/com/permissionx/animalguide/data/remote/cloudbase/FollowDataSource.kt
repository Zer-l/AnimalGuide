package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class FollowDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val MODEL = "follows"
        private const val ENV_TYPE = "prod"
    }

    suspend fun isFollowing(fromUid: String, toUid: String): Result<Boolean> {
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
                            mapOf("fromUid" to mapOf("\$eq" to fromUid)),
                            mapOf("toUid" to mapOf("\$eq" to toUid))
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

    suspend fun follow(fromUid: String, toUid: String): Result<Boolean> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/create",
            body = mapOf(
                "data" to mapOf(
                    "fromUid" to fromUid,
                    "toUid" to toUid
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("操作失败，请稍后重试")) }
        )
    }

    suspend fun unfollow(fromUid: String, toUid: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("fromUid" to mapOf("\$eq" to fromUid)),
                            mapOf("toUid" to mapOf("\$eq" to toUid))
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

    // 获取关注列表（我关注的人）
    suspend fun getFollowingList(
        uid: String,
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
                "orderBy" to listOf(mapOf("createdAt" to "desc")),
                "filter" to mapOf(
                    "where" to mapOf(
                        "fromUid" to mapOf("\$eq" to uid)
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
                val items = records.filterIsInstance<Map<String, Any>>()
                val hasMore = pageNumber * pageSize < total
                Result.success(Pair(items, hasMore))
            },
            onFailure = { Result.failure(Exception("加载失败，请稍后重试")) }
        )
    }

    // 获取粉丝列表（关注我的人）
    suspend fun getFollowerList(
        uid: String,
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
                "orderBy" to listOf(mapOf("createdAt" to "desc")),
                "filter" to mapOf(
                    "where" to mapOf(
                        "toUid" to mapOf("\$eq" to uid)
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
                val items = records.filterIsInstance<Map<String, Any>>()
                val hasMore = pageNumber * pageSize < total
                Result.success(Pair(items, hasMore))
            },
            onFailure = { Result.failure(Exception("加载失败，请稍后重试")) }
        )
    }
}