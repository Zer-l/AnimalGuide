package com.permissionx.animalguide.ui.social.topic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.navigation.Routes
import com.permissionx.animalguide.ui.social.SocialUiState
import com.permissionx.animalguide.ui.social.SocialViewModel
import com.permissionx.animalguide.ui.social.components.PostCard
import com.permissionx.animalguide.ui.social.feed.FeedUiState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(
    navController: NavController,
    viewModel: TopicViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val listState = rememberLazyListState()
    val socialViewModel: SocialViewModel = hiltViewModel()
    val isLoggedIn = socialViewModel.uiState.collectAsState(initial = SocialUiState.Guest).value is SocialUiState.LoggedIn

    // 刷新后滚到顶部
    val justRefreshed = (state as? FeedUiState.Success)?.justRefreshed ?: false
    LaunchedEffect(justRefreshed) {
        if (justRefreshed) {
            listState.scrollToItem(0)
            viewModel.clearJustRefreshed()
        }
    }

    // 滚动到底部自动加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearEnd ->
            if (nearEnd) viewModel.loadMore()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("#${viewModel.tag}", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 推荐 / 最新 Tab
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("推荐") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("最新") }
                )
            }

            when (val s = state) {
                is FeedUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is FeedUiState.Empty -> {
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { viewModel.loadFirstPage() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌿", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "该话题下还没有帖子",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is FeedUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("😢", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadFirstPage() }) { Text("重新加载") }
                        }
                    }
                }

                is FeedUiState.Success -> {
                    PullToRefreshBox(
                        isRefreshing = s.isRefreshing,
                        onRefresh = { viewModel.pullToRefresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(s.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    highlightTag = viewModel.tag,
                                    onClick = { navController.navigate(Routes.postDetail(post.id)) },
                                    onLike = {
                                        if (isLoggedIn) {
                                            val latest = (viewModel.state.value as? FeedUiState.Success)
                                                ?.posts?.find { it.id == post.id } ?: post
                                            viewModel.toggleLike(latest)
                                        }
                                    },
                                    onComment = {
                                        navController.navigate(Routes.postDetail(post.id))
                                    },
                                    onCollect = {
                                        if (isLoggedIn) {
                                            val latest = (viewModel.state.value as? FeedUiState.Success)
                                                ?.posts?.find { it.id == post.id } ?: post
                                            viewModel.toggleCollect(latest)
                                        }
                                    },
                                    onUserClick = { uid ->
                                        navController.navigate(Routes.userProfile(uid))
                                    },
                                    onTagClick = { tag ->
                                        // 同标签不重复压栈
                                        if (tag != viewModel.tag) {
                                            navController.navigate(Routes.topic(tag))
                                        }
                                    }
                                )
                            }

                            if (s.isLoadingMore) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                                }
                            }

                            if (!s.hasMore && s.posts.isNotEmpty()) {
                                item {
                                    Box(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "没有更多了",
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
    }
}
