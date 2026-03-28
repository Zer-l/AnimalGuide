package com.permissionx.animalguide.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class PermissionState(
    val hasCamera: Boolean = false,
    val hasMedia: Boolean = false,
    val hasLocation: Boolean = false,
    val isCameraPermanentlyDenied: Boolean = false,
    val isMediaPermanentlyDenied: Boolean = false,
    val isLocationPermanentlyDenied: Boolean = false
)

@Composable
fun rememberPermissionManager(): PermissionState {
    val context = LocalContext.current

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMedia by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, mediaPermission)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraPermanentlyDenied by remember { mutableStateOf(false) }
    var isMediaPermanentlyDenied by remember { mutableStateOf(false) }
    var isLocationPermanentlyDenied by remember { mutableStateOf(false) }
    var shouldRequestMedia by remember { mutableStateOf(false) }
    var shouldRequestLocation by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasLocation = true
            isLocationPermanentlyDenied = false
        } else {
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                context as ComponentActivity, Manifest.permission.ACCESS_FINE_LOCATION
            )
            isLocationPermanentlyDenied = !canAsk
        }
    }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasMedia = true
            isMediaPermanentlyDenied = false
        } else {
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                context as ComponentActivity, mediaPermission
            )
            isMediaPermanentlyDenied = !canAsk
        }
        shouldRequestLocation = true
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasCamera = true
            isCameraPermanentlyDenied = false
        } else {
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                context as ComponentActivity, Manifest.permission.CAMERA
            )
            isCameraPermanentlyDenied = !canAsk
        }
        shouldRequestMedia = true
    }

    LaunchedEffect(Unit) {
        if (!hasCamera) cameraLauncher.launch(Manifest.permission.CAMERA)
        else if (!hasMedia) mediaLauncher.launch(mediaPermission)
        else if (!hasLocation) locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(shouldRequestMedia) {
        if (shouldRequestMedia) {
            if (!hasMedia) mediaLauncher.launch(mediaPermission)
            else shouldRequestLocation = true
            shouldRequestMedia = false
        }
    }

    LaunchedEffect(shouldRequestLocation) {
        if (shouldRequestLocation) {
            if (!hasLocation) locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            shouldRequestLocation = false
        }
    }

    return PermissionState(
        hasCamera = hasCamera,
        hasMedia = hasMedia,
        hasLocation = hasLocation,
        isCameraPermanentlyDenied = isCameraPermanentlyDenied,
        isMediaPermanentlyDenied = isMediaPermanentlyDenied,
        isLocationPermanentlyDenied = isLocationPermanentlyDenied
    )
}