@file:OptIn(ExperimentalStdlibApi::class)
package com.example.myapplication
import com.example.myapplication.ui.theme.NavigationView
import com.example.myapplication.ui.theme.WifiWriteScreen
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.material.snackbar.Snackbar
import java.nio.charset.Charset
import androidx.compose.runtime.Composable
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.WriteCard.WriteMode
import com.example.myapplication.ui.theme.BottomNavigationApp
import kotlinx.coroutines.launch
import com.example.myapplication.ui.theme.NFCReaderScreen
import com.example.myapplication.ui.theme.WriteCardScreen
import android.nfc.tech.*
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.P2PScreen
import java.io.IOException
import android.app.Activity
import android.nfc.NfcEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    // 使用状态变量存储UI数据
    private var tagInfo by mutableStateOf("")
    private var tagContent by mutableStateOf("")
    private var isButtonVisible by mutableStateOf(true)

    // 状态管理
    enum class WriteMode { IDLE, TEXT, URL }
    private var currentWriteMode by mutableStateOf(WriteMode.IDLE)
    private var inputText by mutableStateOf("")

    companion object {
        private const val TAG = "NFC_DEMO"
        private val URI_PREFIX_MAP = mapOf(
            0x00.toByte() to "",
            0x01.toByte() to "http://www.",
            0x02.toByte() to "https://www.",
            0x03.toByte() to "http://",
            0x04.toByte() to "https://",
            0x05.toByte() to "tel:",
            0x06.toByte() to "mailto:",
            0x07.toByte() to "ftp://anonymous:anonymous@",
            0x08.toByte() to "ftp://ftp.",
            0x09.toByte() to "ftps://",
            0x0A.toByte() to "sftp://",
            0x0B.toByte() to "smb://",
            0x0C.toByte() to "nfs://",
            0x0D.toByte() to "ftp://",
            0x0E.toByte() to "dav://",
            0x0F.toByte() to "news:",
            0x10.toByte() to "telnet://",
            0x11.toByte() to "imap:",
            0x12.toByte() to "rtsp://",
            0x13.toByte() to "urn:",
            0x14.toByte() to "pop:",
            0x15.toByte() to "sip:",
            0x16.toByte() to "sips:",
            0x17.toByte() to "tftp:",
            0x18.toByte() to "btspp://",
            0x19.toByte() to "btl2cap://",
            0x1A.toByte() to "btgoep://",
            0x1B.toByte() to "tcpobex://",
            0x1C.toByte() to "irdaobex://",
            0x1D.toByte() to "file://",
            0x1E.toByte() to "urn:epc:id:",
            0x1F.toByte() to "urn:epc:tag:",
            0x20.toByte() to "urn:epc:pat:",
            0x21.toByte() to "urn:epc:raw:",
            0x22.toByte() to "urn:epc:",
            0x23.toByte() to "urn:nfc:"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // 创建PendingIntent
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        )

        // 设置Intent过滤器
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Failed to add MIME type", e)
            }
        }

        intentFiltersArray = arrayOf(
            ndef,
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        // 支持的所有NFC技术类型
        techListsArray = arrayOf(
            arrayOf(
                NfcA::class.java.name,
                NfcB::class.java.name,
                NfcF::class.java.name,
                NfcV::class.java.name,
                IsoDep::class.java.name,
                MifareClassic::class.java.name,
                MifareUltralight::class.java.name,
                Ndef::class.java.name,
                NdefFormatable::class.java.name
            )
        )

        // 设置Compose UI
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }
                // 创建 NavController
                val navController = rememberNavController()
                BottomNavigationApp(
                    readerScreen = {
                        NFCReaderScreen(
                            tagInfo = tagInfo,
                            tagContent = tagContent,
                            isButtonVisible = isButtonVisible,
                            snackbarHostState = snackbarHostState,
                            onCheckNfcClick = {
                                checkNfcAvailability(
                                    isFirstCheck = false,
                                    showMessage = { messageRes, actionRes, action ->
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = context.getString(messageRes),
                                                actionLabel = if (actionRes != 0) context.getString(actionRes) else null
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                action?.invoke()
                                            }
                                        }
                                    }
                                )
                            }
                        )
                    },
                    writeScreen = {
                        WriteCardScreen(
                            onWriteText = { text ->
                                currentWriteMode = WriteMode.TEXT
                                inputText = text
                                showToast("请将手机靠近NFC标签写入文本")
                            },
                            onWriteUrl = { url ->
                                currentWriteMode = WriteMode.URL
                                inputText = if (url.startsWith("http")) url else "https://$url"

                                showToast("请将手机靠近NFC标签写入网址")
                            },
                            onWriteWifi = {
                                startActivity(Intent(this@MainActivity, WriteWiFi::class.java))
                            },
                            currentMode = currentWriteMode,
                            inputText = inputText
                        )
                    },
                    p2pScreen = {
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
                )
            }
        }
    }

    private fun checkNfcAvailability(
        isFirstCheck: Boolean,
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
                if (!isFirstCheck) {
                    showMessage(R.string.NFCSP, 0, null)
                    isButtonVisible = false
                }
                true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            tagContent = "设备不支持NFC"
            return
        }

        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag == null) {
            tagContent = "未发现NFC标签"
            return
        }

        tagInfo = getString(R.string.scannedTag, tag.toString())
        Log.d(MainActivity.TAG, "Intent Action: ${intent.action}")

        val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawMessages != null) {
            val messages = rawMessages.map { it as NdefMessage }
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
                    Log.e(MainActivity.TAG, "读取NDEF失败", e)
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
                    else -> "未知类型: ${record.toHexString()}\n"
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
            Log.w(MainActivity.TAG, "解析文本记录失败", e)
            "解析错误"
        }
    }

    private fun parseUriRecord(record: NdefRecord): String {
        val prefix = MainActivity.URI_PREFIX_MAP[record.payload[0]] ?: ""
        return try {
            prefix + String(record.payload, 1, record.payload.size - 1, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(MainActivity.TAG, "解析URI记录失败", e)
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
                    val netType = data[0].toInt() and 0xFF
                    val netTypeName = when (netType) {
                        0x00 -> "未知"
                        0x01 -> "基础设施"
                        0x02 -> "独立"
                        else -> "保留/自定义（0x${netType.toString(16)})"
                    }
                    sb.appendLine("网络类型: $netTypeName")
                }
                0x100E -> {
                    sb.append(parseWifiRecord(data))  // 递归解析
                }
                else -> sb.appendLine("未知字段: 0x${type.toString(16)} 数据: ${data.toHexString()}")
            }
        }
        return sb.toString()
    }
    //扩展函数转文字
    fun ByteArray.toMacAddress(): String = joinToString(":") { "%02X".format(it) }

    fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    fun getAuthTypeName(value: Int): String = when(value) {
        0x0001 -> "Open System"
        0x0002 -> "WPA-PSK"
        0x0004 -> "Shared Key"
        0x0008 -> "WPA-EAP"
        0x0010 -> "WPA2-EAP"
        0x0020 -> "WPA2-PSK"
        0x0040 -> "WPA3-SAE"
        else -> "未知（0x%04X）".format(value)
    }

    fun getEncryptionTypeName(value: Int): String = when(value) {
        0x0001 -> "无"
        0x0002 -> "WEP"
        0x0022 -> "WEP"
        0x0004 -> "TKIP"
        0x0008 -> "AES"
        0x0010 -> "AES/TKIP"
        0x0020 -> "AES"
        else -> "未知（0x%04X）".format(value)
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

// 跳过前两个字节
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
            Log.w(MainActivity.TAG, "解析应用记录失败", e)
            "未知应用"
        }
    }

    // 扩展函数
    private fun ByteArray.readUShort(offset: Int): Int =
        (this[offset].toInt() shl 8) or this[offset + 1].toInt()

    private fun ByteArray.readString(offset: Int, length: Int): String =
        String(this, offset, length, Charsets.UTF_8)


    private fun NdefRecord.toHexString(): String =
        this.payload.joinToString("") { "%02X".format(it) }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    private fun handleTagWrite(tag: Tag?) {
        tag?.let {
            if (!isNdefCompatible(it)) {
                showToast("标签不支持NDEF格式")
                currentWriteMode = com.example.myapplication.MainActivity.WriteMode.IDLE
                return
            }

            val message = when(currentWriteMode) {
                com.example.myapplication.MainActivity.WriteMode.TEXT -> NdefMessage(NdefRecord.createTextRecord("en", inputText))
                com.example.myapplication.MainActivity.WriteMode.URL -> NdefMessage(NdefRecord.createUri(inputText))
                com.example.myapplication.MainActivity.WriteMode.IDLE -> return
            }

            try {
                if (Ndef.get(it) != null) {
                    writeNdefMessage(it, message)
                } else {
                    formatAndWrite(it, message)
                }
                showToast("写入成功")
            } catch(e: Exception) {
                showToast("写入失败: ${e.message}")
                Log.e("NFC", "写入错误", e)
            } finally {
                currentWriteMode = com.example.myapplication.MainActivity.WriteMode.IDLE
                inputText = ""
            }
        }
    }

    //P2P 通信页面
}