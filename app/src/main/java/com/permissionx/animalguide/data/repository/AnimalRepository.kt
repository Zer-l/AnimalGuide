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
import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.local.HistoryDao
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.data.remote.dto.DoubaoMessage
import com.permissionx.animalguide.data.remote.dto.DoubaoRequest
import com.permissionx.animalguide.domain.model.AnimalInfo

@Singleton
class AnimalRepository @Inject constructor(
    private val baiduApi: BaiduApi,
    private val doubaoApi: DoubaoApi,
    private val animalDao: AnimalDao,
    private val historyDao: HistoryDao,
    private val locationHelper: LocationHelper,
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
    请根据动物名称"$animalName"生成详细科普内容，严格以JSON格式返回，不要返回任何其他内容，不要加markdown代码块。
    conservationStatus字段必须且只能是以下六个值之一：LC、NT、VU、EN、CR、DD，不能包含任何其他文字。
    如果该动物有多个亚种保护级别不同，请填写最具代表性的亚种的等级。
    {"name":"动物中文名","scientificName":"学名","habitat":"详细的栖息地描述，包括地理分布和具体环境","diet":"详细的食性描述，包括主要食物和觅食方式","lifespan":"平均寿命及影响寿命的因素","conservationStatus":"LC或NT或VU或EN或CR或DD之一","description":"300字以内的详细科普介绍，包括外形特征、行为习性、繁殖方式等"}
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
            // 兜底处理 conservationStatus
            val normalizedStatus = when {
                info.conservationStatus.uppercase().contains("LC") -> "LC"
                info.conservationStatus.uppercase().contains("NT") -> "NT"
                info.conservationStatus.uppercase().contains("VU") -> "VU"
                info.conservationStatus.uppercase().contains("EN") -> "EN"
                info.conservationStatus.uppercase().contains("CR") -> "CR"
                info.conservationStatus.uppercase().contains("DD") -> "DD"
                else -> "LC"
            }
            val normalizedInfo = info.copy(conservationStatus = normalizedStatus)
            Result.success(normalizedInfo)
        } catch (e: Exception) {
            val message = when (e) {
                is java.net.UnknownHostException -> "网络连接失败，请检查网络后重试"
                is java.net.SocketTimeoutException -> "请求超时，请检查网络后重试"
                else -> "科普内容生成失败：${e.message}"
            }
            Result.failure(Exception(message))
        }
    }

    // 保存历史记录
    suspend fun saveHistory(
        animalName: String,
        imageUri: String,
        confidence: Float,
        isSuccess: Boolean,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        historyDao.insertHistory(
            RecognizeHistory(
                animalName = animalName,
                imageUri = imageUri,
                confidence = confidence,
                isSuccess = isSuccess,
                latitude = latitude,
                longitude = longitude
            )
        )
    }

    // 收录进图鉴
    suspend fun saveToPokedex(
        animalName: String,
        imageUri: String,
        info: AnimalInfo,
        latitude: Double? = null,
        longitude: Double? = null,
        isManual: Boolean = false
    ): Boolean {
        val existing = animalDao.getAnimalByName(animalName)
        animalDao.insertOrUpdate(
            AnimalEntry(
                animalName = animalName,
                scientificName = info.scientificName,
                imageUri = imageUri,
                habitat = info.habitat,
                diet = info.diet,
                lifespan = info.lifespan,
                conservationStatus = info.conservationStatus,
                description = info.description,
                lastSeenAt = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                isManual = isManual
            )
        )
        return existing != null
    }

    // 获取图鉴列表
    fun getAllAnimals() = animalDao.getAllAnimals()

    // 获取图鉴数量
    fun getAnimalCount() = animalDao.getAnimalCount()

    // 获取历史记录
    fun getAllHistory() = historyDao.getAllHistory()

    // 删除历史记录
    suspend fun deleteHistory(history: RecognizeHistory) {
        historyDao.deleteHistory(history)
    }

    suspend fun getAnimalByName(name: String) = animalDao.getAnimalByName(name)

    suspend fun clearAllHistory() {
        historyDao.clearAllHistory()
    }

    // 更新科普内容
    suspend fun updateAnimalInfo(animalName: String, info: AnimalInfo) {
        val existing = animalDao.getAnimalByName(animalName) ?: return
        animalDao.updateAnimal(
            existing.copy(
                scientificName = info.scientificName,
                habitat = info.habitat,
                diet = info.diet,
                lifespan = info.lifespan,
                conservationStatus = info.conservationStatus,
                description = info.description
            )
        )
    }

    // 更新备注
    suspend fun updateNote(animalName: String, note: String) {
        animalDao.updateNote(animalName, note)
    }

    // 逆地理编码
    suspend fun getAddress(context: Context, latitude: Double, longitude: Double): String {
        return locationHelper.getAddress(context, latitude, longitude)
    }

    suspend fun deleteAnimal(animal: AnimalEntry) {
        animalDao.deleteAnimal(animal)
    }
}