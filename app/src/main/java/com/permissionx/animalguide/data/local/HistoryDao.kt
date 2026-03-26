package com.permissionx.animalguide.data.local

import androidx.room.*
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY recognizedAt DESC")
    fun getAllHistory(): Flow<List<RecognizeHistory>>

    @Insert
    suspend fun insertHistory(history: RecognizeHistory)

    @Delete
    suspend fun deleteHistory(history: RecognizeHistory)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun getHistoryById(id: Int): RecognizeHistory?

    @Query("SELECT * FROM history")
    suspend fun getAllHistoryOnce(): List<RecognizeHistory>

    @Query("SELECT * FROM history WHERE imageUri = :imageUri LIMIT 1")
    suspend fun getHistoryByImageUri(imageUri: String): RecognizeHistory?
}