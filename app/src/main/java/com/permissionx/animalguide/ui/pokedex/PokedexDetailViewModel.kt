package com.permissionx.animalguide.ui.pokedex

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.domain.model.AnimalInfo
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
        val isEditingNote: Boolean = false,
        val refreshMessage: String = "",  // 新增
        val photos: List<AnimalPhoto> = emptyList()  // 新增
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
                launch {
                    val address = repository.getAddress(context, animal.latitude, animal.longitude)
                    val s = _state.value as? DetailUiState.Success ?: return@launch
                    _state.value = s.copy(address = address)
                }
            }

            // 单独协程监听照片列表变化
            launch {
                repository.getAnimalPhotos(animalName).collect { photos ->
                    val s = _state.value as? DetailUiState.Success ?: return@collect
                    _state.value = s.copy(photos = photos)
                }
            }
        }
    }

    fun deletePhoto(photo: AnimalPhoto) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            repository.deleteAnimalPhoto(photo, s.animal.imageUri)
            // 只更新封面数据，photos 由 Flow 自动驱动更新
            val updated = repository.getAnimalByName(s.animal.animalName) ?: return@launch
            val current = _state.value as? DetailUiState.Success ?: return@launch
            _state.value = current.copy(animal = updated)
        }
    }

    // 刷新科普内容
    fun refreshAnimalInfo() {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            _state.value = s.copy(isRefreshingInfo = true)

            var attempt = 0
            var result: Result<AnimalInfo>? = null
            while (attempt < 2) {
                try {
                    result = repository.generateAnimalInfoOnce(s.animal.animalName)
                    if (result.isSuccess) break
                    if (result.exceptionOrNull()?.message?.contains("超时") == true) {
                        attempt++
                        if (attempt < 2) kotlinx.coroutines.delay(2000)
                    } else {
                        break
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    attempt++
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(2000)
                    } else {
                        result = Result.failure(Exception("请求超时，请检查网络后重试"))
                    }
                } catch (e: Exception) {
                    result = Result.failure(e)
                    break
                }
            }

            val finalResult = result ?: Result.failure(Exception("科普内容生成失败"))
            finalResult.fold(
                onSuccess = { info ->
                    repository.updateAnimalInfo(s.animal.animalName, info)
                    val updated = repository.getAnimalByName(s.animal.animalName)!!
                    _state.value = s.copy(
                        animal = updated,
                        isRefreshingInfo = false,
                        refreshMessage = "科普内容已更新"
                    )
                },
                onFailure = {
                    _state.value = s.copy(
                        isRefreshingInfo = false,
                        refreshMessage = it.message ?: "更新失败，请重试"
                    )
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

    // 删除图鉴
    fun deleteAnimal(onDeleted: () -> Unit) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            repository.deleteAnimalWithPhotos(s.animal)
            onDeleted()
        }
    }

    fun setCoverPhoto(photo: AnimalPhoto) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            repository.setCoverPhoto(s.animal.animalName, photo.imageUri)
            val updated = repository.getAnimalByName(s.animal.animalName) ?: return@launch
            _state.value = s.copy(animal = updated)
        }
    }
}