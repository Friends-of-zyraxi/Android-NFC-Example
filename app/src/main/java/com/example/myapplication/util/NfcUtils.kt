package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import com.example.myapplication.R

fun checkNfcAvailability(
    context: Context,
    isFirstCheck: Boolean = false,
    showMessage: (
        messageRes: Int,
        actionRes: Int,
        action: (() -> Unit)?
    ) -> Unit
) {
    val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    if (nfcAdapter == null) {
        showMessage(R.string.msg_nfc_not_enabled, 0, null)
        return
    }

    if (!nfcAdapter.isEnabled) {
        showMessage(
            R.string.msg_nfc_not_enabled,
            R.string.button_open_settings
        ) {
            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }
    } else if (isFirstCheck) {
        showMessage(R.string.msg_nfc_ready, 0, null)
    }
}
