package com.permissionx.animalguide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.permissionx.animalguide.data.local.entity.ChatConversationEntity

@Dao
interface ChatConversationDao {

    @Query("SELECT * FROM chat_conversations ORDER BY lastMessageAt DESC")
    suspend fun getAll(): List<ChatConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ChatConversationEntity)

    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    suspend fun getById(id: String): ChatConversationEntity?

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    suspend fun delete(id: String)
}
