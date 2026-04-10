package com.permissionx.animalguide.data.remote.cloudbase

import com.permissionx.animalguide.BuildConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.permissionx.animalguide.domain.error.AppError
import com.permissionx.animalguide.domain.error.toAppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudBaseClient @Inject constructor() {

    private val envId = BuildConfig.CLOUDBASE_ENV_ID
    private val baseUrl = "https://$envId.api.tcloudbasegateway.com"
    private val gson = Gson()

    private var accessToken: String = BuildConfig.CLOUDBASE_ACCESS_TOKEN

    // 新增：持有 UserSessionManager 的引用，由 CloudBaseModule 注入后设置
    private var sessionManager: (() -> UserSessionManager)? = null

    fun setSessionManagerProvider(provider: () -> UserSessionManager) {
        sessionManager = provider
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateAccessToken(newToken: String) {
        accessToken = newToken
    }

    fun getAccessToken(): String = accessToken

    fun clearAccessToken() {
        accessToken = BuildConfig.CLOUDBASE_ACCESS_TOKEN
    }

    /**
     * 不带 Authorization 头的请求，专门用于 token 刷新
     */
    suspend fun <T> requestWithoutAuth(
        method: String,
        path: String,
        body: Any? = null,
        typeToken: TypeToken<T>? = null
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl$path"
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            // 不加 Authorization 头

            val jsonBody = if (body != null) {
                gson.toJson(body).toRequestBody("application/json".toMediaType())
            } else {
                "{}".toRequestBody("application/json".toMediaType())
            }
            requestBuilder.post(jsonBody)

            val response = client.newCall(requestBuilder.build()).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    @Suppress("UNCHECKED_CAST")
                    return@withContext Result.success(true as T)
                }
                val result = if (typeToken != null) {
                    gson.fromJson<T>(responseBody, typeToken.type)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    gson.fromJson(responseBody, Any::class.java) as T
                }
                Result.success(result)
            } else {
                Result.failure(AppError.NetworkError("Token刷新失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }

    suspend fun <T> request(
        method: String,
        path: String,
        body: Any? = null,
        customHeaders: Map<String, String> = emptyMap(),
        typeToken: TypeToken<T>? = null
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val result = executeRequest(method, path, body, customHeaders, typeToken)

            // 401 时尝试刷新 token 并重试
            if (result.isFailure && result.exceptionOrNull() is AppError.AuthExpiredError) {
                val newToken = sessionManager?.invoke()?.refreshAccessToken()
                if (newToken != null) {
                    // 刷新成功，重试原请求
                    return@withContext executeRequest(method, path, body, customHeaders, typeToken)
                }
                // 刷新失败，返回原错误
            }

            result
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }

    /**
     * 实际执行请求的内部方法
     */
    private suspend fun <T> executeRequest(
        method: String,
        path: String,
        body: Any?,
        customHeaders: Map<String, String>,
        typeToken: TypeToken<T>?
    ): Result<T> {
        val url = "$baseUrl$path"
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $accessToken")

        customHeaders.forEach { (key, value) ->
            requestBuilder.header(key, value)
        }

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST", "PUT", "PATCH", "DELETE" -> {
                val jsonBody = if (body != null) {
                    gson.toJson(body).toRequestBody("application/json".toMediaType())
                } else {
                    "{}".toRequestBody("application/json".toMediaType())
                }
                when (method.uppercase()) {
                    "POST" -> requestBuilder.post(jsonBody)
                    "PUT" -> requestBuilder.put(jsonBody)
                    "PATCH" -> requestBuilder.patch(jsonBody)
                    "DELETE" -> requestBuilder.delete(jsonBody)
                }
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                @Suppress("UNCHECKED_CAST")
                return Result.success(true as T)
            }
            val result = if (typeToken != null) {
                gson.fromJson<T>(responseBody, typeToken.type)
            } else {
                @Suppress("UNCHECKED_CAST")
                gson.fromJson(responseBody, Any::class.java) as T
            }
            return Result.success(result)
        } else {
            return when (response.code) {
                401 -> Result.failure(AppError.AuthExpiredError())
                403 -> Result.failure(AppError.NetworkError("没有权限执行此操作"))
                404 -> Result.failure(AppError.NotFoundError())
                else -> Result.failure(AppError.UnknownError("请求失败: ${response.code}"))
            }
        }
    }
}