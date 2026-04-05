# FFmpeg 模块 - 视频处理

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **ffmpeg**

## 模块职责

FFmpeg 模块负责幕境应用的视频处理功能：
- **视频片段提取**：从视频中提取指定时间段的片段
- **格式转换**：视频格式转换和编码
- **音频提取**：从视频中提取音频轨道
- **字幕处理**：字幕文件格式转换

## 入口与启动

### 核心工具类
```kotlin
// FFmpegUtil.kt - FFmpeg 工具类
object FFmpegUtil {
    // 提取视频片段
    fun extractSegment(
        videoPath: String,
        startTime: Long,  // 毫秒
        endTime: Long,    // 毫秒
        outputPath: String
    )

    // 提取音频
    fun extractAudio(
        videoPath: String,
        outputPath: String
    )

    // 转换视频格式
    fun convertFormat(
        inputPath: String,
        outputPath: String,
        format: String
    )
}
```

### FFmpeg 配置
```kotlin
// FFmpeg 可执行文件路径
val ffmpegPath = when {
    isWindows -> "resources/windows/ffmpeg/bin/ffmpeg.exe"
    isMacOS && arch == "arm64" -> "resources/macos-arm64/ffmpeg/ffmpeg"
    isMacOS && arch == "x64" -> "resources/macos-x64/ffmpeg/ffmpeg"
    isLinux -> "/usr/bin/ffmpeg"  // 系统路径
    else -> throw UnsupportedOperationException("Unsupported platform")
}
```

## 对外接口

### 视频片段提取
```kotlin
// 提取单个片段
fun extractSegment(
    videoPath: String,
    startTime: Long,
    endTime: Long,
    outputPath: String
)

// 批量提取（用于单词语境）
fun extractSegments(
    videoPath: String,
    segments: List<VideoSegment>,
    outputDir: String
): List<String>

// 提取并压缩
fun extractAndCompressSegment(
    videoPath: String,
    startTime: Long,
    endTime: Long,
    outputPath: String,
    quality: String = "medium"  // low, medium, high
)
```

### 音频处理
```kotlin
// 提取音频为 MP3
fun extractAudioToMp3(
    videoPath: String,
    outputPath: String,
    bitrate: String = "128k"
)

// 提取音频为 WAV
fun extractAudioToWav(
    videoPath: String,
    outputPath: String
)
```

### 字幕处理
```kotlin
// 转换字幕格式
fun convertSubtitle(
    inputPath: String,
    outputPath: String,
    format: SubtitleFormat  // SRT, VTT, ASS
)

// 嵌入字幕到视频
fun embedSubtitle(
    videoPath: String,
    subtitlePath: String,
    outputPath: String
)
```

## 关键依赖与配置

### 依赖项
```kotlin
// FFmpeg Java 包装器
implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")
```

### FFmpeg 参数
```kotlin
// 视频编码参数
val videoParams = listOf(
    "-c:v", "libx264",      // H.264 编码
    "-preset", "medium",    // 编码速度
    "-crf", "23",           // 质量因子 (0-51, 越小质量越高)
    "-pix_fmt", "yuv420p"   // 像素格式
)

// 音频编码参数
val audioParams = listOf(
    "-c:a", "aac",          // AAC 编码
    "-b:a", "128k"          // 比特率
)
```

## 视频处理流程

### 片段提取流程
```
1. 验证输入文件存在
2. 计算时间戳 (HH:MM:SS.mmm)
3. 构建 FFmpeg 命令
4. 执行 FFmpeg 进程
5. 监控进度（可选）
6. 验证输出文件
7. 清理临时文件
```

### 命令示例
```bash
# 提取视频片段（00:01:23 - 00:01:30）
ffmpeg -i input.mp4 \
  -ss 00:01:23 \
  -to 00:01:30 \
  -c:v libx264 \
  -c:a aac \
  output.mp4

# 提取音频
ffmpeg -i input.mp4 \
  -vn \
  -ar 44100 \
  -ac 2 \
  -ab 192k \
  -f mp3 \
  output.mp3
```

## 测试与质量

### 单元测试
- **文件**：`src/test/kotlin/com/mujingx/ffmpeg/TestFFmpegUtil.kt`
- **覆盖范围**：
  - 片段提取功能
  - 音频提取功能
  - 富文本移除测试

### 测试命令
```bash
# 运行 FFmpeg 测试
./gradlew test --tests "*FFmpeg*"

# 测试片段提取
./gradlew test --tests "*TestFFmpegUtil*"
```

### 测试资源
- 测试视频：`src/test/resources/videos/test.mp4`
- 测试字幕：`src/test/resources/subtitles/test.srt`

## 常见问题 (FAQ)

### Q: FFmpeg 命令执行失败怎么办？
A: 检查 FFmpeg 可执行文件路径；确认输入文件格式支持；查看 FFmpeg 错误日志。

### Q: 提取的片段质量如何优化？
A: 调整 `-crf` 参数（越小质量越高），使用 `-preset slower` 提高压缩效率。

### Q: 如何处理超大视频文件？
A: 使用 `-ss` 参数在输入文件前定位（快速但可能不精确），或使用 `-ss` + `-t` 组合。

### Q: 支持哪些视频格式？
A: FFmpeg 支持几乎所有格式，包括 MP4、MKV、AVI、MOV、WMV 等。

### Q: 如何加速视频处理？
A: 使用硬件加速（`-hwaccel`）、多线程（`-threads`）、更快的 preset（`-preset ultrafast`）。

## 相关文件清单

### 核心文件
- `FFmpegUtil.kt` - FFmpeg 工具类

### 测试文件
- `src/test/kotlin/com/mujingx/ffmpeg/TestFFmpegUtil.kt`
- `src/test/kotlin/com/mujingx/ffmpeg/TestRemoveRichText.kt`

### 资源文件
- `resources/macos-arm64/ffmpeg/ffmpeg` - macOS ARM64 FFmpeg
- `resources/macos-x64/ffmpeg/ffmpeg` - macOS x64 FFmpeg
- `resources/linux/ffmpeg/ffmpeg` - Linux FFmpeg
- `resources/windows/ffmpeg/` - Windows FFmpeg

## 变更记录 (Changelog)

### 2026-04-05 - 创建 ffmpeg 模块文档
- 📚 初始化模块文档
- 🗂️ 记录视频处理接口和流程
- 🔧 添加配置说明和常见问题

---

**依赖关系**：
- 被 `player` 模块依赖（视频片段提取）
- 被 `data` 模块依赖（音频提取）
- 被 `ui` 模块依赖（词库生成）
