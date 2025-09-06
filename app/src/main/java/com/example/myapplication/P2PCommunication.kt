package com.example.myapplication

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.nio.charset.StandardCharsets

class P2PCommunication : ComponentActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private var isNfcEnabled by mutableStateOf(false)
    private var isBeamActive by mutableStateOf(false)
    private var receivedMessage by mutableStateOf("")
    private var messageToSend by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            NFCP2PTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NFCApp(
                        isNfcEnabled = isNfcEnabled,
                        isBeamActive = isBeamActive,
                        receivedMessage = receivedMessage,
                        messageToSend = messageToSend,
                        onMessageChange = { messageToSend = it },
                        onEnableBeam = ::enableBeam,
                        onDisableBeam = ::disableBeam,
                        onEnableNfc = ::openNfcSettings
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 检查NFC状态
        checkNfcStatus()

        // 启用前台调度
        if (nfcAdapter != null) {
            val intent = Intent(this, javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, flags
            )

            nfcAdapter.enableForegroundDispatch(
                this,
                pendingIntent,
                null,
                null
            )
        }

        // 处理从NFC Intent启动的情况
        processIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        // 禁用前台调度
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 处理新的NFC Intent
        processIntent(intent)
    }

    private fun checkNfcStatus() {
        if (nfcAdapter == null) {
            isNfcEnabled = false
            Toast.makeText(this, "设备不支持NFC", Toast.LENGTH_SHORT).show()
        } else {
            isNfcEnabled = nfcAdapter.isEnabled
            if (!isNfcEnabled) {
                Toast.makeText(this, "请启用NFC功能", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableBeam() {
        if (nfcAdapter == null || !isNfcEnabled) return

        try {
            val message = createNdefMessage(messageToSend)

            // 使用反射设置 NDEF 消息回调
            val setNdefPushMessageCallbackMethod = nfcAdapter.javaClass.getMethod(
                "setNdefPushMessageCallback",
                NfcAdapter.CreateNdefMessageCallback::class.java,
                Activity::class.java
            )
            setNdefPushMessageCallbackMethod.invoke(
                nfcAdapter,
                NfcAdapter.CreateNdefMessageCallback { _ -> message },
                this
            )

            // 设置发送完成回调
            val setOnNdefPushCompleteCallbackMethod = nfcAdapter.javaClass.getMethod(
                "setOnNdefPushCompleteCallback",
                NfcAdapter.OnNdefPushCompleteCallback::class.java,
                Activity::class.java
            )
            setOnNdefPushCompleteCallbackMethod.invoke(
                nfcAdapter,
                NfcAdapter.OnNdefPushCompleteCallback { event ->
                    runOnUiThread {
                        Toast.makeText(this, "消息已发送", Toast.LENGTH_SHORT).show()
                        disableBeam()
                    }
                },
                this
            )

            isBeamActive = true
        } catch (e: Exception) {
            Toast.makeText(this, "NFC 功能不可用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableBeam() {
        if (nfcAdapter == null) return

        try {
            // 使用反射清除回调
            val setNdefPushMessageCallbackMethod = nfcAdapter.javaClass.getMethod(
                "setNdefPushMessageCallback",
                NfcAdapter.CreateNdefMessageCallback::class.java,
                Activity::class.java
            )
            setNdefPushMessageCallbackMethod.invoke(nfcAdapter, null, this)

            val setOnNdefPushCompleteCallbackMethod = nfcAdapter.javaClass.getMethod(
                "setOnNdefPushCompleteCallback",
                NfcAdapter.OnNdefPushCompleteCallback::class.java,
                Activity::class.java
            )
            setOnNdefPushCompleteCallbackMethod.invoke(nfcAdapter, null, this)

            isBeamActive = false
        } catch (e: Exception) {
            Log.e("NFC", "禁用Beam时出错", e)
        }
    }

    private fun createNdefMessage(text: String): NdefMessage {
        val mimeBytes = "application/com.example.nfcp2p".toByteArray(StandardCharsets.US_ASCII)
        val textBytes = text.toByteArray(StandardCharsets.UTF_8)
        val record = NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeBytes,
            ByteArray(0),
            textBytes
        )
        return NdefMessage(arrayOf(record))
    }

    private fun processIntent(intent: Intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val messages = arrayOfNulls<NdefMessage>(rawMessages.size)
                for (i in rawMessages.indices) {
                    messages[i] = rawMessages[i] as NdefMessage
                }
                // 处理第一个消息的第一个记录
                val record = messages[0]!!.records[0]
                val payload = record.payload
                // 直接解析整个payload
                val text = String(payload, StandardCharsets.UTF_8)
                receivedMessage = text
                Toast.makeText(this, "收到新消息", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openNfcSettings() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }
}

@Composable
fun NFCApp(
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

@Composable
fun NFCP2PTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}