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
import com.permissionx.animalguide.ui.common.InfoGroup
import com.permissionx.animalguide.ui.common.InfoParagraph
import com.permissionx.animalguide.ui.common.InfoRow

@Composable
fun AnimalInfoSection(
    animal: AnimalEntry,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题栏
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
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新科普内容",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. 基础信息
            InfoGroup(title = "基础信息") {
                InfoRow("学   　名", animal.scientificName)
                InfoRow("科   　属", animal.taxonomy)
                InfoRow("分   　布", animal.distribution)
            }

            // 2. 形态特征
            if (animal.morphology.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoGroup(title = "形态特征") {
                    InfoParagraph(animal.morphology)
                }
            }

            // 3. 生态与行为
            Spacer(modifier = Modifier.height(10.dp))
            InfoGroup(title = "生态与行为") {
                InfoRow("栖 息 地", animal.habitat)
                InfoRow("食   　性", animal.diet)
                InfoRow("活动习性", animal.activityPattern)
                InfoRow("社会行为", animal.socialBehavior)
            }

            // 4. 保护与价值
            Spacer(modifier = Modifier.height(10.dp))
            InfoGroup(title = "保护与价值") {
                InfoRow("寿   　命", animal.lifespan)
                if (animal.ecologicalRole.isNotBlank()) {
                    InfoRow("价   　值", animal.ecologicalRole)
                }
            }

            // 5. 科研价值
            if (animal.researchValue.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                InfoGroup(title = "科研价值") {
                    InfoParagraph(animal.researchValue)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 简介（不重复结构化内容，作为叙述性总结）
    if (animal.description.isNotBlank()) {
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
                    text = animal.description,
                    fontSize = 13.sp,
                    lineHeight = 24.sp
                )
            }
        }
    }
}