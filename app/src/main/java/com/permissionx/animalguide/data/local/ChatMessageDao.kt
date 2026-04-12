package com.permissionx.animalguide.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.permissionx.animalguide.data.local.entity.ChatMessageEntity

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessages(conversationId: String): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getCount(conversationId: String): Int

    @Insert
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteAll(conversationId: String)

    /** 删除最旧的 [count] 条，用于维持 100 条上限 */
    @Query(
        "DELETE FROM chat_messages WHERE id IN " +
                "(SELECT id FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC LIMIT :count)"
    )
    suspend fun deleteOldest(conversationId: String, count: Int)
}
