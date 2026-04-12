package com.permissionx.animalguide.ui.social

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.auth.LoginViewModel
import com.permissionx.animalguide.ui.navigation.Routes
import com.permissionx.animalguide.ui.social.feed.FeedScreen
import com.permissionx.animalguide.ui.social.feed.FeedViewModel

@Composable
fun SocialScreen(
    navController: NavController,
    viewModel: SocialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState(initial = SocialUiState.Guest)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showLoginDialog by remember { mutableStateOf(false) }

    // 获取"最新"Tab 的 FeedViewModel，用于响应跨屏导航事件
    val latestFeedViewModel: FeedViewModel = hiltViewModel(key = "feed_latest")

    // 收到"跳转最新"事件：切换 Tab 并刷新
    LaunchedEffect(Unit) {
        viewModel.navigateToLatestEvents.collect {
            selectedTab = 1
            latestFeedViewModel.refresh()
        }
    }

    val loginViewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    // 登录引导弹窗
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("登录后才能使用此功能") },
            text = { Text("登录后可以发帖、点赞、评论，加入晓物社区！") },
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

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "社区",
                fontSize = 22.sp,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            // 搜索按钮
            IconButton(onClick = {
                if (uiState is SocialUiState.LoggedIn) {
                    navController.navigate(Routes.SEARCH_SOCIAL)
                } else {
                    showLoginDialog = true
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索"
                )
            }
            // 发帖按钮
            IconButton(onClick = {
                if (uiState is SocialUiState.LoggedIn) {
                    navController.navigate(Routes.PUBLISH)
                } else {
                    showLoginDialog = true
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "发帖"
                )
            }
        }

        // Tab 切换
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("推荐") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("最新") }
            )
        }

        // 内容区
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> FeedScreen(
                    navController = navController,
                    isLoggedIn = uiState is SocialUiState.LoggedIn,
                    sortByHot = true,
                    onRequireLogin = { showLoginDialog = true },
                    viewModel = hiltViewModel(key = "feed_hot")
                )

                1 -> FeedScreen(
                    navController = navController,
                    isLoggedIn = uiState is SocialUiState.LoggedIn,
                    sortByHot = false,
                    onRequireLogin = { showLoginDialog = true },
                    viewModel = hiltViewModel(key = "feed_latest")
                )
            }
        }
    }
}