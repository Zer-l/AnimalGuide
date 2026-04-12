package com.permissionx.animalguide.ui.pokedex

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import com.permissionx.animalguide.data.remote.cloudbase.UserSessionManager
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.data.repository.PhotoRepository
import com.permissionx.animalguide.data.repository.SocialNavigationEvent
import com.permissionx.animalguide.domain.error.AppError
import com.permissionx.animalguide.domain.usecase.GenerateAnimalInfoUseCase
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
    private val animalRepository: AnimalRepository,
    private val photoRepository: PhotoRepository,
    private val generateAnimalInfoUseCase: GenerateAnimalInfoUseCase,
    private val userSessionManager: UserSessionManager,
    private val socialNavigationEvent: SocialNavigationEvent,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state = _state.asStateFlow()

    val isLoggedIn: Boolean get() = userSessionManager.isLoggedIn

    fun navigateToLatestFeed() {
        viewModelScope.launch { socialNavigationEvent.emitNavigateToLatest() }
    }

    fun loadAnimal(animalName: String) {
        viewModelScope.launch {
            val animal = animalRepository.getAnimalByName(animalName)
            if (animal == null) {
                _state.value = DetailUiState.Error("未找到该动物")
                return@launch
            }
            _state.value = DetailUiState.Success(animal = animal)

            // 异步获取地址
            if (animal.latitude != null && animal.longitude != null) {
                launch {
                    val address =
                        animalRepository.getAddress(context, animal.latitude, animal.longitude)
                    val s = _state.value as? DetailUiState.Success ?: return@launch
                    _state.value = s.copy(address = address)
                }
            }

            // 单独协程监听照片列表变化
            launch {
                photoRepository.getAnimalPhotos(animalName).collect { photos ->
                    val s = _state.value as? DetailUiState.Success ?: return@collect
                    _state.value = s.copy(photos = photos)
                }
            }
        }
    }

    fun deletePhoto(photo: AnimalPhoto) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            photoRepository.deleteAnimalPhoto(photo, s.animal.imageUri)
            // 只更新封面数据，photos 由 Flow 自动驱动更新
            val updated = animalRepository.getAnimalByName(s.animal.animalName) ?: return@launch
            val current = _state.value as? DetailUiState.Success ?: return@launch
            _state.value = current.copy(animal = updated)
        }
    }

    // 刷新科普内容
    fun refreshAnimalInfo() {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            _state.value = s.copy(isRefreshingInfo = true)

            val result = generateAnimalInfoUseCase(s.animal.animalName)
            result.fold(
                onSuccess = { info ->
                    animalRepository.updateAnimalInfo(s.animal.animalName, info)
                    val updated = animalRepository.getAnimalByName(s.animal.animalName)!!
                    _state.value = s.copy(
                        animal = updated,
                        isRefreshingInfo = false,
                        refreshMessage = "科普内容已更新"
                    )
                },
                onFailure = {
                    _state.value = s.copy(
                        isRefreshingInfo = false,
                        refreshMessage = it.message ?: AppError.UnknownError().message
                    )
                }
            )
        }
    }

    // 保存备注
    fun saveNote(note: String) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            animalRepository.updateNote(s.animal.animalName, note)
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
            animalRepository.deleteAnimalWithPhotos(s.animal)
            onDeleted()
        }
    }

    fun setCoverPhoto(photo: AnimalPhoto) {
        val s = _state.value as? DetailUiState.Success ?: return
        viewModelScope.launch {
            animalRepository.setCoverPhoto(s.animal.animalName, photo.imageUri)
            val updated = animalRepository.getAnimalByName(s.animal.animalName) ?: return@launch
            _state.value = s.copy(animal = updated)
        }
    }
}