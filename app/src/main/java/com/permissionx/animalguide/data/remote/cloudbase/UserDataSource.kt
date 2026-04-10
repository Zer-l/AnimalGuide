package com.permissionx.animalguide.data.remote.cloudbase

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class UserDataSource @Inject constructor(
    private val client: CloudBaseClient
) {
    companion object {
        private const val MODEL = "users"
        private const val ENV_TYPE = "prod"
    }

    // 防止并发读写计数字段
    private val countMutex = kotlinx.coroutines.sync.Mutex()

    // 根据 uid 查询用户
    suspend fun getUserByUid(uid: String): Result<Map<String, Any>?> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/list",
            body = mapOf(
                "pageSize" to 1,
                "pageNumber" to 1,
                "filter" to mapOf(
                    "where" to mapOf(
                        "_openid" to mapOf("\$eq" to uid)
                    )
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val data = map["data"] as? Map<*, *>
                val records = data?.get("records") as? List<*>
                Result.success(records?.firstOrNull() as? Map<String, Any>)
            },
            onFailure = { Result.failure(Exception("获取用户信息失败，请稍后重试")) }
        )
    }

    // 创建用户资料
    suspend fun createUser(
        uid: String,
        phone: String,
        nickname: String,
        avatarUrl: String,
        bio: String,
        gender: String
    ): Result<String> {
        val result = client.request<Map<String, Any>>(
            method = "POST",
            path = "/v1/model/$ENV_TYPE/$MODEL/create",
            body = mapOf(
                "data" to mapOf(
                    "nickname" to nickname,
                    "avatarUrl" to avatarUrl,
                    "bio" to bio,
                    "phone" to phone,
                    "gender" to gender,
                    "postCount" to 0,
                    "followCount" to 0,
                    "followerCount" to 0,
                    "likeCount" to 0
                )
            ),
            typeToken = object : TypeToken<Map<String, Any>>() {}
        )
        return result.fold(
            onSuccess = { map ->
                val id = (map["data"] as? Map<*, *>)?.get("id") as? String
                if (id != null) Result.success(id)
                else Result.failure(Exception("创建用户失败"))
            },
            onFailure = { Result.failure(Exception("创建用户失败，请稍后重试")) }
        )
    }

    // 更新用户资料
    suspend fun updateUser(
        uid: String,
        nickname: String,
        avatarUrl: String,
        bio: String,
        gender: String
    ): Result<Boolean> {
        val result = client.request<Any>(
            method = "PUT",
            path = "/v1/model/$ENV_TYPE/$MODEL/update",
            body = mapOf(
                "data" to mapOf(
                    "nickname" to nickname,
                    "avatarUrl" to avatarUrl,
                    "bio" to bio,
                    "gender" to gender
                ),
                "filter" to mapOf(
                    "where" to mapOf(
                        "_openid" to mapOf("\$eq" to uid)
                    )
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("保存失败，请稍后重试")) }
        )
    }

    // 更新用户计数字段（postCount、followCount、followerCount、likeCount等）
    suspend fun updateUserCount(
        uid: String,
        field: String,
        increment: Int = 1
    ): Result<Boolean> {
        countMutex.withLock {
            val userResult = getUserByUid(uid)
            return userResult.fold(
                onSuccess = { user ->
                    if (user == null) {
                        Result.failure(Exception("用户不存在"))
                    } else {
                        val currentValue = (user[field] as? Double)?.toInt() ?: 0
                        val newValue = (currentValue + increment).coerceAtLeast(0)

                        val result = client.request<Any>(
                            method = "PUT",
                            path = "/v1/model/$ENV_TYPE/$MODEL/update",
                            body = mapOf(
                                "data" to mapOf(field to newValue),
                                "filter" to mapOf(
                                    "where" to mapOf(
                                        "_openid" to mapOf("\$eq" to uid)
                                    )
                                )
                            )
                        )
                        result.fold(
                            onSuccess = { Result.success(true) },
                            onFailure = { Result.failure(Exception("更新计数失败，请稍后重试")) }
                        )
                    }
                },
                onFailure = { Result.failure(Exception("获取用户信息失败，请稍后重试")) }
            )
        }
    }

    suspend fun updateBackground(uid: String, backgroundUrl: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "PUT",
            path = "/v1/model/$ENV_TYPE/$MODEL/update",
            body = mapOf(
                "data" to mapOf("backgroundUrl" to backgroundUrl),
                "filter" to mapOf(
                    "where" to mapOf("_openid" to mapOf("\$eq" to uid))
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("保存失败，请稍后重试")) }
        )
    }

    suspend fun updateAvatar(uid: String, avatarUrl: String): Result<Boolean> {
        val result = client.request<Any>(
            method = "PUT",
            path = "/v1/model/$ENV_TYPE/$MODEL/update",
            body = mapOf(
                "data" to mapOf("avatarUrl" to avatarUrl),
                "filter" to mapOf(
                    "where" to mapOf("_openid" to mapOf("\$eq" to uid))
                )
            )
        )
        return result.fold(
            onSuccess = { Result.success(true) },
            onFailure = { Result.failure(Exception("保存失败，请稍后重试")) }
        )
    }
}