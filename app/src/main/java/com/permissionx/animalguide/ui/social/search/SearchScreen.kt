package com.permissionx.animalguide.ui.social.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.permissionx.animalguide.ui.navigation.Routes
import com.permissionx.animalguide.ui.social.components.PostCard
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // 自动聚焦搜索框
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 返回时刷新搜索结果
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshCurrentResults()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box {
                            if (state.query.isEmpty()) {
                                Text(
                                    text = "搜索帖子或用户...",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (state.query.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.onQueryChange("") },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = { navController.popBackStack() },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = "取消",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Tab
        if (state.query.isNotBlank()) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onTabChange(0) },
                    text = { Text("帖子") }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onTabChange(1) },
                    text = { Text("用户") }
                )
            }
        }

        // 内容
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.posts.isEmpty() && state.users.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.posts.isEmpty() && state.users.isEmpty() -> {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.query.isBlank() -> {
                    Text(
                        text = "输入关键词搜索帖子或用户",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.selectedTab == 0 -> {
                    PostSearchResults(
                        state = state,
                        navController = navController,
                        onLoadMore = { viewModel.loadMore() }
                    )
                }

                state.selectedTab == 1 -> {
                    UserSearchResults(
                        state = state,
                        navController = navController,
                        onLoadMore = { viewModel.loadMore() }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostSearchResults(
    state: SearchUiState,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // 滚动加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }

    if (state.posts.isEmpty() && !state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "没有找到相关帖子",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(state.posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onClick = { navController.navigate(Routes.postDetail(post.id)) },
                    onLike = {},
                    onComment = { navController.navigate(Routes.postDetail(post.id)) },
                    onCollect = {},
                    onUserClick = { uid ->
                        navController.navigate(Routes.userProfile(uid))
                    }
                )
            }

            if (state.isLoading) {
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

            if (!state.hasMorePosts && state.posts.isNotEmpty()) {
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

@Composable
private fun UserSearchResults(
    state: SearchUiState,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd) onLoadMore()
        }
    }

    if (state.users.isEmpty() && !state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "没有找到相关用户",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(state.users, key = { it.uid }) { user ->
                UserSearchItem(
                    user = user,
                    onClick = { navController.navigate(Routes.userProfile(user.uid)) }
                )
            }

            if (state.isLoading) {
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
        }
    }
}

@Composable
private fun UserSearchItem(
    user: com.permissionx.animalguide.data.repository.UserSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = user.nickname.take(1),
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.nickname,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.bio.isNotBlank()) {
                Text(
                    text = user.bio,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}