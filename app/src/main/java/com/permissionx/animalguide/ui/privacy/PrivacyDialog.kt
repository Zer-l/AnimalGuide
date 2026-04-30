package com.permissionx.animalguide.ui.privacy

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class DocPage(val title: String, val webView: WebView?)

@Composable
fun PrivacyDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit,
    agreementWebView: WebView?,
    privacyWebView: WebView?
) {
    var docPage by remember { mutableStateOf<DocPage?>(null) }

    // 主弹窗
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "用户协议与隐私政策",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "欢迎使用晓物！我们非常重视您的隐私保护。\n" +
                                "在您使用本应用前，请认真阅读并同意以下内容：",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val linkColor = MaterialTheme.colorScheme.primary
                    val bodyText = buildAnnotatedString {
                        append("我们将依据")
                        pushStringAnnotation("PRIVACY", "privacy")
                        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                            append("《隐私政策》")
                        }
                        pop()
                        append("收集和使用您的必要信息，包括手机号、设备信息、位置信息及图片数据，用于提供识别、科普、社区等服务。\n")
                        append("为实现相关功能，我们接入了百度智能云（动物识别）、字节跳动豆包（AI科普）、腾讯云CloudBase（账号与社区）等第三方服务，详见")
                        pushStringAnnotation("PRIVACY", "privacy")
                        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                            append("《隐私政策》")
                        }
                        pop()
                        append("。\n")
                        append("点击「同意并继续」表示您已阅读并同意")
                        pushStringAnnotation("AGREEMENT", "agreement")
                        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                            append("《用户协议》")
                        }
                        pop()
                        append("与")
                        pushStringAnnotation("PRIVACY", "privacy")
                        withStyle(SpanStyle(color = linkColor, fontWeight = FontWeight.Medium)) {
                            append("《隐私政策》")
                        }
                        pop()
                        append("的全部内容。")
                    }

                    androidx.compose.foundation.text.ClickableText(
                        text = bodyText,
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = { offset ->
                            bodyText.getStringAnnotations("AGREEMENT", offset, offset)
                                .firstOrNull()?.let {
                                    docPage = DocPage("用户协议", agreementWebView)
                                }
                            bodyText.getStringAnnotations("PRIVACY", offset, offset)
                                .firstOrNull()?.let {
                                    docPage = DocPage("隐私政策", privacyWebView)
                                }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("拒绝")
                    }
                    Button(
                        onClick = onAgree,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("同意并继续")
                    }
                }
            }
        }
    }

    // 文档阅读弹窗（叠在主弹窗上层，直接使用预创建的 WebView）
    docPage?.let { page ->
        Dialog(
            onDismissRequest = { docPage = null },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        IconButton(onClick = { docPage = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                        Text(
                            text = page.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider()
                    if (page.webView != null) {
                        AndroidView(
                            factory = { page.webView },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }
}
