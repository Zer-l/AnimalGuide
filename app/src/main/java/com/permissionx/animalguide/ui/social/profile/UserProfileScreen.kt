package com.permissionx.animalguide.ui.social.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.permissionx.animalguide.ui.auth.LoginViewModel

@Composable
fun UserProfileScreen(
    uid: String,
    navController: NavController,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uid) {
        viewModel.load(uid)
    }

    // 返回时刷新
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.load(uid)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            is UserProfileUiState.Loading -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                        Text(text = "用户主页", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UserProfileUiState.Error -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                        Text(text = "用户主页", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.load(uid) }) { Text("重新加载") }
                        }
                    }
                }
            }

            is UserProfileUiState.Success -> {
                var showLoginDialog by remember { mutableStateOf(false) }

                val loginViewModel: LoginViewModel = hiltViewModel(
                    viewModelStoreOwner = LocalContext.current as ComponentActivity
                )

                if (showLoginDialog) {
                    AlertDialog(
                        onDismissRequest = { showLoginDialog = false },
                        title = { Text("登录后才能关注") },
                        text = { Text("登录后可以关注感兴趣的探险家！") },
                        confirmButton = {
                            Button(onClick = {
                                showLoginDialog = false
                                loginViewModel.resetState()
                                navController.navigate(Routes.LOGIN)
                            }) { Text("去登录") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLoginDialog = false }) {
                                Text("暂不登录")
                            }
                        }
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 用户信息区
                    item {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // 背景图 4:3
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 2f)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (s.user.backgroundUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = s.user.backgroundUrl,
                                        contentDescription = "背景图",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // 顶部半透明渐变遮罩
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

                                // 返回按钮 + 标题（覆盖在背景图上层）
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "返回",
                                            tint = Color.White
                                        )
                                    }
                                    Text(
                                        text = "用户主页",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // 头像 + 信息区
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val bgHeight = this@BoxWithConstraints.maxWidth * 2f / 3f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = bgHeight - 40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(3.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
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

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatItem(count = s.user.postCount, label = "帖子")
                                        StatItem(count = s.user.followCount, label = "关注")
                                        StatItem(count = s.user.followerCount, label = "粉丝")
                                        StatItem(count = s.user.likeCount, label = "获赞")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // 关注按钮
                                    if (!viewModel.isOwnProfile) {
                                        Button(
                                            onClick = {
                                                if (viewModel.isLoggedIn) {
                                                    viewModel.toggleFollow()
                                                } else {
                                                    showLoginDialog = true
                                                }
                                            },
                                            enabled = !s.isFollowLoading,
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            colors = if (s.isFollowing) {
                                                ButtonDefaults.outlinedButtonColors()
                                            } else {
                                                ButtonDefaults.buttonColors()
                                            }
                                        ) {
                                            if (s.isFollowLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Text(if (s.isFollowing) "已关注" else "关注")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }

                        HorizontalDivider()
                    }

                    // 帖子标题
                    item {
                        Text(
                            text = "Ta 的帖子",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (s.posts.isEmpty() && !s.isLoadingMorePosts) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无帖子",
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
                            onLike = {},
                            onComment = {
                                navController.navigate(Routes.postDetail(post.id))
                            },
                            onCollect = {}
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
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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