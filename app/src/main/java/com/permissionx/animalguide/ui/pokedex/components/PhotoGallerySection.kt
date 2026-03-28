package com.permissionx.animalguide.ui.pokedex.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.permissionx.animalguide.data.local.entity.AnimalPhoto
import com.permissionx.animalguide.ui.common.FullScreenImageViewer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoGallery(
    photos: List<AnimalPhoto>,
    coverUri: String,
    onDeletePhoto: (AnimalPhoto) -> Unit,
    onSetCover: (AnimalPhoto) -> Unit
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var photoToDelete by remember { mutableStateOf<AnimalPhoto?>(null) }

    // 全屏预览
    selectedIndex?.let { index ->
        FullScreenImageViewer(
            imageUris = photos.map { it.imageUri },
            initialIndex = index,
            onDismiss = { selectedIndex = null }
        )
    }

    // 删除确认弹窗
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("删除照片") },
            text = {
                Text(
                    if (photo.imageUri == coverUri)
                        "这是当前封面图，删除后将自动更换封面。确定删除吗？"
                    else "确定要删除这张照片吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeletePhoto(photo)
                    photoToDelete = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) { Text("取消") }
            }
        )
    }

    Column {
        Text(
            text = "📸 照片墙（${photos.size}张）",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(photos, key = { _, photo -> photo.id }) { index, photo ->
                PhotoItem(
                    photo = photo,
                    isCover = photo.imageUri == coverUri,
                    onClick = { selectedIndex = index },  // 传入当前索引
                    onDelete = { photoToDelete = photo },
                    onSetCover = { onSetCover(photo) }
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoItem(
    photo: AnimalPhoto,
    isCover: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSetCover: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.imageUri.toUri())
                .size(200, 200)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 封面标记
        if (isCover) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(2.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "封面",
                    fontSize = 9.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // 拍摄时间
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(photo.takenAt)),
                fontSize = 9.sp,
                color = Color.White
            )
        }

        // 长按菜单
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 不是封面才显示设为封面选项
            if (!isCover) {
                DropdownMenuItem(
                    text = { Text("设为封面") },
                    onClick = {
                        showMenu = false
                        onSetCover()
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onDelete()
                }
            )
        }
    }
}