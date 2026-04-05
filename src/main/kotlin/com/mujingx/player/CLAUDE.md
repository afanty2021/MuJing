# Player 模块 - 媒体播放器

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **player**

## 模块职责

Player 模块是幕境应用的视频播放核心，负责：
- **视频播放**：基于 VLCJ 的媒体播放器
- **字幕同步**：精确的字幕时间轴同步
- **弹幕系统**：学习单词的弹幕显示和交互
- **播放控制**：播放、暂停、跳转、重复播放
- **画中画**：PiP (Picture-in-Picture) 模式支持

## 入口与启动

### 播放器组件
```kotlin
// VidePlayer.kt - 主播放器组件
@Composable
fun VideoPlayer(
    state: PlayerState,
    modifier: Modifier = Modifier
)

// SimplePlayer.kt - 简化播放器
class SimplePlayer(
    private val mediaPlayer: MediaPlayer
) {
    fun play()
    fun pause()
    fun seek(position: Long)
    fun release()
}

// MediaPlayerComponent.kt - VLCJ 媒体播放器组件
class MediaPlayerComponent : SwingPanel {
    val mediaPlayer: MediaPlayer
    fun initialize()
    fun cleanup()
}
```

### 播放器状态
```kotlin
// PlayerState.kt - 播放器状态管理
data class PlayerState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val volume: Int = 100,
    val rate: Float = 1.0f,
    val subtitles: List<TimedCaption> = emptyList()
)
```

## 对外接口

### 播放控制 API
```kotlin
// 播放控制
fun play()
fun pause()
fun stop()
fun togglePlayPause()

// 位置控制
fun seekTo(position: Long)  // 跳转到指定位置（毫秒）
fun seekForward(delta: Long) // 快进
fun seekBackward(delta: Long) // 快退

// 字幕控制
fun loadSubtitles(subtitles: List<TimedCaption>)
fun showSubtitle(index: Int)
fun getCurrentSubtitle(): TimedCaption?

// 弹幕控制
fun showDanmaku(word: Word, startTime: Long)
fun hideDanmaku()
fun clearDanmaku()
```

### 视频片段提取
```kotlin
// 使用 FFmpeg 提取视频片段
fun extractVideoSegment(
    videoPath: String,
    startTime: Long,
    endTime: Long,
    outputPath: String
)

// 预加载片段
fun preloadSegment(word: Word)
```

## 关键依赖与配置

### 依赖项
```kotlin
// VLCJ - VLC 媒体框架 Java 绑定
implementation("uk.co.caprica:vlcj:4.11.0")

// FFmpeg - 视频处理
implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")

// JNA - Java Native Access
implementation("net.java.dev.jna:jna:5.14.0")
implementation("net.java.dev.jna:jna-platform:5.14.0")
```

### VLC 配置
```kotlin
// VLC 路径配置
val vlcDirectory = if (isWindows) {
    "resources/windows/VLC"
} else if (isMacOS) {
    if (arch == "arm64") "resources/macos-arm64/VLC"
    else "resources/macos-x64/VLC"
} else {
    "/usr/lib/vlc"  // Linux 系统路径
}

// VLC 初始化参数
val args = listOf(
    "--no-xlib",  // 禁用 X11 支持
    "--quiet-synchro",
    "--no-video-title-show",
    "--no-stats"
)
```

## 弹幕系统

### 弹幕组件结构
```
danmaku/
├── CanvasDanmakuContainer.kt - 弹幕容器
├── CanvasDanmakuItem.kt - 单条弹幕
├── CanvasDanmakuRenderer.kt - 弹幕渲染器
├── InteractiveDanmakuRenderer.kt - 交互式弹幕
├── DanmakuStateManager.kt - 弹幕状态管理
├── TimelineSynchronizer.kt - 时间轴同步
├── TrackManager.kt - 轨道管理
└── WordDetail.kt - 单词详情弹幕
```

### 弹幕渲染
```kotlin
// CanvasDanmakuRenderer.kt
class CanvasDanmakuRenderer(
    private val canvas: Canvas
) {
    fun render(danmaku: List<DanmakuItem>)
    fun updateLayout(width: Int, height: Int)
    fun clear()
}

// 弹幕轨道管理
class TrackManager {
    private val tracks = mutableListOf<DanmakuTrack>()

    fun allocateTrack(danmaku: DanmakuItem): Int
    fun releaseTrack(trackId: Int)
    fun isTrackAvailable(trackId: Int): Boolean
}
```

### 时间轴同步
```kotlin
// TimelineSynchronizer.kt
class TimelineSynchronizer {
    fun syncToVideoPosition(position: Long)
    fun syncToSubtitle(subtitleIndex: Int)
    fun showWordDanmaku(word: Word, startTime: Long)
    fun hideExpiredDanmaku(currentTime: Long)
}
```

## 字幕系统

### 字幕数据模型
```kotlin
// TimedCaption.kt - 时间字幕
data class TimedCaption(
    val index: Int,
    val start: Long,      // 开始时间（毫秒）
    val end: Long,        // 结束时间（毫秒）
    val text: String,     // 字幕文本
    val translation: String? = null  // 翻译
)

// NewTimedCaption.kt - 新版时间字幕
data class NewTimedCaption(
    val index: Int,
    val start: Long,
    val end: Long,
    val text: String,
    val translation: String? = null,
    val words: List<Word> = emptyList()  // 包含的单词
)
```

### 字幕交互
```kotlin
// HoverableCaption.kt - 可悬停字幕
@Composable
fun HoverableCaption(
    caption: TimedCaption,
    onHover: () -> Unit,
    onClick: () -> Unit,
    onWordClick: (Word) -> Unit
)

// 字幕工具栏
@Composable
fun CaptionToolbar(
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onReplay: () -> Unit,
    onAddToVocabulary: () -> Unit
)
```

## PiP (画中画) 模式

### PiP 窗口
```kotlin
// PiPVideoWindow.kt
class PiPVideoWindow(
    private val owner: ComposeWindow
) : ComposeWindow() {
    fun enterPiP(originalBounds: Rectangle)
    fun exitPiP()
    fun updatePosition(x: Int, y: Int)
}

// MiniVideoPlayer.kt
@Composable
fun MiniVideoPlayer(
    state: PlayerState,
    modifier: Modifier = Modifier
)
```

## 测试与质量

### 单元测试
- **文件**：`src/test/kotlin/com/mujingx/player/TestNewTimedCaption.kt`
- **覆盖范围**：
  - 字幕时间计算
  - 弹幕轨道分配
  - 时间轴同步逻辑

### 手动测试
播放器功能需要手动测试，测试资源：
- 测试视频：`src/test/resources/videos/`
- 测试字幕：`src/test/resources/subtitles/`

### 测试命令
```bash
# 运行播放器单元测试
./gradlew test --tests "*player*"

# 手动测试：运行应用并打开测试视频
./gradlew run
```

## 常见问题 (FAQ)

### Q: VLC 无法加载视频怎么办？
A: 检查 VLC 路径配置，确保 VLC 库文件存在；Windows 上可能需要安装 VLC 播放器。

### Q: 字幕不同步如何调整？
A: 使用 `TimelineSynchronizer` 的 `syncToVideoPosition()` 方法；检查字幕时间戳格式。

### Q: 弹幕显示位置如何计算？
A: `TrackManager` 负责轨道分配，`CanvasDanmakuRenderer` 负责渲染位置计算。

### Q: 如何支持更多视频格式？
A: VLC 支持大多数格式，如需特殊格式，添加对应的 VLC 编解码器插件。

### Q: PiP 模式在 Windows 上如何实现？
A: 使用 Windows API 的 `SetWindowPos` 函数，通过 JNA 调用（参考 `User32.kt`）。

## 相关文件清单

### 核心文件
- `VidePlayer.kt` - 主播放器组件
- `SimplePlayer.kt` - 简化播放器
- `MediaPlayerComponent.kt` - VLCJ 组件
- `PlayerState.kt` - 播放器状态
- `PiPVideoWindow.kt` - PiP 窗口
- `MiniVideoPlayer.kt` - 迷你播放器
- `TimedCaption.kt` - 时间字幕
- `NewTimedCaption.kt` - 新版时间字幕
- `HoverableCaption.kt` - 可悬停字幕
- `CaptionToolbar.kt` - 字幕工具栏

### 弹幕系统
- `danmaku/CanvasDanmakuContainer.kt` - 弹幕容器
- `danmaku/CanvasDanmakuItem.kt` - 弹幕项
- `danmaku/CanvasDanmakuRenderer.kt` - 弹幕渲染
- `danmaku/DanmakuStateManager.kt` - 弹幕状态
- `danmaku/TimelineSynchronizer.kt` - 时间轴同步
- `danmaku/TrackManager.kt` - 轨道管理
- `danmaku/WordDetail.kt` - 单词详情

### 播放器组件
- `VideoPlayerComponents.kt` - 播放器组件集合
- `SkiaImageVideoSurface.kt` - 视频渲染表面
- `ByteBufferFactory.kt` - 字节缓冲工厂
- `PlayAudio.kt` - 音频播放
- `AudioButton.kt` - 音频按钮
- `CustomCanvas.kt` - 自定义画布
- `CustomTextMenuProvider.kt` - 文本菜单
- `KeepAwake.kt` - 防止休眠

### 测试文件
- `src/test/kotlin/com/mujingx/player/TestNewTimedCaption.kt`

## 变更记录 (Changelog)

### 2026-04-05 - 创建 player 模块文档
- 📚 初始化模块文档
- 🗂️ 记录播放器组件和弹幕系统
- 🎬 添加字幕同步和 PiP 模式说明
- 🔧 记录测试策略和常见问题

---

**依赖关系**：
- 依赖 `data` 模块（字幕数据）
- 依赖 `ffmpeg` 模块（视频片段提取）
- 依赖 `ui` 模块（Compose 集成）
- 依赖 `state` 模块（播放器状态）
