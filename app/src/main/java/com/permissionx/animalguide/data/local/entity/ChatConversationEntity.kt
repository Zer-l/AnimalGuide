package com.permissionx.animalguide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_conversations")
data class ChatConversationEntity(
    @PrimaryKey val id: String,           // animalName 或 UUID
    val title: String,                    // 动物名 或 首条用户消息截断
    val type: String,                     // "animal" 或 "general"
    val createdAt: Long,
    val lastMessageAt: Long,
    val previewText: String
)
