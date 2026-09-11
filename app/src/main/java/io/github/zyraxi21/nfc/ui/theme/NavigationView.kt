package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

/**
 * 应用外壳：顶栏 + 分页内容 + 底部导航。
 *
 * 顶栏底色走 [AppTheme.brandSurface]：浅色模式用品牌色（对齐 Word 安卓版带主题色的标题栏），
 * 深色模式退回中性表面 `NeutralBackground3`。深色不能继续用品牌色的原因是硬性的——
 * 那里的 `BrandBackground1` 取 `brandColor[Color100]`，是亮蓝 `#479EF5`，白字压上去
 * 只有约 2.9:1，远低于 WCAG AA 的 4.5:1。详见 [AppTopBar] 与 `AppTheme.brandSurface`。
 *
 * 顶栏与页面文字都通过 `FluentText` 显式指定颜色，不依赖 `LocalContentColor`。
 *
 * edge-to-edge 要点：
 * - 顶栏自己用 `statusBarsPadding()`，让品牌/表面色块贯通状态栏，避免状态栏区域露出画布底色。
 * - 底栏自己用 `navigationBarsPadding()` 贯通手势条。
 * - `Scaffold` 的 `contentWindowInsets` 置零，系统栏 insets 只由顶栏/底栏消费一次，避免重复留白。
 */
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

    // 页面外边距需要在 Scaffold 之外算出来，才能同时用于顶栏与页面内容，
    // 保证顶栏文字与页面文字共用同一条竖向基准线。
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableWidth = this.maxWidth
        val gutter = PageMetrics.gutterFor(availableWidth)
        Scaffold(
            // 系统栏 insets 由顶栏/底栏各自消费，避免 Scaffold 再叠加一层留白
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = AppTheme.canvas,
            snackbarHost = { snackbarHostState?.let { Snackbar(it) } },
            topBar = { AppTopBar(gutter) },
            bottomBar = { AppBottomBar(tabDataList, selectedItemIndex) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                // 超宽屏幕上限制正文列宽，避免行长过长影响可读性
                val contentModifier = if (availableWidth > PageMetrics.maxContentWidth) {
                    Modifier.width(PageMetrics.maxContentWidth).fillMaxHeight()
                } else {
                    Modifier.fillMaxSize()
                }
                ProvidePageGutter(gutter) {
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
    }
}

/**
 * 顶栏：应用标题 + 版本号，底色使用品牌色（对齐 Word 安卓版的标题栏观感）。
 *
 * 为什么浅色模式用品牌色、深色模式改用中性表面色——这不是偏好问题，而是对比度决定的：
 * 深色模式下 Fluent 的 `BrandBackground1` 取 `brandColor[Color100]`，那是一个**亮**蓝
 * （官方值 `#479EF5`），白字压在上面对比度只有约 2.9:1，低于 WCAG AA 的 4.5:1。
 * Fluent 官方的 `AppBarTokens.backgroundBrush` 对 `FluentStyle.Brand` 也是同样处理：
 * 浅色用品牌色、深色退回 `NeutralBackground3`。本工程的品牌色阶已按官方色阶的亮度分布
 * 生成，因此浅色模式的 `Color80` 与官方蓝同级，白字对比度约 5.4:1。
 *
 * 顶栏是唯一承载数据的表面，版本号刻意降到 0.8 透明度以形成次级层次。
 */
@Composable
private fun AppTopBar(gutter: Dp) {
    val dark = isDarkMode()
    val background = if (dark) AppTheme.elevatedSurface else AppTheme.brandSurface
    val titleColor = if (dark) AppTheme.textPrimary else Color.White
    val dividerColor = if (dark) null else AppTheme.strokeSubtle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .drawBehind {
                val strokeColor = dividerColor ?: return@drawBehind
                val stroke = 1.dp.toPx()
                drawLine(
                    color = strokeColor,
                    start = Offset(0f, size.height - stroke / 2),
                    end = Offset(size.width, size.height - stroke / 2),
                    strokeWidth = stroke
                )
            }
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = gutter, vertical = FluentSpacing.m),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FluentText(
                text = stringResource(R.string.app_title),
                style = FluentTextStyle.Title3,
                color = titleColor,
                centered = true,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(FluentSpacing.xxs))
            FluentText(
                text = stringResource(R.string.version_label),
                style = FluentTextStyle.Caption1,
                color = titleColor.copy(alpha = 0.8f),
                centered = true,
                maxLines = 1
            )
        }
    }
}

/**
 * 底栏：Fluent `TabBar` + 手势条区域同色填充。
 *
 * `TabBar` 内部未选中项的背景是 `NeutralBackground1`，容器必须用同一个 `surface` 令牌，
 * 否则深色模式下会在底栏左右两侧露出画布底色形成色带。
 */
@Composable
private fun AppBottomBar(tabDataList: List<TabData>, selectedIndex: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.surface)
    ) {
        TabBar(
            tabDataList = tabDataList,
            selectedIndex = selectedIndex
        )
        // 手势条区域用同色填充，避免 TabBar 下方留白
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(name = "外壳 · 浅色", showBackground = true)
@Composable
private fun PreviewBottomNavigationAppLight() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        BottomNavigationApp(
            readerScreen = {},
            writeScreen = {},
            p2pScreen = {}
        )
    }
}

@Preview(name = "外壳 · 深色", showBackground = true)
@Composable
private fun PreviewBottomNavigationAppDark() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        BottomNavigationApp(
            readerScreen = {},
            writeScreen = {},
            p2pScreen = {}
        )
    }
}
