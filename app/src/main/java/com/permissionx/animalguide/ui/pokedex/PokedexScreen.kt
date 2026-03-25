package com.permissionx.animalguide.ui.pokedex

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import androidx.core.net.toUri

@Composable
fun PokedexScreen(
    navController: NavController,
    viewModel: PokedexViewModel = hiltViewModel()
) {
    val animals by viewModel.animals.collectAsState(initial = emptyList())
    val animalCount by viewModel.animalCount.collectAsState(initial = 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "动物图鉴",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "已收集 $animalCount 种",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // 进度条
        if (animalCount > 0) {
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
        }

        // 成就栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            viewModel.getAllAchievements().forEach { achievement ->
                val unlocked = viewModel.isAchievementUnlocked(achievement.id)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = achievement.icon,
                        fontSize = 28.sp,
                        color = if (unlocked) Color.Unspecified
                        else Color.Gray.copy(alpha = 0.3f)
                    )
                    Text(
                        text = if (unlocked) achievement.name
                        else "${animalCount}/${achievement.requiredCount}",
                        fontSize = 9.sp,
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (animals.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        } else {
            // 3列网格
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimalGridItem(
    animal: AnimalEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 动物图片
            AsyncImage(
                model = animal.imageUri.toUri(),
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
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
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
                    // 濒危等级小标签
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
                        .align(Alignment.BottomEnd)
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