package com.permissionx.animalguide.ui.social.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.navigation.Routes
import com.permissionx.animalguide.ui.social.components.PostCard
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

// FeedScreen 函数加注解
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    isLoggedIn: Boolean,
    sortByHot: Boolean,
    onRequireLogin: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sortByHot) {
        viewModel.init(sortByHot)
    }

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

    when (val s = state) {
        is FeedUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is FeedUiState.Empty -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌿", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "还没有帖子，快来发第一篇吧！",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is FeedUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😢", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = s.message,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("重新加载")
                    }
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
                            onClick = {
                                navController.navigate(Routes.postDetail(post.id))
                            },
                            onLike = {
                                if (isLoggedIn) {
                                    val latestPost = (viewModel.state.value as? FeedUiState.Success)
                                        ?.posts?.find { it.id == post.id } ?: post
                                    viewModel.toggleLike(latestPost)
                                } else onRequireLogin()
                            },
                            onComment = {
                                if (isLoggedIn) navController.navigate(Routes.postDetail(post.id))
                                else onRequireLogin()
                            },
                            onCollect = {
                                if (isLoggedIn) {
                                    val latestPost = (viewModel.state.value as? FeedUiState.Success)
                                        ?.posts?.find { it.id == post.id } ?: post
                                    viewModel.toggleCollect(latestPost)
                                } else onRequireLogin()
                            },
                            onUserClick = { uid ->
                                navController.navigate(Routes.userProfile(uid))
                            }
                        )
                    }

                    if (s.isLoadingMore) {
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

                    if (!s.hasMore && s.posts.isNotEmpty()) {
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