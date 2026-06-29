package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.util.checkNfcAvailability
import kotlinx.coroutines.launch

@Composable
fun NFCReaderScreen(
    tagInfo: String,
    tagContent: String,
    isButtonVisible: Boolean,
    snackbarHostState: SnackbarHostState? = null,
    onCheckNfcClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    // 冷启动时自动检查一次，后续不再自动检查
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasAutoChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasAutoChecked && snackbarHostState != null) {
            hasAutoChecked = true
            checkNfcAvailability(
                context = context,
                isFirstCheck = true,
                showMessage = { messageRes, actionRes, action ->
                    // NFC 正常时不弹 Snackbar，只在有问题时提示
                    if (messageRes != R.string.msg_nfc_available) {
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = context.getString(messageRes),
                                actionLabel = if (actionRes != 0) context.getString(actionRes) else null
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                action?.invoke()
                            }
                        }
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.reader_title),
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.CenterHorizontally)
                .wrapContentSize(Alignment.Center)
                .padding(24.dp)
        )

        if (isButtonVisible) {
            Button(
                onClick = onCheckNfcClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.reader_check_nfc))
            }
        }

        Text(
            text = stringResource(R.string.reader_supported_formats),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Text(
            text = tagInfo.ifEmpty { stringResource(R.string.reader_scan_hint) },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .weight(1f)
                .padding(vertical = 16.dp)
                .align(Alignment.CenterHorizontally),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                text = tagContent.ifEmpty { stringResource(R.string.reader_display_hint) },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(12.dp)
                    .wrapContentSize(Alignment.Center)
                    .align(alignment = Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NFCReaderScreenPreview() {
    NFCReaderScreen(
        tagInfo = "已扫描标签: Tag[ID:1234]",
        tagContent = "文本: Hello, NFC!\nURI: https://example.com",
        isButtonVisible = true,
        onCheckNfcClick = {},
    )
}
