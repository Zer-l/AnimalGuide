package com.permissionx.animalguide.ui.result

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.domain.achievement.Achievement
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.permissionx.animalguide.ui.navigation.Routes
import com.permissionx.animalguide.ui.result.components.AchievementDialog
import com.permissionx.animalguide.ui.result.components.AnimalInfoCard
import com.permissionx.animalguide.ui.result.components.AnimalResultHeader
import com.permissionx.animalguide.ui.result.components.RecognizeProgressCard

@Composable
fun ResultScreen(
    imageUri: Uri,
    navController: NavController,
    onFinished: () -> Unit = {},
    viewModel: ResultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showSavedAnimation by remember { mutableStateOf(false) }
    var savedScale by remember { mutableFloatStateOf(0f) }
    var savedAlpha by remember { mutableFloatStateOf(0f) }
    var savedIsUpdate by remember { mutableStateOf(false) }
    var manualAnimalName by remember { mutableStateOf("") }
    val manualInputVisible by viewModel.manualInputVisible.collectAsState()
    val achievementQueue = remember { mutableStateListOf<Achievement>() }

    LaunchedEffect(imageUri) {
        viewModel.recognizeAnimal(imageUri)
    }

    DisposableEffect(Unit) {
        onDispose { onFinished() }
    }

    // 成就弹窗队列
    LaunchedEffect(state) {
        if (state is ResultUiState.InfoSuccess) {
            val s = state as ResultUiState.InfoSuccess
            if (s.newAchievements.isNotEmpty()) {
                achievementQueue.addAll(s.newAchievements)
            }
        }
    }

    // 收录成功动画
    LaunchedEffect(state) {
        if (state is ResultUiState.InfoSuccess) {
            val s = state as ResultUiState.InfoSuccess
            if (s.isSaved && !showSavedAnimation) {
                savedIsUpdate = s.isAlreadyExists
                showSavedAnimation = true
                savedScale = 0f
                savedAlpha = 1f
                animate(0f, 1.3f, animationSpec = tween(400)) { v, _ -> savedScale = v }
                animate(1.3f, 1f, animationSpec = tween(200)) { v, _ -> savedScale = v }
                kotlinx.coroutines.delay(1200)
                animate(1f, 0f, animationSpec = tween(500)) { v, _ -> savedAlpha = v }
                showSavedAnimation = false
            }
        }
    }

    // 手动标注弹窗
    if (manualInputVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.hideManualInput() },
            title = { Text("手动标注动物") },
            text = {
                Column {
                    Text("请输入该动物名称：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualAnimalName,
                        onValueChange = { manualAnimalName = it },
                        placeholder = { Text("例如：哈基米") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (manualAnimalName.isNotBlank()) {
                        viewModel.recognizeManually(imageUri, manualAnimalName.trim())
                        manualAnimalName = ""
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.hideManualInput()
                    manualAnimalName = ""
                }) { Text("取消") }
            }
        )
    }

    // 成就弹窗
    if (achievementQueue.isNotEmpty()) {
        AchievementDialog(
            achievement = achievementQueue.first(),
            onDismiss = { achievementQueue.removeAt(0) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 顶部图片
            AnimalResultHeader(
                imageUri = imageUri,
                state = state,
                navController = navController
            )

            // 内容区域
            when (val s = state) {
                is ResultUiState.Idle,
                is ResultUiState.RecognizingAnimal,
                is ResultUiState.RecognizeSuccess,
                is ResultUiState.GeneratingInfo,
                is ResultUiState.GeneratingInfoRetry -> {
                    val isStep2 = s is ResultUiState.RecognizeSuccess
                            || s is ResultUiState.GeneratingInfo
                            || s is ResultUiState.GeneratingInfoRetry
                    val text = when (s) {
                        is ResultUiState.RecognizingAnimal -> "正在识别动物..."
                        is ResultUiState.RecognizeSuccess,
                        is ResultUiState.GeneratingInfo -> "正在生成科普内容..."

                        is ResultUiState.GeneratingInfoRetry -> "第${s.attempt}次重试中..."
                        else -> "准备中..."
                    }
                    RecognizeProgressCard(isStep2 = isStep2, text = text)
                }

                is ResultUiState.InfoSuccess -> {
                    AnimalInfoCard(
                        confidence = s.confidence,
                        info = s.info,
                        otherResults = s.otherResults,
                        onRetake = {
                            navController.navigate(Routes.CAMERA) {
                                popUpTo(Routes.CAMERA) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onSave = { viewModel.saveToPokedex() },
                        isSaved = s.isSaved,
                        isAlreadyExists = s.isAlreadyExists,
                        onRegenerate = { viewModel.regenerateInfo(s.animalName) }
                    )
                }

                is ResultUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("😢", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = s.message,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    navController.navigate(Routes.CAMERA) {
                                        popUpTo(Routes.CAMERA) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            ) { Text("重新拍摄") }
                            OutlinedButton(onClick = { viewModel.retry(imageUri) }) {
                                Text("重新识别")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.showManualInput() }) {
                            Text(
                                text = "识别结果不满意？试试手动进行标注",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 收录成功动画
        if (showSavedAnimation) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (savedIsUpdate) "🔄" else "✅",
                    fontSize = 120.sp,
                    modifier = Modifier.graphicsLayer(
                        scaleX = savedScale,
                        scaleY = savedScale,
                        alpha = savedAlpha
                    )
                )
            }
        }
    }
}
