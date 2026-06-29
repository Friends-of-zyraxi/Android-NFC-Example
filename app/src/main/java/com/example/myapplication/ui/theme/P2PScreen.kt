@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

enum class ConnectionState {
    DISCONNECTED, ADVERTISING, DISCOVERING, CONNECTING, CONNECTED
}

@Composable
fun P2PScreen(
    isNfcEnabled: Boolean,
    connectionState: ConnectionState,
    receivedNearbyMessage: String,
    onMessageChange: (String) -> Unit,
    onStartAdvertising: () -> Unit,
    onStopAdvertising: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onDisconnect: () -> Unit,
    onSendMessage: (String) -> Unit,
    onEnableNfc: () -> Unit,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    // 数据类型 & 输入 —— 仅在已连接后使用
    var selectedType by remember { mutableStateOf(WriteDataType.TEXT) }
    var textInput by remember { mutableStateOf("") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiEncryption by remember { mutableStateOf(WifiEncryption.WPA2_AES) }
    var wifiAuth by remember { mutableStateOf(WifiAuth.WPA2_PSK) }
    var btMac by remember { mutableStateOf("") }
    var btName by remember { mutableStateOf("") }

    LaunchedEffect(selectedType) {
        textInput = ""; wifiSsid = ""; wifiPassword = ""; btMac = ""; btName = ""
    }

    fun buildFormattedMessage(): String = when (selectedType) {
        WriteDataType.TEXT -> "TEXT:$textInput"
        WriteDataType.URL -> "URL:$textInput"
        WriteDataType.WIFI -> "WIFI:$wifiSsid|$wifiPassword|${wifiEncryption.wscValue}|${wifiAuth.wscValue}"
        WriteDataType.BLUETOOTH -> "BT:$btMac|$btName"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        if (!hasPermissions) {
            // ---- 无权限提示（居中） ----
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.p2p_text_permission_required),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRequestPermissions) {
                            Text(stringResource(R.string.button_grant_permission))
                        }
                    }
                }
            }
        } else {
            // ---- 正常 P2P 界面 ----
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = stringResource(R.string.p2p_title),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // NFC 状态检查
                if (!isNfcEnabled) {
                    Text(
                        text = stringResource(R.string.p2p_text_nfc_disabled),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = onEnableNfc) {
                        Text(stringResource(R.string.p2p_button_enable_nfc))
                    }
                } else {
                    // ========================================================
                    // 根据连接状态展示不同内容
                    // ========================================================
                    when (connectionState) {
                        ConnectionState.DISCONNECTED -> {
                            Text(
                                text = stringResource(R.string.p2p_label_select_mode),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(onClick = onStartDiscovery) {
                                    Text(stringResource(R.string.p2p_button_discover))
                                }
                                Button(onClick = onStartAdvertising) {
                                    Text(stringResource(R.string.p2p_button_advertise))
                                }
                            }
                        }

                        ConnectionState.ADVERTISING -> {
                            Spacer(modifier = Modifier.height(32.dp))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.p2p_text_waiting_reader),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onStopAdvertising) {
                                Text(stringResource(R.string.p2p_button_stop_advertise))
                            }
                        }

                        ConnectionState.DISCOVERING -> {
                            Spacer(modifier = Modifier.height(32.dp))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.p2p_text_approach_device),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onStopDiscovery) {
                                Text(stringResource(R.string.p2p_button_stop_discovery))
                            }
                        }

                        ConnectionState.CONNECTING -> {
                            Spacer(modifier = Modifier.height(32.dp))
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.p2p_text_connecting),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }

                        ConnectionState.CONNECTED -> {
                            Text(
                                text = stringResource(R.string.p2p_text_connected),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // ---- 接收消息区域 ----
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = stringResource(R.string.p2p_label_received),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (receivedNearbyMessage.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.p2p_placeholder_waiting),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        ParsedMessageDisplay(receivedNearbyMessage)
                                    }
                                }
                            }

                            // ---- 类型选择 ----
                            var typeExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth(0.85f)) {
                                OutlinedButton(
                                    onClick = { typeExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.format_type_label, stringResource(selectedType.labelResId)))
                                }
                                DropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false }
                                ) {
                                    WriteDataType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(type.labelResId)) },
                                            onClick = { selectedType = type; typeExpanded = false }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ---- 动态输入区 ----
                            when (selectedType) {
                                WriteDataType.TEXT -> {
                                    OutlinedTextField(
                                        value = textInput,
                                        onValueChange = { textInput = it },
                                        label = { Text(stringResource(R.string.label_text_content)) },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        minLines = 2
                                    )
                                }
                                WriteDataType.URL -> {
                                    OutlinedTextField(
                                        value = textInput,
                                        onValueChange = { textInput = it },
                                        label = { Text(stringResource(R.string.label_url_input)) },
                                        placeholder = { Text(stringResource(R.string.placeholder_url_example)) },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        singleLine = true
                                    )
                                }
                                WriteDataType.WIFI -> {
                                    OutlinedTextField(
                                        value = wifiSsid,
                                        onValueChange = { wifiSsid = it },
                                        label = { Text(stringResource(R.string.label_wifi_ssid)) },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = wifiPassword,
                                        onValueChange = { wifiPassword = it },
                                        label = { Text(stringResource(R.string.label_wifi_password)) },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    var encExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = encExpanded,
                                        onExpandedChange = { encExpanded = it },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        OutlinedTextField(
                                            value = stringResource(wifiEncryption.displayResId),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.label_encryption_type)) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = encExpanded) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = encExpanded,
                                            onDismissRequest = { encExpanded = false }
                                        ) {
                                            WifiEncryption.entries.forEach { enc ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(enc.displayResId)) },
                                                    onClick = { wifiEncryption = enc; encExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    var authExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = authExpanded,
                                        onExpandedChange = { authExpanded = it },
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        OutlinedTextField(
                                            value = stringResource(wifiAuth.displayResId),
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(stringResource(R.string.label_auth_type)) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authExpanded) },
                                            modifier = Modifier.fillMaxWidth().menuAnchor()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = authExpanded,
                                            onDismissRequest = { authExpanded = false }
                                        ) {
                                            WifiAuth.entries.forEach { auth ->
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(auth.displayResId)) },
                                                    onClick = { wifiAuth = auth; authExpanded = false }
                                                )
                                            }
                                        }
                                    }
                                }
                                WriteDataType.BLUETOOTH -> {
                                    OutlinedTextField(
                                        value = btMac,
                                        onValueChange = { btMac = it },
                                        label = { Text(stringResource(R.string.label_bt_mac)) },
                                        placeholder = { Text(stringResource(R.string.placeholder_bt_mac)) },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = btName,
                                        onValueChange = { btName = it },
                                        label = { Text(stringResource(R.string.label_bt_name)) },
                                        placeholder = { Text(stringResource(R.string.placeholder_bt_name)) },
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ---- 发送按钮 ----
                            val canSend = when (selectedType) {
                                WriteDataType.TEXT, WriteDataType.URL -> textInput.isNotEmpty()
                                WriteDataType.WIFI -> wifiSsid.isNotEmpty()
                                WriteDataType.BLUETOOTH -> btMac.isNotEmpty()
                            }
                            Button(
                                onClick = {
                                    val formatted = buildFormattedMessage()
                                    onMessageChange(formatted)
                                    onSendMessage(formatted)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                enabled = canSend
                            ) {
                                Text(stringResource(R.string.p2p_button_send))
                            }

                            // ---- 断开连接 ----
                            OutlinedButton(
                                onClick = onDisconnect,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.p2p_button_disconnect))
                            }
                        }
                    }
                }
            }
        }
    }
}


// =======================================================================
// 解析并显示带前缀的消息
// =======================================================================
@Composable
private fun ParsedMessageDisplay(message: String) {
    when {
        message.startsWith("TEXT:") -> Text(stringResource(R.string.format_record_text, message.removePrefix("TEXT:")))
        message.startsWith("URL:") -> Text(stringResource(R.string.format_record_uri, message.removePrefix("URL:")))
        message.startsWith("WIFI:") -> {
            val parts = message.removePrefix("WIFI:").split("|")
            if (parts.size >= 4) {
                val encName = WifiEncryption.entries.find { it.wscValue == (parts[2].toIntOrNull() ?: 0) }?.displayResId
                val authName = WifiAuth.entries.find { it.wscValue == (parts[3].toIntOrNull() ?: 0) }?.displayResId
                Column {
                    Text(stringResource(R.string.format_ssid, parts[0]))
                    if (parts[1].isNotEmpty()) Text(stringResource(R.string.format_wifi_password, parts[1]))
                    if (encName != null) Text(stringResource(R.string.format_encryption_type, stringResource(encName)))
                    if (authName != null) Text(stringResource(R.string.format_auth_type, stringResource(authName)))
                }
            } else Text(message)
        }
        message.startsWith("BT:") -> {
            val parts = message.removePrefix("BT:").split("|")
            if (parts.size >= 2) {
                Column {
                    Text(stringResource(R.string.format_bt_mac, parts[0]))
                    if (parts[1].isNotEmpty()) Text(stringResource(R.string.format_bt_device_name, parts[1]))
                }
            } else Text(message)
        }
        else -> Text(message)
    }
}
