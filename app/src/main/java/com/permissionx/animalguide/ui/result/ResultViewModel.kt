package com.permissionx.animalguide.ui.result

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.data.repository.HistoryRepository
import com.permissionx.animalguide.domain.achievement.Achievement
import com.permissionx.animalguide.domain.error.AppError
import com.permissionx.animalguide.domain.model.AnimalInfo
import com.permissionx.animalguide.domain.usecase.CheckAchievementUseCase
import com.permissionx.animalguide.domain.usecase.GenerateAnimalInfoUseCase
import com.permissionx.animalguide.domain.usecase.RecognizeAnimalUseCase
import com.permissionx.animalguide.domain.usecase.SaveToPokedexUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

sealed class ResultUiState {
    object Idle : ResultUiState()
    object RecognizingAnimal : ResultUiState()
    data class RecognizeSuccess(val results: List<Pair<String, Float>>) : ResultUiState()
    object GeneratingInfo : ResultUiState()

    data class GeneratingInfoRetry(val attempt: Int) : ResultUiState()  // 新增

    data class InfoSuccess(
        val animalName: String,
        val confidence: Float,
        val info: AnimalInfo,
        val otherResults: List<Pair<String, Float>> = emptyList(),
        val imageUri: Uri,
        val isSaved: Boolean = false,
        val isAlreadyExists: Boolean = false,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val isManual: Boolean = false,
        val newAchievements: List<Achievement> = emptyList()  // 新增
    ) : ResultUiState()

    data class Error(val message: String) : ResultUiState()
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val recognizeAnimalUseCase: RecognizeAnimalUseCase,
    private val generateAnimalInfoUseCase: GenerateAnimalInfoUseCase,
    private val saveToPokedexUseCase: SaveToPokedexUseCase,
    private val checkAchievementUseCase: CheckAchievementUseCase,
    private val historyRepository: HistoryRepository,
    private val animalRepository: AnimalRepository,
    private val locationHelper: LocationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Idle)
    val state = _state.asStateFlow()

    private val _manualInputVisible = MutableStateFlow(false)
    val manualInputVisible = _manualInputVisible.asStateFlow()

    fun recognizeAnimal(uri: Uri) {
        if (_state.value !is ResultUiState.Idle) return
        viewModelScope.launch {
            // 并行获取位置
            val locationDeferred = async {
                withTimeoutOrNull(3000) {
                    locationHelper.getCurrentLocation(context)
                }
            }

            // 第一步：识别
            _state.value = ResultUiState.RecognizingAnimal
            val recognizeResult = recognizeAnimalUseCase(uri)
            recognizeResult.onFailure {
                val locationResult = locationDeferred.await()
                historyRepository.saveHistory(
                    animalName = "未知",
                    imageUri = uri.toString(),
                    confidence = 0f,
                    isSuccess = false,
                    latitude = locationResult?.latitude,
                    longitude = locationResult?.longitude
                )
                _state.value = ResultUiState.Error(
                    it.message ?: AppError.UnknownError().message
                )
                return@launch
            }
            val results = recognizeResult.getOrNull() ?: return@launch
            val topAnimal = results.first()

            // 检查非动物
            if (topAnimal.first == "非动物") {
                val locationResult = locationDeferred.await()
                historyRepository.saveHistory(
                    animalName = "非动物",
                    imageUri = uri.toString(),
                    confidence = topAnimal.second,
                    isSuccess = false,
                    latitude = locationResult?.latitude,
                    longitude = locationResult?.longitude
                )
                _state.value = ResultUiState.Error("未识别到动物，请换一张更清晰的图片重试")
                return@launch
            }

            _state.value = ResultUiState.RecognizeSuccess(results)

            // 第二步：生成科普
            _state.value = ResultUiState.GeneratingInfo
            val infoResult = generateAnimalInfoUseCase(topAnimal.first) { attempt ->
                _state.value = ResultUiState.GeneratingInfoRetry(attempt)
            }
            infoResult.onFailure {
                _state.value = ResultUiState.Error(it.message ?: AppError.UnknownError().message)
                return@launch
            }

            val info = infoResult.getOrNull() ?: return@launch
            val locationResult = locationDeferred.await()

            // 保存历史
            historyRepository.saveHistory(
                animalName = topAnimal.first,
                imageUri = uri.toString(),
                confidence = topAnimal.second,
                isSuccess = true,
                latitude = locationResult?.latitude,
                longitude = locationResult?.longitude
            )

            val existing = animalRepository.getAnimalByName(topAnimal.first)

            _state.value = ResultUiState.InfoSuccess(
                animalName = topAnimal.first,
                confidence = topAnimal.second,
                info = info,
                otherResults = results.drop(1),
                imageUri = uri,
                isAlreadyExists = existing != null,
                latitude = locationResult?.latitude,
                longitude = locationResult?.longitude
            )
        }
    }

    fun saveToPokedex() {
        val s = _state.value as? ResultUiState.InfoSuccess ?: return
        viewModelScope.launch {
            val result = saveToPokedexUseCase(
                animalName = s.animalName,
                imageUri = s.imageUri.toString(),
                info = s.info,
                latitude = s.latitude,
                longitude = s.longitude,
                isManual = s.isManual
            )

            // 只有新增才检测成就
            val newAchievements = if (!result.isUpdate) {
                checkAchievementUseCase()
            } else {
                emptyList()
            }

            _state.value = s.copy(
                isSaved = true,
                isAlreadyExists = result.isUpdate,
                newAchievements = newAchievements
            )
        }
    }

    fun recognizeManually(uri: Uri, animalName: String) {
        viewModelScope.launch {
            _manualInputVisible.value = false

            val locationDeferred = async {
                withTimeoutOrNull(3000) {
                    locationHelper.getCurrentLocation(context)
                }
            }

            _state.value = ResultUiState.GeneratingInfo
            val infoResult = generateAnimalInfoUseCase(animalName) { attempt ->
                _state.value = ResultUiState.GeneratingInfoRetry(attempt)
            }
            infoResult.onFailure {
                _state.value = ResultUiState.Error(it.message ?: AppError.UnknownError().message)
                return@launch
            }

            val info = infoResult.getOrNull() ?: return@launch
            val locationResult = locationDeferred.await()

            historyRepository.saveHistory(
                animalName = animalName,
                imageUri = uri.toString(),
                confidence = 1f,
                isSuccess = true,
                latitude = locationResult?.latitude,
                longitude = locationResult?.longitude
            )

            val existing = animalRepository.getAnimalByName(animalName)
            _state.value = ResultUiState.InfoSuccess(
                animalName = animalName,
                confidence = 1f,
                info = info,
                otherResults = emptyList(),
                imageUri = uri,
                isAlreadyExists = existing != null,
                latitude = locationResult?.latitude,
                longitude = locationResult?.longitude,
                isManual = true
            )
        }
    }

    fun retry(uri: Uri) {
        _state.value = ResultUiState.Idle
        recognizeAnimal(uri)
    }

    fun showManualInput() {
        _manualInputVisible.value = true
    }

    fun hideManualInput() {
        _manualInputVisible.value = false
    }

    fun regenerateInfo(animalName: String) {
        val s = _state.value as? ResultUiState.InfoSuccess ?: return
        viewModelScope.launch {
            _state.value = ResultUiState.GeneratingInfo
            val infoResult = generateAnimalInfoUseCase(animalName) { attempt ->
                _state.value = ResultUiState.GeneratingInfoRetry(attempt)
            }
            infoResult.fold(
                onSuccess = { info ->
                    _state.value = s.copy(info = info)
                },
                onFailure = {
                    _state.value =
                        ResultUiState.Error(it.message ?: AppError.UnknownError().message)
                }
            )
        }
    }
}