package com.permissionx.animalguide.ui.chat

import com.permissionx.animalguide.domain.model.ChatMessage

data class AnimalChatUiState(
    val animalName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
