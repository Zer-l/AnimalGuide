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

    // 注销时用：分页收集该用户所有收藏记录（含 postId）
    suspend fun getUserCollectRecords(uid: String): Result<List<Map<String, Any>>> {
        var pageNumber = 1
        val all = mutableListOf<Map<String, Any>>()
        while (true) {
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
            all.addAll(records.filterIsInstance<Map<String, Any>>())
            val total = (data["total"] as? Double)?.toInt() ?: 0
            if (all.size >= total) break
            pageNumber++
        }
        return Result.success(all)
    }

    // 注销时用：按 _id 逐条删除（批量按 uid 删会被安全规则拦截）
    suspend fun deleteCollectById(id: String) {
        client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf("filter" to mapOf("where" to mapOf("_id" to mapOf("\$eq" to id))))
        )
    }

    // 删除某个帖子的所有收藏记录
    suspend fun deleteAllCollects(postId: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "postId" to mapOf("\$eq" to postId)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.success(true) }
        )
    }
}