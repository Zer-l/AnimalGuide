package com.permissionx.animalguide.data.repository

import android.content.Context
import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.local.AnimalPhotoDao
import com.permissionx.animalguide.data.local.HistoryDao
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.domain.model.AnimalInfo
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class AnimalRepository @Inject constructor(
    private val animalDao: AnimalDao,
    private val historyDao: HistoryDao,
    private val animalPhotoDao: AnimalPhotoDao,
    private val locationHelper: LocationHelper,
) {
    fun getAllAnimals(): Flow<List<AnimalEntry>> = animalDao.getAllAnimals()

    fun getAnimalCount(): Flow<Int> = animalDao.getAnimalCount()

    suspend fun getAnimalCountOnce(): Int = animalDao.getAnimalCountOnce()

    suspend fun getAnimalByName(name: String): AnimalEntry? = animalDao.getAnimalByName(name)

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

    suspend fun updateNote(animalName: String, note: String) {
        animalDao.updateNote(animalName, note)
    }

    suspend fun setCoverPhoto(animalName: String, imageUri: String) {
        val animal = animalDao.getAnimalByName(animalName) ?: return
        animalDao.updateAnimal(animal.copy(imageUri = imageUri))
    }

    suspend fun deleteAnimalWithPhotos(animal: AnimalEntry) {
        val photos = animalPhotoDao.getPhotosByAnimalNameOnce(animal.animalName)
        animalPhotoDao.deleteAllPhotosByAnimalName(animal.animalName)
        photos.forEach { photo ->
            val inHistory = historyDao.getHistoryByImageUri(photo.imageUri)
            if (inHistory == null) deleteImageFile(photo.imageUri)
        }
        animalDao.deleteAnimal(animal)
    }

    suspend fun getAddress(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = locationHelper.getAddress(context, latitude, longitude)

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