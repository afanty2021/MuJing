# State 模块 - 应用状态管理

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **state**

## 模块职责

State 模块负责幕境应用的全局状态管理：
- **应用状态**：全局应用状态和配置
- **屏幕导航**：当前屏幕类型和导航历史
- **词库状态**：当前加载的词库
- **设置状态**：用户设置和偏好

## 入口与启动

### 状态管理
```kotlin
// AppState.kt - 应用状态
@Composable
fun rememberAppState(): AppState {
    val currentScreen = remember { mutableStateOf(ScreenType.WordScreen) }
    val currentVocabulary = remember { mutableStateOf<Vocabulary?>(null) }
    // ... 其他状态
    return AppState(currentScreen, currentVocabulary, ...)
}

// GlobalState.kt - 全局状态
object GlobalState {
    var appSettings by mutableStateOf(AppSettings())
    var recentVocabularies by mutableStateOf<List<RecentItem>>(emptyList())
}
```

## 对外接口

### 屏幕导航
```kotlin
// 切换屏幕
fun navigateTo(screen: ScreenType)

// 返回上一屏幕
fun navigateBack()

// 获取当前屏幕
fun getCurrentScreen(): ScreenType
```

### 词库管理
```kotlin
// 加载词库
suspend fun loadVocabulary(file: File): Vocabulary

// 保存词库
suspend fun saveVocabulary(vocabulary: Vocabulary, file: File)

// 获取当前词库
fun getCurrentVocabulary(): Vocabulary?
```

## 相关文件清单

### 核心文件
- `AppState.kt` - 应用状态
- `GlobalState.kt` - 全局状态
- `ScreenType.kt` - 屏幕类型枚举

## 变更记录 (Changelog)

### 2026-04-05 - 创建 state 模块文档
- 📚 初始化模块文档
- 🗂️ 记录状态管理接口

---

**依赖关系**：
- 被 `ui` 模块依赖（所有界面）
- 依赖 `data` 模块（词库数据）
