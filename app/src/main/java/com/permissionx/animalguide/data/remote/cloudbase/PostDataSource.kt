package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class PostDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val MODEL = "posts"
        private const val ENV_TYPE = "prod"
    }

    // 获取帖子列表
    suspend fun getPosts(
        pageSize: Int = 10,
        pageNumber: Int = 1,
        sortByHot: Boolean = false,
        filterUid: String? = null,
        filterTag: String? = null
    ): Result<Pair<List<Map<String, Any>>, Boolean>> {
        val orderBy = if (sortByHot) {
            listOf(mapOf("likeCount" to "desc"))
        } else {
            listOf(mapOf("createdAt" to "desc"))
        }

        val whereConditions = mutableListOf<Map<String, Any>>(
            mapOf("status" to mapOf("\$eq" to "NORMAL"))
        )
        filterUid?.let {
            whereConditions.add(mapOf("uid" to mapOf("\$eq" to it)))
        }
        filterTag?.let {
            whereConditions.add(mapOf("tags" to mapOf("\$in" to listOf(it))))
        }

        val filter = if (whereConditions.size == 1) {
            mapOf("where" to whereConditions.first())
        } else {
            mapOf("where" to mapOf("\$and" to whereConditions))
        }

        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to pageSize,
                "pageNumber" to pageNumber,
                "getCount" to true,
                "orderBy" to orderBy,
                "filter" to filter
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )

        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val records = data?.get("records") as? List<*> ?: emptyList<Any>()
                val total = (data?.get("total") as? Double)?.toInt() ?: 0
                val posts = records.filterIsInstance<Map<String, Any>>()
                val hasMore = pageNumber * pageSize < total
                Result.success(Pair(posts, hasMore))
            },
            onFailure = { Result.failure(Exception("加载失败，请稍后重试")) }
        )
    }

    // 获取帖子详情
    suspend fun getPostById(postId: String): Result<Map<String, Any>?> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to 1,
                "pageNumber" to 1,
                "filter" to mapOf(
                    "where" to mapOf(
                        "_id" to mapOf("\$eq" to postId)
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val records = (map["data"] as? Map<*, *>)
                    ?.get("records") as? List<*>
                Result.success(records?.firstOrNull() as? Map<String, Any>)
            },
            onFailure = { Result.failure(Exception("加载失败，请稍后重试")) }
        )
    }

    // 创建帖子
    suspend fun createPost(data: Map<String, Any>): Result<String> {
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
                else Result.failure(Exception("发帖失败，请重试"))
            },
            onFailure = { Result.failure(Exception("发帖失败，请稍后重试")) }
        )
    }

    // 删除帖子
    suspend fun deletePost(postId: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/delete",
            body = mapOf(
                "filter" to mapOf(
                    "where" to mapOf(
                        "_id" to mapOf("\$eq" to postId)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("删除失败，请稍后重试")) }
        )
    }

    // 更新帖子计数（点赞/评论/收藏）
    suspend fun updatePostCount(
        postId: String,
        field: String,
        newValue: Int
    ): Result<Boolean> {
        val result = client.request<Any>(
            method = "PUT",
            path = "/v1/model/$ENV_TYPE/$MODEL/update",
            body = mapOf(
                "data" to mapOf(field to newValue),
                "filter" to mapOf(
                    "where" to mapOf(
                        "_id" to mapOf("\$eq" to postId)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("操作失败，请稍后重试")) }
        )
    }
}