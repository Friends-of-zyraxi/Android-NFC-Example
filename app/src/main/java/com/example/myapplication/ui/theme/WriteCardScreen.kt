package com.example.myapplication.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.R

// =======================================================================
// 写入状态：由 MainActivity 驱动，控制弹窗显示
// =======================================================================
enum class WriteState {
    IDLE,              // 空闲，用户可以操作
    WAITING_FOR_CARD,  // 等待检测卡片
    WRITING,           // 正在写入
    SUCCESS,           // 写入成功（短暂显示后自动回到 IDLE）
    FAILED,            // 写入失败（短暂显示后自动回到 IDLE）
    EMULATING          // 正在卡模拟（等待读卡器读取）
}

// =======================================================================
// 写入数据类型
// =======================================================================
enum class WriteDataType(val labelResId: Int) {
    TEXT(R.string.data_type_text),
    URL(R.string.data_type_url),
    WIFI(R.string.data_type_wifi),
    BLUETOOTH(R.string.data_type_bluetooth)
}

// =======================================================================
// Wi-Fi 加密/认证枚举（与 WSC 规范兼容）
// =======================================================================
enum class WifiEncryption(val displayResId: Int, val wscValue: Int) {
    NONE(R.string.encryption_none, 0x0001),
    WEP(R.string.encryption_wep, 0x0002),
    WPA_TKIP(R.string.encryption_wpa_tkip, 0x0004),
    WPA2_AES(R.string.encryption_wpa2_aes, 0x0020),
    WPA3_SAE(R.string.encryption_wpa3_sae, 0x0040)
}

enum class WifiAuth(val displayResId: Int, val wscValue: Int) {
    OPEN(R.string.auth_open, 0x0001),
    WPA_PSK(R.string.auth_wpa_psk_short, 0x0002),
    WPA2_PSK(R.string.auth_wpa2_psk_short, 0x0020),
    WPA3_SAE(R.string.auth_wpa3_sae_short, 0x0040)
}


// =======================================================================
// 统一的写卡界面
// =======================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteCardScreen(
    writeState: WriteState,
    writeStatusMessage: String,
    onWriteText: (String) -> Unit,
    onWriteUrl: (String) -> Unit,
    onWriteWifi: (ssid: String, password: String, encryption: WifiEncryption, auth: WifiAuth) -> Unit,
    onWriteBluetooth: (mac: String, name: String) -> Unit,
    onStartEmulation: (
        type: WriteDataType, textInput: String,
        wifiSsid: String, wifiPassword: String,
        wifiEncryption: WifiEncryption, wifiAuth: WifiAuth,
        btMac: String, btName: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onCancelWrite: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var selectedType by remember { mutableStateOf(WriteDataType.TEXT) }

    // ----- 文本/网址 公共输入 -----
    var textInput by remember { mutableStateOf("") }

    // ----- Wi-Fi 表单 -----
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiEncryption by remember { mutableStateOf(WifiEncryption.WPA2_AES) }
    var wifiAuth by remember { mutableStateOf(WifiAuth.WPA2_PSK) }

    // ----- 蓝牙表单 -----
    var btMac by remember { mutableStateOf("") }
    var btName by remember { mutableStateOf("") }

    // 切换类型时清空输入
    LaunchedEffect(selectedType) {
        textInput = ""
        wifiSsid = ""
        wifiPassword = ""
        btMac = ""
        btName = ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- 标题 ----
            Text(
                text = stringResource(R.string.title_write_card),
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ---- 类型选择下拉框 ----
            var typeExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { typeExpanded = true },
                    modifier = Modifier.fillMaxWidth(0.85f)
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
                            onClick = {
                                selectedType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 动态输入区域 ----
            when (selectedType) {
                WriteDataType.TEXT -> {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text(stringResource(R.string.label_text_content)) },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.hint_text_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.hint_url_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = wifiPassword,
                        onValueChange = { wifiPassword = it },
                        label = { Text(stringResource(R.string.label_wifi_password)) },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

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
                                    onClick = {
                                        wifiEncryption = enc
                                        encExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

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
                                    onClick = {
                                        wifiAuth = auth
                                        authExpanded = false
                                    }
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
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = btName,
                        onValueChange = { btName = it },
                        label = { Text(stringResource(R.string.label_bt_name)) },
                        placeholder = { Text(stringResource(R.string.placeholder_bt_name)) },
                        modifier = Modifier.fillMaxWidth(0.85f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.hint_bt_format),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- 写入标签 + 卡模拟 按钮（并列） ----
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        when (selectedType) {
                            WriteDataType.TEXT -> onWriteText(textInput)
                            WriteDataType.URL -> onWriteUrl(textInput)
                            WriteDataType.WIFI -> onWriteWifi(wifiSsid, wifiPassword, wifiEncryption, wifiAuth)
                            WriteDataType.BLUETOOTH -> onWriteBluetooth(btMac, btName)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = writeState == WriteState.IDLE
                ) {
                    Text(stringResource(R.string.button_write_tag))
                }

                Button(
                    onClick = {
                        onStartEmulation(
                            selectedType, textInput,
                            wifiSsid, wifiPassword, wifiEncryption, wifiAuth,
                            btMac, btName
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = writeState == WriteState.IDLE
                ) {
                    Text(stringResource(R.string.button_card_emulation))
                }
            }
        }

        // ================================================================
        // 写入/模拟状态弹窗
        // ================================================================
        if (writeState != WriteState.IDLE) {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (writeState) {
                            WriteState.WAITING_FOR_CARD -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = writeStatusMessage.ifEmpty { stringResource(R.string.dialog_tap_card) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = onCancelWrite) {
                                    Text(stringResource(R.string.button_cancel))
                                }
                            }

                            WriteState.WRITING -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.dialog_writing),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }

                            WriteState.SUCCESS -> {
                                Text(
                                    text = writeStatusMessage.ifEmpty { stringResource(R.string.dialog_write_success) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }

                            WriteState.FAILED -> {
                                Text(
                                    text = writeStatusMessage.ifEmpty { stringResource(R.string.dialog_write_failed) },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }

                            WriteState.EMULATING -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.dialog_emulation_started),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.dialog_approach_reader),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(onClick = onCancelWrite) {
                                    Text(stringResource(R.string.dialog_button_stop_emulation))
                                }
                            }

                            WriteState.IDLE -> { /* unreachable */ }
                        }
                    }
                }
            }
        }
    }
}

// =======================================================================
// 预览
// =======================================================================
@Preview(showBackground = true)
@Composable
fun PreviewWriteCardScreen() {
    MyApplicationTheme {
        WriteCardScreen(
            writeState = WriteState.IDLE,
            writeStatusMessage = "",
            onWriteText = {},
            onWriteUrl = {},
            onWriteWifi = { _, _, _, _ -> },
            onWriteBluetooth = { _, _ -> }
        )
    }
}
