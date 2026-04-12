package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conversationId: String,
    /** "user" 或 "assistant" */
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
