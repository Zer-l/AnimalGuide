package com.permissionx.animalguide.ui.pokedex

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.domain.achievement.ALL_ACHIEVEMENTS
import androidx.core.net.toUri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import com.permissionx.animalguide.ui.common.SearchTopBar

@Composable
fun PokedexScreen(
    navController: NavController,
    viewModel: PokedexViewModel = hiltViewModel()
) {
    val animals by viewModel.animals.collectAsState(initial = emptyList())
    val animalCount by viewModel.animalCount.collectAsState(initial = 0)
    val searchQuery by viewModel.searchQuery.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 顶部标题栏
        SearchTopBar(
            title = "晓物图鉴",
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onClearSearch = { viewModel.clearSearch() },
            trailingContent = {
                Text(
                    text = "已收集 $animalCount 种",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )

        // 收集进度条
        LinearProgressIndicator(
            progress = { (animalCount / 100f).coerceAtMost(1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 成就栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ALL_ACHIEVEMENTS.forEach { achievement ->
                val unlocked = viewModel.isAchievementUnlocked(achievement.id)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // 背景圆
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    color = if (unlocked)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                        // 图标
                        Text(
                            text = achievement.icon,
                            fontSize = 26.sp,
                            modifier = Modifier.graphicsLayer {
                                alpha = if (unlocked) 1f else 0.3f
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (unlocked) {
                        Text(
                            text = achievement.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    } else {
                        Text(
                            text = "$animalCount/${achievement.requiredCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = achievement.name,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        if (animals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (searchQuery.isNotBlank()) {
                        Text("🔍", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "未找到「$searchQuery」",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("🦁", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "还没有收录任何动物",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "去拍摄动物并收录进图鉴吧！",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(animals, key = { it.animalName }) { animal ->
                    AnimalGridItem(
                        animal = animal,
                        onClick = {
                            val encoded = Uri.encode(animal.animalName)
                            navController.navigate("pokedex_detail/$encoded")
                        },
                        onLongClick = {
                            viewModel.deleteAnimal(animal)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimalGridItem(
    animal: AnimalEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit  // 新增
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除图鉴") },
            text = { Text("确定要从图鉴中删除「${animal.animalName}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onLongClick()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Card(
        modifier = Modifier
            .aspectRatio(0.8f)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showDeleteDialog = true }
            ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(animal.imageUri.toUri())
                    .size(300, 300)
                    .crossfade(true)
                    .build(),
                contentDescription = animal.animalName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 底部渐变 + 名称
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = animal.animalName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val normalized = animal.conservationStatus.uppercase().take(2)
                    val statusColor = when (normalized) {
                        "LC" -> Color(0xFF4CAF50)
                        "NT" -> Color(0xFF2196F3)
                        "VU" -> Color(0xFFF9A825)
                        "EN" -> Color(0xFFFF9800)
                        "CR" -> Color(0xFFF44336)
                        "DD" -> Color(0xFF9E9E9E)
                        else -> Color.Gray
                    }
                    Text(
                        text = normalized,
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 手动标注标记
            if (animal.isManual) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "手动",
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}