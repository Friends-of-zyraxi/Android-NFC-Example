package com.example.myapplication

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class MyHostApduService : HostApduService() {

    companion object {
        const val TAG = "MyHostApduService"

        private fun hexStringToByteArray(s: String): ByteArray {
            check(s.length % 2 == 0) { "Hex string must have an even length" }
            return s.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        // 状态字
        val SW_OK = hexStringToByteArray("9000")
        val SW_INS_NOT_SUPPORTED = hexStringToByteArray("6D00")
        val SW_DATA_INVALID = hexStringToByteArray("6984")
        val SW_FILE_NOT_FOUND = hexStringToByteArray("6A82")

        // NFC Forum Type 4 Tag 常量
        val NDEF_TAG_AID = hexStringToByteArray("D2760000850101")
        val CC_FILE_ID = hexStringToByteArray("E103")
        val NDEF_FILE_ID = hexStringToByteArray("E104")

        // 自定义 APDU 指令
        val CMD_GET_DEVICE_NAME = hexStringToByteArray("80010000")
        val CMD_CONFIRM_CONNECTION = hexStringToByteArray("80020000")
        val CMD_READ_EMULATED_DATA = hexStringToByteArray("80030000")

        var deviceNameToShare = "CardDevice_123"
        var connectionEstablished = false

        // 卡模拟数据（由 MainActivity 在启动模拟时设置）
        var emulatedData: ByteArray? = null
        var emulatedDataType: String? = null
    }

    // 当前选中的文件 ID（null = 未选中）
    private var selectedFileId: ByteArray? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.i(TAG, "APDU: ${commandApdu.toHexString()}")
        connectionEstablished = false

        if (commandApdu.size < 4) return SW_DATA_INVALID

        val ins = commandApdu[1].toInt() and 0xFF
        val p1 = commandApdu[2].toInt() and 0xFF
        val p2 = commandApdu[3].toInt() and 0xFF

        // ================================================================
        // SELECT 命令
        // ================================================================
        if (ins == 0xA4) {
            if (commandApdu.size < 5) return SW_DATA_INVALID
            val lc = commandApdu[4].toInt() and 0xFF
            if (commandApdu.size < 5 + lc) return SW_DATA_INVALID
            val data = commandApdu.copyOfRange(5, 5 + lc)

            // SELECT by AID (P1=04)
            if (p1 == 0x04) {
                Log.i(TAG, "SELECT AID: ${data.toHexString()}")
                if (data.contentEquals(NDEF_TAG_AID)) {
                    selectedFileId = null
                    return SW_OK
                }
                // 也接受自定义 AID
                return SW_OK
            }

            // SELECT by File ID (P2=0C)
            if (p2 == 0x0C && data.size == 2) {
                Log.i(TAG, "SELECT File: ${data.toHexString()}")
                if (data.contentEquals(CC_FILE_ID)) {
                    selectedFileId = CC_FILE_ID
                    return SW_OK
                }
                if (data.contentEquals(NDEF_FILE_ID)) {
                    selectedFileId = NDEF_FILE_ID
                    return SW_OK
                }
                return SW_FILE_NOT_FOUND
            }
        }

        // ================================================================
        // READ_BINARY 命令
        // ================================================================
        if (ins == 0xB0) {
            val offset = ((p1 shl 8) or p2)
            // Le is the last byte (expected response length)
            val le = if (commandApdu.size >= 5) (commandApdu.last().toInt() and 0xFF) else 0xFF

            val fileId = selectedFileId
            if (fileId == null) {
                Log.w(TAG, "READ_BINARY: no file selected")
                return SW_DATA_INVALID
            }

            if (fileId.contentEquals(CC_FILE_ID)) {
                val cc = buildCapabilityContainer()
                Log.i(TAG, "READ_BINARY CC offset=$offset len=$le")
                return readFromFile(cc, offset, le)
            }

            if (fileId.contentEquals(NDEF_FILE_ID)) {
                val ndefData = emulatedData ?: ByteArray(0)
                Log.i(TAG, "READ_BINARY NDEF offset=$offset len=$le, total=${ndefData.size}")
                return readFromFile(ndefData, offset, le)
            }
        }

        // ================================================================
        // 自定义命令
        // ================================================================
        val header = commandApdu.copyOfRange(0, 4)

        if (header.contentEquals(CMD_GET_DEVICE_NAME)) {
            Log.i(TAG, "CMD: GET_DEVICE_NAME")
            return deviceNameToShare.toByteArray(Charsets.UTF_8) + SW_OK
        }

        if (header.contentEquals(CMD_CONFIRM_CONNECTION)) {
            Log.i(TAG, "CMD: CONFIRM_CONNECTION")
            connectionEstablished = true
            return "CONN_ACK".toByteArray(Charsets.UTF_8) + SW_OK
        }

        if (header.contentEquals(CMD_READ_EMULATED_DATA)) {
            Log.i(TAG, "CMD: READ_EMULATED_DATA")
            val data = emulatedData
            if (data != null) {
                return readFromFile(data, 0, 240) + SW_OK
            }
            return SW_DATA_INVALID
        }

        Log.w(TAG, "Unknown command INS=${String.format("%02X", ins)}")
        return SW_INS_NOT_SUPPORTED
    }

    override fun onDeactivated(reason: Int) {
        Log.i(TAG, "Deactivated: reason=$reason")
        connectionEstablished = false
        selectedFileId = null
    }

    // ---- 辅助方法 ----

    /** 构建 NFC Forum Type 4 Tag Capability Container */
    private fun buildCapabilityContainer(): ByteArray {
        val ndefData = emulatedData ?: ByteArray(0)
        val maxNdefSize = maxOf(ndefData.size, 256).let { if (it > 0xFF) 0x0FFF else it }

        return byteArrayOf(
            // CCLEN = 0x000F (15 bytes)
            0x00, 0x0F,
            // Mapping version 2.0
            0x20.toByte(),
            // MLe (max R-APDU data size) = 0x00FF
            0x00, 0xFF.toByte(),
            // MLc (max C-APDU data size) = 0x00FF
            0x00, 0xFF.toByte(),
            // NDEF File Control TLV: T=0x04, L=0x06
            0x04, 0x06,
            // NDEF File ID = E104
            0xE1.toByte(), 0x04,
            // Max NDEF size (2 bytes)
            ((maxNdefSize shr 8) and 0xFF).toByte(),
            (maxNdefSize and 0xFF).toByte(),
            // Read access = always (0x00)
            0x00,
            // Write access = never (0xFF, read-only emulation)
            0xFF.toByte()
        )
    }

    /** 从文件中按偏移和长度读取 */
    private fun readFromFile(fileData: ByteArray, offset: Int, reqLen: Int): ByteArray {
        // Le=0 表示请求 256 字节；但限制每次最多返回 252 字节数据 + 2 字节 SW
        val maxRead = minOf(if (reqLen == 0) 256 else reqLen, fileData.size - offset, 252)
        if (maxRead <= 0) return SW_OK

        val result = ByteArray(maxRead + 2)
        System.arraycopy(fileData, offset, result, 0, maxRead)
        result[maxRead] = 0x90.toByte()
        result[maxRead + 1] = 0x00
        return result
    }

    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }
}
