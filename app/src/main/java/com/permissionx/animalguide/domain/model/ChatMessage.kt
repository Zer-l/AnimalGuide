package com.permissionx.animalguide.domain.model

data class ChatMessage(
    val id: Int = 0,
    val role: String,   // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
