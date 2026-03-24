package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class RecognizeHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val animalName: String,
    val imageUri: String,
    val confidence: Float,
    val recognizedAt: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null
)