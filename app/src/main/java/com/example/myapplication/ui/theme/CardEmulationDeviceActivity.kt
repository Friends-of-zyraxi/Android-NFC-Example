package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.MyApplicationTheme // 假设您的主题名称
import com.example.myapplication.ui.theme.P2PScreen
import kotlinx.coroutines.flow.collectLatest

// 简单的 ViewModel 来持有 HCE 服务的状态 (可选，但推荐)
class CardEmulationViewModel : androidx.lifecycle.ViewModel() {
    private val _isCardEmulationActive = mutableStateOf(false)
    val isCardEmulationActive: State<Boolean> = _isCardEmulationActive

    // 这个方法可以被 MyHostApduService 调用来更新状态
    // (例如，通过 LocalBroadcastManager 或直接的 Service-ViewModel 绑定)
    // 为了简单起见，我们这里假设 Activity 只是“启用”它
    fun setCardEmulationActive(isActive: Boolean) {
        _isCardEmulationActive.value = isActive
    }
}

class CardEmulationDeviceActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var hceServiceRunning by mutableStateOf(false) // 简单状态，表示用户已尝试启动

    // (可选) 用于从 MyHostApduService 接收状态更新的 BroadcastReceiver
    // private val hceStatusReceiver = object : BroadcastReceiver() {
    //     override fun onReceive(context: Context?, intent: Intent?) {
    //         if (intent?.action == "com.example.myapplication.CONNECTION_STATUS") {
    //             val isConnected = intent.getBooleanExtra("isConnected", false)
    //             // 更新UI或ViewModel
    //             // cardEmulationViewModel.setCardEmulationActive(isConnected)
    //             Log.d("CardEmulationActivity", "HCE Service isConnected: $isConnected")
    //         }
    //     }
    // }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // val cardEmulationViewModel: CardEmulationViewModel = viewModel() // 获取ViewModel

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CardEmulationScreen(
                        isNfcSupported = nfcAdapter != null,
                        isNfcEnabled = nfcAdapter?.isEnabled ?: false,
                        isHceServiceExplicitlyStarted = hceServiceRunning,
                        // isCardActuallyEmulating = cardEmulationViewModel.isCardEmulationActive.value, // 来自ViewModel
                        onStartEmulationClick = {
                            if (nfcAdapter == null) {
                                Toast.makeText(this, "此设备不支持NFC", Toast.LENGTH_SHORT).show()
                                return@CardEmulationScreen
                            }
                            if (nfcAdapter?.isEnabled == false) {
                                Toast.makeText(this, "请先在系统设置中启用NFC", Toast.LENGTH_LONG).show()
                                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                                return@CardEmulationScreen
                            }
                            // HCE服务是由系统在NFC场检测到匹配的AID时自动启动的。
                            // 这个按钮更多的是一个用户意图的表示，并确保NFC已启用。
                            // 我们不需要显式启动服务，但可以更新UI状态。
                            hceServiceRunning = true
                            // cardEmulationViewModel.setCardEmulationActive(true) // 假设用户点击后即为活动
                            Toast.makeText(this, "卡片模拟已准备就绪。请将设备靠近读取器。", Toast.LENGTH_LONG).show()
                        },
                        onStopEmulationClick = {
                            hceServiceRunning = false
                            // cardEmulationViewModel.setCardEmulationActive(false)
                            Toast.makeText(this, "卡片模拟已停止。", Toast.LENGTH_SHORT).show()
                        },
                        onOpenNfcSettings = {
                            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // (可选) 注册 BroadcastReceiver
        // val filter = IntentFilter("com.example.myapplication.CONNECTION_STATUS")
        // LocalBroadcastManager.getInstance(this).registerReceiver(hceStatusReceiver, filter)

        // 检查NFC状态
        if (nfcAdapter?.isEnabled == false && hceServiceRunning) {
            // 如果用户之前启动了模拟但关闭了NFC，提示他们
            Toast.makeText(this, "NFC已关闭，卡片模拟可能无法工作。", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // (可选) 注销 BroadcastReceiver
        // LocalBroadcastManager.getInstance(this).unregisterReceiver(hceStatusReceiver)
    }
}

@Composable
fun CardEmulationScreen(
    isNfcSupported: Boolean,
    isNfcEnabled: Boolean,
    isHceServiceExplicitlyStarted: Boolean,
    // isCardActuallyEmulating: Boolean, // 更精确的状态，例如来自服务
    onStartEmulationClick: () -> Unit,
    onStopEmulationClick: () -> Unit,
    onOpenNfcSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NFC卡片模拟 (被动设备)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!isNfcSupported) {
            Text("此设备不支持NFC。", color = MaterialTheme.colorScheme.error)
            return
        }

        if (!isNfcEnabled) {
            Text("NFC功能未启用。", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            Button(onClick = onOpenNfcSettings) {
                Text("打开NFC设置")
            }
            return
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isHceServiceExplicitlyStarted) {
            Text(
                "卡片模拟已激活",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                "请将此设备背面靠近另一台NFC设备进行扫描。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(
                onClick = onStopEmulationClick,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("停止模拟卡片")
            }
        } else {
            Text(
                "点击下方按钮开始模拟NFC卡片。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Button(
                onClick = onStartEmulationClick,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("开始模拟NFC卡片")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "注意：实际的卡片模拟由系统NFC服务处理。此界面仅用于指示状态和启用NFC。",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun P2PScreenPreview() {
    CardEmulationScreen(
        true,
        true,
        false,
        // isCardActuallyEmulating: Boolean, // 更精确的状态，例如来自服务
        {},
        {},
        {}
    )
}