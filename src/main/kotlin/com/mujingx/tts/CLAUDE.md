# TTS 模块 - 文字转语音

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **tts**

## 模块职责

TTS 模块负责幕境应用的语音合成功能，提供跨平台的文字转语音服务：
- **多平台支持**：Windows、macOS、Linux 的 TTS 集成
- **云服务集成**：Azure TTS 服务
- **语音控制**：播放、暂停、语速调节
- **音频缓存**：常用单词的语音缓存

## 入口与启动

### TTS 服务接口
```kotlin
// MSTTSpeech.kt - Microsoft TTS 统一接口
interface TextToSpeech {
    fun speak(text: String)
    fun stop()
    fun setRate(rate: Float)  // 语速 (0.5 - 2.0)
    fun setVolume(volume: Float)  // 音量 (0.0 - 1.0)
    fun isSpeaking(): Boolean
}

// 平台特定实现
class MSTTSpeech : TextToSpeech  // Windows TTS
class MacTTS : TextToSpeech       // macOS TTS
class UbuntuTTS : TextToSpeech    // Linux TTS
class AzureTTS : TextToSpeech     // Azure 云服务
```

### 服务初始化
```kotlin
// 根据平台选择 TTS 实现
fun createTTS(): TextToSpeech {
    return when {
        isWindows -> MSTTSpeech()
        isMacOS -> MacTTS()
        isLinux -> UbuntuTTS()
        else -> throw UnsupportedOperationException("Unsupported platform")
    }
}
```

## 对外接口

### 基础 API
```kotlin
// 播放文本
fun speak(text: String)

// 停止播放
fun stop()

// 设置语速
fun setRate(rate: Float)  // 0.5 (慢) - 1.0 (正常) - 2.0 (快)

// 设置音量
fun setVolume(volume: Float)  // 0.0 - 1.0

// 检查是否正在播放
fun isSpeaking(): Boolean

// 等待播放完成
fun awaitCompletion()
```

### 高级 API
```kotlin
// 批量播放（队列）
fun speakQueue(texts: List<String>)

// 预加载语音（缓存）
fun preload(word: String)

// 清除缓存
fun clearCache()

// 获取可用语音列表
fun getAvailableVoices(): List<VoiceInfo>

// 设置语音
fun setVoice(voice: VoiceInfo)
```

## 关键依赖与配置

### Windows TTS (Jacob)
```kotlin
// 使用 Jacob 库调用 Windows COM 接口
implementation(files("lib/jacob-1.20.jar"))

// Windows SAPI (Speech API)
// 注册表路径：HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Speech\Voices
```

### macOS TTS
```kotlin
// 使用 macOS 原生 NSSpeechSynthesizer
// 可用语音列表：say -v ?
// 示例：say -v Ting-Ting "Hello"
```

### Azure TTS
```kotlin
// 需要配置 API Key
// 配置文件：~/.mujing/azure-tts.json
{
  "subscriptionKey": "your-key",
  "region": "eastasia",
  "voice": "zh-CN-XiaoxiaoNeural"
}
```

## 平台实现

### Windows 实现
```kotlin
// MSTTSpeech.kt
class MSTTSpeech : TextToSpeech {
    private val dispatcher = ActiveXComponent("SAPI.SpVoice")

    override fun speak(text: String) {
        dispatcher.invoke("Speak", text, 0)
    }

    override fun setRate(rate: Float) {
        // SAPI rate: -10 (slow) to 10 (fast)
        val sapiRate = ((rate - 1.0) * 10).toInt()
        dispatcher.setProperty("Rate", sapiRate)
    }
}
```

### macOS 实现
```kotlin
// MacTTS.kt
class MacTTS : TextToSpeech {
    private val synthesizer = NSSpeechSynthesizer()

    override fun speak(text: String) {
        synthesizer.startSpeakingString(text)
    }

    override fun setRate(rate: Float) {
        // NSSpeechSynthesizer rate: 0 - 500 words per minute
        synthesizer.rate = (rate * 200).toInt()
    }
}
```

### Linux 实现
```kotlin
// UbuntuTTS.kt
class UbuntuTTS : TextToSpeech {
    override fun speak(text: String) {
        // 使用 espeak 命令
        ProcessBuilder("espeak", "-v", "en", text).start()
    }
}
```

## 语音数据模型

### VoiceInfo (语音信息)
```kotlin
data class VoiceInfo(
    val id: String,        // 语音 ID
    val name: String,      // 语音名称
    val language: String,  // 语言代码 (zh-CN, en-US)
    val gender: Gender,    // 性别
    val description: String? = null  // 描述
)

enum class Gender {
    MALE,
    FEMALE,
    NEUTRAL
}
```

## 测试与质量

### 测试限制
- TTS 模块需要实际语音服务，难以自动化测试
- 建议手动测试各个平台的语音播放

### 手动测试
```kotlin
// 测试脚本
fun testTTS() {
    val tts = createTTS()
    tts.speak("Hello, this is a test.")
    tts.awaitCompletion()

    tts.setRate(1.5f)
    tts.speak("This is faster.")
    tts.awaitCompletion()

    tts.setRate(0.75f)
    tts.speak("This is slower.")
    tts.awaitCompletion()
}
```

## 常见问题 (FAQ)

### Q: Windows 上没有声音怎么办？
A: 检查系统是否安装了语音包；确保 Jacob 库正确加载；检查音频输出设备。

### Q: macOS 上如何切换语音？
A: 使用系统设置中的"辅助功能" > "语音"或使用 `say -v ?` 查看可用语音。

### Q: Linux 上需要安装什么？
A: 安装 `espeak` 或 `festival` 语音合成软件：`sudo apt install espeak`

### Q: Azure TTS 需要付费吗？
A: Azure TTS 有免费额度，超出后按使用量计费。参考 Azure 定价。

### Q: 如何添加新的 TTS 服务？
A: 实现 `TextToSpeech` 接口，在 `createTTS()` 中添加条件分支。

## 相关文件清单

### 核心文件
- `MSTTSpeech.kt` - Microsoft TTS 实现
- `MacTTS.kt` - macOS TTS 实现
- `UbuntuTTS.kt` - Linux TTS 实现
- `Azure TTS.kt` - Azure TTS 实现

### 测试文件
- 无（需要手动测试）

## 变更记录 (Changelog)

### 2026-04-05 - 创建 tts 模块文档
- 📚 初始化模块文档
- 🗂️ 记录 TTS 接口和平台实现
- 🔧 添加配置说明和常见问题

---

**依赖关系**：
- 被 `ui` 模块依赖（单词读音播放）
- 被 `player` 模块依赖（字幕朗读）
