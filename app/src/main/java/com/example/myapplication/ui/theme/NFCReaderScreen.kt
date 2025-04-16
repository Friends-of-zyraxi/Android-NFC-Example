package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import kotlinx.coroutines.launch
import com.example.myapplication.util.checkNfcAvailability

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCReaderScreen(
    tagInfo: String,
    tagContent: String,
    isButtonVisible: Boolean,
    snackbarHostState: SnackbarHostState,
    onCheckNfcClick: () -> Unit,
    onWriteCardClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // 自动检查 NFC 可用性
    LaunchedEffect(Unit) {
        checkNfcAvailability(
            isFirstCheck = true,
            showMessage = { messageRes, actionRes, action ->
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
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.nfc_reader_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            if (isButtonVisible) {
                Button(
                    onClick = onCheckNfcClick,
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(top = 16.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text(text = stringResource(R.string.check_nfc_status))
                }
            }

            Text(
                text = stringResource(R.string.supported_formats),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Text(
                text = tagInfo.ifEmpty { stringResource(R.string.scan_a_tag) },
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
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Text(
                        text = tagContent.ifEmpty { stringResource(R.string.display_content) },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onWriteCardClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = stringResource(R.string.goto_write_card))
            }
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
        snackbarHostState = remember { SnackbarHostState() },
        onCheckNfcClick = {},
        onWriteCardClick = {}
    )
}
