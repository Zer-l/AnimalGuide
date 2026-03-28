package com.permissionx.animalguide.data.repository

import android.content.Context
import android.net.Uri
import com.permissionx.animalguide.BuildConfig
import com.permissionx.animalguide.data.remote.BaiduApi
import com.permissionx.animalguide.data.remote.DoubaoApi
import com.permissionx.animalguide.data.remote.ImageCompressor
import com.permissionx.animalguide.data.remote.dto.DoubaoMessage
import com.permissionx.animalguide.data.remote.dto.DoubaoRequest
import com.permissionx.animalguide.domain.error.AppError
import com.permissionx.animalguide.domain.error.toAppError
import com.permissionx.animalguide.domain.model.AnimalInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecognizeRepository @Inject constructor(
    private val baiduApi: BaiduApi,
    private val doubaoApi: DoubaoApi,
    @ApplicationContext private val context: Context
) {
    // Token 缓存
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
        tokenExpireTime = now + (response.expiresIn - 3600) * 1000
        return response.accessToken
    }

    // 百度识别
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
                Result.failure(AppError.NotAnimalError())
            } else {
                Result.success(results)
            }
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }

    // 豆包科普生成
    suspend fun generateAnimalInfoOnce(animalName: String): Result<AnimalInfo> {
        val angles = listOf(
            "请重点突出它最鲜为人知的冷知识和独特行为",
            "请重点突出它的生存挑战和与人类的关系",
            "请重点突出它的外形特征和辨识要点",
            "请重点突出它在生态系统中的角色和有趣的生活习性",
            "请重点突出它的繁殖行为和家庭生活"
        )
        val randomAngle = angles.random()

        val prompt = """
            请根据动物名称"$animalName"，以轻松有趣的科普风格生成内容，就像在和朋友聊天一样自然。
            本次描述角度：$randomAngle。
            严格以JSON格式返回，不要返回任何其他内容，不要加markdown代码块。
            conservationStatus字段必须且只能是：LC、NT、VU、EN、CR、DD 之一。
            如果该动物有多个亚种保护级别不同，填写最具代表性的等级。
            JSON格式如下：
            {
              "name": "动物中文名",
              "scientificName": "拉丁学名",
              "habitat": "用一两句话描述栖息地，要有画面感",
              "diet": "用有趣的方式描述食性",
              "lifespan": "寿命描述，可以加对比",
              "conservationStatus": "LC或NT或VU或EN或CR或DD之一",
              "description": "200字左右的介绍，语气轻松自然，避免堆砌数据。"
            }
        """.trimIndent()

        return try {
            val response = doubaoApi.generateAnimalInfo(
                authorization = "Bearer ${BuildConfig.DOUBAO_API_KEY}",
                request = DoubaoRequest(
                    model = BuildConfig.DOUBAO_ENDPOINT_ID,
                    messages = listOf(DoubaoMessage(role = "user", content = prompt))
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(AppError.NotFoundError())

            val clean = content.replace("```json", "").replace("```", "").trim()

            if (!clean.startsWith("{")) {
                return Result.failure(AppError.ParseError())
            }

            try {
                val gson = com.google.gson.Gson()
                val info = gson.fromJson(clean, AnimalInfo::class.java)
                val normalizedStatus = when {
                    info.conservationStatus.uppercase().contains("LC") -> "LC"
                    info.conservationStatus.uppercase().contains("NT") -> "NT"
                    info.conservationStatus.uppercase().contains("VU") -> "VU"
                    info.conservationStatus.uppercase().contains("EN") -> "EN"
                    info.conservationStatus.uppercase().contains("CR") -> "CR"
                    info.conservationStatus.uppercase().contains("DD") -> "DD"
                    else -> "LC"
                }
                Result.success(info.copy(conservationStatus = normalizedStatus))
            } catch (_: Exception) {
                Result.failure(AppError.ParseError())
            }
        } catch (_: java.net.SocketTimeoutException) {
            Result.failure(AppError.TimeoutError())
        } catch (e: Exception) {
            Result.failure(e.toAppError())
        }
    }
}