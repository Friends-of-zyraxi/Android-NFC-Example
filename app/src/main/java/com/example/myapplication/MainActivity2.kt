@file:OptIn(ExperimentalStdlibApi::class) // 添加在文件顶部
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityMain2Binding
import com.google.android.material.snackbar.Snackbar
import java.nio.charset.Charset

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

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
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化UI事件
        binding.button.setOnClickListener {
            if (checkNfcAvailability(false)) {
                binding.button.visibility = View.INVISIBLE
            }
        }

        // 在MainActivity2的按钮点击事件中：
        binding.gotoWriteCard.setOnClickListener {
            startActivity(Intent(this, WriteCard::class.java))
            // 如果需要过渡动画：
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // 初始化NFC适配器
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        checkNfcAvailability(true)

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
    }

    private fun checkNfcAvailability(isFirstCheck: Boolean): Boolean {
        return when {
            nfcAdapter == null -> {
                showSnackbar(R.string.NFCNA, R.string.exit) { finish() }
                false
            }
            !nfcAdapter!!.isEnabled -> {
                showSnackbar(R.string.enable_NFC, R.string.gotoSettings) {
                    startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
                false
            }
            else -> {
                if (!isFirstCheck) {
                    showSnackbar(R.string.NFCSP, 0)
                }
                true
            }
        }
    }

    private fun showSnackbar(messageRes: Int, actionRes: Int, action: (() -> Unit)? = null) {
        Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT).apply {
            if (actionRes != 0 && action != null) {
                setAction(actionRes) { action.invoke() }
            }
        }.show()
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            Toast.makeText(this, "设备不支持NFC", Toast.LENGTH_LONG).show()
            return
        }

        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        tag?.let {
            binding.textView2.text = getString(R.string.scannedTag, tag.toString())

            when (intent.action) {
                NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                    intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.let { rawMessages ->
                        val messages = rawMessages.map { it as NdefMessage }
                        binding.textView3.text = parseNdefMessages(messages)
                    }
                }
                else -> {
                    binding.textView3.text = getString(R.string.notSupport)
                }
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
            "application/vnd.wfa.wsc" -> "WiFi配置:\n${parseWifiRecord(record)}"
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

    private fun parseWifiRecord(record: NdefRecord): String {
        return buildString {
            var offset = 0
            val bytes = record.payload

            while (offset < bytes.size - 4) {
                val type = bytes.readUShort(offset)
                val length = bytes.readUShort(offset + 2)

                if (offset + 4 + length > bytes.size) break

                when (type) {
                    0x1045 -> append("SSID: ${bytes.readString(offset + 4, length)}\n")
                    0x1027 -> append("密码: ${bytes.readString(offset + 4, length)}\n")
                    0x1003 -> append("认证: ${parseWifiAuthType(bytes[offset + 4])}\n")
                    0x100F -> append("加密: ${parseWifiEncType(bytes[offset + 4])}\n")
                }

                offset += 4 + length
            }
        }
    }

    private fun parseBluetoothRecord(record: NdefRecord): String {
        return when {
            record.toUri()?.scheme?.startsWith("bt") == true -> {
                val uri = record.toUri()!!
                "MAC: ${uri.host}\n名称: ${uri.path?.substringAfter("/")}"
            }
            else -> {
                val bytes = record.payload
                if (bytes.size < 6) return "无效蓝牙数据"

                "MAC: ${bytes.take(6).joinToString(":") { "%02X".format(it) }}\n" +
                        if (bytes.size > 6) "名称: ${String(bytes, 6, bytes.size - 6, Charsets.UTF_8)}" else ""
            }
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

    // 扩展函数
    private fun ByteArray.readUShort(offset: Int): Int =
        (this[offset].toInt() shl 8) or this[offset + 1].toInt()

    private fun ByteArray.readString(offset: Int, length: Int): String =
        String(this, offset, length, Charsets.UTF_8)

    private fun parseWifiAuthType(value: Byte): String = when (value.toInt()) {
        0x01 -> "开放网络"
        0x02 -> "WPA-PSK"
        0x04 -> "共享密钥"
        0x08 -> "WPA-企业版"
        0x10 -> "WPA2-企业版"
        0x20 -> "WPA2-PSK"
        else -> "未知($value)"
    }

    private fun parseWifiEncType(value: Byte): String = when (value.toInt()) {
        0x01 -> "无"
        0x02 -> "WEP"
        0x04 -> "TKIP"
        0x08 -> "AES"
        0x0C -> "AES/TKIP"
        else -> "未知($value)"
    }

    private fun NdefRecord.toHexString(): String =
        this.payload.joinToString("") { "%02X".format(it) }
}