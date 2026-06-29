package com.example.myapplication.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import kotlinx.coroutines.launch

enum class NavigationItem(
    val titleResId: Int,
    val icon: ImageVector
) {
    READ(R.string.nav_read, Icons.Default.Email),
    WRITE(R.string.nav_write, Icons.Default.Edit),
    P2P(R.string.nav_p2p, Icons.Default.Call)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationApp(
    readerScreen: @Composable () -> Unit,
    writeScreen: @Composable () -> Unit,
    p2pScreen: @Composable () -> Unit,
    snackbarHostState: SnackbarHostState? = null
) {
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(initialPage = selectedItemIndex, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    val navigationItems = listOf(NavigationItem.READ, NavigationItem.WRITE, NavigationItem.P2P)

    // 滑动时同步底部导航
    LaunchedEffect(pagerState.currentPage) {
        selectedItemIndex = pagerState.currentPage
    }
    // 点击导航时同步 pager
    LaunchedEffect(selectedItemIndex) {
        if (selectedItemIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(selectedItemIndex)
        }
    }

    Scaffold(
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_title),
                            modifier = Modifier
                                .wrapContentSize(Alignment.Center)
                                .align(Alignment.CenterHorizontally)
                        )
                    }
                )
                Text(
                    text = stringResource(R.string.version_label),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 4.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = stringResource(item.titleResId)) },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = selectedItemIndex == index,
                        onClick = {
                            selectedItemIndex = index
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
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
    MaterialTheme {
        BottomNavigationApp(
            readerScreen = {},
            writeScreen = {},
            p2pScreen = {}
        )
    }
}
