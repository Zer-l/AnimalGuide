package com.permissionx.animalguide.domain.model

data class ChatConversation(
    val id: String,
    val title: String,
    val type: String,       // "animal" or "general"
    val createdAt: Long,
    val lastMessageAt: Long,
    val previewText: String
)
