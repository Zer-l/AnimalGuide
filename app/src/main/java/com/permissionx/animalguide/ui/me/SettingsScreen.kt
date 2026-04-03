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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.permissionx.animalguide.ui.auth.LoginViewModel
import com.permissionx.animalguide.ui.navigation.Routes

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MeViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    val loginViewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                // 退出登录弹窗确认按钮
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    loginViewModel.resetState()  // 重置登录状态
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

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = "设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        HorizontalDivider()

        // 历史记录
        SettingsItem(
            title = "识别历史",
            onClick = { navController.navigate(Routes.HISTORY) }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        SettingsItem(
            title = "登录密码",
            onClick = { }
        )
        // 说明文字
        Text(
            text = "  密码在注册时设置，暂不支持修改",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // 关于
        SettingsItem(
            title = "关于晓物",
            onClick = { /* 后续实现 */ }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // 退出登录
        SettingsItem(
            title = "退出登录",
            titleColor = MaterialTheme.colorScheme.error,
            showArrow = false,
            onClick = { showLogoutDialog = true }
        )
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