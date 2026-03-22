package com.permissionx.animalguide.ui.result

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.ui.common.RecognizeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val repository: AnimalRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RecognizeState>(RecognizeState.Idle)
    val state = _state.asStateFlow()

    fun recognizeAnimal(uri: Uri) {
        viewModelScope.launch {
            _state.value = RecognizeState.Loading
            val result = repository.recognizeAnimal(uri)
            _state.value = result.fold(
                onSuccess = {
                    RecognizeState.Success(it)
                },
                onFailure = {
                    RecognizeState.Error(it.message ?: "识别失败")
                }
            )
        }
    }
}