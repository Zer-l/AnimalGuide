package com.permissionx.animalguide.data.repository

import android.content.Context
import android.net.Uri
import com.permissionx.animalguide.data.remote.BaiduApi
import com.permissionx.animalguide.data.remote.DoubaoApi
import com.permissionx.animalguide.data.remote.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.permissionx.animalguide.BuildConfig
import com.permissionx.animalguide.data.remote.dto.DoubaoMessage
import com.permissionx.animalguide.data.remote.dto.DoubaoRequest
import com.permissionx.animalguide.domain.model.AnimalInfo

@Singleton
class AnimalRepository @Inject constructor(
    private val baiduApi: BaiduApi,
    private val doubaoApi: DoubaoApi,
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

    suspend fun generateAnimalInfo(animalName: String): Result<AnimalInfo> {
        return try {
            val prompt = """
    请根据动物名称"$animalName"生成详细科普内容，严格以JSON格式返回，不要返回任何其他内容，不要加markdown代码块：
    {"name":"动物中文名","scientificName":"学名","habitat":"详细的栖息地描述，包括地理分布和具体环境","diet":"详细的食性描述，包括主要食物和觅食方式","lifespan":"平均寿命及影响寿命的因素","conservationStatus":"LC/NT/VU/EN/CR之一","description":"300字以内的详细科普介绍，包括外形特征、行为习性、繁殖方式等"}
""".trimIndent()

            val response = doubaoApi.generateAnimalInfo(
                authorization = "Bearer ${BuildConfig.DOUBAO_API_KEY}",
                request = DoubaoRequest(
                    model = BuildConfig.DOUBAO_ENDPOINT_ID,
                    messages = listOf(
                        DoubaoMessage(role = "user", content = prompt)
                    )
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("生成科普内容失败"))

            // 清理多余字符后解析JSON
            val clean = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val gson = com.google.gson.Gson()
            val info = gson.fromJson(clean, AnimalInfo::class.java)
            Result.success(info)
        } catch (e: Exception) {
            val message = when {
                e is java.net.UnknownHostException -> "网络连接失败，请检查网络后重试"
                e is java.net.SocketTimeoutException -> "请求超时，请检查网络后重试"
                else -> "科普内容生成失败：${e.message}"
            }
            Result.failure(Exception(message))
        }
    }
}