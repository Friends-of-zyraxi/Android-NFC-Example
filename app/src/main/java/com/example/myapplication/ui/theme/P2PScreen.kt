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
import com.example.myapplication.R
import kotlinx.coroutines.launch
import com.example.myapplication.util.checkNfcAvailability

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2PScreen(
    isNfcEnabled: Boolean,
    isBeamActive: Boolean,
    receivedMessage: String,
    messageToSend: String,
    onMessageChange: (String) -> Unit,
    onEnableBeam: () -> Unit,
    onDisableBeam: () -> Unit,
    onEnableNfc: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NFC P2P通信",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!isNfcEnabled) {
            Text(
                text = "NFC功能未启用",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(onClick = onEnableNfc) {
                Text("启用NFC")
            }
        } else {
            // 接收消息区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "接收到的消息",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = receivedMessage.ifEmpty { "等待接收消息..." },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }

            // 发送消息区域
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "发送消息",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = messageToSend,
                        onValueChange = onMessageChange,
                        label = { Text("输入要发送的消息") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onEnableBeam,
                            enabled = messageToSend.isNotEmpty() && !isBeamActive
                        ) {
                            Text("激活发送")
                        }

                        Button(
                            onClick = onDisableBeam,
                            enabled = isBeamActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("停止发送")
                        }
                    }
                }
            }

            // 使用说明
            Text(
                text = "使用说明：\n" +
                        "1. 输入消息并点击'激活发送'\n" +
                        "2. 将设备背靠背靠近另一台设备\n" +
                        "3. 触摸屏幕发送消息",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )

            if (isBeamActive) {
                Text(
                    text = "发送模式已激活 - 准备发送消息",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun P2PScreenPreview() {
    P2PScreen(
        isNfcEnabled = true,
        isBeamActive = true,
        receivedMessage = "收到的信息",
        messageToSend = "准备发送的信息",
        onMessageChange = {},
        onEnableBeam = {},
        onDisableBeam = {},
        onEnableNfc = {}
    )
}
