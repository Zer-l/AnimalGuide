package com.permissionx.animalguide.ui.result.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConservationBadge(status: String) {
    val normalized = status.uppercase().take(2)
    val (label, color) = when (normalized) {
        "LC" -> "无危" to Color(0xFF4CAF50)
        "NT" -> "近危" to Color(0xFF2196F3)
        "VU" -> "易危" to Color(0xFFF9A825)
        "EN" -> "濒危" to Color(0xFFFF9800)
        "CR" -> "极危" to Color(0xFFF44336)
        "DD" -> "数据不足" to Color(0xFF9E9E9E)
        else -> "未知" to Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = "$normalized · $label",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}