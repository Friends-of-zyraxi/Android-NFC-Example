@file:OptIn(ExperimentalStdlibApi::class)

package com.example.myapplication

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*

import com.example.myapplication.ui.theme.BottomNavigationApp
import com.example.myapplication.ui.theme.ConnectionState
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.NFCReaderScreen
import com.example.myapplication.ui.theme.P2PScreen
import com.example.myapplication.ui.theme.WriteCardScreen
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

class MainActivity : ComponentActivity() {

    // NFC 相关
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    // Nearby Connections 相关
    private lateinit var connectionsClient: ConnectionsClient
    private val serviceId = "com.example.myapplication.P2P"

    // 读卡 UI 状态 (来自 ReadCard.kt)
    private var tagInfo by mutableStateOf("")
    private var tagContent by mutableStateOf("")
    private var isReaderButtonVisible by mutableStateOf(true) // 为了避免冲突重命名

    // 写卡 UI 状态 (来自 WriteCard.kt)
    enum class WriteMode { IDLE, TEXT, URL }
    private var currentWriteMode by mutableStateOf(WriteMode.IDLE)
    private var inputText by mutableStateOf("")
    private var snackbarHostState: SnackbarHostState? = null

    // P2P通信相关状态变量
    private var p2pConnectionState by mutableStateOf(ConnectionState.DISCONNECTED)
    private var receivedNearbyMessage by mutableStateOf("")
    private var messageToSend by mutableStateOf("")
    private var currentEndpointId: String? = null

    // 常量 (来自 ReadCard.kt)
    companion object {
        const val TAG = "NFC_DEMO"
        val URI_PREFIX_MAP = mapOf(
            0x00.toByte() to "", 0x01.toByte() to "http://www.", 0x02.toByte() to "https://www.",
            0x03.toByte() to "http://", 0x04.toByte() to "https://", 0x05.toByte() to "tel:",
            0x06.toByte() to "mailto:", 0x07.toByte() to "ftp://anonymous:anonymous@",
            0x08.toByte() to "ftp://ftp.", 0x09.toByte() to "ftps://", 0x0A.toByte() to "sftp://",
            0x0B.toByte() to "smb://", 0x0C.toByte() to "nfs://", 0x0D.toByte() to "ftp://",
            0x0E.toByte() to "dav://", 0x0F.toByte() to "news:", 0x10.toByte() to "telnet://",
            0x11.toByte() to "imap:", 0x12.toByte() to "rtsp://", 0x13.toByte() to "urn:",
            0x14.toByte() to "pop:", 0x15.toByte() to "sip:", 0x16.toByte() to "sips:",
            0x17.toByte() to "tftp:", 0x18.toByte() to "btspp://", 0x19.toByte() to "btl2cap://",
            0x1A.toByte() to "btgoep://", 0x1B.toByte() to "tcpobex://", 0x1C.toByte() to "irdaobex://",
            0x1D.toByte() to "file://", 0x1E.toByte() to "urn:epc:id:", 0x1F.toByte() to "urn:epc:tag:",
            0x20.toByte() to "urn:epc:pat:", 0x21.toByte() to "urn:epc:raw:", 0x22.toByte() to "urn:epc:",
            0x23.toByte() to "urn:nfc:"
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 Nearby Connections 客户端
        connectionsClient = Nearby.getConnectionsClient(this)

        // 初始化 NFC 适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // 创建PendingIntent
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )

        // 设置Intent过滤器 (来自 ReadCard.kt)
        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Failed to add MIME type", e)
            }
        }
        @Suppress("DEPRECATION")
        intentFiltersArray = arrayOf(
            ndefFilter,
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        // 支持的所有NFC技术类型
        techListsArray = arrayOf(
            arrayOf(
                NfcA::class.java.name, NfcB::class.java.name, NfcF::class.java.name,
                NfcV::class.java.name, IsoDep::class.java.name, MifareClassic::class.java.name,
                MifareUltralight::class.java.name, Ndef::class.java.name, NdefFormatable::class.java.name
            )
        )

        setContent {
            MyApplicationTheme {
                val coroutineScope = rememberCoroutineScope()
                snackbarHostState = remember { SnackbarHostState() } // 设置全局状态
                BottomNavigationApp(
                    readerScreen = {
                        NFCReaderScreen(
                            tagInfo = tagInfo,
                            tagContent = tagContent,
                            isButtonVisible = isReaderButtonVisible,
                            snackbarHostState = snackbarHostState!!,
                            onCheckNfcClick = {
                                checkNfcAvailability { messageRes, actionRes, action ->
                                    coroutineScope.launch {
                                        val result = snackbarHostState!!.showSnackbar(
                                            message = getString(messageRes),
                                            actionLabel = if (actionRes != 0) getString(actionRes) else null
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            action?.invoke()
                                        }
                                    }
                                }
                            }
                        )
                    },
                    writeScreen = {
                        WriteCardScreen(
                            onWriteText = {
                                currentWriteMode = WriteMode.TEXT
                                inputText = it
                                showToast("请将卡靠近设备背面进行写入...")
                            },
                            onWriteUrl = {
                                currentWriteMode = WriteMode.URL
                                inputText = it
                                showToast("请将卡靠近设备背面进行写入...")
                            },
                            onWriteWifi = {
                                // 启动独立的写入 WiFi 配置 Activity (WriteWiFi.kt)
                                startActivity(Intent(this@MainActivity, WriteWiFi::class.java))
                            },
                            currentMode = currentWriteMode,
                            inputText = inputText
                        )
                    },
                    p2pScreen = {
                        P2PScreen(
                            isNfcEnabled = nfcAdapter?.isEnabled == true,
                            connectionState = p2pConnectionState,
                            receivedNearbyMessage = receivedNearbyMessage,
                            messageToSend = messageToSend,
                            onMessageChange = { messageToSend = it },
                            onStartAdvertising = { startAdvertising() },
                            onStopAdvertising = { stopAdvertising() },
                            onStartDiscovery = { startDiscovery() },
                            onSendMessage = { message ->
                                if (currentEndpointId != null && p2pConnectionState == ConnectionState.CONNECTED) {
                                    sendNearbyMessage(currentEndpointId!!, message)
                                } else {
                                    showToast("未连接到任何设备")
                                }
                            },
                            onEnableNfc = {
                                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                            }
                        )
                    }
                )
            }
        }
    }

    // =======================================================================
    // NFC 读卡（来自 ReadCard.kt）
    // =======================================================================

    private fun checkNfcAvailability(
        showMessage: (Int, Int, (() -> Unit)?) -> Unit
    ): Boolean {
        return when {
            nfcAdapter == null -> {
                showMessage(R.string.NFCNA, R.string.exit) { finish() }
                false
            }
            !nfcAdapter!!.isEnabled -> {
                showMessage(R.string.enable_NFC, R.string.gotoSettings) {
                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
                false
            }
            else -> {
                showMessage(R.string.NFCSP, 0, null)
                isReaderButtonVisible = false
                true
            }
        }
    }

    private fun handleNfcIntent(intent: Intent) {
        // P2P/写卡模式下的处理
        if (currentWriteMode != WriteMode.IDLE) {
            handleWriteIntent(intent)
            return
        }

        // 读卡模式下的处理 (来自 ReadCard.kt)
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            tagContent = "设备不支持NFC"
            return
        }

        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)

        if (tag == null) {
            tagContent = "未发现NFC标签"
            return
        }

        tagInfo = getString(R.string.scannedTag, tag.toString())
        Log.d(TAG, "Intent Action: ${intent.action}")

        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        if (rawMessages != null) {
            val messages = rawMessages.toList()
            tagContent = parseNdefMessages(messages)
        } else {
            // 如果标签没有 NDEF 消息，但可以识别为特定技术类型，也可以处理
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    val message = ndef.ndefMessage
                    tagContent = message?.let { parseNdefMessages(listOf(it)) }
                        ?: getString(R.string.notSupport)
                    ndef.close()
                } catch (e: Exception) {
                    Log.e(TAG, "读取NDEF失败", e)
                    tagContent = "读取标签失败"
                }
            } else {
                tagContent = getString(R.string.notSupport)
            }
        }
    }

    private fun parseNdefMessages(messages: List<NdefMessage>): String {
        val result = StringBuilder()

        messages.forEach { message ->
            message.records.forEach { record ->
                result.append(when (record.tnf) {
                    NdefRecord.TNF_WELL_KNOWN -> parseWellKnownRecord(record)
                    NdefRecord.TNF_MIME_MEDIA -> parseMimeRecord(record)
                    NdefRecord.TNF_EXTERNAL_TYPE -> parseExternalRecord(record)
                    else -> "未知类型: ${record.payload.toHexString()}\n"
                })
            }
        }

        return result.toString().trim()
    }

    private fun parseWellKnownRecord(record: NdefRecord): String {
        return when {
            record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                "文本: ${parseTextRecord(record)}\n"
            }
            record.type.contentEquals(NdefRecord.RTD_URI) -> {
                "URI: ${parseUriRecord(record)}\n"
            }
            else -> "未知Well Known类型\n"
        }
    }

    private fun parseMimeRecord(record: NdefRecord): String {
        return when (record.toMimeType()) {
            "application/vnd.wfa.wsc" -> {
                val wifiPayload = record.payload
                val wifiInfo = parseWifiRecord(wifiPayload)
                "WiFi配置:\n$wifiInfo"
            }
            "application/vnd.bluetooth.ep.oob" -> "蓝牙配置:\n${parseBluetoothRecord(record)}"
            else -> "MIME类型: ${record.toMimeType()}\n内容: ${record.payload.toHexString()}\n"
        }
    }

    private fun parseExternalRecord(record: NdefRecord): String {
        return when (String(record.type)) {
            "android.com:pkg" -> "应用: ${parseApplicationRecord(record)}\n"
            else -> "外部类型: ${String(record.type)}\n"
        }
    }

    private fun parseTextRecord(record: NdefRecord): String {
        return try {
            val payload = record.payload
            val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"
            val languageCodeLength = payload[0].toInt() and 0x3F
            String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1,
                Charset.forName(textEncoding))
        } catch (e: Exception) {
            Log.w(TAG, "解析文本记录失败", e)
            "解析错误"
        }
    }

    private fun parseUriRecord(record: NdefRecord): String {
        val prefix = URI_PREFIX_MAP[record.payload[0]] ?: ""
        return try {
            prefix + String(record.payload, 1, record.payload.size - 1, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "解析URI记录失败", e)
            "无效URI"
        }
    }

    fun parseWifiRecord(payload: ByteArray): String {
        val sb = StringBuilder()
        var index = 0
        while (index + 4 <= payload.size) {
            val type = ((payload[index].toInt() and 0xFF) shl 8) or (payload[index + 1].toInt() and 0xFF)
            val length = ((payload[index + 2].toInt() and 0xFF) shl 8) or (payload[index + 3].toInt() and 0xFF)
            index += 4
            if (index + length > payload.size) break

            val data = payload.copyOfRange(index, index + length)
            index += length

            when (type) {
                0x1045 -> sb.appendLine("SSID: ${String(data)}")
                0x1027 -> sb.appendLine("密码: ${String(data)}")
                0x1003 -> {
                    if (data.size >= 2) {
                        val encryptionType = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        sb.appendLine("加密类型: ${getEncryptionTypeName(encryptionType)}")
                    } else {
                        sb.appendLine("加密类型: 数据不足")
                    }
                }
                0x100F -> {
                    if (data.size >= 2) {
                        val authType = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                        sb.appendLine("身份验证类型: ${getAuthTypeName(authType)}")
                    } else {
                        sb.appendLine("身份验证类型: 数据不足")
                    }
                }
                0x1020 -> sb.appendLine("MAC地址: ${data.toMacAddress()}")
                0x1026 -> {
                    val netTypeName = when (val netType = data[0].toInt() and 0xFF) {
                        0x00 -> "未知"
                        0x01 -> "基础设施"
                        0x02 -> "独立"
                        else -> "保留/自定义（0x${netType.toString(16)})"
                    }
                    sb.appendLine("网络类型: $netTypeName")
                }
                0x100E -> {
                    sb.append(parseWifiRecord(data))
                }
                else -> sb.appendLine("未知字段: 0x${type.toString(16)} 数据: ${data.toHexString()}")
            }
        }
        return sb.toString()
    }

    private fun parseBluetoothRecord(record: NdefRecord): String {
        val bytes = record.payload
        val hexDump = bytes.joinToString(" ") { "%02X".format(it) }

        if (record.toUri()?.scheme?.startsWith("bt") == true) {
            val uri = record.toUri()!!
            val mac = uri.host ?: "未知"
            val name = uri.path?.substringAfter("/")?.trim().orEmpty()
            return "MAC: $mac\n名称: $name\n原始字节: $hexDump"
        } else {
            val bytes = record.payload
            Log.d("BluetoothTag", "原始数据: ${bytes.joinToString(" ") { "%02X".format(it) }}")

            if (bytes.size < 8) return "无效蓝牙数据（长度不足）"
            val mac = bytes.copyOfRange(2, 8).reversed().joinToString(":") { "%02X".format(it) }
            val name = if (bytes.size > 8) {
                String(bytes, 8, bytes.size - 8, Charsets.UTF_8).trim()
            } else {
                ""
            }
            return "MAC: $mac\n名称: $name"
        }
    }


    private fun parseApplicationRecord(record: NdefRecord): String {
        return try {
            String(record.payload, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "解析应用记录失败", e)
            "未知应用"
        }
    }

    // 扩展函数 (来自 ReadCard.kt)
    private fun ByteArray.toMacAddress(): String = joinToString(":") { "%02X".format(it) }
    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    private fun getAuthTypeName(value: Int): String = when(value) {
        0x0001 -> "Open System"
        0x0002 -> "WPA-PSK"
        0x0004 -> "Shared Key"
        0x0008 -> "WPA-EAP"
        0x0010 -> "WPA2-EAP"
        0x0020 -> "WPA2-PSK"
        0x0040 -> "WPA3-SAE"
        else -> "未知（0x%04X）".format(value)
    }

    private fun getEncryptionTypeName(value: Int): String = when(value) {
        0x0001 -> "无"
        0x0002, 0x0022 -> "WEP"
        0x0004 -> "TKIP"
        0x0008, 0x0020 -> "AES"
        0x0010 -> "AES/TKIP"
        else -> "未知（0x%04X）".format(value)
    }


    // =======================================================================
    // NFC 写卡（来自 WriteCard.kt）
    // =======================================================================

    private fun handleWriteIntent(intent: Intent) {
        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java) ?: return

        if (currentWriteMode == WriteMode.IDLE) return // 非写入模式不处理

        try {
            val message: NdefMessage = when (currentWriteMode) {
                WriteMode.TEXT -> createTextNdefMessage(inputText)
                WriteMode.URL -> createUriNdefMessage(inputText)
                WriteMode.IDLE -> return
            }

            if (isNdefCompatible(tag)) {
                writeNdefMessage(tag, message)
            } else if (NdefFormatable.get(tag) != null) {
                formatAndWrite(tag, message)
            } else {
                throw IOException("该标签不支持 NDEF 或无法格式化")
            }
            showToast("写入成功")
        } catch(e: Exception) {
            showToast("写入失败: ${e.message}")
            Log.e("NFC", "写入错误", e)
        } finally {
            currentWriteMode = WriteMode.IDLE
            inputText = ""
        }
    }

    private fun createTextNdefMessage(text: String): NdefMessage {
        val lang = Locale.getDefault().language.toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val payload = ByteArray(lang.size + 1 + textBytes.size)
        payload[0] = lang.size.toByte() // 状态字节：UTF-8 且语言代码长度
        System.arraycopy(lang, 0, payload, 1, lang.size)
        System.arraycopy(textBytes, 0, payload, 1 + lang.size, textBytes.size)
        val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
        return NdefMessage(record)
    }

    private fun createUriNdefMessage(uri: String): NdefMessage {
        val record = NdefRecord.createUri(uri)
        return NdefMessage(record)
    }

    private fun isNdefCompatible(tag: Tag): Boolean {
        return Ndef.get(tag) != null || NdefFormatable.get(tag) != null
    }

    private fun formatAndWrite(tag: Tag, message: NdefMessage) {
        NdefFormatable.get(tag)?.use { formatable ->
            formatable.connect()
            formatable.format(message)
        } ?: throw IOException("无法格式化标签")
    }

    private fun writeNdefMessage(tag: Tag, message: NdefMessage) {
        Ndef.get(tag)?.use { ndef ->
            ndef.connect()
            if (!ndef.isWritable) throw IOException("标签不可写")
            if (ndef.maxSize < message.toByteArray().size) throw IOException("内容超出标签容量")
            ndef.writeNdefMessage(message)
        } ?: throw IOException("标签不支持NDEF写入")
    }

    // =======================================================================
    // Nearby Connections P2P (来自 MainActivity.kt 原始代码和 P2PCommunication.kt)
    // =======================================================================

    // （此处省略原 MainActivity 中的 Nearby Connections 相关函数，
    //  如 connectionLifecycleCallback, payloadCallback, startAdvertising,
    //  stopAdvertising, startDiscovery, sendNearbyMessage, requestNearbyConnection
    //  这些函数保持不变）
    // ...
    // Nearby Connections 核心回调
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d("Nearby", "连接被发起: $endpointId")
            p2pConnectionState = ConnectionState.CONNECTING
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            showToast("接受连接请求中...")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d("Nearby", "连接成功: $endpointId")
                    p2pConnectionState = ConnectionState.CONNECTED
                    currentEndpointId = endpointId
                    showToast("连接成功！可以开始传输数据。")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d("Nearby", "连接被拒绝: $endpointId")
                    p2pConnectionState = ConnectionState.DISCONNECTED
                    currentEndpointId = null
                    showToast("连接被拒绝。")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.d("Nearby", "连接错误: $endpointId")
                    p2pConnectionState = ConnectionState.DISCONNECTED
                    currentEndpointId = null
                    showToast("连接失败。")
                }
                else -> {
                    Log.d("Nearby", "连接结果未知: ${result.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d("Nearby", "连接断开: $endpointId")
            p2pConnectionState = ConnectionState.DISCONNECTED
            currentEndpointId = null
            showToast("连接已断开。")
            // 重新开始广告或发现，以备下次连接
            startAdvertising()
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val message = String(bytes, StandardCharsets.UTF_8)
                receivedNearbyMessage = message
                Log.d("Nearby", "收到消息: $message")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // 您可以在此处理传输进度，例如显示进度条
            Log.d("Nearby", "数据传输更新: $update")
        }
    }

    // 启动广告模式
    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            "P2P Device",
            serviceId,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            p2pConnectionState = ConnectionState.ADVERTISING
            showToast("Nearby广告已启动，等待读取器发现...")
        }.addOnFailureListener { e ->
            p2pConnectionState = ConnectionState.DISCONNECTED
            showToast("广告启动失败: ${e.message}")
        }
    }

    // 停止广告模式
    private fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        p2pConnectionState = ConnectionState.DISCONNECTED
        showToast("广告已停止")
    }

    // 启动发现模式
    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(
            serviceId,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    Log.d("Nearby", "发现端点: $endpointId")
                    // 在这里，您应该立即发起连接，因为您没有依赖NFC握手
                    requestNearbyConnection(endpointId)
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d("Nearby", "丢失端点: $endpointId")
                }
            },
            discoveryOptions
        ).addOnSuccessListener {
            p2pConnectionState = ConnectionState.DISCOVERING
            showToast("Nearby发现已启动，正在扫描附近设备...")
        }.addOnFailureListener { e ->
            p2pConnectionState = ConnectionState.DISCONNECTED
            showToast("发现启动失败: ${e.message}")
        }
    }

    // 发送Nearby Connections消息
    private fun sendNearbyMessage(endpointId: String, message: String) {
        val payload = Payload.fromBytes(message.toByteArray(StandardCharsets.UTF_8))
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                showToast("消息发送成功")
                Log.d("Nearby", "消息发送成功")
            }
            .addOnFailureListener { e ->
                showToast("消息发送失败: ${e.message}")
                Log.e("Nearby", "消息发送失败", e)
            }
    }

    // 发起Nearby Connections连接请求
    private fun requestNearbyConnection(endpointId: String) {
        val endpointName = "P2P Device" // 随便取个名字
        connectionsClient.requestConnection(
            endpointName,
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            showToast("已发送连接请求，等待对方接受")
        }.addOnFailureListener { e ->
            p2pConnectionState = ConnectionState.DISCONNECTED
            showToast("连接请求失败: ${e.message}")
        }
    }


    // =======================================================================
    // Activity 生命周期
    // =======================================================================

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保应用销毁时停止所有Nearby活动
        connectionsClient.stopAllEndpoints()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 根据当前模式决定是读取还是写入
        if (currentWriteMode != WriteMode.IDLE) {
            handleWriteIntent(intent)
        } else {
            handleNfcIntent(intent)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// 注意：MyHostApduService.kt 必须保持独立文件
// WriteWiFi.kt (Activity) 暂时保留，因为它启动了另一个界面
// P2PCommunication.kt 的 Compose UI 应该作为单独的 Composable 文件