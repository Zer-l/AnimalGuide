package com.permissionx.animalguide.ui.result

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.permissionx.animalguide.domain.model.AnimalInfo

@Composable
fun ResultScreen(
    imageUri: String,
    viewModel: ResultViewModel = hiltViewModel()
) {
    val uri = remember { Uri.parse(imageUri) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(uri) {
        viewModel.recognizeAnimal(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is ResultUiState.Idle,
            is ResultUiState.RecognizingAnimal -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在识别动物...", fontSize = 14.sp)
                    }
                }
            }

            is ResultUiState.RecognizeSuccess -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("识别完成，正在生成科普内容...", fontSize = 14.sp)
                    }
                }
            }

            is ResultUiState.GeneratingInfo -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在生成科普内容...", fontSize = 14.sp)
                    }
                }
            }

            is ResultUiState.InfoSuccess -> {
                AnimalInfoCard(
                    animalName = s.animalName,
                    confidence = s.confidence,
                    info = s.info
                )
            }

            is ResultUiState.Error -> {
                Text(
                    text = "❌ ${s.message}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AnimalInfoCard(
    animalName: String,
    confidence: Float,
    info: AnimalInfo
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // 动物名称
        Text(
            text = info.name,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        // 学名
        Text(
            text = info.scientificName,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 置信度 + 濒危等级
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "识别置信度：${"%.1f".format(confidence * 100)}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            ConservationBadge(status = info.conservationStatus)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 科普信息卡片
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(label = "栖息地", value = info.habitat)
                InfoRow(label = "食　性", value = info.diet)
                InfoRow(label = "寿　命", value = info.lifespan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 科普介绍
        Text(
            text = "简介",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = info.description,
            fontSize = 15.sp,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(48.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, fontSize = 14.sp)
    }
}

@Composable
fun ConservationBadge(status: String) {
    val (label, color) = when (status.uppercase()) {
        "LC" -> "无危" to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        "NT" -> "近危" to androidx.compose.ui.graphics.Color(0xFF2196F3)
        "VU" -> "易危" to androidx.compose.ui.graphics.Color(0xFFFFEB3B)
        "EN" -> "濒危" to androidx.compose.ui.graphics.Color(0xFFFF9800)
        "CR" -> "极危" to androidx.compose.ui.graphics.Color(0xFFF44336)
        else -> status to androidx.compose.ui.graphics.Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "$status · $label",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}