package com.example.myapplication

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.IOException
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.ui.theme.WriteCardScreen

@OptIn(ExperimentalStdlibApi::class)
class WriteCard : ComponentActivity() {
    // NFC相关变量
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    // 状态管理
    enum class WriteMode { IDLE, TEXT, URL }
    private var currentWriteMode by mutableStateOf(WriteMode.IDLE)
    private var inputText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: run {
            showToast("设备不支持NFC")
            finish()
            return
        }

        // 设置Compose界面
        setContent {
            MyApplicationTheme {
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
                    onBackToRead = ::navigateBackToRead,
                    currentMode = currentWriteMode,
                    inputText = inputText
                )
            }
        }

        // NFC初始化
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        )

        intentFiltersArray = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                try { addDataType("*/*") } catch (e: Exception) { Log.e("NFC", "MIME类型错误", e) }
            },
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        techListsArray = arrayOf(arrayOf(
            NfcA::class.java.name,
            NfcB::class.java.name,
            NfcF::class.java.name,
            NfcV::class.java.name,
            IsoDep::class.java.name,
            MifareClassic::class.java.name,
            MifareUltralight::class.java.name,
            Ndef::class.java.name,
            NdefFormatable::class.java.name
        ))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (currentWriteMode != WriteMode.IDLE) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            handleTagWrite(tag)
        }
    }

    private fun handleTagWrite(tag: Tag?) {
        tag?.let {
            if (!isNdefCompatible(it)) {
                showToast("标签不支持NDEF格式")
                currentWriteMode = WriteMode.IDLE
                return
            }

            val message = when(currentWriteMode) {
                WriteMode.TEXT -> NdefMessage(NdefRecord.createTextRecord("en", inputText))
                WriteMode.URL -> NdefMessage(NdefRecord.createUri(inputText))
                WriteMode.IDLE -> return
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
                currentWriteMode = WriteMode.IDLE
                inputText = ""
            }
        }
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

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    private fun navigateBackToRead() {
        //startActivity(Intent(this, MainActivity2::class.java))
        //finish()
        startActivity(Intent(this@WriteCard, MainActivity2::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

