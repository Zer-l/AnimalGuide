package com.permissionx.animalguide.data.local

import androidx.room.*
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalPhotoDao {

    @Query("SELECT * FROM animal_photos WHERE animalName = :animalName ORDER BY takenAt DESC")
    fun getPhotosByAnimalName(animalName: String): Flow<List<AnimalPhoto>>

    @Query("SELECT * FROM animal_photos WHERE animalName = :animalName ORDER BY takenAt DESC")
    suspend fun getPhotosByAnimalNameOnce(animalName: String): List<AnimalPhoto>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhoto(photo: AnimalPhoto)

    @Delete
    suspend fun deletePhoto(photo: AnimalPhoto)

    @Query("DELETE FROM animal_photos WHERE animalName = :animalName")
    suspend fun deleteAllPhotosByAnimalName(animalName: String)

    @Query("SELECT * FROM animal_photos WHERE imageUri = :imageUri LIMIT 1")
    suspend fun getPhotoByUri(imageUri: String): AnimalPhoto?
}