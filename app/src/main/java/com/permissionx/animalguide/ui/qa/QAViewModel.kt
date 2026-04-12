package com.permissionx.animalguide.ui.qa

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.ConversationRepository
import com.permissionx.animalguide.domain.model.ChatConversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QAUiState {
    data object Loading : QAUiState()
    data class Success(val conversations: List<ChatConversation>) : QAUiState()
}

@HiltViewModel
class QAViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<QAUiState>(QAUiState.Loading)
    val state: StateFlow<QAUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val list = conversationRepository.getAll()
            _state.value = QAUiState.Success(list)
        }
    }

    fun newConversationId(): String = conversationRepository.generateNewId()

    fun delete(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.delete(conversationId)
            load()
        }
    }
}
