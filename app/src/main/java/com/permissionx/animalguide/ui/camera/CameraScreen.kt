package com.permissionx.animalguide.ui.camera

import android.Manifest
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.OrientationEventListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

@Composable
fun CameraScreen(
    navController: NavController,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasMediaPermission by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var showCameraRationale by remember { mutableStateOf(false) }
    var showMediaRationale by remember { mutableStateOf(false) }
    var showCameraGoSettings by remember { mutableStateOf(false) }
    var showMediaGoSettings by remember { mutableStateOf(false) }
    var shouldRequestMedia by remember { mutableStateOf(false) }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasMediaPermission = true
            showMediaRationale = false
        } else {
            val activity = context as ComponentActivity
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, mediaPermission
            )
            if (canAsk) {
                showMediaRationale = true
            } else {
                showMediaGoSettings = true
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasCameraPermission = true
            showCameraRationale = false
        } else {
            val activity = context as ComponentActivity
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA
            )
            if (canAsk) {
                showCameraRationale = true
            } else {
                showCameraGoSettings = true
            }
        }
        // 相机权限处理完后触发图片权限申请
        shouldRequestMedia = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val encoded = Uri.encode(it.toString())
            navController.navigate("result/$encoded")
        }
    }

    // 启动时只申请相机权限
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 相机权限完成后申请图片权限
    LaunchedEffect(shouldRequestMedia) {
        if (shouldRequestMedia) {
            mediaPermissionLauncher.launch(mediaPermission)
            shouldRequestMedia = false
        }
    }

//    // 第一次拒绝相机权限弹窗
//    if (showCameraRationale) {
//        AlertDialog(
//            onDismissRequest = {},
//            title = { Text("需要相机权限") },
//            text = { Text("拍摄动物照片需要使用相机，请授予相机权限") },
//            confirmButton = {
//                TextButton(onClick = {
//                    showCameraRationale = false
//                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
//                }) { Text("重新授权") }
//            },
//            dismissButton = {
//                TextButton(onClick = {
//                    showCameraRationale = false
//                }) { Text("取消") }
//            }
//        )
//    }

    // 永久拒绝相机权限弹窗
    if (showCameraGoSettings) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("相机权限被禁止") },
            text = { Text("请前往设置 → 应用 → AnimalGuide → 权限，手动开启相机权限") },
            confirmButton = {
                TextButton(onClick = {
                    showCameraGoSettings = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showCameraGoSettings = false }) { Text("取消") }
            }
        )
    }

//    // 第一次拒绝图片权限弹窗
//    if (showMediaRationale) {
//        AlertDialog(
//            onDismissRequest = {},
//            title = { Text("需要相册权限") },
//            text = { Text("从相册选择动物图片需要访问您的图片，请授予相册权限") },
//            confirmButton = {
//                TextButton(onClick = {
//                    showMediaRationale = false
//                    mediaPermissionLauncher.launch(mediaPermission)
//                }) { Text("重新授权") }
//            },
//            dismissButton = {
//                TextButton(onClick = {
//                    showMediaRationale = false
//                }) { Text("取消") }
//            }
//        )
//    }

    // 永久拒绝图片权限弹窗
    if (showMediaGoSettings) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("相册权限被禁止") },
            text = { Text("请前往设置 → 应用 → AnimalGuide → 权限，手动开启存储权限") },
            confirmButton = {
                TextButton(onClick = {
                    showMediaGoSettings = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showMediaGoSettings = false }) { Text("取消") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    val orientationEventListener = object : OrientationEventListener(ctx) {
                        override fun onOrientationChanged(orientation: Int) {
                            if (orientation == ORIENTATION_UNKNOWN) return
                            val rotation = when (orientation) {
                                in 45..134 -> android.view.Surface.ROTATION_270
                                in 135..224 -> android.view.Surface.ROTATION_180
                                in 225..314 -> android.view.Surface.ROTATION_90
                                else -> android.view.Surface.ROTATION_0
                            }
                            imageCapture?.targetRotation = rotation
                        }
                    }
                    orientationEventListener.enable()

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "相机启动失败: ${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("需要相机权限才能拍照", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("授予权限")
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (hasMediaPermission) {
                        galleryLauncher.launch("image/*")
                    } else {
                        mediaPermissionLauncher.launch(mediaPermission)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "从相册选择",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Button(
                onClick = {
                    takePhoto(
                        context = context,
                        imageCapture = imageCapture,
                        executor = ContextCompat.getMainExecutor(context),
                        onSuccess = { uri ->
                            val encoded = Uri.encode(uri.toString())
                            navController.navigate("result/$encoded")
                        },
                        onError = {
                            Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.size(72.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                )
            }

            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: Executor,
    onSuccess: (Uri) -> Unit,
    onError: () -> Unit
) {
    val capture = imageCapture ?: return

    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.CHINA
        ).format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSuccess(Uri.fromFile(photoFile))
            }

            override fun onError(exception: ImageCaptureException) {
                onError()
            }
        }
    )
}