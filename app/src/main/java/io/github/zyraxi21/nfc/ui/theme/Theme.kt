package io.github.zyraxi21.nfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.microsoft.fluentui.theme.FluentTheme
import com.microsoft.fluentui.theme.ThemeMode
import com.microsoft.fluentui.theme.token.AliasTokens
import com.microsoft.fluentui.theme.token.FluentAliasTokens
import com.microsoft.fluentui.theme.token.TokenSet
import kotlin.math.pow

/**
 * 默认品牌色：Fluent 2 官方 brandColor 色阶的 Color80。
 *
 * 之所以不能只给一个"品牌色"：Fluent 的 brand 色板是 16 级色阶，
 * `BrandBackground1` / `BrandForeground1` 等 alias token 分别引用其中不同的一级，
 * 用来保证"品牌色当背景"和"品牌色当前景"两种场景各自达到对比度要求。
 * 因此这里必须给出完整色阶，而不是一个孤立的颜色。
 */
private val FLUENT_BRAND_RAMP: Map<FluentAliasTokens.BrandColorTokens, Color> = mapOf(
    FluentAliasTokens.BrandColorTokens.Color10 to Color(0xFF061724),
    FluentAliasTokens.BrandColorTokens.Color20 to Color(0xFF082338),
    FluentAliasTokens.BrandColorTokens.Color30 to Color(0xFF0A2E4A),
    FluentAliasTokens.BrandColorTokens.Color40 to Color(0xFF0C3B5E),
    FluentAliasTokens.BrandColorTokens.Color50 to Color(0xFF0E4775),
    FluentAliasTokens.BrandColorTokens.Color60 to Color(0xFF0F548C),
    FluentAliasTokens.BrandColorTokens.Color70 to Color(0xFF115EA3),
    FluentAliasTokens.BrandColorTokens.Color80 to Color(0xFF0F6CBD),
    FluentAliasTokens.BrandColorTokens.Color90 to Color(0xFF2886DE),
    FluentAliasTokens.BrandColorTokens.Color100 to Color(0xFF479EF5),
    FluentAliasTokens.BrandColorTokens.Color110 to Color(0xFF62ABF5),
    FluentAliasTokens.BrandColorTokens.Color120 to Color(0xFF77B7F7),
    FluentAliasTokens.BrandColorTokens.Color130 to Color(0xFF96C6FA),
    FluentAliasTokens.BrandColorTokens.Color140 to Color(0xFFB4D6FA),
    FluentAliasTokens.BrandColorTokens.Color150 to Color(0xFFCFE4FA),
    FluentAliasTokens.BrandColorTokens.Color160 to Color(0xFFEBF3FC)
)

/**
 * WCAG 2.1 定义的相对亮度。
 *
 * 纯 Kotlin 实现，不复用 `android.graphics.Color` / `ColorUtils.calculateLuminance`：
 * 一是可以在 JVM 单元测试里直接跑（不需要 Robolectric），
 * 二是 ColorUtils 的系数是历史近似值，WCAG 的正式公式更准确。
 */
internal fun relativeLuminance(color: Color): Double {
    fun channel(component: Float): Double {
        val v = component.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}

/**
 * Fluent 官方品牌色阶的**相对亮度分布**，作为任意品牌色展开时的锚点。
 *
 * 直接照搬官方 `getBrandRamp`（把种子亮度按比例混向黑/白）在系统动态色下会失效：
 * 该算法假定种子是一颗亮度中段的品牌色，一旦种子偏亮（浅黄、中性灰）或偏暗（深海军蓝），
 * 算出来的 Color80 / Color100 就会落到无法阅读的亮度上。实测过的坏结果：
 * 浅黄 `#FFD54F` → Color80 白底 1.69:1；中性灰 `#9A9A9A` → 2.35:1；
 * 深海军蓝 `#001E30` → Color100 深色底 4.00:1。
 *
 * 因此改为：**只借种子的色相与饱和度，亮度逐级对齐官方色阶**。
 * 这样无论系统主题色是什么，得到的色阶在明暗两端都具备与官方品牌色相同的可读性。
 *
 * 这几档关键亮度对应的对比度（官方默认蓝已验证）：
 * - Color80（浅色模式的 BrandBackground1）：白字压上去 5.38:1；
 * - Color90（深色模式的 TabBar 选中态）：深色画布 `#141414` 上 4.87:1；
 * - Color100（深色模式的 BrandForeground1）：深色表面 `#1F1F1F` 上 5.87:1。
 */
private val OFFICIAL_RAMP_LUMINANCE = doubleArrayOf(
    0.0078, 0.0154, 0.0251, 0.0401,
    0.0588, 0.0834, 0.1077, 0.1450,
    0.2278, 0.3239, 0.3832, 0.4450,
    0.5377, 0.6470, 0.7565, 0.8879
)

/** 品牌色阶的 16 级枚举，顺序与 [OFFICIAL_RAMP_LUMINANCE] 一一对应。 */
private val RAMP_TOKENS = listOf(
    FluentAliasTokens.BrandColorTokens.Color10,
    FluentAliasTokens.BrandColorTokens.Color20,
    FluentAliasTokens.BrandColorTokens.Color30,
    FluentAliasTokens.BrandColorTokens.Color40,
    FluentAliasTokens.BrandColorTokens.Color50,
    FluentAliasTokens.BrandColorTokens.Color60,
    FluentAliasTokens.BrandColorTokens.Color70,
    FluentAliasTokens.BrandColorTokens.Color80,
    FluentAliasTokens.BrandColorTokens.Color90,
    FluentAliasTokens.BrandColorTokens.Color100,
    FluentAliasTokens.BrandColorTokens.Color110,
    FluentAliasTokens.BrandColorTokens.Color120,
    FluentAliasTokens.BrandColorTokens.Color130,
    FluentAliasTokens.BrandColorTokens.Color140,
    FluentAliasTokens.BrandColorTokens.Color150,
    FluentAliasTokens.BrandColorTokens.Color160
)

/** HSL → RGB。h/s/l 均取 [0,1]。 */
private fun hslToColor(h: Float, s: Float, l: Float): Color {
    if (s <= 0f) return Color(red = l, green = l, blue = l, alpha = 1f)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q

    fun channel(t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }

    return Color(
        red = channel(h + 1f / 3f),
        green = channel(h),
        blue = channel(h - 1f / 3f),
        alpha = 1f
    )
}

/** 按给定比例把 [color] 混向 [target]（0 = 原色，1 = 目标色）。 */
private fun blend(color: Color, target: Color, fraction: Double): Color {
    val f = fraction.coerceIn(0.0, 1.0).toFloat()
    return Color(
        red = color.red + (target.red - color.red) * f,
        green = color.green + (target.green - color.green) * f,
        blue = color.blue + (target.blue - color.blue) * f,
        alpha = 1f
    )
}

private val BLEND_TO_WHITE = Color(0xFFFFFFFF)
private val BLEND_TO_BLACK = Color(0xFF000000)

/**
 * 从种子色展开 16 级品牌色阶，逐级对齐 [OFFICIAL_RAMP_LUMINANCE]。
 *
 * 保留种子的色相，饱和度按官方蓝标定为 0.85；亮度用二分查找求解，
 * 使每一级的 WCAG 相对亮度命中官方色阶的对应值。
 *
 * 若某级目标亮度超出"该色相+饱和度在亮度 1.0 时"能达到的上限（黄色系必然如此），
 * 直接取该上限（即 l = 1.0 的最亮端），保证色阶仍然单调、不出现回退。
 */
internal fun generateBrandRamp(seed: Int): Map<FluentAliasTokens.BrandColorTokens, Color> {
    val base = Color(seed or 0xFF000000.toInt())
    val maxChannel = maxOf(base.red, base.green, base.blue)
    val minChannel = minOf(base.red, base.green, base.blue)
    val lightness = (maxChannel + minChannel) / 2f
    val chroma = maxChannel - minChannel

    // 只取色相与饱和度，亮度完全由官方色阶的亮度基准决定
    val saturation = when {
        chroma == 0f -> 0f
        lightness < 0.5f -> chroma / (maxChannel + minChannel)
        else -> chroma / (2f - maxChannel - minChannel)
    }.coerceIn(0f, 1f)
    val hue = when {
        chroma == 0f -> 0f
        maxChannel == base.red ->
            ((base.green - base.blue) / chroma).let { if (it < 0f) it + 6f else it } / 6f
        maxChannel == base.green -> ((base.blue - base.red) / chroma + 2f) / 6f
        else -> ((base.red - base.green) / chroma + 4f) / 6f
    }

    return RAMP_TOKENS.mapIndexed { index, token ->
        token to colorAtLuminance(hue, saturation, OFFICIAL_RAMP_LUMINANCE[index])
    }.toMap()
}

/**
 * 在固定色相与饱和度的前提下，求使 WCAG 相对亮度最接近 [targetLuminance] 的颜色。
 *
 * 亮度对 HSL 的 L 单调递增，因此可以二分；[targetLuminance] 高于该色相/饱和度能
 * 达到的上限时返回最亮端。色阶展开只做一次（remember 缓存），成本可忽略。
 */
private fun colorAtLuminance(hue: Float, saturation: Float, targetLuminance: Double): Color {
    if (relativeLuminance(hslToColor(hue, saturation, 1f)) <= targetLuminance) {
        return hslToColor(hue, saturation, 1f)
    }
    var low = 0f
    var high = 1f
    repeat(24) {
        val mid = (low + high) / 2f
        if (relativeLuminance(hslToColor(hue, saturation, mid)) > targetLuminance) {
            high = mid
        } else {
            low = mid
        }
    }
    return hslToColor(hue, saturation, low)
}

/** 用自定义品牌色阶替换 Fluent 默认色阶，其余 alias token 沿用官方实现。 */
private class MonetAliasTokens(seed: Int?) : AliasTokens() {
    private val ramp = seed?.let { generateBrandRamp(it) } ?: FLUENT_BRAND_RAMP

    override val brandColor: TokenSet<FluentAliasTokens.BrandColorTokens, Color>
        get() = TokenSet { token ->
            ramp[token] ?: FLUENT_BRAND_RAMP.getValue(FluentAliasTokens.BrandColorTokens.Color80)
        }
}

/**
 * 应用主题。
 *
 * 这里只做三件事，且都必须做对，否则深色模式一定出问题：
 *
 * 1. 把 [ThemeMode] 交给 `FluentTheme`。Fluent 的 alias token 是
 *    `FluentColor(light, dark)` 双值结构，只有拿到正确的 `themeMode`，
 *    `neutralForegroundColor` / `neutralBackgroundColor` 才会解析成深色模式的那一半。
 * 2. 通过 `LocalContentColor` 提供"前景色"。`BasicText` 的默认颜色是
 *    `Color.Unspecified`，由 `LocalContentColor` 解析；Fluent 自身只提供 alias token、
 *    不会设置这个 CompositionLocal，所以必须由主题层补齐。
 *    **绝不能写成 `if (darkTheme) Color.White else Color.Black`**：那样在浅色模式下
 *    会让所有文字变白，在深色模式下又依赖背景恰好是黑的，任何背景色变化都会导致文字消失。
 * 3. 通过 `LocalTextStyle` 提供默认排版（Fluent Body1）。同样只给字号/行高/字重，
 *    颜色继续走 `LocalContentColor`，避免排版里固化一个不会随主题变化的颜色。
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val themeMode = if (darkTheme) ThemeMode.Dark else ThemeMode.Light

    // Monet 取色：用系统动态色的 primary 作为品牌色种子。
    // minSdk = 34，动态取色（API 31+）在所有支持的设备上都可用，无需版本判断。
    val context = LocalContext.current
    val monetSeed = remember(darkTheme, dynamicColor) {
        if (dynamicColor) {
            val scheme = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            scheme.primary.toArgb()
        } else {
            null
        }
    }
    val aliasTokens = remember(monetSeed) { MonetAliasTokens(monetSeed) }

    FluentTheme(
        aliasTokens = aliasTokens,
        themeMode = themeMode
    ) {
        val defaultTextStyle = FluentTextStyle.Body1.style()
        CompositionLocalProvider(
            LocalContentColor provides AppTheme.textPrimary,
            LocalTextStyle provides defaultTextStyle,
            content = content
        )
    }
}

/** 供预览使用：当前主题模式的可读名字。 */
@Composable
@ReadOnlyComposable
internal fun currentThemeModeLabel(): String = if (isDarkMode()) "Dark" else "Light"
