package com.permissionx.animalguide.ui.pokedex.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import com.permissionx.animalguide.ui.result.components.InfoRowIfValid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiscoverySection(
    animal: AnimalEntry,
    address: String
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

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
            when {
                address.isNotEmpty() -> {
                    InfoRowIfValid(label = "📍 发现地点", value = address)
                }

                animal.latitude != null -> {
                    InfoRowIfValid(
                        label = "📍 发现地点",
                        value = "${"%.4f".format(animal.latitude)}, ${"%.4f".format(animal.longitude)}"
                    )
                }

                else -> {
                    InfoRowIfValid(
                        label = "📍 发现地点",
                        value = "暂无记录（请授予位置权限后重新收录）"
                    )
                }
            }
        }
    }
}