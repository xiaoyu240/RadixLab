package com.radixlab.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 剪贴板工具。
 *
 * 隐私说明：只在用户点击「复制」时**写入**剪贴板，从不读取剪贴板内容。
 */
object ClipboardUtils {

    /** 复制文本并给出轻提示 */
    fun copy(context: Context, text: String, label: String = "RadixLab") {
        if (text.isBlank()) {
            Toast.makeText(context, "没有可复制的内容", Toast.LENGTH_SHORT).show()
            return
        }
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (manager == null) {
            Toast.makeText(context, "当前设备不支持剪贴板", Toast.LENGTH_SHORT).show()
            return
        }
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "已复制：${preview(text)}", Toast.LENGTH_SHORT).show()
    }

    private fun preview(text: String): String =
        if (text.length > 28) text.take(28) + "…" else text
}

/**
 * Compose 便捷函数：`val copy = rememberCopyAction()`
 * 之后在任意按钮里 `copy(value)` 即可。
 */
@Composable
fun rememberCopyAction(label: String = "RadixLab"): (String) -> Unit {
    val context = LocalContext.current
    return remember(context, label) {
        { text: String -> ClipboardUtils.copy(context, text, label) }
    }
}
