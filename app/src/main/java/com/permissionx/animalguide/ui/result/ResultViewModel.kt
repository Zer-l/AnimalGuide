package com.permissionx.animalguide.ui.result

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.data.repository.AnimalRepository
import com.permissionx.animalguide.domain.achievement.Achievement
import com.permissionx.animalguide.domain.achievement.AchievementManager
import com.permissionx.animalguide.domain.model.AnimalInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.async

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
    private val repository: AnimalRepository,
    private val locationHelper: LocationHelper,
    private val achievementManager: AchievementManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Idle)
    val state = _state.asStateFlow()

    // 手动标注输入框状态
    private val _manualInputVisible = MutableStateFlow(false)
    val manualInputVisible = _manualInputVisible.asStateFlow()

    fun recognizeAnimal(uri: Uri) {
        if (_state.value !is ResultUiState.Idle) return
        viewModelScope.launch {

            // 用 async 并行获取位置
            val locationDeferred = async {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    locationHelper.getCurrentLocation(context)
                } else {
                    null
                }
            }

            _state.value = ResultUiState.RecognizingAnimal
            val recognizeResult = repository.recognizeAnimal(uri)
            recognizeResult.onFailure {
                repository.saveHistory(
                    animalName = "未知",
                    imageUri = uri.toString(),
                    confidence = 0f,
                    isSuccess = false
                )
                _state.value = ResultUiState.Error(it.message ?: "识别失败")
                return@launch
            }

            val results = recognizeResult.getOrNull() ?: return@launch
            val topAnimal = results.first()

            if (topAnimal.first == "非动物") {
                val locationResult = locationDeferred.await()
                repository.saveHistory(
                    animalName = "非动物",
                    imageUri = uri.toString(),
                    confidence = topAnimal.second,
                    isSuccess = false,
                    latitude = locationResult?.latitude,
                    longitude = locationResult?.longitude
                )
                _state.value = ResultUiState.Error("未识别到动物，请重新试试...")
                return@launch
            }

            _state.value = ResultUiState.RecognizeSuccess(results)

            var attempt = 0
            var infoResult: Result<AnimalInfo>? = null
            while (attempt < 2) {
                if (attempt > 0) {
                    _state.value = ResultUiState.GeneratingInfoRetry(attempt)
                } else {
                    _state.value = ResultUiState.GeneratingInfo
                }
                try {
                    infoResult = repository.generateAnimalInfoOnce(topAnimal.first)
                    if (infoResult.isSuccess) break
                    if (infoResult.exceptionOrNull()?.message?.contains("超时") == true) {
                        attempt++
                    } else {
                        break
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    attempt++
                    if (attempt < 2) {
                        kotlinx.coroutines.delay(2000) // 等待2秒再重试
                    } else {
                        infoResult = Result.failure(Exception("请求超时，请检查网络后重试"))
                    }
                } catch (e: Exception) {
                    infoResult = Result.failure(e)
                    break
                }
            }

            val finalResult = infoResult ?: Result.failure(Exception("科普内容生成失败"))
            finalResult.onFailure {
                _state.value = ResultUiState.Error(it.message ?: "科普内容生成失败")
                return@launch
            }

            val info = finalResult.getOrNull() ?: return@launch

            // 等待位置结果
            val locationResult = locationDeferred.await()

            repository.saveHistory(
                animalName = topAnimal.first,
                imageUri = uri.toString(),
                confidence = topAnimal.second,
                isSuccess = true,
                latitude = locationResult?.latitude,
                longitude = locationResult?.longitude
            )

            val existing = repository.getAnimalByName(topAnimal.first)

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

    // 收录进图鉴
    fun saveToPokedex() {
        val s = _state.value as? ResultUiState.InfoSuccess ?: return
        viewModelScope.launch {
            val alreadyExists = repository.saveToPokedex(
                animalName = s.animalName,
                imageUri = s.imageUri.toString(),
                info = s.info,
                latitude = s.latitude,
                longitude = s.longitude,
                isManual = s.isManual
            )

            // 写入照片墙
            repository.addAnimalPhoto(
                animalName = s.animalName,
                imageUri = s.imageUri.toString()
            )

            // 检测成就
            val newAchievements = if (!alreadyExists) {
                val currentCount = repository.getAnimalCountOnce()
                achievementManager.checkAchievements(currentCount)
            } else {
                emptyList()
            }

            _state.value = s.copy(
                isSaved = true,
                isAlreadyExists = alreadyExists,
                newAchievements = newAchievements
            )
        }
    }

    // 手动标注
    fun showManualInput() {
        _manualInputVisible.value = true
    }

    fun hideManualInput() {
        _manualInputVisible.value = false
    }

    fun recognizeManually(uri: Uri, animalName: String) {
        viewModelScope.launch {
            _manualInputVisible.value = false

            val locationDeferred = async {
                locationHelper.getCurrentLocation(context)
            }

            var attempt = 0
            var infoResult: Result<AnimalInfo>? = null
            while (attempt < 2) {
                if (attempt > 0) {
                    _state.value = ResultUiState.GeneratingInfoRetry(attempt)
                } else {
                    _state.value = ResultUiState.GeneratingInfo
                }
                try {
                    infoResult = repository.generateAnimalInfoOnce(animalName)
                    if (infoResult.isSuccess) break
                    if (infoResult.exceptionOrNull()?.message?.contains("超时") == true) {
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
                        infoResult = Result.failure(Exception("请求超时，请检查网络后重试"))
                    }
                } catch (e: Exception) {
                    infoResult = Result.failure(e)
                    break
                }
            }

            val finalResult = infoResult ?: Result.failure(Exception("科普内容生成失败"))
            finalResult.onFailure {
                _state.value = ResultUiState.Error(it.message ?: "科普内容生成失败")
                return@launch
            }
            val info = finalResult.getOrNull() ?: return@launch

            val locationResult = locationDeferred.await()

            repository.saveHistory(
                animalName = animalName,
                imageUri = uri.toString(),
                confidence = 1f,
                isSuccess = true,
                latitude = locationResult?.latitude,
                longitude = locationResult?.longitude
            )

            val existing = repository.getAnimalByName(animalName)
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

    fun regenerateInfo(animalName: String) {
        val s = _state.value as? ResultUiState.InfoSuccess ?: return
        viewModelScope.launch {
            var attempt = 0
            var infoResult: Result<AnimalInfo>? = null
            while (attempt < 2) {
                if (attempt > 0) {
                    _state.value = ResultUiState.GeneratingInfoRetry(attempt)
                } else {
                    _state.value = ResultUiState.GeneratingInfo
                }
                try {
                    infoResult = repository.generateAnimalInfoOnce(animalName)
                    if (infoResult.isSuccess) break
                    if (infoResult.exceptionOrNull()?.message?.contains("超时") == true) {
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
                        infoResult = Result.failure(Exception("请求超时，请检查网络后重试"))
                    }
                } catch (e: Exception) {
                    infoResult = Result.failure(e)
                    break
                }
            }

            val finalResult = infoResult ?: Result.failure(Exception("科普内容生成失败"))
            finalResult.fold(
                onSuccess = { info ->
                    // 用新名称和科普替换，保留原有位置、图片、置信度
                    val existing = repository.getAnimalByName(animalName)
                    _state.value = s.copy(
                        animalName = animalName,
                        info = info,
                        otherResults = emptyList(),
                        isSaved = false,
                        isAlreadyExists = existing != null
                    )
                },
                onFailure = {
                    _state.value = ResultUiState.Error(it.message ?: "科普内容生成失败")
                }
            )
        }
    }
}