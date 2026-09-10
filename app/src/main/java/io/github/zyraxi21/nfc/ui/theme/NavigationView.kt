package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.tokenized.AppBar
import com.microsoft.fluentui.tokenized.navigation.TabBar
import com.microsoft.fluentui.tokenized.navigation.TabData
import com.microsoft.fluentui.tokenized.notification.Snackbar
import com.microsoft.fluentui.tokenized.notification.SnackbarState
import io.github.zyraxi21.nfc.R
import kotlinx.coroutines.launch

enum class NavigationItem(
    val titleResId: Int,
    val icon: ImageVector
) {
    READ(R.string.nav_read, Icons.Default.Email),
    WRITE(R.string.nav_write, Icons.Default.Edit),
    P2P(R.string.nav_p2p, Icons.Default.Call)
}

@Composable
fun BottomNavigationApp(
    readerScreen: @Composable () -> Unit,
    writeScreen: @Composable () -> Unit,
    p2pScreen: @Composable () -> Unit,
    snackbarHostState: SnackbarState? = null
) {
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedItemIndex, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val navigationItems = listOf(NavigationItem.READ, NavigationItem.WRITE, NavigationItem.P2P)

    // 用户滑动 pager 结束后，同步底部导航高亮（用 settledPage 避免动画中间帧干扰）
    LaunchedEffect(pagerState.settledPage) {
        selectedItemIndex = pagerState.settledPage
    }

    val tabDataList = navigationItems.mapIndexed { index, item ->
        TabData(
            title = stringResource(item.titleResId),
            icon = item.icon,
            selected = selectedItemIndex == index,
            onClick = {
                selectedItemIndex = index
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            }
        )
    }

    val isDark = isSystemInDarkTheme()
    Scaffold(
        containerColor = if (isDark) Color(0xFF1B1A19) else Color(0xFFF3F2F1),
        snackbarHost = { snackbarHostState?.let { Snackbar(it) } },
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                AppBar(
                    title = stringResource(R.string.app_title),
                    centerAlignAppBar = true
                )
                BasicText(
                    text = stringResource(R.string.version_label),
                    style = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                TabBar(
                    tabDataList = tabDataList,
                    selectedIndex = selectedItemIndex
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentModifier = if (this.maxWidth > 680.dp) {
                Modifier.widthIn(max = 680.dp).fillMaxSize()
            } else {
                Modifier.fillMaxSize()
            }
            HorizontalPager(
                state = pagerState,
                modifier = contentModifier,
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> readerScreen()
                    1 -> writeScreen()
                    2 -> p2pScreen()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBottomNavigationApp() {
    FluentTheme {
        BottomNavigationApp(
            readerScreen = {},
            writeScreen = {},
            p2pScreen = {}
        )
    }
}
