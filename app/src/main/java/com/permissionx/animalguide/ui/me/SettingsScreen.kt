package com.permissionx.animalguide.ui.me

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.auth.LoginViewModel
import com.permissionx.animalguide.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MeViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordTip by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val loginViewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    val currentUser by viewModel.currentUser.collectAsState()
    val deleteAccountState by viewModel.deleteAccountState.collectAsState()

    LaunchedEffect(deleteAccountState) {
        if (deleteAccountState is DeleteAccountState.Success) {
            loginViewModel.resetState()
            viewModel.resetDeleteAccountState()
            navController.navigate(Routes.ME) {
                popUpTo(Routes.SETTINGS) { inclusive = true }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    loginViewModel.resetState()
                    navController.navigate(Routes.ME) {
                        popUpTo(Routes.SETTINGS) { inclusive = true }
                    }
                }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("注销账号") },
            text = {
                Text("注销后您的账号将被永久删除，包括发布的帖子、评论等所有数据，且无法恢复。\n\n确定要注销账号吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        viewModel.deleteAccount()
                    },
                    enabled = deleteAccountState !is DeleteAccountState.Loading
                ) {
                    Text("确定注销", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("取消") }
            }
        )
    }

    val errorState = deleteAccountState as? DeleteAccountState.Error
    if (errorState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetDeleteAccountState() },
            title = { Text("注销失败") },
            text = { Text(errorState.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.resetDeleteAccountState() }) { Text("知道了") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalDivider()

            SettingsItem(
                title = "识别历史",
                onClick = { navController.navigate(Routes.HISTORY) }
            )

            HorizontalDivider()

            SettingsItem(
                title = "登录密码",
                onClick = { showPasswordTip = true }
            )

            if (showPasswordTip) {
                AlertDialog(
                    onDismissRequest = { showPasswordTip = false },
                    title = { Text("登录密码") },
                    text = { Text("密码在注册时设置，暂不支持修改") },
                    confirmButton = {
                        TextButton(onClick = { showPasswordTip = false }) { Text("知道了") }
                    }
                )
            }

            HorizontalDivider()

            SettingsItem(
                title = "关于晓物",
                onClick = { navController.navigate(Routes.ABOUT) }
            )

            HorizontalDivider()

            if (currentUser != null) {
                SettingsItem(
                    title = "退出登录",
                    titleColor = MaterialTheme.colorScheme.error,
                    showArrow = false,
                    onClick = { showLogoutDialog = true }
                )

                HorizontalDivider()

                if (deleteAccountState is DeleteAccountState.Loading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "正在注销...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    SettingsItem(
                        title = "注销账号",
                        titleColor = MaterialTheme.colorScheme.error,
                        showArrow = false,
                        onClick = { showDeleteAccountDialog = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
