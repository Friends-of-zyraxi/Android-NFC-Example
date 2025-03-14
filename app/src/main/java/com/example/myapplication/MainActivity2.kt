package com.example.myapplication

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.myapplication.databinding.ActivityMain2Binding
import com.google.android.material.snackbar.Snackbar
import java.nio.charset.Charset

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding

    private var defaultNfcAdapter: NfcAdapter? = null //声明为可空类型
    private lateinit var pendingIntent: PendingIntent //延迟初始化
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            if (checkNfcAvailability(false)) {
                binding.button.visibility = View.INVISIBLE
            }
        }

        binding.gotoWriteCard.setOnClickListener {
            val intent = Intent()
            intent.setClass(this, WriteCard::class.java)
            startActivity(intent)
            finish()
        }

        defaultNfcAdapter = NfcAdapter.getDefaultAdapter(this)    //定义一个 adapter
        checkNfcAvailability(true)

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE
        )
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")    /* Handles all MIME based dispatches.
                                 You should specify only the ones that you need. */
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }
        }

        intentFiltersArray = arrayOf(
            ndef,
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        // 配置支持的所有技术类型
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
    }

    private fun checkNfcAvailability(isFirstCheck: Boolean): Boolean {

        when {
            defaultNfcAdapter == null -> {
                class ExitApp : View.OnClickListener {

                    override fun onClick(v: View) {
                        finish() // 直接关闭 Activity
                    }
                }
                Snackbar.make(findViewById(R.id.button), R.string.NFCNA, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.exit, ExitApp())
                    .show()
                return false
            }

            !defaultNfcAdapter!!.isEnabled -> {
                class OpenNfcSettings : View.OnClickListener {

                    override fun onClick(v: View) {
                        startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) //跳转到系统 NFC 设置界面
                    }
                }
                Snackbar.make(findViewById(R.id.button), R.string.enable_NFC, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.gotoSettings, OpenNfcSettings())
                    .show()
                return false
            }

            else -> {
                if (!isFirstCheck) {
                    Snackbar.make(findViewById(R.id.button), R.string.NFCSP, Snackbar.LENGTH_SHORT)
                        .show()
                }
                return true
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        defaultNfcAdapter?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        defaultNfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent) // 修复错误 1
        handleNfcIntent(intent)

        // 修复错误 2 的两种方式：

        // 方式 1：使用新 API 写法（推荐，API 33+）
        // val tagFromIntent: Tag? = intent.getParcelableExtra(
        //     NfcAdapter.EXTRA_TAG,
        //     Tag::class.java
        // )

        // 方式 2：兼容旧 API 的强制类型转换（不推荐）
//        @Suppress("DEPRECATION")
//        val tagFromIntent: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag?
//
//        tagFromIntent?.let {
//
//        }
    }

    private fun handleNfcIntent(intent: Intent) {
        Toast.makeText(this, R.string.scanned, Toast.LENGTH_SHORT).show()

        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) as Tag?
        }

        tag?.let {
            // 这里可以处理原始 Tag 对象

            val scannedText = findViewById<TextView>(R.id.textView2)
            scannedText.text = getString(R.string.scannedTag, tag.toString())
            val scannedContext = findViewById<TextView>(R.id.textView3)

            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                    ?.also { rawMessages ->
                        val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                        // 处理消息数组
                        val stringBuilder = StringBuilder()

                        // 修改后的 handleNfcIntent 记录处理逻辑
                        messages.forEach { ndefMessage ->
                            ndefMessage.records.forEach { record ->
                                when (record.tnf) {
                                    NdefRecord.TNF_WELL_KNOWN -> {
                                        when {
                                            record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                                                val text = parseTextRecord(record)
                                                stringBuilder.append(getString(R.string.text_type, text))
                                            }
                                            record.type.contentEquals(NdefRecord.RTD_URI) -> {
                                                val uri = parseUriRecord(record)
                                                stringBuilder.append(getString(R.string.uri_type, uri))
                                            }
                                        }
                                    }
                                    NdefRecord.TNF_MIME_MEDIA -> {
                                        when (record.toMimeType()) {
                                            "application/vnd.bluetooth.ep.oob" -> {
                                                val bluetooth = processBluetoothRecord(record)
                                                stringBuilder.append(getString(R.string.bluetooth_type, bluetooth))
                                            }
                                            "application/vnd.wfa.wsc" -> {
                                                val wifi = parseWifiRecord(record)
                                                stringBuilder.append(getString(R.string.wifi_type, wifi))
                                            }
                                        }
                                    }
                                    NdefRecord.TNF_EXTERNAL_TYPE -> {
                                        when (String(record.type)) {
                                            "android.com:pkg" -> {
                                                val app = parseApplicationRecord(record)
                                                stringBuilder.append(getString(R.string.app_type, app))
                                            }
                                        }
                                    }
                                    else -> {
                                        stringBuilder.append(getString(R.string.unknown_type, record.toHexString()))
                                    }
                                }
                            }
                        }


                        scannedContext.text = stringBuilder.toString()


                    }
            } else if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
                scannedContext.text = getString(R.string.notSupport)
            } else {
                scannedContext.text = getString(R.string.notSupport)
            }

//            Log.d("NFC", "发现标签: ${it.id.toHexString()}")
//            processRawTag(it)
        }
    }

    class MifareUltralightTagTester {

        fun writeTag(tag: Tag, tagText: String) {
            MifareUltralight.get(tag)?.use { ultralight ->
                ultralight.connect()
                Charset.forName("US-ASCII").also { usAscii ->
                    ultralight.writePage(4, "abcd".toByteArray(usAscii))
                    ultralight.writePage(5, "efgh".toByteArray(usAscii))
                    ultralight.writePage(6, "ijkl".toByteArray(usAscii))
                    ultralight.writePage(7, "mnop".toByteArray(usAscii))
                }
            }
        }

        fun readTag(tag: Tag): String? {
            return MifareUltralight.get(tag)?.use { mifare ->
                mifare.connect()
                val payload = mifare.readPages(4)
                String(payload, Charset.forName("US-ASCII"))
            }
        }
    }

    // 解析文本记录的扩展函数
    private fun parseTextRecord(record: NdefRecord): String {
        val payload = record.payload
        val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"
        val languageCodeLength = payload[0].toInt() and 0x3F
        return String(
            payload,
            languageCodeLength + 1,
            payload.size - languageCodeLength - 1,
            Charset.forName(textEncoding)
        )
    }

    // 解析 URI 记录的扩展函数
    private fun parseUriRecord(record: NdefRecord): String {
        val prefixMap = mapOf(
            0x00.toByte() to "",
            0x01.toByte() to "http://www.",
            0x02.toByte() to "https://www.",
            // ... 其他前缀映射
        )
        val prefix = prefixMap[record.payload[0]] ?: ""
        return prefix + String(record.payload, 1, record.payload.size - 1, Charsets.UTF_8)
    }

    // 将未知记录转为十六进制字符串
    private fun NdefRecord.toHexString(): String {
        return this.payload.joinToString("") { "%02x".format(it) }
    }

    // 解析应用程序记录的扩展函数
    private fun parseApplicationRecord(record: NdefRecord): String {
        if (record.tnf == NdefRecord.TNF_EXTERNAL_TYPE
            && record.type.contentEquals("android.com:pkg".toByteArray())) {
            val packageName = String(record.payload, Charsets.UTF_8)
            // 示例：直接启动对应应用
            // val intent = packageManager.getLaunchIntentForPackage(packageName)
            // startActivity(intent)
            return packageName
        }
        else {
            return ""
        }
    }

    // 处理 Wi-Fi
    private fun parseWifiRecord(record: NdefRecord): String {
        val stringBuilder = StringBuilder()
        if (record.toMimeType() == "application/vnd.wfa.wsc") {
            val payload = record.payload
            // 按 WFA 规范解析二进制数据
            val ssid = parseWscString(payload, 0x1045) // SSID 标签代码 0x1045
            val password = parseWscString(payload, 0x1027) // 密码标签代码 0x1027
            stringBuilder.append(getString(R.string.ssid, ssid))
            stringBuilder.append(getString(R.string.wifi_password, password))
        }
        return stringBuilder.toString()
    }

    private fun parseWscString(payload: ByteArray, tagCode: Int): String? {
        var index = 0
        while (index < payload.size) {
            val type = (payload[index].toInt() shl 8) or payload[index + 1].toInt()
            val length = (payload[index + 2].toInt() shl 8) or payload[index + 3].toInt()
            if (type == tagCode) {
                return String(payload, index + 4, length, Charsets.UTF_8)
            }
            index += 4 + length
        }
        return null
    }

    // 处理蓝牙
    private fun processBluetoothRecord(record: NdefRecord): String {
        val stringBuilder = StringBuilder()
        when {
            // 检查蓝牙配对记录（常见格式）
            record.toUri()?.scheme == "bluetooth" -> {
                val btUri = record.toUri()
                val macAddress = btUri?.host // 蓝牙 MAC 地址
                val name = btUri?.path?.removePrefix("/") // 设备名称
                // 示例URI：bluetooth://00:11:22:33:44:55/MyHeadset
                stringBuilder.append(getString(R.string.bluetooth_mac, macAddress))
                stringBuilder.append(getString(R.string.bluetooth_device_name, name))
            }

            // 处理 Android Beam 蓝牙配对记录（旧格式）
            record.toMimeType() == "application/vnd.bluetooth.ep.oob" -> {
                val payload = record.payload
                // 解析蓝牙OOB数据（需要按规范解析）
                stringBuilder.append(payload)
            }
        }
        return stringBuilder.toString()
    }
}
