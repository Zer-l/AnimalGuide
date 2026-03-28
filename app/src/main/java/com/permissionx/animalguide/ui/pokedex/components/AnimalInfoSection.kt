package com.permissionx.animalguide.ui.pokedex.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.ui.result.components.InfoRowIfValid

@Composable
fun AnimalInfoSection(
    animal: AnimalEntry,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    // 科普基本信息
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
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    IconButton(
                        onClick = onRefresh,
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
}