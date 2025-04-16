package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import com.example.myapplication.R

fun checkNfcAvailability(
    isFirstCheck: Boolean = false,
    showMessage: (
        messageRes: Int,
        actionRes: Int,
        action: (() -> Unit)?
    ) -> Unit
) {
    val context = currentContext ?: return
    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    if (nfcAdapter == null) {
        showMessage(R.string.nfc_not_supported, 0, null)
        return
    }

    if (!nfcAdapter.isEnabled) {
        showMessage(
            R.string.nfc_not_enabled,
            R.string.open_settings
        ) {
            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }
    } else if (isFirstCheck) {
        showMessage(R.string.nfc_ready, 0, null)
    }
}

// 用于提供 context，避免在非 composable 中传 context
private var currentContext: Context? = null

fun setGlobalContext(context: Context) {
    currentContext = context.applicationContext
}
