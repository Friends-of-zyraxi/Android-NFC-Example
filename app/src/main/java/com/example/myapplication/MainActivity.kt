@file:OptIn(ExperimentalStdlibApi::class)

package com.example.myapplication

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
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
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    // NFC 相关
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    // Nearby Connections 相关
    private lateinit var connectionsClient: ConnectionsClient
    private val SERVICE_ID = "com.example.myapplication.P2P"
    private val NFC_RECORD_TYPE = "nearby_endpoint_id"

    // Composable UI 状态
    private var tagInfo by mutableStateOf("")
    private var tagContent by mutableStateOf("")
    private var isButtonVisible by mutableStateOf(true)

    enum class WriteMode { IDLE, TEXT, URL }
    private var currentWriteMode by mutableStateOf(WriteMode.IDLE)
    private var inputText by mutableStateOf("")

    // P2P通信相关状态变量
    private var p2pConnectionState by mutableStateOf(ConnectionState.DISCONNECTED)
    private var receivedNearbyMessage by mutableStateOf("")
    private var messageToSend by mutableStateOf("")
    private var currentEndpointId: String? = null

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        )

        // 设置Intent过滤器
        intentFiltersArray = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addDataType("*/*") },
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
                val snackbarHostState = remember { SnackbarHostState() }
                val navController = rememberNavController()

                BottomNavigationApp(
                    readerScreen = {
                        NFCReaderScreen(
                            tagInfo = tagInfo,
                            tagContent = tagContent,
                            isButtonVisible = isButtonVisible,
                            snackbarHostState = snackbarHostState,
                            onCheckNfcClick = {
                                // ... (NFC状态检查逻辑不变)
                            }
                        )
                    },
                    writeScreen = {
                        WriteCardScreen(
                            onWriteText = { /* ... (写入文本逻辑不变) */ },
                            onWriteUrl = { /* ... (写入URL逻辑不变) */ },
                            onWriteWifi = { startActivity(Intent(this@MainActivity, WriteWiFi::class.java)) },
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
                            onStartAdvertising = {
                                startAdvertising()
                            },
                            onStopAdvertising = {
                                stopAdvertising()
                            },
                            onStartDiscovery = {
                                startDiscovery()
                            },
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
            SERVICE_ID,
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
            SERVICE_ID,
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

    // NFC 生命周期回调
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

    // 处理新收到的NFC Intent
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return

        // 由于移除了NFC握手，这里不再检查p2pConnectionState
        // 只按原有的读取逻辑处理
        // ... (原有的标签信息和内容解析逻辑)
        tagInfo = "这是个普通的NFC标签"
        tagContent = "解析内容待定..."
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // 省略其他辅助函数，如 parseTextRecord 等
}