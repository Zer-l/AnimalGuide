package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokedex")
data class AnimalEntry(
    @PrimaryKey
    val animalName: String,
    val scientificName: String,
    val imageUri: String,
    val habitat: String,
    val diet: String,
    val lifespan: String,
    val conservationStatus: String,
    val description: String,
    val unlockedAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val recognizeCount: Int = 1,
    val isManual: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val note: String = "",
    val taxonomy: String = "",
    val distribution: String = "",
    val morphology: String = "",
    val activityPattern: String = "",
    val socialBehavior: String = "",
    val ecologicalRole: String = "",
    val funFacts: String = ""
)