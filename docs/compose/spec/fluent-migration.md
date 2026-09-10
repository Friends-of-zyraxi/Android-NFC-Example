---
feature: fluent-migration
status: delivered
updated: 2026-09-28
branch: fluent-migration
commits: fd60a55..HEAD
---

# Material 3 → Fluent 2 Design 迁移

## Report

**What was built** — 将应用全部 Material 3 视觉控件替换为 Fluent UI Android 的 Compose 组件。依赖从聚合包 `FluentUIAndroid:0.3.14` 改为 8 个按需模块。主题层 `MyApplicationTheme` 内部改为调用 `FluentTheme`。四个界面文件（NavigationView、NFCReaderScreen、WriteCardScreen、P2PScreen）中的 Button/Card/TextField/TopAppBar/NavigationBar/DropdownMenu/CircularProgressIndicator 全部替换为 Fluent 等价物。业务逻辑零改动。

**Verification** — `./gradlew :app:assembleDebug` BUILD SUCCESSFUL；`./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL；全局搜索确认无 Material3 视觉组件残留 import。

**Journey log** — Fluent 的 `TextField` 不支持 `minLines`，多行文本输入改为默认行为；`ExposedDropdownMenuBox` 无直接等价物，用 `Button(OutlinedButton)` + `Menu` 组合替代；`CircularProgressIndicator` 有确定/不确定两个重载，不传 `progress` 即为 indeterminate 动画；FluentTheme 内部依赖 `runtime-livedata` 适配器，缺失导致启动闪退；`BasicText` 不自动跟随主题，需在 Theme 层通过 `LocalContentColor` 提供自适应前景色；Fluent `Menu` 的 Popup 内容需显式宽度约束；Material3 仅保留 `Scaffold`（布局骨架）和 `LocalContentColor`，所有视觉控件均已 Fluent 化。

## [S1] Problem

应用界面全部使用 Jetpack Compose Material 3 控件构建，与 Microsoft Fluent 2 设计语言不一致。需要将所有 Material 控件替换为 Fluent UI Android 提供的 Compose 组件，同时保证功能不变。

项目已在 `build.gradle.kts` 中声明了聚合依赖 `FluentUIAndroid:0.3.14`，但界面代码尚未使用任何 Fluent 组件。

## [S2] Design

### 依赖策略

改用按需引入的模块化依赖（替代聚合包），版本号与源码仓库 `config.gradle` 对齐：

| 模块 | 版本 | 用途 |
|---|---|---|
| `fluentui_core` | 0.3.11 | FluentTheme、tokens |
| `fluentui_controls` | 0.3.3 | Button、TextField、BasicCard |
| `fluentui_progress` | 0.3.7 | CircularProgressIndicator |
| `fluentui_topappbars` | 0.3.9 | AppBar |
| `fluentui_tablayout` | 0.3.5 | TabBar（底部导航） |
| `fluentui_notification` | 0.3.10 | Snackbar |
| `fluentui_menus` | 0.3.5 | Menu、Dialog |
| `fluentui_listitem` | 0.3.7 | Divider |

移除：`com.microsoft.fluentui:FluentUIAndroid`（聚合包）、`androidx.compose.material3`（不再需要）。

保留：`androidx.compose.material:material`（Fluent 内部依赖 rememberRipple，需传递保留）。

### 组件映射

| Material 3 | Fluent UI Android | 包路径 |
|---|---|---|
| `Button` | `Button(style=ButtonStyle.Button)` | `tokenized.controls` |
| `OutlinedButton` | `Button(style=ButtonStyle.OutlinedButton)` | `tokenized.controls` |
| `Card` + `CardDefaults` | `BasicCard(cardType=CardType.Elevated)` | `tokenized.controls` |
| `OutlinedTextField` | `TextField` | `tokenized.controls` |
| `CircularProgressIndicator` | `CircularProgressIndicator` | `tokenized.progress` |
| `TopAppBar` | `AppBar(title=...)` | `tokenized` (topappbars) |
| `NavigationBar` + `NavigationBarItem` | `TabBar(tabDataList, selectedIndex)` | `tokenized.navigation` |
| `SnackbarHost` + `SnackbarHostState` | 自定义 SnackbarHostState + `Snackbar` | `tokenized.notification` |
| `DropdownMenu` + `DropdownMenuItem` | `Menu` + `ListItem` | `tokenized.menu` / `tokenized.listitem` |
| `Dialog` + `DialogProperties` | `Dialog` | `tokenized.menu` |
| `MaterialTheme` | `FluentTheme` | `theme` |
| `ExposedDropdownMenuDefaults.TrailingIcon` | 手动用 `Icon` + 旋转 | — |

### 主题改造

- 根节点用 `FluentTheme(themeMode=ThemeMode.Auto)` 包裹（替代 `MyApplicationTheme`）
- `MaterialTheme.typography.*` → `FluentStyle` / `LocalTextStyle` / 直接指定 `TextStyle`
- `MaterialTheme.colorScheme.*` → `FluentTheme` token 颜色或直接使用语义色
- `MyApplicationTheme` 函数保留但内部改为调用 `FluentTheme`

### Scaffold 策略

Material `Scaffold` 仅作为布局骨架使用（topBar/bottomBar/snackbarHost/content padding），本身无视觉样式。保留 `Scaffold`，替换其插槽内的视觉组件为 Fluent 等价物。

### 文件改动清单

| 文件 | 改动 |
|---|---|
| `app/build.gradle.kts` | 依赖替换 |
| `ui/theme/Theme.kt` | MyApplicationTheme → FluentTheme 包装 |
| `ui/theme/NavigationView.kt` | TopAppBar→AppBar, NavigationBar→TabBar, SnackbarHost→Fluent Snackbar |
| `ui/theme/NFCReaderScreen.kt` | Card→BasicCard, Button→Fluent Button, Text 样式适配 |
| `ui/theme/WriteCardScreen.kt` | Card→BasicCard, Button→Fluent Button, OutlinedTextField→TextField, DropdownMenu→Menu |
| `ui/theme/P2PScreen.kt` | 同上 + CircularProgressIndicator, OutlinedButton→Fluent Button(Outlined) |
| `MainActivity.kt` | import 替换，MaterialTheme→FluentTheme |
| `ui/theme/Color.kt` | 保留（Fluent 主题自带配色，此文件可能不再需要） |
| `ui/theme/Type.kt` | 保留或精简（Fluent 自带排版） |

## [S3] Out of Scope

- 不改变任何业务逻辑（NFC 读写、HCE、Nearby Connections）
- 不改变导航结构（仍为 3 页 HorizontalPager）
- 不添加新的 UI 功能或页面
- 不处理系统主题切换动画（FluentTheme 的 Auto 模式自动处理）

## Tasks

- [x] T1: 更新 build.gradle.kts 依赖 — acceptance: 项目同步成功，无重复 FluentUIAndroid 聚合依赖 (covers: S2)
- [x] T2: 改造 Theme.kt 为 FluentTheme 包装 — acceptance: 根节点使用 FluentTheme，MyApplicationTheme 对外签名不变 (covers: S2)
- [x] T3: 迁移 NavigationView.kt — acceptance: AppBar 替换 TopAppBar，TabBar 替换 NavigationBar，Snackbar 走 Fluent (covers: S2; depends: T1)
- [x] T4: 迁移 NFCReaderScreen.kt — acceptance: Card/Button 全部替换，编译通过 (covers: S2; depends: T1)
- [x] T5: 迁移 WriteCardScreen.kt — acceptance: Card/Button/TextField/Menu 全部替换，编译通过 (covers: S2; depends: T1)
- [x] T6: 迁移 P2PScreen.kt — acceptance: 全部 Material 控件替换，编译通过 (covers: S2; depends: T1)
- [x] T7: 迁移 MainActivity.kt — acceptance: import 更新，FluentTheme 接入，编译通过 (covers: S2; depends: T2)
- [x] T8: 全量构建验证 — acceptance: `assembleDebug` 成功，无 Material3 残留 import (covers: S2)
