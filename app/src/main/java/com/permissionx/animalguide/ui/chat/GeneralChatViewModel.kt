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
class GeneralChatViewModel @Inject constructor(
    private val repository: AnimalChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = savedStateHandle["conversationId"] ?: ""

    // 首条用户消息确定后锁定，避免后续发消息时覆盖
    private var conversationTitle: String? = null

    private val _state = MutableStateFlow(AnimalChatUiState(animalName = "动物百科问答"))
    val state: StateFlow<AnimalChatUiState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val history = repository.getHistory(conversationId)
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

        // 首条用户消息确定标题，之后不再变更
        if (conversationTitle == null) conversationTitle = text.take(15)
        val title = conversationTitle!!

        _state.value = _state.value.copy(
            inputText = "",
            isSending = true,
            errorMessage = null,
            messages = _state.value.messages + optimisticMsg
        )

        viewModelScope.launch {
            val result = repository.sendMessage(
                conversationId = conversationId,
                type = AnimalChatRepository.TYPE_GENERAL,
                title = title,
                userText = text,
                systemPrompt = AnimalChatRepository.GENERAL_SYSTEM_PROMPT
            )
            result.fold(
                onSuccess = {
                    val updated = repository.getHistory(conversationId)
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
            repository.clearHistory(conversationId)
            _state.value = _state.value.copy(messages = emptyList())
        }
    }
}
