package com.permissionx.animalguide.ui.history

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.data.local.entity.RecognizeHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyList.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    // 清空确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空历史记录") },
            text = { Text("确定要清空所有历史记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) { Text("确定", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

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
                text = "识别历史",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (historyList.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "清空历史",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📷", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有识别记录",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "去拍摄动物开始识别吧！",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList, key = { it.id }) { history ->
                    HistoryItem(
                        history = history,
                        onDelete = { viewModel.deleteHistory(history) },
                        onClick = {
                            if (history.isSuccess) {
                                val encoded = Uri.encode(history.imageUri)
                                navController.navigate("result/$encoded")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    history: RecognizeHistory,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
    val timeStr = dateFormat.format(Date(history.recognizedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            AsyncImage(
                model = history.imageUri.toUri(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (history.isSuccess) history.animalName else "识别失败",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (history.isSuccess)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (history.isSuccess && history.confidence > 0f) {
                    Text(
                        text = "置信度 ${"%.1f".format(history.confidence * 100)}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 删除按钮
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}