# UI 模块 - 用户界面

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **ui**

## 模块职责

UI 模块是幕境应用的前端界面层，负责：
- **主应用框架**：Compose Desktop 窗口管理、主题配置
- **单词学习界面**：拼写练习、听写测试、记忆卡片
- **视频播放界面**：播放器控制、字幕显示、弹幕系统
- **字幕浏览器**：字幕导航、跟读练习、选择性播放
- **词库编辑器**：单词编辑、词库管理、导入导出
- **对话框组件**：设置、关于、捐赠等对话框

## 入口与启动

### 应用入口
```kotlin
// Main.kt - 应用启动点
fun main() = application {
    init()  // 初始化 FileKit 和 FlatLaf
    App()   // 启动主应用
}

// App.kt - 主应用组件
@Composable
fun App() {
    val state = rememberAppState()
    val windowState = rememberWindowState()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "幕境 v${BuildConfig.APP_VERSION}"
    ) {
        MenuBar()
        when (state.currentScreen) {
            ScreenType.WordScreen -> WordScreen(state)
            ScreenType.SubtitleScreen -> SubtitleScreen(state)
            ScreenType.TextEditScreen -> TextScreen(state)
            ScreenType.EditVocabulary -> EditVocabulary(state)
            ScreenType.Search -> Search(state)
        }
    }
}
```

### 界面导航
应用采用**单窗口多屏幕**架构，通过 `AppState.currentScreen` 切换：
- `WordScreen` - 单词学习界面
- `SubtitleScreen` - 字幕浏览器
- `TextEditScreen` - 文档阅读器
- `EditVocabulary` - 词库编辑器
- `Search` - 单词搜索

## 对外接口

### 公共 UI 组件
```kotlin
// components/Toolbar.kt - 工具栏
@Composable
fun Toolbar(
    onBack: () -> Unit,
    actions: List<ToolbarAction>
)

// components/SidebarButton.kt - 侧边栏按钮
@Composable
fun SidebarButton(
    icon: Painter,
    text: String,
    onClick: () -> Unit
)

// dialog/ConfirmDialog.kt - 确认对话框
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
)
```

### 屏幕组件
```kotlin
// wordscreen/WordScreen.kt - 单词学习界面
@Composable
fun WordScreen(state: AppState)

// subtitlescreen/SubtitleScreen.kt - 字幕浏览器
@Composable
fun SubtitleScreen(state: AppState)

// textscreen/TextScreen.kt - 文档阅读器
@Composable
fun TextScreen(state: AppState)

// edit/EditVocabulary.kt - 词库编辑器
@Composable
fun EditVocabulary(state: AppState)
```

## 关键依赖与配置

### 依赖项
```kotlin
// Compose Desktop
implementation(compose.desktop.currentOs)

// Material Icons
implementation("org.jetbrains.compose.material:material-icons-extended:1.0.1")

// FlatLaf (Swing Look & Feel)
implementation("com.formdev:flatlaf:3.6.1")
implementation("com.formdev:flatlaf-extras:2.6")

// FileKit (文件选择器)
implementation("io.github.vinceglb:filekit-dialogs:0.12.0")

// 拖拽排序
implementation("sh.calvin.reorderable:reorderable:3.0.0")
```

### 主题配置
```kotlin
// theme/colors.kt - 颜色方案
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)
val Teal200 = Color(0xFF03DAC5)

// theme/CustomLocalProvider.kt - 自定义主题提供者
@Composable
fun ProvideCustomTheme(
    darkTheme: Boolean = isSystemDarkMode(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(
        LocalCustomColors provides colors,
        LocalTypography provides typography,
        content = content
    )
}
```

## UI 组件结构

### 核心屏幕
```
App.kt
├── WordScreen (单词学习)
│   ├── WordScreenSidebar (侧边栏)
│   ├── Word (单词卡片)
│   ├── DictationState (听写模式)
│   └── MemoryStrategy (记忆策略)
│
├── SubtitleScreen (字幕浏览器)
│   ├── SubtitlesSidebar (字幕列表)
│   ├── Caption (字幕显示)
│   ├── MultipleLines (多行字幕)
│   └── SubtitlesState (字幕状态)
│
├── TextScreen (文档阅读器)
│   └── TextState (文本状态)
│
├── EditVocabulary (词库编辑器)
│   ├── ChooseEditVocabulary (选择词库)
│   ├── ExportVocabulary (导出词库)
│   ├── VocabularyInfo (词库信息)
│   ├── HighLightCell (高亮单元格)
│   └── SearchState (搜索状态)
│
└── Search (单词搜索)
    └── SearchResultInfo (搜索结果)
```

### 对话框组件
```
dialog/
├── AboutDialog (关于对话框)
├── SettingsDialog (设置对话框)
├── EditWordDialog (编辑单词对话框)
├── GenerateVocabularyDialog (生成词库对话框)
├── LinkVocabularyDialog (链接词库对话框)
├── MergeVocabularyDialog (合并词库对话框)
├── ShortcutKeyDialog (快捷键对话框)
└── DonateDialog (捐赠对话框)
```

### 工具组件
```
util/
├── GenerateVocabulary (词库生成工具)
├── SubtitleConverter (字幕转换器)
├── VideoUtil (视频工具)
├── WhisperModelDownload (Whisper 模型下载)
├── MirrorSettings (镜像设置)
└── DragAndDropUtil (拖放工具)
```

## 测试与质量

### UI 测试
- **文件**：
  - `src/test/kotlin/com/mujingx/ui/WordScreenTest.kt`
  - `src/test/kotlin/com/mujingx/ui/dialog/SettingsDialogTest.kt`
- **框架**：Compose UI Testing
- **限制**：需要显示环境（GUI），CI/CD 环境通常跳过

### 测试命令
```bash
# 运行 UI 测试（需要显示环境）
./gradlew testUi

# 运行标准单元测试（排除 UI 测试）
./gradlew test

# 运行特定对话框测试
./gradlew testUi --tests "*SettingsDialog*"

# 运行特定屏幕测试
./gradlew test --tests "*WordScreen*"
```

### 快捷键测试
应用使用 Compose 的 `KeyEvent` 系统处理快捷键：
- `Ctrl + J` - 播放单词读音
- `Ctrl + 1/2/3` - 播放视频片段
- `Ctrl + V/P/L/E/K/R/S` - 显示/隐藏信息
- `Enter/Page Up/Down` - 导航单词

## 常见问题 (FAQ)

### Q: 如何添加新的屏幕？
A: 在 `ScreenType.kt` 中添加新类型，在 `App.kt` 中添加对应的 `when` 分支，实现新的 `@Composable` 屏幕组件。

### Q: 如何自定义主题颜色？
A: 修改 `theme/colors.kt` 中的颜色定义，并在 `CustomLocalProvider.kt` 中应用。

### Q: FlatLaf 和 Compose 主题如何同步？
A: 在 `App.kt` 的 `init()` 中初始化 FlatLaf，在 `updateFlatLaf()` 中响应主题切换。

### Q: 如何实现对话框的打开和关闭？
A: 使用 `remember` 和 `mutableStateOf()` 管理对话框状态，通过 `Boolean` 值控制显示。

### Q: 支持哪些语言的界面？
A: 目前仅支持简体中文，计划使用国际化资源文件支持多语言。

### Q: 如何优化列表性能？
A: 所有 LazyColumn/LazyVerticalGrid 已添加 `key` 和 `contentType` 参数，使用唯一标识符（如文件绝对路径、单词值等）作为 key，避免不必要的重组，提升滚动性能。

## 相关文件清单

### 核心文件
- `App.kt` - 主应用组件
- `components/` - 可复用 UI 组件
- `dialog/` - 对话框组件
- `edit/` - 词库编辑器
- `search/` - 搜索界面
- `subtitlescreen/` - 字幕浏览器
- `textscreen/` - 文档阅读器
- `wordscreen/` - 单词学习界面
- `util/` - UI 工具类
- `flatlaf/` - FlatLaf 集成
- `window/` - 窗口工具

### 测试文件
- `src/test/kotlin/com/mujingx/ui/WordScreenTest.kt`
- `src/test/kotlin/com/mujingx/ui/dialog/SettingsDialogTest.kt` (10个测试用例，100%通过率)

### 资源文件
- `resources/common/icon/` - 图标资源
- `resources/common/logo/` - Logo 文件

## 变更记录 (Changelog)

### 2026-04-06 - 添加 SettingsDialog 测试和性能优化
- ✅ **UI 测试扩展**：新增 SettingsDialogTest.kt，包含 10 个测试用例
  - 覆盖主题设置、字体样式、发音设置、页面导航等功能
  - 所有测试通过 (10/10, 100% 成功率)
  - 添加 testTag 标识符支持可靠的元素定位测试
- 🚀 **性能优化**：所有 LazyColumn/LazyVerticalGrid 添加 key 和 contentType 参数
  - BuiltInVocabularyMenu, BuiltInVocabularyDialog, GenerateVocabularyDialog
  - LinkVocabularyDialog, SpeechDialog, SubtitleScreen, TextScreen
  - 使用文件绝对路径/单词值等作为唯一 key，避免不必要的重组
- 🔧 **构建配置**：添加 testUi Gradle 任务，UI 测试与单元测试分离

### 2026-04-05 - 创建 ui 模块文档
- 📚 初始化模块文档
- 🗂️ 记录 UI 组件结构和接口
- 🎨 添加主题配置说明
- 🔧 记录测试策略和常见问题

---

**依赖关系**：
- 依赖 `data` 模块（词库数据）
- 依赖 `player` 模块（视频播放）
- 依赖 `tts` 模块（语音播放）
- 依赖 `state` 模块（应用状态）
- 依赖 `event` 模块（事件总线）
