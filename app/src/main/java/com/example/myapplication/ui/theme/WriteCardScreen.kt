package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.myapplication.MainActivity

@Composable
fun WriteCardScreen(
    onWriteText: (String) -> Unit,
    onWriteUrl: (String) -> Unit,
    onWriteWifi: () -> Unit,
    currentMode: MainActivity.WriteMode,
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
        val (title, formatHint, input, buttonRow, backBtn, writeURI, writeText, writeWiFi) = createRefs()

        // 标题
        Text(
            text = "写入 NFC 标签",
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
            modifier = Modifier
                .constrainAs(writeURI) {
                    top.linkTo(input.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Text(
                text = "写入网址",
                modifier = Modifier
                    .wrapContentSize(Alignment.Center)
                    .align(alignment = Alignment.CenterVertically)
            )
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
            modifier = Modifier
                .constrainAs(writeText) {
                    top.linkTo(writeURI.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Text("写入文本")
        }

        // 写入 Wi-Fi 按钮
        Button(
            onClick = {
                onWriteWifi()
            },
            modifier = Modifier
                .constrainAs(writeWiFi) {
                    top.linkTo(writeText.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            Text("写入 Wi-Fi")
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

@Preview(showBackground = true)
@Composable
fun PreviewWriteCardScreen() {
    MyApplicationTheme {
        WriteCardScreen(
            onWriteText = {},
            onWriteUrl = {},
            onWriteWifi = {},
            currentMode = MainActivity.WriteMode.IDLE,
            inputText = ""
        )
    }
}

