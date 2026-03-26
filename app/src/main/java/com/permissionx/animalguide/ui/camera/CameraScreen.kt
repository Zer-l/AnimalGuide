package com.permissionx.animalguide.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.Canvas
import androidx.core.content.ContextCompat
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

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasMediaPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, mediaPermission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    var showCameraGoSettings by remember { mutableStateOf(false) }
    var showMediaGoSettings by remember { mutableStateOf(false) }
    var showLocationGoSettings by remember { mutableStateOf(false) }
    var shouldRequestMedia by remember { mutableStateOf(false) }
    var shouldRequestLocation by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            val activity = context as ComponentActivity
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (!canAsk) showLocationGoSettings = true
        }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasMediaPermission = true
        } else {
            val activity = context as ComponentActivity
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, mediaPermission
            )
            if (!canAsk) showMediaGoSettings = true
        }
        shouldRequestLocation = true
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            hasCameraPermission = true
        } else {
            val activity = context as ComponentActivity
            val canAsk = ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA
            )
            if (!canAsk) showCameraGoSettings = true
        }
        shouldRequestMedia = true
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedUri = copyUriToCache(context, it)
            val encoded = Uri.encode(copiedUri.toString())
            navController.navigate("result/$encoded")
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(shouldRequestMedia) {
        if (shouldRequestMedia) {
            mediaPermissionLauncher.launch(mediaPermission)
            shouldRequestMedia = false
        }
    }

    LaunchedEffect(shouldRequestLocation) {
        if (shouldRequestLocation) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            shouldRequestLocation = false
        }
    }

    // 监听缩放比例
    LaunchedEffect(camera) {
        camera?.cameraInfo?.zoomState?.observeForever { state ->
            zoomRatio = state.zoomRatio
        }
    }

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

    // 永久拒绝位置权限弹窗
    if (showLocationGoSettings) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("位置权限被禁止") },
            text = { Text("如需记录发现位置，请前往设置手动开启位置权限（可选）") },
            confirmButton = {
                TextButton(onClick = {
                    showLocationGoSettings = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationGoSettings = false }) { Text("跳过") }
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
                            camera = cameraProvider.bindToLifecycle(
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
                update = { previewView ->
                    val scaleGestureDetector = android.view.ScaleGestureDetector(
                        previewView.context,
                        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                                val zoomState = camera?.cameraInfo?.zoomState?.value
                                val currentZoom = zoomState?.zoomRatio ?: 1f
                                val minZoom = zoomState?.minZoomRatio ?: 1f
                                val maxZoom = zoomState?.maxZoomRatio ?: 1f
                                var newZoom =
                                    (currentZoom * detector.scaleFactor).coerceIn(minZoom, maxZoom)
                                if (newZoom < 1f) newZoom = 1f
                                camera?.cameraControl?.setZoomRatio(newZoom)
                                return true
                            }
                        }
                    )

                    previewView.setOnTouchListener { _, event ->
                        scaleGestureDetector.onTouchEvent(event)
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)
                            val action = androidx.camera.core.FocusMeteringAction.Builder(point)
                                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                .build()
                            camera?.cameraControl?.startFocusAndMetering(action)
                        }
                        true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 取景框四角装饰
            CornerFrame(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.Center)
            )

            // 缩放比例显示
            if (zoomRatio >= 1f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .clickable {
                            val targetZoom = when {
                                zoomRatio < 1.5f -> 2f  // 当前约1倍，切换到2倍
                                zoomRatio < 2.5f -> 1f  // 当前约2倍，切换到1倍
                                else -> 1f              // 其他倍数，切换到1倍
                            }
                            camera?.cameraControl?.setZoomRatio(targetZoom)
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "${"%.1f".format(zoomRatio)}x",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

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

        // 底部操作栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .navigationBarsPadding()
                .padding(bottom = 32.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 相册按钮
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

                // 拍照按钮
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
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
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(3.dp, Color.White, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White, CircleShape)
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

@Composable
fun CornerFrame(modifier: Modifier = Modifier) {
    val color = Color.White
    val cornerLength = 40f
    val strokeWidth = 4f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawLine(color, Offset(0f, cornerLength), Offset(0f, 0f), strokeWidth)
        drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth)

        drawLine(color, Offset(w - cornerLength, 0f), Offset(w, 0f), strokeWidth)
        drawLine(color, Offset(w, 0f), Offset(w, cornerLength), strokeWidth)

        drawLine(color, Offset(0f, h - cornerLength), Offset(0f, h), strokeWidth)
        drawLine(color, Offset(0f, h), Offset(cornerLength, h), strokeWidth)

        drawLine(color, Offset(w - cornerLength, h), Offset(w, h), strokeWidth)
        drawLine(color, Offset(w, h - cornerLength), Offset(w, h), strokeWidth)

        drawLine(
            color,
            Offset(((w / 2) - cornerLength), h / 2),
            Offset(((w / 2) + cornerLength), h / 2),
            strokeWidth
        )
        drawLine(
            color,
            Offset(w / 2, ((h / 2) - cornerLength)),
            Offset(w / 2, ((h / 2) + cornerLength)),
            strokeWidth
        )
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
        context.filesDir,
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

private fun copyUriToCache(context: Context, uri: Uri): Uri {
    val fileName = "gallery_${System.currentTimeMillis()}.jpg"
    val destFile = File(context.filesDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return Uri.fromFile(destFile)
}