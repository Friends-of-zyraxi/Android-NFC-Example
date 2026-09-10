package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.theme.ThemeMode

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    FluentTheme(
        themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light,
    ) {
        // BasicText 不会自动跟随主题，通过 LocalContentColor 提供自适应前景色
        CompositionLocalProvider(
            LocalContentColor provides if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF000000)
        ) {
            content()
        }
    }
}
