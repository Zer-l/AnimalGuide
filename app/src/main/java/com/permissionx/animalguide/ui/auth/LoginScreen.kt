package com.permissionx.animalguide.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.permissionx.animalguide.data.remote.cloudbase.AuthValidator
import com.permissionx.animalguide.ui.navigation.Routes


@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
) {
    val state by viewModel.state.collectAsState()
    val countdown by viewModel.countdown.collectAsState()

    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isCodeMode by remember { mutableStateOf(false) }

    val isLoading = state is LoginUiState.SendingCode
            || state is LoginUiState.Verifying
            || state is LoginUiState.LoggingIn

    var navigated by remember { mutableStateOf(false) }
    val nextFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state) {
        if (navigated) return@LaunchedEffect
        when (val s = state) {
            is LoginUiState.Success -> {
                navigated = true
                navController.popBackStack(Routes.LOGIN, inclusive = true)
                navController.navigate(Routes.ME) {
                    popUpTo(Routes.QA) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            is LoginUiState.CodeVerified -> {
                navigated = true
                navController.navigate(Routes.setPassword(s.phone, s.verificationToken))
            }

            is LoginUiState.Idle -> {
                navigated = false
            }

            else -> {}
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentBackStackEntry?.destination?.route) {
        if (currentBackStackEntry?.destination?.route == Routes.LOGIN) {
            navigated = false
            viewModel.resetError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "🐼", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "欢迎来到晓物",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 手机号
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 11) {
                        phone = it
                        phoneError = ""
                    }
                },
                label = { Text("手机号") },
                placeholder = { Text("请输入手机号") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { nextFieldFocusRequester.requestFocus() }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if (phoneError.isNotEmpty()) {
                        Text(
                            text = phoneError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 验证码 / 密码
            if (isCodeMode) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it },
                    label = { Text("验证码") },
                    placeholder = { Text("请输入6位验证码") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (phone.length == 11 && code.length == 6) {
                                viewModel.loginWithCode(phone, code)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nextFieldFocusRequester),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                val validation = AuthValidator.validatePhone(phone)
                                if (validation.isFailure) {
                                    phoneError =
                                        validation.exceptionOrNull()?.message ?: "手机号格式错误"
                                } else {
                                    phoneError = ""
                                    viewModel.sendCode(phone)
                                }
                            },
                            enabled = phone.length == 11
                                    && countdown == 0
                                    && state !is LoginUiState.SendingCode
                        ) {
                            Text(
                                text = if (countdown > 0) "${countdown}s" else "发送验证码",
                                fontSize = 13.sp
                            )
                        }
                    }
                )
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    placeholder = { Text("请输入密码") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nextFieldFocusRequester),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (phone.length == 11 && password.isNotBlank()) {
                                viewModel.loginWithPassword(phone, password)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    }
                )
            }

            // 错误提示
            if (state is LoginUiState.Error) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = (state as LoginUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 登录按钮
            Button(
                onClick = {
                    if (isCodeMode) {
                        viewModel.loginWithCode(phone, code)
                    } else {
                        viewModel.loginWithPassword(phone, password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = if (isCodeMode) {
                    phone.length == 11 && code.length == 6 && !isLoading
                } else {
                    phone.length == 11 && password.isNotBlank() && !isLoading
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isCodeMode) "登录 / 注册" else "登录",
                        fontSize = 16.sp
                    )
                }
            }

            // 验证码模式提示 + 切换链接
            if (isCodeMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "未注册的手机号验证后将自动创建账号",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = {
                    isCodeMode = false
                    viewModel.resetError()
                }) {
                    Text(
                        "密码登录",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(onClick = {
                    isCodeMode = true
                    viewModel.resetError()
                }) {
                    Text("验证码登录", color = MaterialTheme.colorScheme.primary)
                }
            }

            // 游客模式
            TextButton(
                onClick = {
                    navController.navigate(Routes.SOCIAL) {
                        popUpTo(Routes.QA) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "暂不登录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
