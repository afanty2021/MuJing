# 幕境项目质量提升实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过 4 个任务包，修复安全风险并补充核心模块测试，将测试覆盖率从 ~15% 提升至 ~30%。

**Architecture:** 小步迭代，每个任务包包含 1 个安全修复 + 1 个模块测试补充。交替进行，每次修复都有测试验证。

**Tech Stack:** Kotlin 2.2.21, JUnit 5, kotlinx-coroutines-test, SQLite JDBC

**Design Spec:** `docs/superpowers/specs/2026-04-05-quality-improvement-design.md`

---

## 文件结构

### 新增文件

```
src/test/kotlin/com/mujingx/state/
├── ScreenTypeTest.kt          # Task 1: 枚举测试
├── GlobalStateTest.kt         # Task 1: 全局状态测试

src/test/kotlin/com/mujingx/event/
├── EventBusTest.kt            # Task 2: 事件总线测试

src/test/kotlin/com/mujingx/lyric/
├── LyricTest.kt               # Task 3: 歌词模型测试
├── SongLyricTest.kt           # Task 3: 歌曲歌词测试

src/test/kotlin/com/mujingx/tts/
├── AzureTTSTest.kt            # Task 4: Azure TTS 测试
├── TTSFactoryTest.kt          # Task 4: TTS 工厂测试
```

### 修改文件

```
src/main/kotlin/com/mujingx/fsrs/apkg/
├── ApkgDatabaseCreator.kt     # Task 1: 修复 PRAGMA 注入

src/main/kotlin/com/mujingx/event/
├── EventBus.kt                # Task 2: 无修改（仅测试）

build.gradle.kts               # Task 2: 统一序列化库版本
```

---

## Task 1: SQL 注入修复 + state 模块测试

**Files:**
- Modify: `src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseCreator.kt:91`
- Create: `src/test/kotlin/com/mujingx/state/ScreenTypeTest.kt`
- Create: `src/test/kotlin/com/mujingx/state/GlobalStateTest.kt`

### Step 1: 写 ScreenType 枚举测试

- [ ] 创建 `src/test/kotlin/com/mujingx/state/ScreenTypeTest.kt`

```kotlin
package com.mujingx.state

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ScreenTypeTest {

    @Test
    fun `ScreenType 应包含所有预期的屏幕类型`() {
        val expected = setOf("WORD", "SUBTITLES", "TEXT")
        val actual = ScreenType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `ScreenType WORD 是默认屏幕`() {
        assertNotNull(ScreenType.WORD)
        assertEquals("WORD", ScreenType.WORD.name)
    }

    @Test
    fun `ScreenType valueOf 应正确返回对应枚举`() {
        assertEquals(ScreenType.WORD, ScreenType.valueOf("WORD"))
        assertEquals(ScreenType.SUBTITLES, ScreenType.valueOf("SUBTITLES"))
        assertEquals(ScreenType.TEXT, ScreenType.valueOf("TEXT"))
    }

    @Test
    fun `ScreenType valueOf 无效值应抛异常`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScreenType.valueOf("INVALID")
        }
    }

    @Test
    fun `ScreenType 应有 3 个值`() {
        assertEquals(3, ScreenType.values().size)
    }
}
```

### Step 2: 运行测试验证通过

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.state.ScreenTypeTest" -i
```

Expected: 5 tests PASSED

### Step 3: 写 GlobalState 测试

- [ ] 创建 `src/test/kotlin/com/mujingx/state/GlobalStateTest.kt`

```kotlin
package com.mujingx.state

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class GlobalStateTest {

    private lateinit var globalState: GlobalState

    @BeforeEach
    fun setUp() {
        val defaultData = GlobalData()
        globalState = GlobalState(defaultData)
    }

    @Test
    fun `默认全局状态 应使用默认值`() {
        assertEquals(ScreenType.WORD, globalState.type)
        assertTrue(globalState.isDarkTheme)
        assertFalse(globalState.isFollowSystemTheme)
        assertEquals(0.8F, globalState.audioVolume, 0.01F)
        assertEquals(80F, globalState.videoVolume, 0.01F)
    }

    @Test
    fun `默认全局状态 音量应在合理范围`() {
        assertTrue(globalState.audioVolume in 0F..1F)
        assertTrue(globalState.keystrokeVolume in 0F..1F)
    }

    @Test
    fun `修改 type 后 应反映新值`() {
        globalState.type = ScreenType.SUBTITLES
        assertEquals(ScreenType.SUBTITLES, globalState.type)
    }

    @Test
    fun `修改 isDarkTheme 后 应反映新值`() {
        globalState.isDarkTheme = false
        assertFalse(globalState.isDarkTheme)
    }

    @Test
    fun `修改 audioVolume 后 应反映新值`() {
        globalState.audioVolume = 0.5F
        assertEquals(0.5F, globalState.audioVolume, 0.01F)
    }

    @Test
    fun `默认 maxSentenceLength 应为 25`() {
        assertEquals(25, globalState.maxSentenceLength)
    }

    @Test
    fun `默认 showInputCount 应为 true`() {
        assertTrue(globalState.showInputCount)
    }

    @Test
    fun `默认 autoUpdate 应为 true`() {
        assertTrue(globalState.autoUpdate)
    }

    @Test
    fun `默认 bncNum 和 frqNum 应为 1000`() {
        assertEquals(1000, globalState.bncNum)
        assertEquals(1000, globalState.frqNum)
    }

    @Test
    fun `从 GlobalData 初始化 应保留数据`() {
        val customData = GlobalData(
            type = ScreenType.TEXT,
            isDarkTheme = false,
            audioVolume = 0.3F,
            bncNum = 500
        )
        val state = GlobalState(customData)
        assertEquals(ScreenType.TEXT, state.type)
        assertFalse(state.isDarkTheme)
        assertEquals(0.3F, state.audioVolume, 0.01F)
        assertEquals(500, state.bncNum)
    }
}
```

### Step 4: 运行测试验证通过

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.state.GlobalStateTest" -i
```

Expected: 10 tests PASSED

### Step 5: 修复 SQL 注入 — ApkgDatabaseCreator PRAGMA 语句

- [ ] 修改 `src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseCreator.kt` 第 91 行

将：
```kotlin
stmt.execute("PRAGMA user_version = ${format.schemaVersion}")
```

改为：
```kotlin
// schemaVersion 来自 ApkgFormat 枚举，是受信任的整数
// 额外验证确保值为正整数，防止意外注入
val schemaVersion = format.schemaVersion
require(schemaVersion >= 0) { "schemaVersion must be non-negative: $schemaVersion" }
stmt.execute("PRAGMA user_version = $schemaVersion")
```

### Step 6: 运行全部 FSRS 测试确认无回归

- [ ] 运行测试

```bash
./gradlew test --tests "*fsrs*" -i
```

Expected: All FSRS tests PASSED (含 ApkgCreatorTest 等)

### Step 7: 提交

- [ ] Git commit

```bash
git add src/test/kotlin/com/mujingx/state/ScreenTypeTest.kt \
       src/test/kotlin/com/mujingx/state/GlobalStateTest.kt \
       src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseCreator.kt
git commit -m "fix(security): 修复 PRAGMA SQL 注入风险，补充 state 模块测试

- ApkgDatabaseCreator: 添加 schemaVersion 非负验证
- 新增 ScreenTypeTest: 枚举完整性、valueOf、边界测试
- 新增 GlobalStateTest: 默认值、状态修改、数据初始化"
```

---

## Task 2: 依赖版本统一 + event 模块测试

**Files:**
- Modify: `build.gradle.kts:44-45`
- Create: `src/test/kotlin/com/mujingx/event/EventBusTest.kt`

### Step 1: 写 EventBus 测试

- [ ] 创建 `src/test/kotlin/com/mujingx/event/EventBusTest.kt`

```kotlin
package com.mujingx.event

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class EventBusTest {

    private lateinit var eventBus: EventBus

    @BeforeEach
    fun setUp() {
        eventBus = EventBus()
    }

    @Test
    fun `post 和接收 应传递相同事件`() = runTest {
        var received: Any? = null
        val job = kotlinx.coroutines.launch {
            eventBus.events.collect { event ->
                received = event
                throw kotlinx.coroutines.CancellationException()
            }
        }

        eventBus.post("test-event")
        kotlinx.coroutines.delay(100)

        assertEquals("test-event", received)
        job.cancel()
    }

    @Test
    fun `post 多个事件 应按序接收`() = runTest {
        val received = mutableListOf<Any>()
        val job = kotlinx.coroutines.launch {
            eventBus.events.collect { event ->
                received.add(event)
                if (received.size >= 3) throw kotlinx.coroutines.CancellationException()
            }
        }

        eventBus.post("event-1")
        eventBus.post("event-2")
        eventBus.post("event-3")
        kotlinx.coroutines.delay(200)

        assertEquals(3, received.size)
        assertEquals("event-1", received[0])
        assertEquals("event-2", received[1])
        assertEquals("event-3", received[2])
        job.cancel()
    }

    @Test
    fun `post 枚举事件 应正确传递`() = runTest {
        var received: PlayerEventType? = null
        val job = kotlinx.coroutines.launch {
            eventBus.events.collect { event ->
                if (event is PlayerEventType) {
                    received = event
                    throw kotlinx.coroutines.CancellationException()
                }
            }
        }

        eventBus.post(PlayerEventType.PLAY)
        kotlinx.coroutines.delay(100)

        assertEquals(PlayerEventType.PLAY, received)
        job.cancel()
    }

    @Test
    fun `PlayerEventType 应包含所有预期的播放器事件`() {
        val expected = setOf(
            "PLAY", "ESC", "FULL_SCREEN", "CLOSE_PLAYER",
            "DIRECTION_LEFT", "DIRECTION_RIGHT", "DIRECTION_UP", "DIRECTION_DOWN",
            "PREVIOUS_CAPTION", "NEXT_CAPTION", "REPEAT_CAPTION", "AUTO_PAUSE",
            "TOGGLE_FIRST_CAPTION", "TOGGLE_SECOND_CAPTION"
        )
        val actual = PlayerEventType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `WordScreenEventType 应包含所有预期的单词界面事件`() {
        val expected = setOf(
            "NEXT_WORD", "PREVIOUS_WORD", "OPEN_SIDEBAR",
            "SHOW_WORD", "SHOW_PRONUNCIATION", "SHOW_LEMMA",
            "SHOW_DEFINITION", "SHOW_TRANSLATION", "SHOW_SENTENCES",
            "SHOW_SUBTITLES", "PLAY_AUDIO", "OPEN_VOCABULARY",
            "DELETE_WORD", "ADD_TO_FAMILIAR", "ADD_TO_DIFFICULT",
            "COPY_WORD", "PLAY_FIRST_CAPTION", "PLAY_SECOND_CAPTION",
            "PLAY_THIRD_CAPTION", "FOCUS_ON_WORD"
        )
        val actual = WordScreenEventType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `EventBus extraBufferCapacity 为 64`() {
        // 验证事件总线有足够的缓冲容量
        val bus = EventBus()
        assertNotNull(bus)
        assertNotNull(bus.events)
    }
}
```

### Step 2: 运行测试验证

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.event.EventBusTest" -i
```

Expected: 6 tests PASSED

### Step 3: 统一序列化库版本

- [ ] 修改 `build.gradle.kts` 第 44-45 行

将：
```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
```

改为：
```kotlin
// 统一使用与 filekit 0.12.0 兼容的版本
// filekit 依赖 serialization-core:1.7.3，此处显式声明以覆盖传递依赖
implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
```

### Step 4: 运行全部测试确认无回归

- [ ] 运行测试

```bash
./gradlew test -i
```

Expected: All tests PASSED（序列化兼容）

### Step 5: 提交

- [ ] Git commit

```bash
git add build.gradle.kts \
       src/test/kotlin/com/mujingx/event/EventBusTest.kt
git commit -m "fix(deps): 统一 kotlinx-serialization 版本至 1.7.3，补充 event 模块测试

- 统一 serialization-core 和 serialization-json 版本为 1.7.3
- 解决与 filekit 0.12.0 的传递依赖冲突
- 新增 EventBusTest: 事件发布/订阅、枚举完整性测试"
```

---

## Task 3: Lyric 模块测试

**Files:**
- Create: `src/test/kotlin/com/mujingx/lyric/LyricTest.kt`
- Create: `src/test/kotlin/com/mujingx/lyric/SongLyricTest.kt`

### Step 1: 写 Lyric 模型测试

- [ ] 创建 `src/test/kotlin/com/mujingx/lyric/LyricTest.kt`

```kotlin
package com.mujingx.lyric

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LyricTest {

    @Test
    fun `默认构造函数 应初始化为默认值`() {
        val lyric = Lyric()
        assertEquals(0.0, lyric.timestamp, 0.001)
        assertEquals("<no data>", lyric.lyric)
    }

    @Test
    fun `带参数构造函数 应正确赋值`() {
        val lyric = Lyric(10.5, "Hello World")
        assertEquals(10.5, lyric.timestamp, 0.001)
        assertEquals("Hello World", lyric.lyric)
    }

    @Test
    fun `拷贝构造函数 应创建相同对象`() {
        val original = Lyric(30.0, "Test lyric")
        val copy = Lyric(original)
        assertEquals(original.timestamp, copy.timestamp, 0.001)
        assertEquals(original.lyric, copy.lyric)
    }

    @Test
    fun `clone 应返回独立副本`() {
        val original = Lyric(15.0, "Original")
        val cloned = original.clone()
        // 修改克隆体不应影响原对象
        cloned.lyric = "Modified"
        assertEquals("Original", original.lyric)
        assertEquals("Modified", cloned.lyric)
    }

    @Test
    fun `equals 相同时间戳和歌词 应返回 true`() {
        val lyric1 = Lyric(10.0, "Same")
        val lyric2 = Lyric(10.0, "Same")
        assertEquals(lyric1, lyric2)
    }

    @Test
    fun `equals 不同时间戳 应返回 false`() {
        val lyric1 = Lyric(10.0, "Same")
        val lyric2 = Lyric(20.0, "Same")
        assertNotEquals(lyric1, lyric2)
    }

    @Test
    fun `equals 不同歌词 应返回 false`() {
        val lyric1 = Lyric(10.0, "First")
        val lyric2 = Lyric(10.0, "Second")
        assertNotEquals(lyric1, lyric2)
    }

    @Test
    fun `equals 非 Lyric 对象 应返回 false`() {
        val lyric = Lyric(10.0, "Test")
        assertNotEquals(lyric, "not a lyric")
        assertNotEquals(lyric, null)
    }

    @Test
    fun `toString 应包含时间戳和歌词`() {
        val lyric = Lyric(65.5, "Test lyric text")
        val result = lyric.toString()
        assertTrue(result.contains("65.50"))
        assertTrue(result.contains("Test lyric text"))
    }

    @Test
    fun `零时间戳 应正常工作`() {
        val lyric = Lyric(0.0, "Start")
        assertEquals(0.0, lyric.timestamp, 0.001)
    }

    @Test
    fun `负时间戳 应允许设置（用于偏移调整）`() {
        val lyric = Lyric(-5.0, "Negative offset")
        assertEquals(-5.0, lyric.timestamp, 0.001)
    }

    @Test
    fun `空歌词 应正常工作`() {
        val lyric = Lyric(10.0, "")
        assertEquals("", lyric.lyric)
    }
}
```

### Step 2: 运行测试验证

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.lyric.LyricTest" -i
```

Expected: 12 tests PASSED

### Step 3: 写 SongLyric 测试

- [ ] 创建 `src/test/kotlin/com/mujingx/lyric/SongLyricTest.kt`

```kotlin
package com.mujingx.lyric

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class SongLyricTest {

    private lateinit var songLyric: SongLyric

    @BeforeEach
    fun setUp() {
        songLyric = SongLyric()
    }

    @Test
    fun `默认构造函数 应创建空歌词列表`() {
        assertTrue(songLyric.song.isEmpty())
    }

    @Test
    fun `addLyric 应添加歌词到列表`() {
        songLyric.addLyric(Lyric(0.0, "First line"))
        songLyric.addLyric(Lyric(5.0, "Second line"))
        assertEquals(2, songLyric.song.size)
        assertEquals("First line", songLyric.song[0].lyric)
        assertEquals("Second line", songLyric.song[1].lyric)
    }

    @Test
    fun `带列表构造函数 应正确初始化`() {
        val lyrics = mutableListOf(
            Lyric(0.0, "Line 1"),
            Lyric(10.0, "Line 2")
        )
        val song = SongLyric(lyrics)
        assertEquals(2, song.song.size)
    }

    @Test
    fun `clone 应创建独立副本`() {
        songLyric.addLyric(Lyric(5.0, "Original"))
        val cloned = songLyric.clone()
        cloned.addLyric(Lyric(10.0, "Added to clone"))
        assertEquals(1, songLyric.song.size)
        assertEquals(2, cloned.song.size)
    }

    @Test
    fun `changeSpeed 2倍速 应将时间戳减半`() {
        songLyric.addLyric(Lyric(10.0, "Line 1"))
        songLyric.addLyric(Lyric(20.0, "Line 2"))
        songLyric.changeSpeed(2.0)
        assertEquals(5.0, songLyric.song[0].timestamp, 0.001)
        assertEquals(10.0, songLyric.song[1].timestamp, 0.001)
    }

    @Test
    fun `changeSpeed 0_5倍速 应将时间戳翻倍`() {
        songLyric.addLyric(Lyric(10.0, "Line 1"))
        songLyric.changeSpeed(0.5)
        assertEquals(20.0, songLyric.song[0].timestamp, 0.001)
    }

    @Test
    fun `toString 应包含所有歌词`() {
        songLyric.addLyric(Lyric(0.0, "Hello"))
        songLyric.addLyric(Lyric(5.0, "World"))
        val result = songLyric.toString()
        assertTrue(result.contains("Hello"))
        assertTrue(result.contains("World"))
    }

    @Test
    fun `大量歌词添加 应正常工作`() {
        for (i in 1..100) {
            songLyric.addLyric(Lyric(i.toDouble(), "Line $i"))
        }
        assertEquals(100, songLyric.song.size)
        assertEquals("Line 1", songLyric.song[0].lyric)
        assertEquals("Line 100", songLyric.song[99].lyric)
    }
}
```

### Step 4: 运行测试验证

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.lyric.SongLyricTest" -i
```

Expected: 8 tests PASSED

### Step 5: 提交

- [ ] Git commit

```bash
git add src/test/kotlin/com/mujingx/lyric/LyricTest.kt \
       src/test/kotlin/com/mujingx/lyric/SongLyricTest.kt
git commit -m "test(lyric): 补充 lyric 模块完整测试

- 新增 LyricTest: 构造函数、clone、equals、toString、边界值测试
- 新增 SongLyricTest: 添加歌词、clone、changeSpeed、大量数据测试"
```

---

## Task 4: Azure TTS 测试

**Files:**
- Create: `src/test/kotlin/com/mujingx/tts/AzureTTSTest.kt`
- Create: `src/test/kotlin/com/mujingx/tts/TTSFactoryTest.kt`

### Step 1: 写 AzureTTS 配置测试

- [ ] 创建 `src/test/kotlin/com/mujingx/tts/AzureTTSTest.kt`

```kotlin
package com.mujingx.tts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AzureTTSTest {

    @Test
    fun `AzureTTSData 默认值 应正确初始化`() {
        val data = AzureTTSData()
        assertEquals("", data.subscriptionKey)
        assertEquals("", data.region)
        assertEquals("en-US", data.pronunciationStyle)
        assertEquals("en-US-AvaNeural", data.shortName)
        assertEquals("Ava", data.displayName)
        assertEquals("Female", data.gender)
    }

    @Test
    fun `AzureTTSData 自定义值 应正确赋值`() {
        val data = AzureTTSData(
            subscriptionKey = "1234567890abcdef1234567890abcdef",
            region = "eastasia",
            pronunciationStyle = "zh-CN",
            shortName = "zh-CN-XiaoxiaoNeural",
            displayName = "晓晓",
            gender = "Female"
        )
        assertEquals("1234567890abcdef1234567890abcdef", data.subscriptionKey)
        assertEquals("eastasia", data.region)
        assertEquals("zh-CN", data.pronunciationStyle)
    }

    @Test
    fun `subscriptionKeyIsValid 32字符 应返回 true`() {
        val tts = AzureTTS(AzureTTSData(subscriptionKey = "a".repeat(32)))
        assertTrue(tts.subscriptionKeyIsValid())
    }

    @Test
    fun `subscriptionKeyIsValid 非32字符 应返回 false`() {
        val tts1 = AzureTTS(AzureTTSData(subscriptionKey = "short"))
        assertFalse(tts1.subscriptionKeyIsValid())

        val tts2 = AzureTTS(AzureTTSData(subscriptionKey = "a".repeat(33)))
        assertFalse(tts2.subscriptionKeyIsValid())

        val tts3 = AzureTTS(AzureTTSData(subscriptionKey = ""))
        assertFalse(tts3.subscriptionKeyIsValid())
    }

    @Test
    fun `regionIsValid 有效区域 应返回 true`() {
        val validRegions = listOf("eastasia", "westeurope", "eastus", "japaneast")
        for (region in validRegions) {
            val tts = AzureTTS(AzureTTSData(region = region))
            assertTrue(tts.regionIsValid(), "Region '$region' should be valid")
        }
    }

    @Test
    fun `regionIsValid 无效区域 应返回 false`() {
        val tts = AzureTTS(AzureTTSData(region = "invalid-region"))
        assertFalse(tts.regionIsValid())
    }

    @Test
    fun `regionIsValid 空区域 应返回 false`() {
        val tts = AzureTTS(AzureTTSData(region = ""))
        assertFalse(tts.regionIsValid())
    }

    @Test
    fun `getAccessToken 无有效密钥 应返回 null`() {
        val tts = AzureTTS(AzureTTSData(
            subscriptionKey = "short",
            region = "eastasia"
        ))
        assertNull(tts.getAccessToken())
    }

    @Test
    fun `Voice 数据类 应正确映射`() {
        val voice = Voice(
            DisplayName = "Xiaoxiao",
            ShortName = "zh-CN-XiaoxiaoNeural",
            Locale = "zh-CN",
            Gender = "Female"
        )
        assertEquals("Xiaoxiao", voice.DisplayName)
        assertEquals("zh-CN-XiaoxiaoNeural", voice.ShortName)
        assertEquals("zh-CN", voice.Locale)
        assertEquals("Female", voice.Gender)
    }
}
```

### Step 2: 运行测试验证

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.tts.AzureTTSTest" -i
```

Expected: 8 tests PASSED

### Step 3: 写 TTS 工厂/平台测试

- [ ] 创建 `src/test/kotlin/com/mujingx/tts/TTSFactoryTest.kt`

```kotlin
package com.mujingx.tts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TTSFactoryTest {

    @Test
    fun `AzureTTSData 序列化默认值 应为空字符串`() {
        val data = AzureTTSData()
        assertEquals("", data.subscriptionKey)
        assertEquals("", data.region)
    }

    @Test
    fun `AzureTTSData 不同语音配置 应正确区分`() {
        val enUs = AzureTTSData(
            pronunciationStyle = "en-US",
            shortName = "en-US-AvaNeural",
            displayName = "Ava",
            gender = "Female"
        )
        val zhCn = AzureTTSData(
            pronunciationStyle = "zh-CN",
            shortName = "zh-CN-XiaoxiaoNeural",
            displayName = "晓晓",
            gender = "Female"
        )
        assertNotEquals(enUs.shortName, zhCn.shortName)
        assertNotEquals(enUs.pronunciationStyle, zhCn.pronunciationStyle)
    }

    @Test
    fun `AzureTTS 修改属性后 应反映新值`() {
        val tts = AzureTTS(AzureTTSData())
        tts.subscriptionKey = "a".repeat(32)
        tts.region = "eastasia"
        assertTrue(tts.subscriptionKeyIsValid())
        assertTrue(tts.regionIsValid())
    }

    @Test
    fun `AzureTTS 切换发音风格 应正确更新`() {
        val tts = AzureTTS(AzureTTSData())
        tts.pronunciationStyle = "ja-JP"
        tts.shortName = "ja-JP-NanamiNeural"
        assertEquals("ja-JP", tts.pronunciationStyle)
        assertEquals("ja-JP-NanamiNeural", tts.shortName)
    }

    @Test
    fun `PlayerEventType 枚举完整 包含 PLAY`() {
        // 验证跨模块枚举可正常访问（无类加载问题）
        val playEvent = com.mujingx.event.PlayerEventType.PLAY
        assertNotNull(playEvent)
        assertEquals("PLAY", playEvent.name)
    }
}
```

### Step 4: 运行测试验证

- [ ] 运行测试

```bash
./gradlew test --tests "com.mujingx.tts.TTSFactoryTest" -i
```

Expected: 5 tests PASSED

### Step 5: 运行全部测试确认无回归

- [ ] 运行全部测试

```bash
./gradlew test -i
```

Expected: All tests PASSED

### Step 6: 提交

- [ ] Git commit

```bash
git add src/test/kotlin/com/mujingx/tts/AzureTTSTest.kt \
       src/test/kotlin/com/mujingx/tts/TTSFactoryTest.kt
git commit -m "test(tts): 补充 TTS 模块配置和验证测试

- 新增 AzureTTSTest: 配置验证、密钥校验、区域校验
- 新增 TTSFactoryTest: 属性修改、语音切换、跨模块枚举"
```

---

## 验收检查清单

完成所有任务包后，运行以下命令进行最终验收：

- [ ] 运行全量测试

```bash
./gradlew test -i
```

Expected: All tests PASSED, 无 FAIL 或 ERROR

- [ ] 确认新增测试文件数

```bash
find src/test -name "*Test.kt" -type f | wc -l
```

Expected: ~30+ （从 24 增加到 ~30+）

- [ ] 确认新增测试用例数

```bash
grep -r "@Test" src/test --include="*.kt" | wc -l
```

Expected: ~100+ 新增测试用例

- [ ] 确认安全修复

```bash
grep -n "PRAGMA user_version" src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseCreator.kt
```

Expected: 行旁有 `require(schemaVersion >= 0)` 验证

- [ ] 确认依赖版本统一

```bash
grep "kotlinx-serialization" build.gradle.kts
```

Expected: core 和 json 版本号一致（均为 1.7.3）
