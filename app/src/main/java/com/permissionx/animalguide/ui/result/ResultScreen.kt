package com.permissionx.animalguide.ui.result

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.domain.model.AnimalInfo
import androidx.core.net.toUri
import com.permissionx.animalguide.domain.achievement.Achievement

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun ResultScreen(
    imageUri: String,
    navController: NavController,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uri = remember { imageUri.toUri() }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uri) {
        viewModel.recognizeAnimal(uri)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val manualInputVisible by viewModel.manualInputVisible.collectAsState()
        var manualAnimalName by remember { mutableStateOf("") }

        if (manualInputVisible) {
            AlertDialog(
                onDismissRequest = { viewModel.hideManualInput() },
                title = { Text("手动标注动物") },
                text = {
                    Column {
                        Text("请输入你认识的动物名称：", fontSize = 14.sp)
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
                    TextButton(
                        onClick = {
                            if (manualAnimalName.isNotBlank()) {
                                viewModel.recognizeManually(uri, manualAnimalName.trim())
                                manualAnimalName = ""
                            }
                        }
                    ) { Text("确认") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.hideManualInput()
                        manualAnimalName = ""
                    }) { Text("取消") }
                }
            )
        }

        val achievementQueue = remember { mutableStateListOf<Achievement>() }

        // 监听新成就
        LaunchedEffect(state) {
            if (state is ResultUiState.InfoSuccess) {
                val newAchievements = (state as ResultUiState.InfoSuccess).newAchievements
                if (newAchievements.isNotEmpty()) {
                    achievementQueue.addAll(newAchievements)
                }
            }
        }

        // 逐个显示成就弹窗
        if (achievementQueue.isNotEmpty()) {
            AchievementDialog(
                achievement = achievementQueue.first(),
                onDismiss = { achievementQueue.removeFirst() }
            )
        }

        // 顶部图片区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 顶部渐变遮罩
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )

            // 返回按钮
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            // 成功状态在图片底部显示动物名称
            if (state is ResultUiState.InfoSuccess) {
                val s = state as ResultUiState.InfoSuccess
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = s.info.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = s.info.scientificName,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 内容区域
        when (val s = state) {
            is ResultUiState.Idle,
            is ResultUiState.RecognizingAnimal,
            is ResultUiState.RecognizeSuccess,
            is ResultUiState.GeneratingInfo -> {
                val (step, total, text) = when (s) {
                    is ResultUiState.RecognizingAnimal -> Triple(1, 2, "正在识别动物...")
                    is ResultUiState.RecognizeSuccess,
                    is ResultUiState.GeneratingInfo -> Triple(2, 2, "正在生成科普内容...")

                    else -> Triple(0, 2, "准备中...")
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { step / 2f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (step > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "步骤 $step / $total",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            is ResultUiState.InfoSuccess -> {
                AnimalInfoCard(
                    confidence = s.confidence,
                    info = s.info,
                    otherResults = s.otherResults,
                    onRetake = { navController.popBackStack() },
                    onSave = { viewModel.saveToPokedex() },
                    isSaved = s.isSaved,
                    isAlreadyExists = s.isAlreadyExists
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
                        OutlinedButton(onClick = { navController.popBackStack() }) {
                            Text("重新拍摄")
                        }
                        OutlinedButton(onClick = { viewModel.retry(uri) }) {
                            Text("重新识别")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.showManualInput() }) {
                        Text("识别结果不满意？试试手动标注")
                    }
                }
            }
        }
    }
}

@Composable
fun AnimalInfoCard(
    confidence: Float,
    info: AnimalInfo,
    otherResults: List<Pair<String, Float>>,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean,
    isAlreadyExists: Boolean
) {
    var showOthers by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {

        // 置信度 + 濒危等级
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ConservationBadge(status = info.conservationStatus)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "置信度 ${"%.1f".format(confidence * 100)}%",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 基本信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRowIfValid(label = "🏕 栖息地", value = info.habitat)
                InfoRowIfValid(label = "🍖 食　性", value = info.diet)
                InfoRowIfValid(label = "⏳ 寿　命", value = info.lifespan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 科普介绍
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📖 简介",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\t\t\t\t" + info.description,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )
            }
        }

        // 其他候选结果
        if (otherResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOthers = !showOthers },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔍 其他可能",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showOthers) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AnimatedVisibility(visible = showOthers) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            otherResults.forEachIndexed { index, (name, score) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 2}. $name",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${"%.1f".format(score * 100)}%",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (index < otherResults.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 收录图鉴按钮
        when {
            isSaved && isAlreadyExists -> {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false
                ) {
                    Text("✅ 已更新图鉴记录")
                }
            }

            isSaved -> {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = false
                ) {
                    Text("✅ 已收录进图鉴")
                }
            }

            isAlreadyExists -> {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔄 更新图鉴记录")
                }
            }

            else -> {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📖 收录进图鉴")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 再拍一张
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📷 再拍一张")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InfoRowIfValid(label: String, value: String) {
    // 过滤无效值
    val invalid = setOf("n/a", "无", "暂无", "无数据", "-", "")
    if (value.lowercase().trim() in invalid) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(88.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
fun ConservationBadge(status: String) {
    // 只取前两个字符匹配标准等级
    val normalized = status.uppercase().take(2)
    val (label, color) = when (normalized) {
        "LC" -> "无危" to Color(0xFF4CAF50)
        "NT" -> "近危" to Color(0xFF2196F3)
        "VU" -> "易危" to Color(0xFFF9A825)
        "EN" -> "濒危" to Color(0xFFFF9800)
        "CR" -> "极危" to Color(0xFFF44336)
        "DD" -> "数据不足" to Color(0xFF9E9E9E)
        else -> "未知" to Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = "${normalized.take(2)} · $label",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AchievementDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 40.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "成就解锁！",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = achievement.icon,
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = achievement.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("太棒了！")
                }
            }
        }
    }
}