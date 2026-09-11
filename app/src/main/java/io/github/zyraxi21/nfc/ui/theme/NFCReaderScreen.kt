package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.github.zyraxi21.nfc.R

/**
 * 读卡页。
 *
 * 布局规范：
 * - 左右边距由 `PageColumn` 统一提供（手机 24dp、宽屏 32/40dp），任何文字与控件都不贴屏。
 * - 所有文字统一居中，且颜色由 `FluentText` 显式指定，不依赖 `LocalContentColor`。
 * - 内容卡片用 `weight(1f)` 撑满剩余空间，内部可滚动，因为 NDEF 解析结果长度不可预期
 *   （Wi-Fi / 蓝牙记录可能带十六进制转储）。
 */
@Composable
fun NFCReaderScreen(
    tagInfo: String,
    tagContent: String,
    isButtonVisible: Boolean,
    onCheckNfcClick: () -> Unit = {},
) {
    PageColumn(verticalArrangement = Arrangement.spacedBy(FluentSpacing.m)) {
        FluentText(
            text = stringResource(R.string.reader_title),
            style = FluentTextStyle.Title3
        )

        if (isButtonVisible) {
            FluentButton(
                onClick = onCheckNfcClick,
                text = stringResource(R.string.reader_check_nfc),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PageMetrics.minTouchTarget)
            )
        }

        FluentText(
            text = stringResource(R.string.reader_supported_formats),
            style = FluentTextStyle.Caption1,
            color = AppTheme.textHint
        )

        // 标签类型信息：无标签时显示扫描提示
        FluentText(
            text = tagInfo.ifEmpty { stringResource(R.string.reader_scan_hint) },
            style = if (tagInfo.isEmpty()) FluentTextStyle.Body2 else FluentTextStyle.Body2Strong
        )

        FluentCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(FluentSpacing.mPlus),
                contentAlignment = Alignment.Center
            ) {
                FluentText(
                    text = tagContent.ifEmpty { stringResource(R.string.reader_display_hint) },
                    style = FluentTextStyle.Body2,
                    color = if (tagContent.isEmpty()) AppTheme.textHint else AppTheme.textPrimary,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip
                )
            }
        }

        // 卡片与底栏之间留出呼吸空间，避免最后一行内容紧贴底栏边缘
        Spacer(modifier = Modifier.height(FluentSpacing.s))
    }
}

@Preview(name = "读卡 · 浅色", showBackground = true)
@Composable
private fun NFCReaderScreenPreviewLight() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        NFCReaderScreen(
            tagInfo = "该 NFC 标签的类型：TAG: Tech [android.nfc.tech.Ndef]",
            tagContent = "文本: Hello, NFC!\nURI: https://example.com",
            isButtonVisible = true,
            onCheckNfcClick = {},
        )
    }
}

@Preview(name = "读卡 · 深色", showBackground = true)
@Composable
private fun NFCReaderScreenPreviewDark() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        NFCReaderScreen(
            tagInfo = "该 NFC 标签的类型：TAG: Tech [android.nfc.tech.Ndef]",
            tagContent = "文本: Hello, NFC!\nURI: https://example.com",
            isButtonVisible = true,
            onCheckNfcClick = {},
        )
    }
}

@Preview(name = "读卡 · 深色 · 空", showBackground = true)
@Composable
private fun NFCReaderScreenPreviewDarkEmpty() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        NFCReaderScreen(
            tagInfo = "",
            tagContent = "",
            isButtonVisible = true,
            onCheckNfcClick = {},
        )
    }
}
