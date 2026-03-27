package com.permissionx.animalguide.ui.result

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
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset

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

    // 动画状态提升到最外层
    var showSavedAnimation by remember { mutableStateOf(false) }
    var savedScale by remember { mutableFloatStateOf(0f) }
    var savedAlpha by remember { mutableFloatStateOf(0f) }
    var savedIsUpdate by remember { mutableStateOf(false) }

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

    // 最外层 Box 撑满整个屏幕
    Box(modifier = Modifier.fillMaxSize()) {
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
                    onDismiss = { achievementQueue.removeAt(0) }
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

                // 识别中遮罩
                val isRecognizing =
                    state is ResultUiState.Idle || state is ResultUiState.RecognizingAnimal
                if (isRecognizing) {
                    ScanOverlay()
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
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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

                    RecognizeProgressCard(
                        isStep2 = isStep2,
                        text = text
                    )
                }

                is ResultUiState.InfoSuccess -> {
                    AnimalInfoCard(
                        confidence = s.confidence,
                        info = s.info,
                        otherResults = s.otherResults,
                        onRetake = {
                            navController.navigate("camera") {
                                popUpTo("camera") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onSave = { viewModel.saveToPokedex() },
                        isSaved = s.isSaved,
                        isAlreadyExists = s.isAlreadyExists,
                        onRegenerate = { animalName ->
                            viewModel.regenerateInfo(animalName)
                        }
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
                                    navController.navigate("camera") {
                                        popUpTo("camera") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            ) {
                                Text("重新拍摄")
                            }
                            OutlinedButton(onClick = { viewModel.retry(uri) }) {
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
        // 动画固定在屏幕正中央，不受滚动影响
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

@Composable
fun AnimalInfoCard(
    confidence: Float,
    info: AnimalInfo,
    otherResults: List<Pair<String, Float>>,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean,
    isAlreadyExists: Boolean,
    onRegenerate: (String) -> Unit  // 新增
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

        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📷 再拍一张")
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 重新输入入口
        var showRegenerateDialog by remember { mutableStateOf(false) }
        var regenerateInput by remember { mutableStateOf("") }

        if (showRegenerateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showRegenerateDialog = false
                    regenerateInput = ""
                },
                title = { Text("重新生成科普内容") },
                text = {
                    Column {
                        Text("请输入正确的动物名称：", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = regenerateInput,
                            onValueChange = { regenerateInput = it },
                            placeholder = { Text("例如：哈基米") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (regenerateInput.isNotBlank()) {
                                onRegenerate(regenerateInput.trim())
                                regenerateInput = ""
                                showRegenerateDialog = false
                            }
                        }
                    ) { Text("重新生成") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRegenerateDialog = false
                        regenerateInput = ""
                    }) { Text("取消") }
                }
            )
        }

        TextButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { showRegenerateDialog = true },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "内容不对？试试输入动物名称重新生成科普内容",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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

@Composable
fun ScanOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        // 扫描线
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height * scanOffset
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Green.copy(alpha = 0.8f),
                        Color.White,
                        Color.Green.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3f
            )

            // 扫描线上下光晕
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Green.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    startY = (y - 30f).coerceAtLeast(0f),
                    endY = (y + 30f).coerceAtMost(size.height)
                )
            )
        }

        // 识别中文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "正在识别...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun RecognizeProgressCard(
    isStep2: Boolean,
    text: String
) {
    val progressAnim by animateFloatAsState(
        targetValue = if (isStep2) 1f else 0.5f,
        animationSpec = tween(600),
        label = "progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标 + 主文字
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isStep2) "✨" else "🔍",
                    fontSize = 22.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 步骤指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 步骤1圆点
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                )

                // 连接线 + 进度
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnim)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }

                // 步骤2圆点
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (isStep2) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        )
                        .then(
                            if (!isStep2) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(50)
                            ) else Modifier
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 步骤标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "动物识别",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "科普生成",
                    fontSize = 12.sp,
                    color = if (isStep2) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}