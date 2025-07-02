@file:OptIn(ExperimentalStdlibApi::class)
package com.example.myapplication
import com.example.myapplication.ui.theme.NavigationView
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
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.ui.theme.MyApplicationTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myapplication.ui.theme.BottomNavigationApp
import kotlinx.coroutines.launch
import com.example.myapplication.ui.theme.NFCReaderScreen

class MainActivity : ComponentActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFiltersArray: Array<IntentFilter>
    private lateinit var techListsArray: Array<Array<String>>

    // 使用状态变量存储UI数据
    private var tagInfo by mutableStateOf("")
    private var tagContent by mutableStateOf("")
    private var isButtonVisible by mutableStateOf(true)

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
        // 设置Compose UI
        setContent {
            MyApplicationTheme {
                BottomNavigationApp()
            }
        }
    }
}