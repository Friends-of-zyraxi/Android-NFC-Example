package io.github.zyraxi21.nfc.util

import android.content.Context
import android.util.Base64

/**
 * HCE 模拟数据的持久化存储。
 *
 * HCE 服务可能在应用进程被系统回收后由系统重新绑定启动，此时
 * [io.github.zyraxi21.nfc.MyHostApduService] 的静态字段已经丢失，
 * 读卡端会读到空内容。因此模拟数据在写入时同步落盘，并在服务被重新拉起时恢复。
 *
 * 数据以 Base64 字符串存放，避免 SharedPreferences 无法直接保存字节数组的限制。
 */
object HceDataStore {

    private const val PREFS_NAME = "hce_emulated_data"
    private const val KEY_DATA = "ndef_bytes"
    private const val KEY_TYPE = "data_type"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 保存模拟数据；[data] 为 null 表示清除已保存的内容 */
    fun save(context: Context, data: ByteArray?, type: String?) {
        prefs(context).edit().apply {
            if (data == null) {
                remove(KEY_DATA)
            } else {
                putString(KEY_DATA, Base64.encodeToString(data, Base64.NO_WRAP))
            }
            if (type == null) remove(KEY_TYPE) else putString(KEY_TYPE, type)
        }.apply()
    }

    /**
     * 读取已保存的模拟数据。
     *
     * @return (NDEF 消息字节, 数据类型名)；从未保存过或数据损坏时返回 null
     */
    fun load(context: Context): Pair<ByteArray, String?>? {
        val prefs = prefs(context)
        val encoded = prefs.getString(KEY_DATA, null) ?: return null
        val bytes = runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null
        return bytes to prefs.getString(KEY_TYPE, null)
    }
}
