package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.myapplication.R
import com.example.myapplication.WriteCard
import kotlinx.coroutines.launch
import com.example.myapplication.util.checkNfcAvailability

@Composable
fun WriteCardScreen(
    onWriteText: (String) -> Unit,
    onWriteUrl: (String) -> Unit,
    onWriteWifi: () -> Unit,
    onBackToRead: () -> Unit,
    currentMode: WriteCard.WriteMode,
    inputText: String
) {
    var text by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val (title, formatHint, input, buttonRow, backBtn) = createRefs()

        // 标题
        Text(
            text = "写入NFC标签",
            fontSize = 20.sp,
            modifier = Modifier
                .constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(24.dp)
        )

        // 输入框
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("输入网址或文本内容") },
            modifier = Modifier
                .constrainAs(input) {
                    top.linkTo(title.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(16.dp)
                .fillMaxWidth(),
            minLines = 3
        )

        // 按钮行
        Row(
            modifier = Modifier
                .constrainAs(buttonRow) {
                    top.linkTo(input.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 写入网址按钮
            Button(
                onClick = {
                    if (text.isEmpty()) {
                        dialogMessage = "请输入网址内容"
                        showDialog = true
                    } else {
                        onWriteUrl(text)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("写入网址")
            }

            // 写入文本按钮
            Button(
                onClick = {
                    if (text.isEmpty()) {
                        dialogMessage = "请输入文本内容"
                        showDialog = true
                    } else {
                        onWriteText(text)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("写入文本")
            }

            // 写入 Wi-Fi 按钮
            Button(
                onClick = {
                    onWriteWifi()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("写入 Wi-Fi")
            }
        }

        // 返回按钮
        Button(
            onClick = onBackToRead,
            modifier = Modifier
                .constrainAs(backBtn) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("返回读卡")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("提示") },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@Preview
@Composable
fun PreviewWriteCardScreen() {
    MyApplicationTheme {
        WriteCardScreen(
            onWriteText = {},
            onWriteUrl = {},
            onWriteWifi = {},
            onBackToRead = {},
            currentMode = WriteCard.WriteMode.IDLE,
            inputText = ""
        )
    }
}

