package com.permissionx.animalguide.ui.social.detail

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.social.detail.components.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.permissionx.animalguide.ui.auth.LoginViewModel
import com.permissionx.animalguide.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    navController: NavController,
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val loginViewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    // 上拉加载更多评论
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd) viewModel.loadMoreComments()
        }
    }

    // 删除确认弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除帖子") },
            text = { Text("确定要删除这篇帖子吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deletePost { navController.popBackStack() }
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    state.post?.let { post ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                navController.navigate(Routes.userProfile(post.uid))
                            }
                        ) {
                            // 头像
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (post.avatarUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = post.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = post.nickname.take(1),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = post.nickname,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = formatTime(post.createdAt),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 关注按钮（非自己的帖子）
                    state.post?.let { post ->
                        if (post.uid != viewModel.currentUserId) {
                            var showLoginDialog by remember { mutableStateOf(false) }

                            if (showLoginDialog) {
                                AlertDialog(
                                    onDismissRequest = { showLoginDialog = false },
                                    title = { Text("登录后才能关注") },
                                    text = { Text("登录后可以关注感兴趣的探险家！") },
                                    confirmButton = {
                                        Button(// 关注按钮未登录时
                                            onClick = {
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

                            OutlinedButton(
                                onClick = {
                                    if (viewModel.currentUserId != null) {
                                        viewModel.toggleFollow()
                                    } else {
                                        showLoginDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (state.isFollowing) "已关注" else "+ 关注",
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }

                    // 更多菜单
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (state.post?.uid == viewModel.currentUserId) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "删除帖子",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("举报") },
                                onClick = { showMoreMenu = false }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            CommentInputBar(
                replyTo = state.replyTo,
                isSubmitting = state.isSubmittingComment,
                focusRequester = focusRequester,
                onSubmit = { viewModel.submitComment(it) },
                onCancelReply = { viewModel.setReplyTo(null) }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.error ?: "加载失败", color = MaterialTheme.colorScheme.error)
                }
            }

            state.post != null -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // 帖子内容
                    item {
                        PostDetailHeader(
                            post = state.post!!,
                            currentUserId = viewModel.currentUserId,
                        )
                    }

                    // 操作栏
                    item {
                        PostActionBar(
                            post = state.post!!,
                            onLike = { viewModel.toggleLike() },
                            onComment = { focusRequester.requestFocus() },
                            onCollect = { viewModel.toggleCollect() }
                        )
                    }

                    // 评论标题
                    item {
                        Text(
                            text = "全部评论 ${state.post!!.commentCount}条",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            )
                        )
                    }

                    // 离线提示（帖子内容来自缓存，评论暂不可用）
                    if (state.isOffline) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = "离线模式 · 评论暂不可用",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 评论列表
                    if (state.comments.isEmpty() && !state.isLoading && !state.isOffline) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "还没有评论，快来发表第一条评论吧！",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(state.comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            currentUserId = viewModel.currentUserId,
                            isExpanded = state.expandedReplies.contains(comment.id),
                            replies = state.repliesMap[comment.id] ?: emptyList(),
                            onReply = { viewModel.setReplyTo(it) },
                            onLike = { viewModel.likeComment(it) },
                            onDelete = { viewModel.deleteComment(it.id) },
                            onToggleReplies = { viewModel.toggleReplies(it) },
                            onUserClick = { uid ->
                                navController.navigate(Routes.userProfile(uid))
                            }
                        )
                    }

                    // 加载更多评论
                    if (state.isLoadingMoreComments) {
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

                    if (!state.hasMoreComments && state.comments.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有更多评论了",
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

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        else -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
            .format(java.util.Date(timestamp))
    }
}