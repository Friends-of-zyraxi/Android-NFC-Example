package com.example.myapplication

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.util.Arrays

class MyHostApduService : HostApduService() {

    companion object {
        const val TAG = "MyHostApduService"

        // 将 hexStringToByteArray 移到 companion object 内部
        private fun hexStringToByteArray(s: String): ByteArray {
            check(s.length % 2 == 0) { "Hex string must have an even length" }
            return s.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        // 状态字：成功
        val SW_OK = hexStringToByteArray("9000")
        // 状态字：未找到CLA（Class byte）
        val SW_CLA_NOT_SUPPORTED = hexStringToByteArray("6E00")
        // 状态字：未找到INS（Instruction byte）
        val SW_INS_NOT_SUPPORTED = hexStringToByteArray("6D00")
        // 状态字：数据错误
        val SW_DATA_INVALID = hexStringToByteArray("6984")
        // 状态字：未知错误
        val SW_UNKNOWN_ERROR = hexStringToByteArray("6F00")

        // 我们的应用特定的APDU指令 (示例)
        // CLA INS P1 P2 Lc Data Le
        // 00 A4 04 00 Lc <AID> 00  (SELECT by AID command)
        val SELECT_APDU_HEADER = hexStringToByteArray("00A40400")
        // 示例：获取设备名称的指令
        val CMD_GET_DEVICE_NAME = hexStringToByteArray("80010000")
        // 示例：发送连接确认的指令 (可以由读取器发送，卡片响应)
        val CMD_CONFIRM_CONNECTION = hexStringToByteArray("80020000")

        // 模拟的设备名称（可以动态设置）
        var deviceNameToShare = "CardDevice_123"
        var connectionEstablished = false
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        Log.i(TAG, "Received APDU: " + commandApdu.toHexString())
        connectionEstablished = false // Reset on new command unless it's a confirmation

        val header = commandApdu.copyOfRange(0, 4) // CLA INS P1 P2

        // 1. 处理 SELECT AID 命令 (通常由系统处理，但我们可以记录或有特定逻辑)
        if (Arrays.equals(SELECT_APDU_HEADER, header) && commandApdu.size >= 5) {
            val aidLength = commandApdu[4].toInt()
            if (commandApdu.size >= 5 + aidLength) {
                val aid = commandApdu.copyOfRange(5, 5 + aidLength)
                Log.i(TAG, "AID Selected: ${aid.toHexString()}")
                // 通常，如果AID匹配，系统已经选择了我们的服务。
                // 我们可以在这里返回一个成功响应，或者一个包含应用信息的响应。
                // 对于简单的连接，返回 SW_OK 即可。
                // 或者，可以发送一些初始数据
                // return "HELLO_FROM_CARD".toByteArray() + SW_OK
                return SW_OK
            } else {
                return SW_DATA_INVALID
            }
        }

        // 2. 处理我们自定义的命令
        if (Arrays.equals(CMD_GET_DEVICE_NAME, header)) {
            Log.i(TAG, "Command: GET_DEVICE_NAME")
            val nameBytes = deviceNameToShare.toByteArray(Charsets.UTF_8)
            return nameBytes + SW_OK
        }

        if (Arrays.equals(CMD_CONFIRM_CONNECTION, header)) {
            Log.i(TAG, "Command: CONFIRM_CONNECTION")
            // 可以在这里处理数据 (Lc 和 Data 部分)
            // val lc = commandApdu[4].toInt()
            // val data = commandApdu.copyOfRange(5, 5 + lc)
            // Log.i(TAG, "Data from reader: ${data.toHexString()} (${String(data)})")
            connectionEstablished = true // 标记连接已建立
            // 通知UI (例如通过 LocalBroadcastManager 或 StateFlow)
            // sendConnectionStatusBroadcast(true)
            return "CONN_ACK".toByteArray(Charsets.UTF_8) + SW_OK // Acknowledge connection
        }


        // 如果命令不被识别
        Log.w(TAG, "Unknown command.")
        return SW_INS_NOT_SUPPORTED // 或者 SW_CLA_NOT_SUPPORTED
    }

    override fun onDeactivated(reason: Int) {
        Log.i(TAG, "Deactivated: reason $reason")
        connectionEstablished = false
        // 通知UI连接已断开
        // sendConnectionStatusBroadcast(false)
    }

    // 辅助函数将十六进制字符串转换为字节数组
    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    // 辅助函数将字节数组转换为十六进制字符串 (用于日志)
    private fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

    // (可选) 使用 LocalBroadcastManager 或其他方式通知 Activity 连接状态
    /*
    private fun sendConnectionStatusBroadcast(isConnected: Boolean) {
        val intent = Intent("com.example.myapplication.CONNECTION_STATUS")
        intent.putExtra("isConnected", isConnected)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    */
}