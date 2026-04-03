package com.permissionx.animalguide.ui.social.publish

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.data.repository.PostRepository
import com.permissionx.animalguide.domain.model.social.PostType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class PublishViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val locationHelper: LocationHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<PublishUiState>(PublishUiState.Idle)
    val state = _state.asStateFlow()

    fun publish(
        title: String,
        content: String,
        imageUris: List<Uri>,
        tags: List<String>,
        type: PostType,
        animalName: String = ""
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
            android.util.Log.d("PublishVM", "开始发布，图片数量: ${imageUris.size}")

            // 位置获取加5秒超时，超时则跳过
            val location = withTimeoutOrNull(3000) {
                locationHelper.getCurrentLocation(context)
            }
            android.util.Log.d("PublishVM", "位置获取完成: $location")

            val result = postRepository.createPost(
                title = title,
                content = content,
                imageUris = imageUris,
                tags = tags,
                type = type,
                animalName = animalName,
                location = "",
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            android.util.Log.d(
                "PublishVM",
                "发帖结果: ${result.isSuccess}, 错误: ${result.exceptionOrNull()?.message}"
            )
            android.util.Log.d("PublishVM", "传入图片数量: ${imageUris.size}, 列表: $imageUris")
            result.fold(
                onSuccess = { _state.value = PublishUiState.Success },
                onFailure = {
                    android.util.Log.e("PublishVM", "发帖失败详情: ${it.message}", it)
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
}