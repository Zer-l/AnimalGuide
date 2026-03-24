package com.permissionx.animalguide.ui.pokedex

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.repository.AnimalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(
        val animal: AnimalEntry,
        val address: String = "",
        val isRefreshingInfo: Boolean = false,
        val isEditingNote: Boolean = false
    ) : DetailUiState()

    data class Error(val message: String) : DetailUiState()
}

@HiltViewModel
class PokedexDetailViewModel @Inject constructor(
    private val repository: AnimalRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state = _state.asStateFlow()

    fun loadAnimal(animalName: String) {
        viewModelScope.launch {
            val animal = repository.getAnimalByName(animalName)
            if (animal == null) {
                _state.value = DetailUiState.Error("未找到该动物")
                return@launch
            }
            _state.value = DetailUiState.Success(animal = animal)

            // 异步获取地址
            if (animal.latitude != null && animal.longitude != null) {
                val address = repository.getAddress(context, animal.latitude, animal.longitude)
                val s = _state.value as? DetailUiState.Success ?: return@launch
                _state.value = s.copy(address = address)
            }
        }
    }

    // 刷新科普内容
    fun refreshAnimalInfo() {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            _state.value = s.copy(isRefreshingInfo = true)
            val result = repository.generateAnimalInfo(s.animal.animalName)
            result.fold(
                onSuccess = { info ->
                    repository.updateAnimalInfo(s.animal.animalName, info)
                    val updated = repository.getAnimalByName(s.animal.animalName)!!
                    _state.value = s.copy(
                        animal = updated,
                        isRefreshingInfo = false
                    )
                },
                onFailure = {
                    _state.value = s.copy(isRefreshingInfo = false)
                }
            )
        }
    }

    // 保存备注
    fun saveNote(note: String) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            repository.updateNote(s.animal.animalName, note)
            _state.value = s.copy(
                animal = s.animal.copy(note = note),
                isEditingNote = false
            )
        }
    }

    fun startEditNote() {
        val s = _state.value as? DetailUiState.Success ?: return
        _state.value = s.copy(isEditingNote = true)
    }

    fun cancelEditNote() {
        val s = _state.value as? DetailUiState.Success ?: return
        _state.value = s.copy(isEditingNote = false)
    }

    fun deleteAnimal(onDeleted: () -> Unit) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            repository.deleteAnimal(s.animal)
            onDeleted()
        }
    }
}