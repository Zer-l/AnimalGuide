package com.permissionx.animalguide.ui.me

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.permissionx.animalguide.ui.navigation.Routes
import com.permissionx.animalguide.ui.social.components.PostCard
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import com.permissionx.animalguide.ui.auth.LoginViewModel
import androidx.compose.ui.graphics.Color
import com.permissionx.animalguide.utils.CropImageContract
import com.permissionx.animalguide.utils.CropImageInput
import com.permissionx.animalguide.utils.CropType

@Composable
fun MeScreen(
    navController: NavController,
    viewModel: MeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    val currentUser by viewModel.currentUser.collectAsState()

    val loginViewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    LaunchedEffect(currentUser) {
        viewModel.load()
    }

    // 页面恢复时刷新数据（如发帖后返回）
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 上拉加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd) viewModel.loadMorePosts()
        }
    }

    // 裁剪相关
    var pendingCropType by remember { mutableStateOf<CropType?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { croppedUri ->
        croppedUri?.let { uri ->
            when (pendingCropType) {
                CropType.BACKGROUND -> viewModel.updateBackground(uri)
                CropType.AVATAR -> viewModel.updateAvatar(uri)
                else -> {}
            }
        }
        pendingCropType = null
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingCropType?.let { type ->
                cropLauncher.launch(CropImageInput(it, type))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is MeUiState.Guest, is MeUiState.Loading, is MeUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 顶部栏（只在非 Success 状态显示）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "我的",
                            fontSize = 22.sp,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置"
                            )
                        }
                    }

                    when (val s = state) {
                        is MeUiState.Guest -> {
                            // 未登录
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🦁", fontSize = 64.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "登录后查看个人主页",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            loginViewModel.resetState()
                                            navController.navigate(Routes.LOGIN)
                                        },
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("去登录")
                                    }
                                }
                            }
                        }

                        is MeUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is MeUiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(s.message, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.load() }) { Text("重新加载") }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }

            is MeUiState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 用户信息区
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // 背景图（4:3 比例）
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 2f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        pendingCropType = CropType.BACKGROUND
                                        imagePickerLauncher.launch("image/*")
                                    }
                            ) {
                                if (s.user.backgroundUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = s.user.backgroundUrl,
                                        contentDescription = "背景图",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "点击更换背景图",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 顶部半透明渐变遮罩（让文字更清晰）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.3f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                // "我的" + 设置按钮（覆盖在背景图上层）
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "我的",
                                        fontSize = 22.sp,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "设置",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // 头像 + 信息区（覆盖在背景图底部）
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val bgHeight = this@BoxWithConstraints.maxWidth * 2f / 3f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = bgHeight - 40.dp)
                                ) {
                                    // 头像行
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // 头像
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(3.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .clickable {
                                                    pendingCropType = CropType.AVATAR
                                                    imagePickerLauncher.launch("image/*")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (s.user.avatarUrl.isNotEmpty()) {
                                                AsyncImage(
                                                    model = s.user.avatarUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Text(
                                                    text = s.user.nickname.take(1),
                                                    fontSize = 32.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // 性别图标 + 昵称
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Text(
                                                text = s.user.nickname,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            if (s.user.gender != "SECRET") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (s.user.gender == "MALE")
                                                                Color(0xFF4FC3F7)
                                                            else
                                                                Color(0xFFF48FB1)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (s.user.gender == "MALE") "♂" else "♀",
                                                        fontSize = 14.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 简介
                                    if (s.user.bio.isNotEmpty()) {
                                        Text(
                                            text = s.user.bio,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 6.dp
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 数据统计
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatItem(count = s.user.postCount, label = "帖子")
                                        StatItem(
                                            count = s.user.followCount,
                                            label = "关注",
                                            onClick = {
                                                navController.navigate(
                                                    Routes.followingList(currentUser?.uid ?: "")
                                                )
                                            }
                                        )
                                        StatItem(
                                            count = s.user.followerCount,
                                            label = "粉丝",
                                            onClick = {
                                                navController.navigate(
                                                    Routes.followerList(currentUser?.uid ?: "")
                                                )
                                            }
                                        )
                                        StatItem(count = s.user.likeCount, label = "获赞")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 编辑资料按钮
                                    OutlinedButton(
                                        onClick = { navController.navigate(Routes.EDIT_PROFILE) },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Text("编辑资料")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }

                        HorizontalDivider()
                    }

                    // Tab
                    item {
                        TabRow(selectedTabIndex = s.selectedTab) {
                            Tab(
                                selected = s.selectedTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                text = { Text("我的帖子") }
                            )
                            Tab(
                                selected = s.selectedTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                text = { Text("我的收藏") }
                            )
                        }
                    }

                    // 帖子列表
                    if (s.posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (s.selectedTab == 0) "还没有帖子，去发一篇吧！"
                                    else "还没有收藏，去社区逛逛吧！",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(s.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            onClick = {
                                navController.navigate(Routes.postDetail(post.id))
                            },
                            onLike = {
                                // 从最新 state 获取帖子
                                val latestPost = (viewModel.state.value as? MeUiState.Success)
                                    ?.posts?.find { it.id == post.id } ?: post
                                viewModel.toggleLike(latestPost)
                            },
                            onComment = {
                                navController.navigate(Routes.postDetail(post.id))
                            },
                            onCollect = {
                                val latestPost = (viewModel.state.value as? MeUiState.Success)
                                    ?.posts?.find { it.id == post.id } ?: post
                                viewModel.toggleCollect(latestPost)
                            }
                        )
                    }

                    if (s.isLoadingMorePosts) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    if (!s.hasMorePosts && s.posts.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有更多了",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(count: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) {
            Modifier.clickable { onClick() }
        } else Modifier
    ) {
        Text(
            text = "$count",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}