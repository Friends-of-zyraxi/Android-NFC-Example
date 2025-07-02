package com.example.myapplication.ui.theme
import com.example.myapplication.ui.theme.NFCReaderScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 1. 使用枚举定义导航项
enum class NavigationItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("读卡", Icons.Default.Email),
    PROFILE("写卡", Icons.Default.Edit),
    SETTINGS("端对端", Icons.Default.Call)
}

// 2. 主界面组件 - 使用索引保存状态
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationApp() {
    // 使用整数索引保存状态（系统支持的基本类型）
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    val navigationItems = listOf(
        NavigationItem.HOME,
        NavigationItem.PROFILE,
        NavigationItem.SETTINGS
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(navigationItems[selectedItemIndex].title) }
            )
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
                NavigationItem.HOME -> HomeScreen()
                NavigationItem.PROFILE -> ProfileScreen()
                NavigationItem.SETTINGS -> SettingsScreen()
            }
        }
    }
}

// 3. 视图组件保持不变
@Composable
fun HomeScreen() {
    Text("读卡界面", style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun ProfileScreen() {
    Text("写卡界面", style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun SettingsScreen() {
    Text("端对端通信界面", style = MaterialTheme.typography.headlineMedium)
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomNavigationApp() {
    MaterialTheme {
        BottomNavigationApp()
    }
}
class NavigationView {
}