package com.permissionx.animalguide.ui.pokedex.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.data.local.entity.AnimalEntry
import androidx.core.net.toUri
import com.permissionx.animalguide.ui.common.FullScreenImageViewer

@Composable
fun AnimalHeaderSection(
    animal: AnimalEntry,
    navController: NavController,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除图鉴") },
            text = { Text("确定要从图鉴中删除「${animal.animalName}」吗？历史记录不会被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    var showImageViewer by remember { mutableStateOf(false) }

    if (showImageViewer) {
        FullScreenImageViewer(
            imageUris = listOf(animal.imageUri),
            onDismiss = { showImageViewer = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 2f)
    ) {
        AsyncImage(
            model = animal.imageUri.toUri(),
            contentDescription = animal.animalName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clickable { showImageViewer = true }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // 返回按钮
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(8.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
        }

        // 删除按钮
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .padding(8.dp)
                .size(40.dp)
                .align(Alignment.TopEnd)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = Color.White
            )
        }

        // 动物名称
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = animal.animalName,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = animal.scientificName,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}