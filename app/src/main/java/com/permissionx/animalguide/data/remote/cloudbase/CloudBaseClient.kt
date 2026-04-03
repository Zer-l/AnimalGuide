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

    suspend fun <T> request(
        method: String,
        path: String,
        body: Any? = null,
        customHeaders: Map<String, String> = emptyMap(),
        typeToken: TypeToken<T>? = null
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
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
                val errorBody = response.body?.string()
                android.util.Log.e("CloudBaseClient", "请求失败: ${response.code}")
                android.util.Log.e("CloudBaseClient", "请求路径: $path")
                android.util.Log.e("CloudBaseClient", "错误详情: $errorBody")
                when (response.code) {
                    401 -> Result.failure(AppError.NetworkError("登录已过期，请重新登录"))
                    403 -> Result.failure(AppError.NetworkError("没有权限执行此操作"))
                    404 -> Result.failure(AppError.UserNotFoundError())
                    else -> Result.failure(AppError.UnknownError("操作失败，请稍后重试"))
                }
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(AppError.NetworkError())
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(AppError.TimeoutError())
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }
}