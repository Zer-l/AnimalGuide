package com.permissionx.animalguide.ui.pokedex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.ui.result.ConservationBadge
import com.permissionx.animalguide.ui.result.InfoRowIfValid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Close

@Composable
fun FullScreenImageViewer(
    imageUri: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += panChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUri.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = transformState)
            )

            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun PokedexDetailScreen(
    animalName: String,
    navController: NavController,
    viewModel: PokedexDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(animalName) {
        viewModel.loadAnimal(animalName)
    }

    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is DetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message)
            }
        }

        is DetailUiState.Success -> {
            PokedexDetailContent(
                state = s,
                navController = navController,
                onRefreshInfo = { viewModel.refreshAnimalInfo() },
                onSaveNote = { viewModel.saveNote(it) },
                onStartEditNote = { viewModel.startEditNote() },
                onCancelEditNote = { viewModel.cancelEditNote() },
                onDelete = {
                    viewModel.deleteAnimal {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}

@Composable
fun PokedexDetailContent(
    state: DetailUiState.Success,
    navController: NavController,
    onRefreshInfo: () -> Unit,
    onSaveNote: (String) -> Unit,
    onStartEditNote: () -> Unit,
    onCancelEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    val animal = state.animal
    var noteText by remember(animal.note) { mutableStateOf(animal.note) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除图鉴") },
            text = { Text("确定要从图鉴中删除「${animal.animalName}」吗？历史记录不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    var showImageViewer by remember { mutableStateOf(false) }

    if (showImageViewer) {
        FullScreenImageViewer(
            imageUri = animal.imageUri,
            onDismiss = { showImageViewer = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部图片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            AsyncImage(
                model = animal.imageUri.toUri(),
                contentDescription = animal.animalName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showImageViewer = true }
            )

            // 顶部渐变
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

            // 删除按钮
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .padding(8.dp)
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Color.White
                )
            }

            // 动物名称
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = animal.animalName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = animal.scientificName,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            // 濒危等级 + 识别次数
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ConservationBadge(status = animal.conservationStatus)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "已识别 ${animal.recognizeCount} 次",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 发现记录卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🗺 发现记录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRowIfValid(
                        label = "🕐 首次发现",
                        value = dateFormat.format(Date(animal.unlockedAt))
                    )
                    InfoRowIfValid(
                        label = "🕑 最近发现",
                        value = dateFormat.format(Date(animal.lastSeenAt))
                    )
                    if (state.address.isNotEmpty()) {
                        InfoRowIfValid(
                            label = "📍 发现地点",
                            value = state.address
                        )
                    } else if (animal.latitude != null) {
                        InfoRowIfValid(
                            label = "📍 发现坐标",
                            value = "${"%.4f".format(animal.latitude)}, ${"%.4f".format(animal.longitude)}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 科普信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📚 科普信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.isRefreshingInfo) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            IconButton(
                                onClick = onRefreshInfo,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新科普内容",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowIfValid(label = "🏕 栖息地", value = animal.habitat)
                    InfoRowIfValid(label = "🍖 食　性", value = animal.diet)
                    InfoRowIfValid(label = "⏳ 寿　命", value = animal.lifespan)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 科普简介
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
                        text = "\t\t\t\t" + animal.description,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 用户备注卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📝 我的备注",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (!state.isEditingNote) {
                            IconButton(
                                onClick = onStartEditNote,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "编辑备注",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isEditingNote) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("记录你的发现...") },
                            minLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    noteText = animal.note
                                    onCancelEditNote()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("取消") }
                            Button(
                                onClick = { onSaveNote(noteText) },
                                modifier = Modifier.weight(1f)
                            ) { Text("保存") }
                        }
                    } else {
                        Text(
                            text = animal.note.ifEmpty { "点击右上角编辑添加备注" },
                            fontSize = 14.sp,
                            color = if (animal.note.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}