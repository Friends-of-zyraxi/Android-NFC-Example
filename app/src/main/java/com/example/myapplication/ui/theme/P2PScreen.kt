package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 定义一个枚举类来表示连接状态，让 UI 更好地响应不同阶段
enum class ConnectionState {
    DISCONNECTED,       // 初始状态，无连接
    ADVERTISING,        // 设备正在广告其 Nearby Endpoint ID (卡模拟模式)
    DISCOVERING,        // 设备正在发现其他设备 (读取器模式)
    CONNECTING,         // 接收到NFC数据，正在通过Nearby发起连接
    CONNECTED           // 已通过 Nearby Connections 建立连接
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2PScreen(
    isNfcEnabled: Boolean,
    connectionState: ConnectionState,
    receivedNearbyMessage: String,
    messageToSend: String,
    onMessageChange: (String) -> Unit,
    onStartAdvertising: () -> Unit,
    onStopAdvertising: () -> Unit,
    onStartDiscovery: () -> Unit,
    onSendMessage: (String) -> Unit,
    onEnableNfc: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "NFC 近距通信",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // NFC 状态检查
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
            // 根据连接状态展示不同的 UI
            when (connectionState) {
                ConnectionState.DISCONNECTED -> {
                    // 初始状态：让用户选择作为广告者还是发现者
                    Text(
                        text = "选择连接模式",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onStartDiscovery) {
                            Text("用户 1") // 发现者
                        }
                        Button(onClick = onStartAdvertising) {
                            Text("用户 2") // 广播者
                        }
                    }
                }
                ConnectionState.ADVERTISING -> {
                    // 广告中：显示等待连接的提示
                    Text(
                        text = "正在等待读取器扫描...",
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = onStopAdvertising) {
                        Text("停止广播")
                    }
                }
                ConnectionState.DISCOVERING -> {
                    // 发现中：显示正在扫描的提示
                    Text(
                        text = "请将设备靠近另一台设备...",
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                ConnectionState.CONNECTING -> {
                    // 连接中：显示正在建立 Nearby Connections
                    Text(
                        text = "已发现设备，正在建立近距连接...",
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                ConnectionState.CONNECTED -> {
                    // 已连接：显示发送和接收数据的 UI
                    Text(
                        text = "已建立近距连接",
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )

                    // 接收消息区域
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "接收到的消息", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = receivedNearbyMessage.ifEmpty { "等待接收消息..." },
                                modifier = Modifier.defaultMinSize(minHeight = 50.dp)
                            )
                        }
                    }

                    // 发送消息区域
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "发送消息", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = messageToSend,
                                onValueChange = onMessageChange,
                                label = { Text("输入要发送的消息") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = { onSendMessage(messageToSend) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = messageToSend.isNotEmpty()
                            ) {
                                Text("发送")
                            }
                        }
                    }
                }
            }
        }
    }
}

// 预览函数
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun P2PScreenPreview() {
    MyApplicationTheme {
        P2PScreen(
            isNfcEnabled = true,
            connectionState = ConnectionState.CONNECTED,
            receivedNearbyMessage = "接收到的消息",
            messageToSend = "要发送的消息",
            onMessageChange = {},
            onStartAdvertising = {},
            onStopAdvertising = {},
            onStartDiscovery = {},
            onSendMessage = {},
            onEnableNfc = {}
        )
    }
}