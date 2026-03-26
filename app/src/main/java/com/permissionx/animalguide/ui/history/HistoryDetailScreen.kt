package com.permissionx.animalguide.ui.history

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

@Composable
fun HistoryDetailScreen(
    historyId: Int,
    navController: NavController,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(historyId) {
        viewModel.loadDetail(historyId)
    }

    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is HistoryDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is HistoryDetailUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message)
            }
        }

        is HistoryDetailUiState.Success -> {
            HistoryDetailContent(state = s, navController = navController)
        }
    }
}

@Composable
fun HistoryDetailContent(
    state: HistoryDetailUiState.Success,
    navController: NavController
) {
    val history = state.history
    val animal = state.animal
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

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
                model = history.imageUri.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
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

            // 底部动物名称
            if (animal != null) {
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
            } else {
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
                    Text(
                        text = history.animalName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {

            // 识别信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔍 识别信息",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRowIfValid(
                        label = "🕐 识别时间",
                        value = dateFormat.format(Date(history.recognizedAt))
                    )
                    if (history.confidence > 0f) {
                        InfoRowIfValid(
                            label = "📊 置信度",
                            value = "${"%.1f".format(history.confidence * 100)}%"
                        )
                    }
                    if (state.address.isNotEmpty()) {
                        InfoRowIfValid(
                            label = "📍 识别地点",
                            value = state.address
                        )
                    }
                }
            }

            // 如果有图鉴数据则展示科普内容
            if (animal != null) {
                Spacer(modifier = Modifier.height(16.dp))

                // 濒危等级
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConservationBadge(status = animal.conservationStatus)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "已收录图鉴",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 基本信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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

                // 跳转图鉴详情
                OutlinedButton(
                    onClick = {
                        val encoded = Uri.encode(animal.animalName)
                        navController.navigate("pokedex_detail/$encoded")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📖 查看完整图鉴")
                }

            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("😢", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (history.isSuccess) "该动物未收录图鉴" else "识别失败",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (history.isSuccess) "重新识别后可收录" else "可以重新尝试识别",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val encoded = Uri.encode(history.imageUri)
                                navController.navigate("result/$encoded")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🔄 重新识别")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}