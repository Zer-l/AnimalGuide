package com.permissionx.animalguide.data.local

import androidx.room.*
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {

    @Query("SELECT * FROM pokedex ORDER BY lastSeenAt DESC")
    fun getAllAnimals(): Flow<List<AnimalEntry>>

    @Query("SELECT * FROM pokedex WHERE animalName = :name LIMIT 1")
    suspend fun getAnimalByName(name: String): AnimalEntry?

    @Query("SELECT COUNT(*) FROM pokedex")
    fun getAnimalCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnimal(animal: AnimalEntry)

    @Update
    suspend fun updateAnimal(animal: AnimalEntry)

    // 收录时：存在则更新图片和次数，不存在则插入
    @Transaction
    suspend fun insertOrUpdate(animal: AnimalEntry) {
        val existing = getAnimalByName(animal.animalName)
        if (existing == null) {
            insertAnimal(animal)
        } else {
            updateAnimal(
                existing.copy(
                    imageUri = animal.imageUri,
                    recognizeCount = existing.recognizeCount + 1,
                    lastSeenAt = animal.lastSeenAt,
                    latitude = animal.latitude ?: existing.latitude,
                    longitude = animal.longitude ?: existing.longitude,
                    // 同步更新科普内容
                    scientificName = animal.scientificName,
                    habitat = animal.habitat,
                    diet = animal.diet,
                    lifespan = animal.lifespan,
                    conservationStatus = animal.conservationStatus,
                    description = animal.description
                )
            )
        }
    }

    @Delete
    suspend fun deleteAnimal(animal: AnimalEntry)

    @Query("UPDATE pokedex SET note = :note WHERE animalName = :name")
    suspend fun updateNote(name: String, note: String)

    @Query("UPDATE pokedex SET lastSeenAt = :time, recognizeCount = recognizeCount + 1 WHERE animalName = :name")
    suspend fun updateLastSeen(name: String, time: Long)

    @Query("SELECT COUNT(*) FROM pokedex")
    suspend fun getAnimalCountOnce(): Int

    @Query("SELECT * FROM pokedex WHERE imageUri = :imageUri LIMIT 1")
    suspend fun getAnimalByImageUri(imageUri: String): AnimalEntry?
}