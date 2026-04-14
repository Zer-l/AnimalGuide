package com.permissionx.animalguide.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Column(content = content)
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            modifier = Modifier
                .weight(1f)
                .offset(x = 0.dp, y = 2.dp),
            lineHeight = 20.sp
        )
    }
}

@Composable
fun InfoParagraph(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth()
    )
}
