package io.github.zyraxi21.nfc.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.core.graphics.ColorUtils
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.theme.ThemeMode
import com.microsoft.fluentui.theme.token.AliasTokens
import com.microsoft.fluentui.theme.token.FluentAliasTokens
import com.microsoft.fluentui.theme.token.TokenSet

/**
 * 从 Monet 种子色生成 16 级 Fluent brand 色阶。
 * Color10 最深 → Color160 最浅，模拟 Fluent 自带的 brand 渐变。
 */
private fun generateBrandScale(seed: Int): Map<FluentAliasTokens.BrandColorTokens, Color> {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed, hsl)
    val (h, s, _) = hsl
    // 16 级：亮度从 0.12 到 0.96 线性分布
    val steps = listOf(
        FluentAliasTokens.BrandColorTokens.Color10 to 0.12f,
        FluentAliasTokens.BrandColorTokens.Color20 to 0.18f,
        FluentAliasTokens.BrandColorTokens.Color30 to 0.24f,
        FluentAliasTokens.BrandColorTokens.Color40 to 0.30f,
        FluentAliasTokens.BrandColorTokens.Color50 to 0.36f,
        FluentAliasTokens.BrandColorTokens.Color60 to 0.42f,
        FluentAliasTokens.BrandColorTokens.Color70 to 0.48f,
        FluentAliasTokens.BrandColorTokens.Color80 to 0.54f,
        FluentAliasTokens.BrandColorTokens.Color90 to 0.60f,
        FluentAliasTokens.BrandColorTokens.Color100 to 0.66f,
        FluentAliasTokens.BrandColorTokens.Color110 to 0.72f,
        FluentAliasTokens.BrandColorTokens.Color120 to 0.78f,
        FluentAliasTokens.BrandColorTokens.Color130 to 0.84f,
        FluentAliasTokens.BrandColorTokens.Color140 to 0.89f,
        FluentAliasTokens.BrandColorTokens.Color150 to 0.93f,
        FluentAliasTokens.BrandColorTokens.Color160 to 0.96f,
    )
    // 饱和度在中段略高、两端略低，更接近 Fluent 自带色阶的观感
    return steps.associate { (token, lightness) ->
        val adjustedSat = when {
            lightness < 0.2f -> s * 0.7f
            lightness > 0.85f -> s * 0.5f
            else -> s
        }
        token to Color(ColorUtils.HSLToColor(floatArrayOf(h, adjustedSat.coerceIn(0f, 1f), lightness)))
    }
}

private class MonetAliasTokens(seed: Int) : AliasTokens() {
    private val brandScale = generateBrandScale(seed)

    override val brandColor: TokenSet<FluentAliasTokens.BrandColorTokens, Color>
        get() = TokenSet { token -> brandScale[token] ?: Color(0xFF0F6CBD) }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val fgColor = if (darkTheme) Color(0xFFFFFFFF) else Color(0xFF000000)

    // Monet 取色：Android 12+ 用系统动态色，低版本回退 Fluent 默认蓝
    val context = LocalContext.current
    val monetSeed = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.primary.toArgb()
    } else {
        0xFF0F6CBD.toInt() // Fluent 默认 brand 蓝
    }
    val aliasTokens = remember(monetSeed) { MonetAliasTokens(monetSeed) }

    FluentTheme(
        aliasTokens = aliasTokens,
        themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(color = fgColor),
            LocalContentColor provides fgColor
        ) {
            content()
        }
    }
}
