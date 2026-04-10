package com.permissionx.animalguide.ui.social.publish

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.local.AnimalDao
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.domain.model.social.PostType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val animalDao: AnimalDao,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _state = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val state = _state.asStateFlow()

    fun publish(
        title: String,
        content: String,
        imageUris: List<Uri>,
        tags: List<String>,
        type: PostType,
        animalName: String = "",
        location: String = "",
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        if (title.isBlank()) {
            _state.value = PublishUiState.Error("请输入标题")
            return
        }
        if (content.isBlank()) {
            _state.value = PublishUiState.Error("请输入内容")
            return
        }

        viewModelScope.launch {
            _state.value = PublishUiState.Publishing

            val result = postRepository.createPost(
                title = title,
                content = content,
                imageUris = imageUris,
                tags = tags,
                type = type,
                animalName = animalName,
                location = location,
                latitude = latitude,
                longitude = longitude
            )
            result.fold(
                onSuccess = { _state.value = PublishUiState.Success },
                onFailure = {
                    _state.value = PublishUiState.Error(
                        it.message ?: "发布失败，请重试"
                    )
                }
            )
        }
    }

    fun resetError() {
        _state.value = PublishUiState.Idle
    }

    suspend fun getAnimalImageUri(animalName: String): Uri? {
        val animal = animalDao.getAnimalByName(animalName) ?: return null
        return try {
            Uri.parse(animal.imageUri)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLocationForPublish(context: android.content.Context): Triple<String?, Double?, Double?> {
        val result = locationHelper.getCurrentLocation(context)
            ?: return Triple(null, null, null)

        val geocoder = android.location.Geocoder(context, java.util.Locale.CHINA)
        val cityName = try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(result.latitude, result.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.adminArea ?: ""
                val district = addr.subLocality ?: ""
                if (city.isNotEmpty() && district.isNotEmpty()) "$city · $district"
                else city.ifEmpty { null }
            } else null
        } catch (_: Exception) {
            null
        }

        val displayName = cityName
            ?: "位置 (${String.format("%.2f", result.latitude)}, ${
                String.format(
                    "%.2f",
                    result.longitude
                )
            })"

        return Triple(displayName, result.latitude, result.longitude)
    }
}