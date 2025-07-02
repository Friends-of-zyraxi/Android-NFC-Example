package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.WifiWriteScreen

class WriteWiFi  : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置Compose界面
        setContent {
            MyApplicationTheme {
                WifiWriteScreen(
                    onWriteCardClick = {}
                )
            }
        }
    }
}