package com.example.myapplication.ui.theme
import androidx.compose.foundation.background
import com.example.myapplication.ui.theme.NFCReaderScreen
import com.example.myapplication.ui.theme.WriteCardScreen
import com.example.myapplication.ui.theme.P2PScreen
import com.example.myapplication.ReadCard
import com.example.myapplication.WriteCard
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R

// 1. 使用枚举定义导航项
enum class NavigationItem(
    val title: String,
    val icon: ImageVector
) {
    READ("读卡", Icons.Default.Email),
    WRITE("写卡", Icons.Default.Edit),
    P2P("端对端", Icons.Default.Call)
}

// 2. 主界面组件 - 使用索引保存状态
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationApp(
    readerScreen: @Composable () -> Unit,
    writeScreen: @Composable () -> Unit,
    p2pScreen: @Composable () -> Unit
) {
    // 使用整数索引保存状态（系统支持的基本类型）
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    val navigationItems = listOf(
        NavigationItem.READ,
        NavigationItem.WRITE,
        NavigationItem.P2P
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.nfc_reader_title),
                            modifier = Modifier
                                .wrapContentSize(Alignment.Center)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                )
                Text(
                    text = "版本: 20250917",
                    style = MaterialTheme.typography.bodySmall, // 使用较小的文本样式
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally) // 使文本在 TopAppBar 下方居中
                        .padding(bottom = 4.dp) // 添加一些底部间距
                )
            }
        },
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItemIndex == index,
                        onClick = { selectedItemIndex = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (navigationItems[selectedItemIndex]) {
                NavigationItem.READ -> readerScreen()
                NavigationItem.WRITE -> writeScreen()
                NavigationItem.P2P -> p2pScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomNavigationApp() {
    MaterialTheme {
        BottomNavigationApp(
            readerScreen={},
            writeScreen={},
            p2pScreen = {}
        )
    }
}
class NavigationView {
}