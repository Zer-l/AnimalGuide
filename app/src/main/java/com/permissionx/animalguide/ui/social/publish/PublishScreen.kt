package com.permissionx.animalguide.ui.social.publish

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.domain.model.social.PostType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublishScreen(
    navController: NavController,
    animalName: String = "",
    viewModel: PublishViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var tagInput by remember { mutableStateOf("") }
    var showTagInput by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf("") }
    val context = LocalContext.current
    val postType = if (animalName.isNotBlank()) PostType.ANIMAL_SHARE else PostType.ORIGINAL

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isLoadingLocation = true
            locationError = ""
        } else {
            locationError = "未授予定位权限"
        }
    }

    // 发布成功后：通知前一个页面，再返回
    LaunchedEffect(state) {
        if (state is PublishUiState.Success) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("publish_success", true)
            navController.popBackStack()
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val remaining = 9 - imageUris.size
        imageUris = imageUris + uris.take(remaining)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = if (postType == PostType.ANIMAL_SHARE) "分享动物" else "发布帖子",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    viewModel.publish(
                        title = title,
                        content = content,
                        imageUris = imageUris,
                        tags = tags,
                        type = postType,
                        animalName = animalName,
                        location = locationText,
                        latitude = latitude,
                        longitude = longitude
                    )
                },
                enabled = state !is PublishUiState.Publishing,
                shape = RoundedCornerShape(20.dp)
            ) {
                if (state is PublishUiState.Publishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("发布")
                }
            }
        }

        HorizontalDivider()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
//            // 动物分享标签
//            if (animalName.isNotBlank()) {
//                Surface(
//                    shape = RoundedCornerShape(20.dp),
//                    color = MaterialTheme.colorScheme.primaryContainer
//                ) {
//                    Text(
//                        text = "🐼 分享动物：$animalName",
//                        fontSize = 13.sp,
//                        color = MaterialTheme.colorScheme.onPrimaryContainer,
//                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//                    )
//                }
//                Spacer(modifier = Modifier.height(12.dp))
//            }

            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 50) title = it },
                placeholder = { Text("输入标题（必填）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("${title.length}/50") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 正文输入
            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                placeholder = { Text("分享你的发现...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(12.dp),
                supportingText = { Text("${content.length}/1000") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 图片选择区
            Text(
                text = "添加图片（${imageUris.size}/9）",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(imageUris) { uri ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { imageUris = imageUris - uri },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "删除图片",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // 添加图片按钮
                if (imageUris.size < 9) {
                    item {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { imageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加图片",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "添加图片",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 标签
            Text(
                text = "添加标签（可选）",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { tags = tags - tag },
                        label = { Text("#$tag") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "删除标签",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }

                if (tags.size < 5) {
                    if (showTagInput) {
                        val tagFocusRequester = remember { FocusRequester() }

                        LaunchedEffect(Unit) {
                            tagFocusRequester.requestFocus()
                        }

                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { if (it.length <= 10) tagInput = it },
                            placeholder = { Text("输入标签") },
                            singleLine = true,
                            modifier = Modifier
                                .width(160.dp)
                                .focusRequester(tagFocusRequester),
                            shape = RoundedCornerShape(20.dp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (tagInput.isNotBlank()) {
                                        tags = tags + tagInput.trim()
                                        tagInput = ""
                                    }
                                    showTagInput = false
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    tagInput = ""
                                    showTagInput = false
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "取消",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    } else {
                        AssistChip(
                            onClick = { showTagInput = true },
                            label = { Text("+ 添加标签") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val locationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    isLoadingLocation = true
                }
            }

            // 从图鉴分享时，自动加载图鉴图片
            LaunchedEffect(animalName) {
                if (animalName.isNotBlank()) {
                    val uri = viewModel.getAnimalImageUri(animalName)
                    if (uri != null && imageUris.isEmpty()) {
                        imageUris = listOf(uri)
                    }
                }
            }

            LaunchedEffect(animalName) {
                if (animalName.isNotBlank()) {
                    // 自动添加图鉴图片
                    val uri = viewModel.getAnimalImageUri(animalName)
                    if (uri != null && imageUris.isEmpty()) {
                        imageUris = listOf(uri)
                    }
                    // 自动添加动物名称为标签
                    if (tags.isEmpty()) {
                        tags = listOf(animalName)
                    }
                }
            }

            // 获取定位
            LaunchedEffect(isLoadingLocation) {
                if (isLoadingLocation) {
                    locationError = ""
                    val (name, lat, lng) = viewModel.getLocationForPublish(context)
                    if (name != null && lat != null) {
                        locationText = name
                        latitude = lat
                        longitude = lng
                    } else {
                        locationError = "定位失败，请检查定位服务后重试"
                    }
                    isLoadingLocation = false
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (locationText.isEmpty() && !isLoadingLocation) {
                            val hasPermission = androidx.core.content.ContextCompat
                                .checkSelfPermission(
                                    context,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                isLoadingLocation = true
                                locationError = ""
                            } else {
                                locationPermissionLauncher.launch(
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            }
                        } else if (locationText.isNotEmpty()) {
                            locationText = ""
                            latitude = null
                            longitude = null
                            locationError = ""
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (locationText.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                when {
                    isLoadingLocation -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "定位中...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    locationText.isNotEmpty() -> {
                        Text(
                            text = locationText,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除地点",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    locationError.isNotEmpty() -> {
                        Text(
                            text = locationError,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        Text(
                            text = "添加地点",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 错误提示
            if (state is PublishUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (state as PublishUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}