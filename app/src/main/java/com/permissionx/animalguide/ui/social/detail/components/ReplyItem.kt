package com.permissionx.animalguide.ui.social.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.domain.model.social.Comment

@Composable
fun ReplyItem(
    reply: Comment,
    currentUserId: String?,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                append(reply.nickname)
            }
            if (reply.replyToNickname != null) {
                append(" 回复 ")
                withStyle(SpanStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                    append("@${reply.replyToNickname}")
                }
            }
            append("  ")
            withStyle(SpanStyle(fontSize = 13.sp)) {
                append(reply.content)
            }
        }
        Text(text = text, lineHeight = 20.sp)

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formatTime(reply.createdAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "回复",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onReply() }
            )
            if (reply.uid == currentUserId) {
                Text(
                    text = "删除",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        else -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
            .format(java.util.Date(timestamp))
    }
}