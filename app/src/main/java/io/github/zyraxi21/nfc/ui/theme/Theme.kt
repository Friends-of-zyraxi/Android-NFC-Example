package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.theme.ThemeMode

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    FluentTheme(
        themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light,
        content = content
    )
}
