package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class LikeDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val MODEL = "likes"
        private const val ENV_TYPE = "prod"
    }

    suspend fun isLiked(
        uid: String,
        targetId: String,
        targetType: String
    ): Result<Boolean> {
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
                            mapOf("targetId" to mapOf("\$eq" to targetId)),
                            mapOf("targetType" to mapOf("\$eq" to targetType))
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

    suspend fun like(
        uid: String,
        targetId: String,
        targetType: String
    ): Result<Boolean> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/create",
            body = mapOf(
                "data" to mapOf(
                    "uid" to uid,
                    "targetId" to targetId,
                    "targetType" to targetType
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("操作失败，请稍后重试")) }
        )
    }

    suspend fun unlike(
        uid: String,
        targetId: String,
        targetType: String
    ): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("uid" to mapOf("\$eq" to uid)),
                            mapOf("targetId" to mapOf("\$eq" to targetId)),
                            mapOf("targetType" to mapOf("\$eq" to targetType))
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

    // 删除某个目标的所有点赞记录
    suspend fun deleteAllLikes(targetId: String, targetType: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "\$and" to listOf(
                            mapOf("targetId" to mapOf("\$eq" to targetId)),
                            mapOf("targetType" to mapOf("\$eq" to targetType))
                        )
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.success(true) }  // 清理失败不影响主流程
        )
    }
}