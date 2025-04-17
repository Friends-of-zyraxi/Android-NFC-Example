package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.util.checkNfcAvailability
import kotlinx.coroutines.launch

class WifiWriteScreen {
}

@Composable
fun WifiWriteScreen(
    onWriteCardClick: () -> Unit
) {
    var text by remember{ mutableStateOf("")}

    Scaffold() {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Wi-Fi 配置信息",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            OutlinedTextField(
                value = "",
                onValueChange = {
                    text = it
                },
                singleLine = true,
                label = {
                    Text("Wi-Fi 名称（SSID）")
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            OutlinedTextField(
                value = "",
                onValueChange = {
                    text = it
                },
                singleLine = true,
                label = {
                    Text("密码")
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            OutlinedTextField(
                value = "",
                onValueChange = {
                    text = it
                },
                singleLine = true,
                label = {
                    Text("加密类型")
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )
            OutlinedTextField(
                value = "",
                onValueChange = {
                    text = it
                },
                singleLine = true,
                label = {
                    Text("身份验证类型")
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Button(
                onClick = onWriteCardClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(top = 24.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = "确定")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WifiReaderScreenPreview() {
    WifiWriteScreen(
        onWriteCardClick = {}
    )
}