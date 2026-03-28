package com.permissionx.animalguide.data.repository

import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.local.AnimalPhotoDao
import com.permissionx.animalguide.data.local.HistoryDao
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class PhotoRepository @Inject constructor(
    private val animalPhotoDao: AnimalPhotoDao,
    private val animalDao: AnimalDao,
    private val historyDao: HistoryDao
) {
    fun getAnimalPhotos(animalName: String): Flow<List<AnimalPhoto>> =
        animalPhotoDao.getPhotosByAnimalName(animalName)

    suspend fun addAnimalPhoto(animalName: String, imageUri: String) {
        val existing = animalPhotoDao.getPhotoByUri(imageUri)
        if (existing == null) {
            animalPhotoDao.insertPhoto(
                AnimalPhoto(animalName = animalName, imageUri = imageUri)
            )
        }
    }

    suspend fun deleteAnimalPhoto(photo: AnimalPhoto, currentCoverUri: String): String {
        animalPhotoDao.deletePhoto(photo)

        return if (photo.imageUri == currentCoverUri) {
            val remaining = animalPhotoDao.getPhotosByAnimalNameOnce(photo.animalName)
            val newCover = remaining.firstOrNull()?.imageUri ?: currentCoverUri
            val animal = animalDao.getAnimalByName(photo.animalName)
            if (animal != null && newCover != currentCoverUri) {
                animalDao.updateAnimal(animal.copy(imageUri = newCover))
            }
            val inHistory = historyDao.getHistoryByImageUri(photo.imageUri)
            if (inHistory == null && newCover != photo.imageUri) {
                deleteImageFile(photo.imageUri)
            }
            newCover
        } else {
            val inHistory = historyDao.getHistoryByImageUri(photo.imageUri)
            if (inHistory == null) deleteImageFile(photo.imageUri)
            currentCoverUri
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