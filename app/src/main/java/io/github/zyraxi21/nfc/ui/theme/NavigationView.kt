package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.theme.token.FluentAliasTokens
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
    val brandColor = FluentTheme.aliasTokens.brandColor[FluentAliasTokens.BrandColorTokens.Color80]
    val surfaceColor = if (isDark) Color(0xFF1B1A19) else Color(0xFFF3F2F1)
    val onBrandColor = Color.White

    Scaffold(
        // 关闭 Scaffold 默认的系统栏 insets，由顶栏/底栏自行处理，
        // 使背景色能贯通状态栏和手势条
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = surfaceColor,
        snackbarHost = { snackbarHostState?.let { Snackbar(it) } },
        topBar = {
            // 品牌色背景贯通状态栏，内容在色块内部下移
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brandColor)
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    // 标题栏
                    BasicText(
                        text = stringResource(R.string.app_title),
                        style = TextStyle(
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = onBrandColor
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    // 版本号放在标题栏内
                    BasicText(
                        text = stringResource(R.string.version_label),
                        style = TextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = onBrandColor.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
                    )
                }
            }
        },
        bottomBar = {
            // 底栏背景贯通手势条，TabBar 内容在色块内部上移
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
            ) {
                TabBar(
                    tabDataList = tabDataList,
                    selectedIndex = selectedItemIndex
                )
                // 手势条区域用同色填充，避免 TabBar 下方留白
                Spacer(modifier = Modifier.navigationBarsPadding())
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
