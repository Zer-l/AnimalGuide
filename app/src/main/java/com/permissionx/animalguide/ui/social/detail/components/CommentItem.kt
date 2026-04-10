package com.permissionx.animalguide.ui.social.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.permissionx.animalguide.domain.model.social.Comment

@Composable
fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    isExpanded: Boolean,
    replies: List<Comment>,
    onReply: (Comment) -> Unit,
    onLike: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    onToggleReplies: (String) -> Unit,
    onUserClick: ((String) -> Unit)? = null  // 新增
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // 头像
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = onUserClick != null) {
                        onUserClick?.invoke(comment.uid)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (comment.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = comment.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = comment.nickname.take(1),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = comment.nickname,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = onUserClick != null) {
                                onUserClick?.invoke(comment.uid)
                            }
                    )
                    Text(
                        text = formatTime(comment.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = comment.content,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 操作行
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 点赞
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLike(comment) }
                    ) {
                        Icon(
                            imageVector = if (comment.isLiked) Icons.Default.Favorite
                            else Icons.Default.FavoriteBorder,
                            contentDescription = "点赞",
                            tint = if (comment.isLiked) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${comment.likeCount}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 回复
                    Text(
                        text = "回复",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onReply(comment) }
                    )

                    // 删除（自己的评论）
                    if (comment.uid == currentUserId) {
                        Text(
                            text = "删除",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { onDelete(comment) }
                        )
                    }
                }

                // 回复列表
                if (comment.replyCount > 0 || replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            replies.forEach { reply ->
                                ReplyItem(
                                    reply = reply,
                                    currentUserId = currentUserId,
                                    onReply = { onReply(reply) },
                                    onDelete = { onDelete(reply) }
                                )
                            }
                        }
                        Text(
                            text = "收起回复",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { onToggleReplies(comment.id) }
                        )
                    } else {
                        Text(
                            text = "查看全部${comment.replyCount}条回复 >",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onToggleReplies(comment.id) }
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp)
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        else -> java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
            .format(java.util.Date(timestamp))
    }
}