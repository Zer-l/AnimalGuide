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
import androidx.core.net.toUri
import com.permissionx.animalguide.data.local.AnimalPhotoDao
import com.permissionx.animalguide.data.local.entity.AnimalPhoto

@Singleton
class AnimalRepository @Inject constructor(
    private val baiduApi: BaiduApi,
    private val doubaoApi: DoubaoApi,
    private val animalDao: AnimalDao,
    private val historyDao: HistoryDao,
    private val animalPhotoDao: AnimalPhotoDao,
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
                Result.failure(Exception("未识别到动物，请重新试试..."))
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

        val response = doubaoApi.generateAnimalInfo(
            authorization = "Bearer ${BuildConfig.DOUBAO_API_KEY}",
            request = DoubaoRequest(
                model = BuildConfig.DOUBAO_ENDPOINT_ID,
                messages = listOf(DoubaoMessage(role = "user", content = prompt))
            )
        )

        val content = response.choices?.firstOrNull()?.message?.content
            ?: return Result.failure(Exception("生成科普内容失败，请重试"))

        val clean = content
            .replace("```json", "")
            .replace("```", "")
            .trim()

        if (!clean.startsWith("{")) {
            return Result.failure(Exception("未找到该动物的相关信息，请检查名称是否正确"))
        }

        return try {
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
        } catch (_: java.net.SocketTimeoutException) {
            Result.failure(Exception("请求超时，请检查网络后重试"))
        } catch (e: Exception) {
            val message = when (e) {
                is java.net.UnknownHostException -> "网络连接失败，请检查网络后重试"
                else -> "科普内容生成失败，请重试"
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

    suspend fun getAnimalByName(name: String) = animalDao.getAnimalByName(name)

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

    suspend fun getAnimalCountOnce() = animalDao.getAnimalCountOnce()

    suspend fun getHistoryById(id: Int) = historyDao.getHistoryById(id)

    suspend fun deleteHistory(history: RecognizeHistory) {
        historyDao.deleteHistory(history)
        // 检查图鉴和照片墙是否引用该图片
        val inPokedex = animalDao.getAnimalByImageUri(history.imageUri)
        val inPhotos = animalPhotoDao.getPhotoByUri(history.imageUri)
        if (inPokedex == null && inPhotos == null) {
            deleteImageFile(history.imageUri)
        }
    }

    suspend fun clearAllHistory() {
        val allHistory = historyDao.getAllHistoryOnce()
        historyDao.clearAllHistory()
        allHistory.forEach { history ->
            // 检查图鉴是否引用该图片
            val inPokedex = animalDao.getAnimalByImageUri(history.imageUri)
            // 检查照片墙是否引用该图片
            val inPhotos = animalPhotoDao.getPhotoByUri(history.imageUri)
            // 两处都没有引用才删除文件
            if (inPokedex == null && inPhotos == null) {
                deleteImageFile(history.imageUri)
            }
        }
    }

    private fun deleteImageFile(imageUri: String) {
        try {
            val uri = imageUri.toUri()
            val path = uri.path ?: return
            val file = java.io.File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // 删除失败不影响主流程
        }
    }

    // 获取动物照片列表
    fun getAnimalPhotos(animalName: String) = animalPhotoDao.getPhotosByAnimalName(animalName)

    // 收录时写入照片
    suspend fun addAnimalPhoto(animalName: String, imageUri: String) {
        val existing = animalPhotoDao.getPhotoByUri(imageUri)
        if (existing == null) {
            animalPhotoDao.insertPhoto(AnimalPhoto(animalName = animalName, imageUri = imageUri))
        }
    }

    // 删除单张照片
    suspend fun deleteAnimalPhoto(photo: AnimalPhoto, currentCoverUri: String): String {
        animalPhotoDao.deletePhoto(photo)

        return if (photo.imageUri == currentCoverUri) {
            val remaining = animalPhotoDao.getPhotosByAnimalNameOnce(photo.animalName)
            val newCover = remaining.firstOrNull()?.imageUri ?: currentCoverUri
            val animal = animalDao.getAnimalByName(photo.animalName)
            if (animal != null && newCover != currentCoverUri) {
                animalDao.updateAnimal(animal.copy(imageUri = newCover))
            }
            // 检查历史记录是否引用该图片，没有引用才删文件
            val inHistory = historyDao.getHistoryByImageUri(photo.imageUri)
            if (inHistory == null && newCover != photo.imageUri) {
                deleteImageFile(photo.imageUri)
            }
            newCover
        } else {
            // 检查历史记录是否引用该图片，没有引用才删文件
            val inHistory = historyDao.getHistoryByImageUri(photo.imageUri)
            if (inHistory == null) {
                deleteImageFile(photo.imageUri)
            }
            currentCoverUri
        }
    }

    // 删除图鉴时同步删除所有照片
    suspend fun deleteAnimalWithPhotos(animal: AnimalEntry) {
        val photos = animalPhotoDao.getPhotosByAnimalNameOnce(animal.animalName)
        animalPhotoDao.deleteAllPhotosByAnimalName(animal.animalName)
        photos.forEach { photo ->
            val inHistory = historyDao.getHistoryByImageUri(photo.imageUri)
            if (inHistory == null) deleteImageFile(photo.imageUri)
        }
        animalDao.deleteAnimal(animal)
    }

    suspend fun setCoverPhoto(animalName: String, imageUri: String) {
        val animal = animalDao.getAnimalByName(animalName) ?: return
        animalDao.updateAnimal(animal.copy(imageUri = imageUri))
    }
}