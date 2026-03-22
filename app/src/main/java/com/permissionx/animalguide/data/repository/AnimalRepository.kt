package com.permissionx.animalguide.data.repository

import android.content.Context
import android.net.Uri
import com.permissionx.animalguide.data.remote.BaiduApi
import com.permissionx.animalguide.data.remote.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.permissionx.animalguide.BuildConfig

@Singleton
class AnimalRepository @Inject constructor(
    private val baiduApi: BaiduApi,
    @ApplicationContext private val context: Context
) {
    // 缓存token，避免频繁获取
    private var cachedToken: String? = null
    private var tokenExpireTime: Long = 0L

    private suspend fun getAccessToken(): String {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpireTime) {
            return cachedToken!!
        }
        val response = baiduApi.getAccessToken(
            apiKey = BuildConfig.BAIDU_API_KEY,
            secretKey = BuildConfig.BAIDU_SECRET_KEY
        )
        cachedToken = response.accessToken
        // 提前1小时过期，单位毫秒
        tokenExpireTime = now + (response.expiresIn - 3600) * 1000
        return response.accessToken
    }

    suspend fun recognizeAnimal(imageUri: Uri): Result<List<Pair<String, Float>>> {
        return try {
            val token = getAccessToken()
            val base64 = ImageCompressor.uriToBase64(context, imageUri)
            val response = baiduApi.recognizeAnimal(
                accessToken = token,
                imageBase64 = base64,
                topNum = 3,
                baikeNum = 1
            )

            val results = response.result
                ?.map { Pair(it.name, it.score.toFloat()) }
                ?: emptyList()

            if (results.isEmpty()) {
                Result.failure(Exception("未识别到动物，请换一张更清晰的图片"))
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            val message = when {
                e is java.net.UnknownHostException -> "网络连接失败，请检查网络后重试"
                e is java.net.SocketTimeoutException -> "请求超时，请检查网络后重试"
                e.message?.contains("Unable to resolve host") == true -> "网络连接失败，请检查网络后重试"
                else -> "识别失败：${e.message}"
            }
            Result.failure(Exception(message))
        }
    }
}