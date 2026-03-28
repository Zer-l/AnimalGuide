package com.permissionx.animalguide.ui.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    private val _pendingImageUri = MutableStateFlow<Uri?>(null)
    val pendingImageUri = _pendingImageUri.asStateFlow()

    fun setPendingImageUri(uri: Uri) {
        _pendingImageUri.value = uri
    }

    fun clearPendingImageUri() {
        _pendingImageUri.value = null
    }
}