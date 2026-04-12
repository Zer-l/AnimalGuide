package com.permissionx.animalguide.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.AnimalChatRepository
import com.permissionx.animalguide.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnimalChatViewModel @Inject constructor(
    private val repository: AnimalChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // conversationId == animalName，既是对话 ID 也是标题
    private val animalName: String = savedStateHandle["animalName"] ?: ""

    private val _state = MutableStateFlow(AnimalChatUiState(animalName = animalName))
    val state: StateFlow<AnimalChatUiState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = repository.getHistory(animalName)
            _state.value = _state.value.copy(messages = history)
        }
    }

    fun onInputChanged(text: String) {
        _state.value = _state.value.copy(inputText = text, errorMessage = null)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isSending) return

        val optimisticMsg = ChatMessage(role = "user", content = text)
        _state.value = _state.value.copy(
            inputText = "",
            isSending = true,
            errorMessage = null,
            messages = _state.value.messages + optimisticMsg
        )

        viewModelScope.launch {
            val result = repository.sendMessage(
                conversationId = animalName,
                type = AnimalChatRepository.TYPE_ANIMAL,
                title = animalName,
                userText = text,
                systemPrompt = AnimalChatRepository.animalSystemPrompt(animalName)
            )
            result.fold(
                onSuccess = {
                    val updated = repository.getHistory(animalName)
                    _state.value = _state.value.copy(messages = updated, isSending = false)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        messages = _state.value.messages.dropLast(1),
                        inputText = text,
                        isSending = false,
                        errorMessage = e.message ?: "发送失败，请重试"
                    )
                }
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory(animalName)
            _state.value = _state.value.copy(messages = emptyList())
        }
    }
}
