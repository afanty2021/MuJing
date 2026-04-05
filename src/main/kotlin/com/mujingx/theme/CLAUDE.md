# Theme 模块 - 主题配置

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **theme**

## 模块职责

Theme 模块负责幕境应用的主题系统：
- **颜色方案**：明暗主题颜色定义
- **字体配置**：字体家族和大小
- **主题提供者**：Compose 主题注入

## 入口与启动

### 主题配置
```kotlin
// colors.kt - 颜色定义
val Purple200 = Color(0xFFBB86FC)
val Purple500 = Color(0xFF6200EE)
val Purple700 = Color(0xFF3700B3)

// CustomLocalProvider.kt - 主题提供者
@Composable
fun ProvideCustomTheme(
    darkTheme: Boolean = isSystemDarkMode(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(
        LocalCustomColors provides colors,
        content = content
    )
}
```

## 对外接口

### 主题切换
```kotlin
// 检测系统主题
fun isSystemDarkMode(): Boolean

// 切换主题
fun toggleTheme()

// 获取当前主题
fun getCurrentTheme(): ThemeColors
```

## 相关文件清单

### 核心文件
- `colors.kt` - 颜色定义
- `CustomLocalProvider.kt` - 主题提供者

## 变更记录 (Changelog)

### 2026-04-05 - 创建 theme 模块文档
- 📚 初始化模块文档
- 🗂️ 记录主题配置接口

---

**依赖关系**：
- 被 `ui` 模块依赖（所有界面）
- 被 `App.kt` 依赖（主题初始化）
