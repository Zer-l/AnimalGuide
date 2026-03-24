package com.permissionx.animalguide.ui.camera

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.permissionx.animalguide.data.location.LocationHelper
import com.permissionx.animalguide.data.location.LocationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _currentLocation = MutableStateFlow<LocationResult?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    fun fetchLocation(context: Context) {
        viewModelScope.launch {
            _currentLocation.value = locationHelper.getCurrentLocation(context)
        }
    }
}