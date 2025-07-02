package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.myapplication.R

// 定义加密类型选项
enum class EncryptionType(val displayName: String) {
    NONE("无"),
    WEP("WEP"),
    WPA("WPA"),
    WPA2("WPA2"),
    WPA3("WPA3")
}

// 定义身份验证类型选项
enum class AuthType(val displayName: String) {
    NONE("无"),
    EAP("EAP"),
    PEAP("PEAP"),
    TLS("TLS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiWriteScreen(
    onWriteCardClick: () -> Unit
) {
    // 状态管理
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedEncryption by remember { mutableStateOf(EncryptionType.WPA2) }
    var selectedAuth by remember { mutableStateOf(AuthType.NONE) }

    // 下拉菜单状态
    var encryptionExpanded by remember { mutableStateOf(false) }
    var authExpanded by remember { mutableStateOf(false) }

    // 文本框宽度
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    Scaffold() { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Wi-Fi 配置信息",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Wi-Fi 名称输入框
            OutlinedTextField(
                value = ssid,
                onValueChange = { ssid = it },
                singleLine = true,
                label = { Text("Wi-Fi 名称（SSID）") },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // 密码输入框
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text("密码") },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // 加密类型下拉菜单
            ExposedDropdownMenuBox(
                expanded = encryptionExpanded,
                onExpandedChange = { encryptionExpanded = !encryptionExpanded },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                // 文本框
                OutlinedTextField(
                    value = selectedEncryption.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = encryptionExpanded)
                    },
                    label = { Text("加密类型") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        }
                )

                // 下拉菜单
                ExposedDropdownMenu(
                    expanded = encryptionExpanded,
                    onDismissRequest = { encryptionExpanded = false }
                ) {
                    EncryptionType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedEncryption = type
                                encryptionExpanded = false
                            }
                        )
                    }
                }
            }

            // 身份验证类型下拉菜单
            ExposedDropdownMenuBox(
                expanded = authExpanded,
                onExpandedChange = { authExpanded = !authExpanded },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                // 文本框
                OutlinedTextField(
                    value = selectedAuth.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = authExpanded)
                    },
                    label = { Text("身份验证类型") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                // 下拉菜单
                ExposedDropdownMenu(
                    expanded = authExpanded,
                    onDismissRequest = { authExpanded = false }
                ) {
                    AuthType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                selectedAuth = type
                                authExpanded = false
                            }
                        )
                    }
                }
            }

            // 确定按钮
            Button(
                onClick = {
                    // 这里可以添加验证逻辑
                    onWriteCardClick()
                },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = "确定")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WifiReaderScreenPreview() {
    WifiWriteScreen(
        onWriteCardClick = {}
    )
}