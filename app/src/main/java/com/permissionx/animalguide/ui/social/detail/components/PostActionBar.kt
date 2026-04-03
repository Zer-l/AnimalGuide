package com.permissionx.animalguide.ui.social.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.domain.model.social.Post

@Composable
fun PostActionBar(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onCollect: () -> Unit
) {
    HorizontalDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 点赞
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onLike() }
        ) {
            Icon(
                imageVector = if (post.isLiked) Icons.Default.Favorite
                else Icons.Default.FavoriteBorder,
                contentDescription = "点赞",
                tint = if (post.isLiked) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${post.likeCount}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 评论
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onComment() }
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "评论",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${post.commentCount}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 收藏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onCollect() }
        ) {
            Icon(
                imageVector = if (post.isCollected) Icons.Default.Star
                else Icons.Outlined.StarBorder,
                contentDescription = "收藏",
                tint = if (post.isCollected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${post.collectCount}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}