---
feature: dark-mode-token-conformance
status: delivered
updated: 2026-09-28
branch: main
commits: HEAD
---

# 深色模式适配修复与 Fluent 2 规范化

## Report

**What was built** — 新增 `ui/theme/DesignTokens.kt` 作为 Fluent 2 设计令牌在本工程的唯一入口，把颜色、排版、间距、形状、高度、动效全部收敛成语义令牌；**所有文本一律走 `FluentText`，颜色在组合期就被显式写进 `TextStyle`，不再依赖 `LocalContentColor`**；顶栏底色改为品牌色（浅色）与中性表面（深色）；三页布局统一走 `PageColumn` 的响应式外边距，文字全部居中；文本输入框通过自定义 `TextFieldTokens` + 外层 `Surface` 实现圆角；全应用圆角统一为 16dp；Snackbar 自绘以统一圆角与间距。新增 `ThemeContrastTest`（15 个用例）把 WCAG 对比度与色阶亮度基准写成断言。

**Verification** — `./gradlew :app:assembleDebug` BUILD SUCCESSFUL；`./gradlew :app:testDebugUnitTest` 15/15 通过；`./gradlew :app:lintDebug` BUILD SUCCESSFUL，0 error，新增 0 条告警（33 条均为既有的依赖版本提示与模板遗留资源）。无模拟器可用，深色模式的实际观感仍需真机确认。

**Journey log** — 踩坑记录：

1. **`ColorUtils.calculateLuminance` 在 JVM 单测里会抛 `Method red in android.graphics.Color not mocked`**。它内部调用 `android.graphics.Color`，Robolectric 未接入时只有 stub。解决办法是把 WCAG 相对亮度与颜色混合都改成纯 Kotlin 实现，顺带也比 `ColorUtils` 的近似系数更符合 WCAG 正式公式。
2. **`Base.Theme.MyApplication` 会被 AAPT 解析成父样式 `Base.Theme`**，报 `resource style/Base.Theme not found`。Android 把 style 名字里第一个点当作父样式分隔符。基础主题改名 `BaseTheme_MyApplication`，并删掉 `values-v23/themes.xml`——多级主题只放 `values/` 与 `values-night/`，由 DayNight 限定符切换。
3. **`BasicText` 没有 `textAlign` 参数**，对齐必须写进 `TextStyle`。
4. **Fluent 的 `TextField` 没有任何颜色参数**（签名里只有 value/onValueChange/label/hintText/errorString），配色必须通过 `textFieldTokens`；而且它的 `TextFieldTokens` 里**没有 cornerRadius 令牌**，圆角只能靠外层容器提供。另外 `TextField` 没有 `singleLine` 参数，它天然是多行输入。
5. **`TextFieldTokens` 的配色方法都是 `@Composable open fun`**，覆写时漏掉 `@Composable` 会变成"同名不同签名"的新函数，报 `Conflicting overloads`。
6. **`verticalScroll` 之后接 `fillMaxSize` 会丢失父级高度**，改为 `fillMaxWidth().fillMaxHeight()`，否则 `weight(1f)` 的卡片会塌缩成内容高度。
7. **`enableEdgeToEdge()` 默认跟随系统 `isSystemInDarkTheme()` 判断系统栏图标明暗**，不跟随应用主题。深色模式下顶栏是 `#292929`，若系统栏图标仍是深色就完全看不见，必须显式传 `SystemBarStyle`。

**第二轮修复的根因说明** — 第一轮把「文字全黑」归因为 `Theme.kt` 里写反的 `if (darkTheme) Color.BLACK else Color.WHITE`，改成从 `NeutralForeground*` 取值后，用户实测**仍然全黑**（浅色模式下则是全白）。像素采样结果：深色截图文字 `#000000`、浅色截图文字 `#FFFFFF`，与旧代码的病征一模一样。核对 APK 的 DEX 确认新代码确实已打进包（`classes5.dex` 含 `DesignTokensKt` / `AppTheme` / `MonetAliasTokens`），因此判断问题出在 `BasicText` 的默认色 `Color.Unspecified` 于运行期向 `LocalContentColor` 求值时落到了 `Color.Black`——这条链路编译期与单测都无法验证。**最终修法是釜底抽薪：不再让任何文本依赖这个隐式的 CompositionLocal**，改为 `FluentText` 在组合期把颜色直接写进 `TextStyle`。

**对比度测试发现的两个真实缺陷**（都是先写断言才暴露的）：

- Fluent 的 `NeutralForeground3` 浅色值是 `#808080`，压在画布 `#F5F5F5` 上只有 **3.62:1**，达不到 AA 的 4.5:1。这是 Fluent 官方色阶本身的取值，因此新增 `AppTheme.textHint`（取次级前景色值）专门用于小号说明文字，并把 `textTertiary` 限制为装饰性元素。
- 官方 `getBrandRamp` 在系统动态色下会产出不可读的品牌色：浅黄 `#FFD54F` → Color80 白底 **1.69:1**；中性灰 `#9A9A9A` → **2.35:1**；深海军蓝 `#001E30` → Color100 深色底 **4.00:1**。根因是该算法假定种子是亮度中段的品牌色。解决方案见下文"品牌色阶"。
## [S1] Problem

应用在深色模式下不可用：

1. **顶栏特别亮**。`NavigationView.kt` 取 `FluentTheme.aliasTokens.brandColor[Color80]` 当顶栏底色。`brandColor` 是**单一**色阶，不含明暗双值；在深色模式下 color80 仍是浅色（Monet 种子时尤其明显），而标题文字固定为白色，白字压浅蓝底的对比度最低可到 1.02:1。
2. **文字全是黑色，与背景混在一起**。`Theme.kt` 用 `val fgColor = if (darkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)` 同时提供给 `LocalTextStyle` 与 `LocalContentColor`——浅色白字、深色黑字，正好反了。`BasicText` 的默认颜色是 `Color.Unspecified`，由 `LocalContentColor` 运行时解析，Fluent 自身不提供这个 CompositionLocal，因此全部正文都落到默认黑色。深色画布 `#1B1A19` 上黑字对比度 1.13:1。**这一点在第一轮修复后依然复现**（详见 Report 的「第二轮修复的根因说明」），最终改为把颜色显式写进 `TextStyle` 才彻底解决。
3. **文字贴屏**。各元素混用 `fillMaxWidth()` / `fillMaxWidth(0.8f)` / `fillMaxWidth(0.9f)`，只有外层一个 16dp padding，导致"支持的格式"一行比标题更靠边；`Color.Gray` 在深色底上 3.4:1，`Color.Red` 4.0:1，都不达 AA。

另有 `values-v23/themes.xml` 把 `Theme.MyApplication` 的父样式换成 `Base.Theme.MyApplication`（Material3 DayNight），并写死 `windowLightStatusBar = true`，深色模式下状态栏是深色图标压在深色内容上。

## [S2] Design

### 令牌分层

界面代码只允许通过令牌取色，禁止 `Color.Black` / `Color.White` / `Color.Gray` / `Color.Red` 这类固定色。

| 令牌 | 来源 | 浅色 | 深色 |
|---|---|---|---|
| `AppTheme.canvas` | `NeutralBackgroundColor.CanvasBackground` | `#F5F5F5` | `#141414` |
| `AppTheme.surface` | `NeutralBackgroundColor.Background1` | `#FFFFFF` | `#1F1F1F` |
| `AppTheme.elevatedSurface` | `NeutralBackgroundColor.Background3` | `#FFFFFF` | `#292929` |
| `AppTheme.textPrimary` | `NeutralForegroundColor.Foreground1` | `#242424` | `#FFFFFF` |
| `AppTheme.textSecondary` | `NeutralForegroundColor.Foreground2` | `#616161` | `#D6D6D6` |
| `AppTheme.textHint` | 同 `textSecondary` | `#616161` | `#D6D6D6` |
| `AppTheme.textTertiary` | `NeutralForegroundColor.Foreground3` | `#808080` | `#ADADAD` |
| `AppTheme.stroke` | `NeutralStrokeColor.Stroke1` | `#E0E0E0` | `#4D4D4D` |
| `AppTheme.brand` | `BrandForegroundColor.BrandForeground1` | `Color80` | `Color100` |
| `AppTheme.danger` | `ErrorAndStatusColor.DangerForeground1` | `#B10E1C` | `#FF99A4` |
| `AppTheme.success` | `ErrorAndStatusColor.SuccessForeground1` | — | — |

### 顶栏底色：浅色用品牌色，深色用中性表面

对照微软 Word 安卓版（标题栏带主题色）以及 Fluent 官方 `AppBarTokens.backgroundBrush` 的 `FluentStyle.Brand` 分支：**浅色用 `BrandBackground1`（品牌色）+ 白字，深色退回 `NeutralBackground3` + 浅字**。深色不能继续用品牌色是硬性原因——那里的 `BrandBackground1` 取 `brandColor[Color100]`，是亮蓝 `#479EF5`，白字压上去只有约 2.9:1，低于 AA。

本工程按同一策略实现为 `AppTheme.brandSurface`（`FluentColor(light = BrandBackground1, dark = NeutralBackground3)`）。因为品牌色阶已按官方色阶的亮度分布生成，浅色模式的 `Color80` 与官方蓝同级，白字对比度约 5.4:1。

浅色模式顶栏与画布不同色，用 1dp `strokeSubtle` 描边分隔；深色模式靠 `#292929` 与 `#141414` 的表面色差，不加描边（与 Fluent AppBar 一致）。

### 品牌色阶

不照搬官方 `getBrandRamp`。该算法把种子色按比例混向黑/白，**假定种子是亮度中段的品牌色**；系统动态色不满足这个前提，会产生不可读的结果（见 Report 的实测数据）。

改为：**只取种子的色相与饱和度，亮度逐级对齐官方 Fluent 色阶的亮度分布**（`OFFICIAL_RAMP_LUMINANCE`，取自官方默认蓝的 16 级 WCAG 相对亮度）。

这样无论系统主题色是什么，色阶在明暗两端都与官方品牌色具有相同的可读性：

| 色阶 | 用途 | 官方亮度 | 对比度 |
|---|---|---|---|
| Color80 | 浅色 `BrandBackground1`（顶栏）| 0.1450 | 白字 5.38:1 |
| Color90 | 深色底栏选中态 | 0.2278 | 深色画布 4.87:1 |
| Color100 | 深色 `BrandForeground1` | 0.3239 | 深色表面 5.87:1 |

超出该色相/饱和度可达上限的级别（黄色系的高亮端）停在最亮端，保证色阶单调不回退。

### 圆角统一

全应用**只有一个圆角值**：`FluentShapes.radius = 16dp`（Fluent `CornerRadius160`）。按钮、输入框、下拉选择框、卡片、菜单浮层、对话框、Snackbar 全部引用它。

保持单一值而不是分档：同一屏出现 12dp 与 16dp 反而显得不齐；统一后任何新控件只要引用 `radius` 就自动与既有界面一致。48dp 高的控件用 16dp 圆角仍在矩形控件范围内，不会趋近胶囊形（胶囊形需要半径达到高度的一半，即 24dp）。

Fluent 默认值是按钮 4dp / 菜单 8dp / 卡片 12dp，这里整体放大。部分控件无法通过参数指定圆角，因此在 `DesignTokens.kt` 里为每个控件覆写 `cornerRadius` 令牌，并包一层同名 `Fluent*` 组件：

- `FluentButton` → 覆写 `ButtonTokens.cornerRadius`
- `FluentCard` → 覆写 `BasicCardTokens.cornerRadius`
- `FluentDropdownMenu` → 覆写 `MenuTokens.cornerRadius`
- `FluentTextField` → 外层 `Surface` 的 `RoundedCornerShape`（`TextFieldTokens` 没有圆角令牌）
- `FluentSnackbarHost` → 完全自绘，原因见下

**不用自定义 `ControlTokens` 整套替换主题**：`FluentTheme` 的文档明确写着「若同时显式提供 `aliasTokens` 与 `controlTokens`，`FluentTheme` 不再触发 Fluent Control 的更新」，那会牺牲运行期换肤能力。按控件传 tokens 是官方支持、影响面最小的方式。

### Snackbar：自绘宿主

Fluent 的 `Snackbar` 组件**无法**满足统一圆角与间距的要求，因此改为自绘：

1. 它的圆角在内部硬编码为 `RoundedCornerShape(8.dp)`，而 `SnackBarTokens` 里没有任何圆角令牌，无法通过 token 改；
2. 负责时长与超时逻辑的 `NotificationContainer` 是 `internal`，外部拿不到，所以也不能"自己画外层、复用内层"来绕过；
3. 它在根布局里自带 `padding(horizontal = 16.dp)`，外挂背景会与内容错位。

好在 `SnackbarState.currentSnackbar` 与 `SnackbarMetadata` 都是**公开** API（含 `clicked()` / `dismiss()` / `timedOut()` 与 `duration`），于是 `FluentSnackbarHost` 直接用自己的 `Surface` 渲染，圆角、间距、配色全部走语义令牌；时长与无障碍超时沿用 Fluent 的 `NotificationDuration.convertToMillis`，行为不退化。

间距方面：`Scaffold` 会把 `snackbarHost` 槽位紧贴在底栏上沿（`layoutHeight - snackbarHeight - bottomBarHeight`），原本与底栏粘在一起；现在由 `FluentSpacing.snackbarBottomSpacing = 16dp` 让出空隙，左右再各让一个页面外边距，与页面内容对齐。

### 底栏：自实现点击区

原先底栏直接用 Fluent 的 `TabBar`，手势条那一段的点击与涟漪反馈到不了屏幕底部，按下去能明显看出白色手势条是脱开的。

**先后试过三种在 `TabBar` 上打补丁的做法，都不成立**：

1. `TabBar` + `Spacer(navigationBarsPadding())`：手势条区域虽然被同色填充，但它不属于任何 `TabItem`；
2. 给 `TabBar` 加 `navigationBarsPadding()`：`TabBar` 内部的 `Row` 是 `Modifier.fillMaxWidth()`，**没有等高**，`TabItem` 的 `weight(1F)` 在 `Row` 里只分配宽度，高度仍是内容高度——`padding` 只是在 `Column` 里多出一段空白，点击区并没有跟着下去；
3. 给 `TabBar` 设更大的固定高度：`Row` 同样不会跟着长高。

根因是 Fluent 的 `TabItem` 高度完全由内容决定，且 `TabBar` 不暴露 `TabItem` 的 modifier。因此**底栏改为自实现**：

- 每个 Tab 是一个 `fillMaxHeight` 的点击区，高度 = 顶部留白 + 图标 + 文字 + `WindowInsets.navigationBars`，点击与涟漪铺满整块并一直延伸到屏幕底部；
- 图标与文字下方用 `Spacer(navigationBarsPadding())` 撑出手势条区域，这段与 Tab **同一背景色**，视觉上背景是连通的，不会有"图标区一块、手势条一块"的割裂感；
- inset 由 `WindowInsets.navigationBars` 换算，不硬编码手势条高度，三键导航同样成立；
- 配色沿用原 `TabBar` 的语义：容器用 `AppTheme.surface`（Fluent 的 `TabItem` 未选中项背景也是 `NeutralBackground1`），未选中文字/图标用次级前景色，选中用品牌色，顶部保留 1dp 描边。

### 自实现控件必须显式提供点击涟漪

底栏改成自实现后一度完全没有点击反馈。原因不是布局，而是 `Modifier.clickable` 在不传 `indication` 时会取 `LocalIndication.current`，而本工程的主题栈（`FluentTheme` + 一个 Material3 的 `CompositionLocalProvider`）并不会把涟漪装进这个 CompositionLocal。

Fluent 的组件之所以没暴露这个问题，是因为它们**全部**显式传了 `rememberRipple()`——`Button.kt`、`TabItem.kt`、`ListItem.kt` 等十个文件都 import 了 `androidx.compose.material.ripple.rememberRipple`。

因此本工程凡是自实现的可点击控件，都必须写成：

```kotlin
Modifier.clickable(
    indication = rememberRipple(color = AppTheme.ripple),
    interactionSource = remember { MutableInteractionSource() },
    onClick = onClick
)
```

涟漪色由 `AppTheme.ripple` 提供（浅色黑 / 深色白，与 Fluent `TabItemTokens.rippleColor` 一致），不硬编码颜色。

当前涉及两处：`FluentDropdownItem`（下拉选项）与 `BottomTab`（底栏 Tab）。刻意的例外是 `P2PScreen` / `WriteCardScreen` 里用于"点空白处收起键盘"的整页 `clickable`，它们显式传 `indication = null`——这类背景触摸目标不该有涟漪。

### 下拉浮层宽度对齐

Fluent 的 `Menu` 浮层宽度取自**内容固有宽度**（`MenuContent` 内部是 `Modifier.width(IntrinsicSize.Max)`），因此默认会明显窄于 `fillMaxWidth()` 的触发按钮——在本工程里按钮约 363dp、浮层只有内容宽度，视觉上完全对不齐。

`FluentDropdown` 的解法是用 `onGloballyPositioned` 量出触发按钮的实际宽度，再把它作为浮层宽度。这样无论屏幕多宽、页面外边距多大都自动对齐，**不需要在任何地方硬编码宽度**（原先写死的 `widthIn(min = 220.dp)` 已删除）。

### 布局规范

- 页面外边距（`PageMetrics.gutterFor`）：< 600dp 用 **24dp**，600–1000dp 用 32dp，≥ 1000dp 用 40dp。
  最初取 Android 默认的 16dp，实测文字与控件几乎贴屏，故整体加大一档。
- 正文列最大宽度 680dp，超出后居中，避免行长过长。
- 交互元素最小高度 48dp（Fluent 与 Android 共同要求），下拉菜单行同样适用。
- 间距全部取自 4px 基准的 Fluent 间距阶梯。
- **所有文本居中**，由 `FluentText(centered = true)` 默认开启；需要靠左时显式传 `centered = false`。
- 排版固定为「顶栏 `Title3`(16sp) → 页面 `Title3`(16sp) → 正文 `Body1`/`Body2` → 说明 `Caption1`」，禁止只改 `fontSize` 不配行高。
- 所有圆角只用 `FluentShapes.radius` 一个值（见下文「圆角统一」）。

### 文本与输入框的实现约定

`FluentText` 是本工程唯一的文本组件：它把颜色**在组合期写进 `TextStyle`**，不依赖 `LocalContentColor`。`BasicText` 的隐式取色链路在编译期与单测里都无法验证，一旦落到默认值就是黑字压深色底，因此这里刻意不走那条路。

`FluentTextField` 由两部分组成：外层 `Surface` 提供 12dp 圆角底板与 1dp 描边；内层 Fluent `TextField` 通过自定义 `TextFieldTokens` 把输入文字、标签、提示、光标、分隔线颜色全部显式指向 `AppTheme.*`，并把自身背景设为透明（否则方角底板会从圆角边缘露出）。Fluent 没有现成的圆角令牌，这是唯一可行且不破坏控件行为的方式。

### edge-to-edge

- `enableEdgeToEdge()` 负责系统栏透明；**图标明暗必须显式传入 `SystemBarStyle`**，因为它默认跟随系统 `isSystemInDarkTheme()` 而非应用主题。深色模式顶栏是 `#292929`，若系统栏图标仍是深色就完全看不见（第一版截图里状态栏几乎不可读就是这个原因）。
- 顶栏自己 `statusBarsPadding()`、底栏自己 `navigationBarsPadding()`；`Scaffold` 的 `contentWindowInsets` 置零，系统栏 insets 只被消费一次。
- `values/` 与 `values-night/` 各提供一份窗口主题，只声明 `windowBackground` 与系统栏图标明暗。窗口背景与 `AppTheme.canvas` 对齐，避免冷启动首帧闪白。

### 文件改动清单

| 文件 | 改动 |
|---|---|
| `ui/theme/DesignTokens.kt` | 新增：令牌层（颜色/排版/间距/形状/高度/动效）+ `FluentText`、`FluentTextField`、`FluentButton`、`FluentCard`、`FluentDropdown`、`FluentSnackbarHost`、`PageColumn` |
| `ui/theme/Theme.kt` | 移除固定黑白前景色；品牌色阶改为按官方亮度分布展开 |
| `ui/theme/NavigationView.kt` | 顶栏改用 `AppTheme.brandSurface`（浅色品牌色 / 深色中性）+ 居中文字；底栏自实现以延伸点击区 |
| `ui/theme/NFCReaderScreen.kt` | 改用 `PageColumn` + `FluentText`；卡片内部滚动；边距加大 |
| `ui/theme/WriteCardScreen.kt` | 同上；输入框改 `FluentTextField`（圆角）；失败提示用 `AppTheme.danger` |
| `ui/theme/P2PScreen.kt` | 同上；成功态用 `AppTheme.success`；菜单行 48dp 且居中 |
| `MainActivity.kt` | `enableEdgeToEdge` 显式传入跟随应用主题的 `SystemBarStyle` |
| `res/values/themes.xml` | 重写：DayNight 基础主题 + 窗口背景 + 系统栏 |
| `res/values-night/themes.xml` | 重写：深色窗口背景与浅色系统栏图标 |
| `res/values-night/colors.xml` | 新增：深色窗口背景色 |
| `res/values-v23/themes.xml` | 删除（主题叠加交由 DayNight 限定符） |
| `res/values/colors.xml` | 精简为 `fluent_canvas` |
| `res/drawable/edittext_background.xml` | 删除（硬编码白底，已无引用） |
| `test/.../ThemeContrastTest.kt` | 新增：15 个对比度、形状与色阶基准回归用例 |

## [S3] Out of Scope

- 不改变任何业务逻辑（NFC 读写、HCE、Nearby Connections）
- 不改变导航结构（仍为 3 页 HorizontalPager）
- 不引入深色模式手动切换开关（继续跟随系统）
- 不处理 `screenOrientation="portrait"` 锁定与既有未使用资源（`ic_home_black_24dp` 等模板遗留）
- 未做真机/模拟器截图验证（本机无 AVD；Robolectric 需联网下载且沙箱提权被拒）

## Tasks

- [x] T1: 建立 `DesignTokens.kt` 令牌层 — acceptance: 颜色/排版/间距/形状/高度/动效均有语义令牌 (covers: S2)
- [x] T2: 文本颜色显式化 — acceptance: 无任何文本依赖 `LocalContentColor`，全走 `FluentText` (covers: S1)
- [x] T3: 顶栏品牌色 — acceptance: 浅色白字/品牌底色 ≥ 4.5:1，深色模式顶栏亮度 < 0.10 (covers: S1)
- [x] T4: 布局规范化 — acceptance: 外边距 ≥ 24dp、文字全部居中、交互元素 ≥ 48dp (covers: S2)
- [x] T5: 圆角统一 — acceptance: 全应用只有 `FluentShapes.radius` = 16dp 一个值，无散落的其他值 (covers: S2)
- [x] T6: 下拉浮层与按钮对齐 — acceptance: 浮层宽度等于触发按钮实测宽度，无硬编码宽度 (covers: S2)
- [x] T7: Snackbar 自绘 — acceptance: 16dp 圆角、与底栏留 16dp 间距、文字对比度 ≥ 4.5:1 (covers: S2)
- [x] T8: 底栏自实现 — acceptance: Tab 的 clickable 与涟漪覆盖到屏幕底部，图标文字仍在手势条上方 (covers: S2)
- [x] T9: 窗口主题与 edge-to-edge — acceptance: 系统栏图标明暗跟随应用主题 (covers: S2)
- [x] T10: 对比度回归测试 — acceptance: 15 个用例全绿，覆盖正文/顶栏/底栏/Snackbar/圆角/品牌色阶亮度基准 (covers: S1)
- [x] T11: 全量构建与 lint — acceptance: `assembleDebug` + `testDebugUnitTest` + `lintDebug` 全部成功 (covers: S2)
