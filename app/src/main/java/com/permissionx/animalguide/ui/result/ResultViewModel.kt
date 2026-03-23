package com.permissionx.animalguide.ui.result

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.domain.model.AnimalInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResultUiState {
    object Idle : ResultUiState()
    object RecognizingAnimal : ResultUiState()
    data class RecognizeSuccess(val results: List<Pair<String, Float>>) : ResultUiState()
    object GeneratingInfo : ResultUiState()
    data class InfoSuccess(val animalName: String, val confidence: Float, val info: AnimalInfo) :
        ResultUiState()

    data class Error(val message: String) : ResultUiState()
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: AnimalRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Idle)
    val state = _state.asStateFlow()

    fun recognizeAnimal(uri: Uri) {
        if (_state.value !is ResultUiState.Idle) return
        viewModelScope.launch {
            // 第一步：识别动物
            _state.value = ResultUiState.RecognizingAnimal
            val recognizeResult = repository.recognizeAnimal(uri)
            recognizeResult.onFailure {
                _state.value = ResultUiState.Error(it.message ?: "识别失败")
                return@launch
            }

            val results = recognizeResult.getOrNull() ?: return@launch
            _state.value = ResultUiState.RecognizeSuccess(results)

            // 第二步：用置信度最高的结果生成科普
            val topAnimal = results.first()
            // 过滤非动物结果
            if (topAnimal.first == "非动物") {
                _state.value = ResultUiState.Error("未识别到动物，请换一张更清晰的图片重试")
                return@launch
            }

            _state.value = ResultUiState.GeneratingInfo
            val infoResult = repository.generateAnimalInfo(topAnimal.first)
            _state.value = infoResult.fold(
                onSuccess = { info ->
                    ResultUiState.InfoSuccess(topAnimal.first, topAnimal.second, info)
                },
                onFailure = {
                    ResultUiState.Error(it.message ?: "科普内容生成失败")
                }
            )
        }
    }
}