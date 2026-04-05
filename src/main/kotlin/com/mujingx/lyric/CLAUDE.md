# Lyric 模块 - 歌词/字幕处理

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **lyric**

## 模块职责

Lyric 模块负责幕境应用的歌词和字幕解析：
- **LRC 格式**：歌词文件解析
- **歌词同步**：时间轴同步显示
- **歌词管理**：歌词文件加载和保存

## 入口与启动

### 歌词解析
```kotlin
// Lyric.kt - 歌词模型
data class Lyric(
    val time: Long,  // 时间（毫秒）
    val text: String // 歌词文本
)

// SongLyric.kt - 歌曲歌词
class SongLyric {
    fun parse(lrcContent: String): List<Lyric>
    fun getLyricAtTime(time: Long): Lyric?
}

// FileManager.kt - 文件管理
object LyricFileManager {
    fun loadLyric(file: File): SongLyric
    fun saveLyric(lyric: SongLyric, file: File)
}
```

## 对外接口

### 歌词解析
```kotlin
// 解析 LRC 文件
fun parseLrc(content: String): List<Lyric>

// 获取当前歌词
fun getCurrentLyric(time: Long): Lyric?

// 获取下一句歌词
fun getNextLyric(time: Long): Lyric?
```

## 相关文件清单

### 核心文件
- `Lyric.kt` - 歌词模型
- `SongLyric.kt` - 歌曲歌词
- `FileManager.kt` - 文件管理

## 变更记录 (Changelog)

### 2026-04-05 - 创建 lyric 模块文档
- 📚 初始化模块文档
- 🗂️ 记录歌词解析接口

---

**依赖关系**：
- 被 `player` 模块依赖（歌词显示）
- 被 `ui` 模块依赖（歌词界面）
