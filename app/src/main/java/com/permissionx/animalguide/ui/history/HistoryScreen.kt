package com.permissionx.animalguide.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.common.SearchTopBar
import com.permissionx.animalguide.ui.history.components.HistoryItem
import com.permissionx.animalguide.ui.history.components.StatChip
import com.permissionx.animalguide.ui.navigation.Routes

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyList.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()

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
        SearchTopBar(
            title = "识别历史",
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onClearSearch = { viewModel.clearSearch() },
            trailingContent = {
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
        )

        // 统计信息
        if (historyList.isNotEmpty()) {
            val successCount = historyList.count { it.isSuccess }
            val failCount = historyList.size - successCount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatChip(
                    icon = "✅",
                    text = "成功 $successCount 次",
                    color = MaterialTheme.colorScheme.primary
                )
                StatChip(
                    icon = "❌",
                    text = "失败 $failCount 次",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (historyList.isEmpty()) {
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
                            navController.navigate(Routes.historyDetail(history.id))
                        }
                    )
                }
            }
        }
    }
}