package com.permissionx.animalguide.ui.social.detail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.permissionx.animalguide.domain.model.social.Comment

@Composable
fun CommentInputBar(
    replyTo: Comment?,
    isSubmitting: Boolean,
    focusRequester: FocusRequester,
    onSubmit: (String) -> Unit,
    onCancelReply: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    // 切换回复对象时清空输入框
    LaunchedEffect(replyTo) {
        text = ""
        if (replyTo != null) focusRequester.requestFocus()
    }

    Column {
        // 回复提示
        if (replyTo != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "回复 @${replyTo.nickname}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onCancelReply,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("取消", fontSize = 12.sp)
                    }
                }
            }
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        text = if (replyTo != null) "回复 @${replyTo.nickname}..."
                        else "说点什么...",
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank() && !isSubmitting) {
                            onSubmit(text)
                            text = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (text.isNotBlank() && !isSubmitting) {
                        onSubmit(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank() && !isSubmitting,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("发送", fontSize = 14.sp)
                }
            }
        }
    }
}