package com.permissionx.animalguide.ui.result.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.domain.model.AnimalInfo
import com.permissionx.animalguide.ui.common.InfoGroup
import com.permissionx.animalguide.ui.common.InfoParagraph
import com.permissionx.animalguide.ui.common.InfoRow

@Composable
fun AnimalInfoCard(
    confidence: Float,
    info: AnimalInfo,
    otherResults: List<Pair<String, Float>>,
    onRetake: () -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean,
    isAlreadyExists: Boolean,
    onRegenerate: (String) -> Unit
) {
    var showOthers by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        // 置信度 + 濒危等级
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ConservationBadge(status = info.conservationStatus)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "置信度 ${"%.1f".format(confidence * 100)}%",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                Text(
                    text = "📚 科普信息",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 1. 基础信息
                InfoGroup(title = "基础信息") {
                    InfoRow("学   　名", info.scientificName)
                    InfoRow("科   　属", info.taxonomy)
                    InfoRow("分   　布", info.distribution)
                }

                // 2. 形态特征
                if (info.morphology.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoGroup(title = "形态特征") {
                        InfoParagraph(info.morphology)
                    }
                }

                // 3. 生态与行为
                Spacer(modifier = Modifier.height(10.dp))
                InfoGroup(title = "生态与行为") {
                    InfoRow("栖 息 地", info.habitat)
                    InfoRow("食   　性", info.diet)
                    InfoRow("活动习性", info.activityPattern)
                    InfoRow("社会行为", info.socialBehavior)
                }

                // 4. 保护与价值
                Spacer(modifier = Modifier.height(10.dp))
                InfoGroup(title = "保护与价值") {
                    InfoRow("寿   　命", info.lifespan)
                    if (info.ecologicalRole.isNotBlank()) {
                        InfoRow("价   　值", info.ecologicalRole)
                    }
                }

                // 5. 科研价值
                if (info.researchValue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoGroup(title = "科研价值") {
                        InfoParagraph(info.researchValue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 简介
        if (info.description.isNotBlank()) {
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
                        text = info.description,
                        fontSize = 13.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = { onRegenerate(info.name) },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("换个角度介绍", fontSize = 12.sp)
        }

        // 其他候选
        if (otherResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            OtherResultsCard(
                otherResults = otherResults,
                showOthers = showOthers,
                onToggle = { showOthers = !showOthers }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 收录按钮
        SaveButton(
            isSaved = isSaved,
            isAlreadyExists = isAlreadyExists,
            onSave = onSave
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 再拍一张
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📷 再拍一张")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}