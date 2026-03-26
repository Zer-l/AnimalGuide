package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal_photos")
data class AnimalPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val animalName: String,
    val imageUri: String,
    val takenAt: Long = System.currentTimeMillis()
)