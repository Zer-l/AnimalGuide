package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.local.AnimalPhotoDao
import com.permissionx.animalguide.data.local.HistoryDao
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import com.permissionx.animalguide.domain.error.toAppError

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val animalDao: AnimalDao,
    private val animalPhotoDao: AnimalPhotoDao
) {
    fun getAllHistory(): Flow<List<RecognizeHistory>> = historyDao.getAllHistory()

    suspend fun getHistoryById(id: Int): RecognizeHistory? = historyDao.getHistoryById(id)

    suspend fun saveHistory(
        animalName: String,
        imageUri: String,
        confidence: Float,
        isSuccess: Boolean,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        try {
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
        } catch (e: Exception) {
            // 存储失败不影响主流程，静默处理
            android.util.Log.e("HistoryRepository", "保存历史记录失败: ${e.toAppError().message}")
        }
    }

    suspend fun deleteHistory(history: RecognizeHistory) {
        historyDao.deleteHistory(history)
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
            val inPokedex = animalDao.getAnimalByImageUri(history.imageUri)
            val inPhotos = animalPhotoDao.getPhotoByUri(history.imageUri)
            if (inPokedex == null && inPhotos == null) {
                deleteImageFile(history.imageUri)
            }
        }
    }

    private fun deleteImageFile(imageUri: String) {
        try {
            val uri = imageUri.toUri()
            val path = uri.path ?: return
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
            // 删除失败不影响主流程
        }
    }
}