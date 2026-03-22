package com.permissionx.animalguide.ui.result

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ResultScreen(imageUri: String) {
    val uri = Uri.parse(imageUri)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxWidth())
            Text("图片已获取，阶段三接入识别API", fontSize = 16.sp)
        }
    }
}