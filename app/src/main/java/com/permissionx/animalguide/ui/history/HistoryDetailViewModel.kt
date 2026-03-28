package com.permissionx.animalguide.ui.history

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistoryDetailUiState {
    object Loading : HistoryDetailUiState()
    data class Success(
        val history: RecognizeHistory,
        val animal: AnimalEntry?,
        val address: String = ""
    ) : HistoryDetailUiState()

    data class Error(val message: String) : HistoryDetailUiState()
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val animalRepository: AnimalRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryDetailUiState>(HistoryDetailUiState.Loading)
    val state = _state.asStateFlow()

    fun loadDetail(historyId: Int) {
        viewModelScope.launch {
            val history = historyRepository.getHistoryById(historyId)
            if (history == null) {
                _state.value = HistoryDetailUiState.Error("未找到该记录")
                return@launch
            }

            // 查询图鉴数据
            val animal = if (history.isSuccess) {
                animalRepository.getAnimalByName(history.animalName)
            } else null

            _state.value = HistoryDetailUiState.Success(
                history = history,
                animal = animal
            )

            // 异步获取地址
            if (history.latitude != null && history.longitude != null) {
                val address =
                    animalRepository.getAddress(context, history.latitude, history.longitude)
                val s = _state.value as? HistoryDetailUiState.Success ?: return@launch
                _state.value = s.copy(address = address)
            }
        }
    }
}