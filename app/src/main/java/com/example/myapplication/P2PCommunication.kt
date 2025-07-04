//package com.example.myapplication
//
//import android.content.Intent
//import android.nfc.NdefMessage
//import android.nfc.NdefRecord
//import android.nfc.NfcAdapter
//import android.os.Bundle
//import android.provider.Settings
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import java.nio.charset.Charset
//import android.app.Activity
//import android.os.Build
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.darkColorScheme
//import androidx.compose.material3.dynamicDarkColorScheme
//import androidx.compose.material3.dynamicLightColorScheme
//import androidx.compose.material3.lightColorScheme
//import androidx.compose.material3.Typography
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.SideEffect
//import androidx.compose.ui.graphics.toArgb
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalView
//import androidx.core.view.WindowCompat
//import com.example.myapplication.ui.theme.Pink40
//import com.example.myapplication.ui.theme.Pink80
//import com.example.myapplication.ui.theme.Purple40
//import com.example.myapplication.ui.theme.Purple80
//import com.example.myapplication.ui.theme.PurpleGrey40
//import com.example.myapplication.ui.theme.PurpleGrey80
//
//class P2PCommunication : ComponentActivity() {
//    private lateinit var nfcAdapter: NfcAdapter
//    private var isNfcEnabled by mutableStateOf(false)
//    private var isBeamActive by mutableStateOf(false)
//    private var receivedMessage by mutableStateOf("")
//    private var messageToSend by mutableStateOf("")
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val DarkColorScheme = darkColorScheme(
//            primary = Purple80,
//            secondary = PurpleGrey80,
//            tertiary = Pink80
//        )
//
//        val LightColorScheme = lightColorScheme(
//            primary = Purple40,
//            secondary = PurpleGrey40,
//            tertiary = Pink40
//        )
//
//        @Composable
//        fun NFCP2PTheme(
//            darkTheme: Boolean = isSystemInDarkTheme(),
//            // 动态颜色仅在Android 12+上可用
//            dynamicColor: Boolean = true,
//            content: @Composable () -> Unit
//        ) {
//            val colorScheme = when {
//                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//                    val context = LocalContext.current
//                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//                }
//                darkTheme -> DarkColorScheme
//                else -> LightColorScheme
//            }
//            val view = LocalView.current
//            if (!view.isInEditMode) {
//                SideEffect {
//                    val window = (view.context as Activity).window
//                    window.statusBarColor = colorScheme.primary.toArgb()
//                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
//                }
//            }
//
//            MaterialTheme(
//                colorScheme = colorScheme,
//                typography = Typography,
//                content = content
//            )
//        }
//        // 初始化NFC适配器
//        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//
//        setContent {
//            NFCP2PTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    NFCApp(
//                        isNfcEnabled = isNfcEnabled,
//                        isBeamActive = isBeamActive,
//                        receivedMessage = receivedMessage,
//                        messageToSend = messageToSend,
//                        onMessageChange = { messageToSend = it },
//                        onEnableBeam = ::enableBeam,
//                        onDisableBeam = ::disableBeam,
//                        onEnableNfc = ::openNfcSettings
//                    )
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // 检查NFC状态
//        checkNfcStatus()
//
//        // 启用前台调度
//        if (nfcAdapter != null) {
//            nfcAdapter.enableForegroundDispatch(
//                this,
//                PendingIntent.getActivity(
//                    this, 0, Intent(this, javaClass)
//                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
//                    null,
//                    null
//                )
//        }
//
//        // 处理从NFC Intent启动的情况
//        processIntent(intent)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        // 禁用前台调度
//        if (nfcAdapter != null) {
//            nfcAdapter.disableForegroundDispatch(this)
//        }
//    }
//
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        // 处理新的NFC Intent
//        processIntent(intent)
//    }
//
//    private fun checkNfcStatus() {
//        if (nfcAdapter == null) {
//            isNfcEnabled = false
//            Toast.makeText(this, "设备不支持NFC", Toast.LENGTH_SHORT).show()
//        } else {
//            isNfcEnabled = nfcAdapter.isEnabled
//            if (!isNfcEnabled) {
//                Toast.makeText(this, "请启用NFC功能", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun enableBeam() {
//        if (nfcAdapter != null && isNfcEnabled) {
//            val message = createNdefMessage(messageToSend)
//            nfcAdapter.setNdefPushMessage(message, this)
//            nfcAdapter.setOnNdefPushCompleteCallback({ event ->
//                runOnUiThread {
//                    Toast.makeText(this, "消息已发送", Toast.LENGTH_SHORT).show()
//                }
//            }, this)
//            isBeamActive = true
//        }
//    }
//
//    private fun disableBeam() {
//        if (nfcAdapter != null) {
//            nfcAdapter.setNdefPushMessage(null, this)
//            isBeamActive = false
//        }
//    }
//
//    private fun createNdefMessage(text: String): NdefMessage {
//        val mimeBytes = "application/com.example.nfcp2p".toByteArray(Charset.forName("US-ASCII"))
//        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
//        val record = NdefRecord(
//            NdefRecord.TNF_MIME_MEDIA,
//            mimeBytes,
//            ByteArray(0),
//            textBytes
//        )
//        return NdefMessage(arrayOf(record))
//    }
//
//    private fun processIntent(intent: Intent) {
//        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
//            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
//            if (rawMessages != null) {
//                val messages = arrayOfNulls<NdefMessage>(rawMessages.size)
//                for (i in rawMessages.indices) {
//                    messages[i] = rawMessages[i] as NdefMessage
//                }
//                // 处理第一个消息的第一个记录
//                val record = messages[0]!!.records[0]
//                val payload = record.payload
//                // 跳过mime类型
//                val text = String(payload, 3, payload.size - 3, Charset.forName("UTF-8"))
//                receivedMessage = text
//                Toast.makeText(this, "收到新消息", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//    private fun openNfcSettings() {
//        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
//    }
//}
//
//@Composable
//fun NFCApp(
//    isNfcEnabled: Boolean,
//    isBeamActive: Boolean,
//    receivedMessage: String,
//    messageToSend: String,
//    onMessageChange: (String) -> Unit,
//    onEnableBeam: () -> Unit,
//    onDisableBeam: () -> Unit,
//    onEnableNfc: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(
//            text = "NFC P2P通信",
//            style = MaterialTheme.typography.headlineLarge,
//            modifier = Modifier.padding(bottom = 24.dp)
//        )
//
//        if (!isNfcEnabled) {
//            Text(
//                text = "NFC功能未启用",
//                color = MaterialTheme.colorScheme.error,
//                modifier = Modifier.padding(bottom = 16.dp)
//            )
//            Button(onClick = onEnableNfc) {
//                Text("启用NFC")
//            }
//        } else {
//            // 接收消息区域
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 24.dp),
//                elevation = CardDefaults.cardElevation(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        text = "接收到的消息",
//                        style = MaterialTheme.typography.titleMedium,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                    Text(
//                        text = receivedMessage.ifEmpty { "等待接收消息..." },
//                        style = MaterialTheme.typography.bodyLarge,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(8.dp)
//                    )
//                }
//            }
//
//            // 发送消息区域
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(bottom = 24.dp),
//                elevation = CardDefaults.cardElevation(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier.padding(16.dp)
//                ) {
//                    Text(
//                        text = "发送消息",
//                        style = MaterialTheme.typography.titleMedium,
//                        modifier = Modifier.padding(bottom = 8.dp)
//                    )
//                    OutlinedTextField(
//                        value = messageToSend,
//                        onValueChange = onMessageChange,
//                        label = { Text("输入要发送的消息") },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(bottom = 16.dp)
//                    )
//
//                    Row(
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Button(
//                            onClick = onEnableBeam,
//                            enabled = messageToSend.isNotEmpty() && !isBeamActive
//                        ) {
//                            Text("激活发送")
//                        }
//
//                        Button(
//                            onClick = onDisableBeam,
//                            enabled = isBeamActive,
//                            colors = ButtonDefaults.buttonColors(
//                                containerColor = MaterialTheme.colorScheme.errorContainer,
//                                contentColor = MaterialTheme.colorScheme.onErrorContainer
//                            )
//                        ) {
//                            Text("停止发送")
//                        }
//                    }
//                }
//            }
//
//            // 使用说明
//            Text(
//                text = "使用说明：\n" +
//                        "1. 输入消息并点击'激活发送'\n" +
//                        "2. 将设备背靠背靠近另一台设备\n" +
//                        "3. 触摸屏幕发送消息",
//                style = MaterialTheme.typography.bodyMedium,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.padding(top = 16.dp)
//            )
//
//            if (isBeamActive) {
//                Text(
//                    text = "发送模式已激活 - 准备发送消息",
//                    color = MaterialTheme.colorScheme.primary,
//                    style = MaterialTheme.typography.bodyLarge,
//                    modifier = Modifier.padding(top = 16.dp)
//                )
//            }
//        }
//    }
//
//
//}