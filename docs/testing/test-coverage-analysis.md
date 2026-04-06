# 幕境项目 - 测试覆盖率分析报告

> 生成日期：2026-04-05
> 项目版本：v2.12.3
> 当前测试覆盖率：~25% (31 个测试文件 / ~160 个源文件)

---

## 测试覆盖率概览

| 模块 | 测试文件 | 源文件数 | 覆盖率 | 优先级 |
|------|---------|---------|--------|--------|
| **fsrs** | 15 | ~20 | ⭐ 75%+ | - |
| **player** | 5 | ~10 | ✅ 50% | P2 |
| **lyric** | 2 | ~5 | ✅ 40% | P2 |
| **state** | 2 | ~5 | ✅ 40% | P3 |
| **tts** | 2 | ~5 | ✅ 40% | P3 |
| **theme** | 2 | ~3 | ✅ 67% | - |
| **event** | 1 | ~2 | ✅ 50% | - |
| **data** | 1 | ~10 | 🟡 10% | **P0** |
| **ui** | 1 | ~79 | 🟡 1% | **P0** |
| **ffmpeg** | 0 | ~1 | ❌ 0% | **P0** |
| **icons** | 0 | ~5 | ❌ 0% | P3 |

---

## 需要优先添加测试的模块

### 🔴 P0 - 高优先级（核心功能，无测试或测试极少）

#### 1. ffmpeg 模块（0% 覆盖率）

**文件**：
- `FFmpegUtil.kt` - 视频处理工具类

**需要测试的功能**：
```kotlin
// FFmpegUtil.kt 核心方法
- extractVideoSegment()     // 提取视频片段
- extractAudio()             // 提取音频
- getVideoDuration()         // 获取视频时长
- convertVideoFormat()       // 格式转换
- getVideoInfo()             // 获取视频信息
```

**建议测试文件**：
```
src/test/kotlin/com/mujingx/ffmpeg/
├── FFmpegUtilTest.kt         // FFmpeg 工具测试
└── FFmpegIntegrationTest.kt  // 集成测试（需要测试视频文件）
```

**测试难点**：
- 需要真实的视频文件作为测试资源
- FFmpeg 命令执行可能需要 mock
- 集成测试需要较长时间

---

#### 2. data 模块（10% 覆盖率）

**当前测试**：`DictionaryTest.kt`（词典查询测试）

**缺少测试的文件**：
- `Vocabulary.kt` - 词库数据模型（核心）
- `Dictionary.kt` - 词典查询（部分覆盖）
- `Wikitionary.kt` - 维基词典集成
- `crypt.kt` - 加密工具
- `VocabularyType.kt` - 词库类型
- `RecentItem.kt` - 最近使用记录
- `MujingRelease.kt` - 版本信息
- `GitHubRelease.kt` - GitHub 发布信息

**建议测试文件**：
```
src/test/kotlin/com/mujingx/data/
├── VocabularyTest.kt         // 词库序列化/反序列化
├── VocabularyTypeTest.kt     // 词库类型测试
├── CryptTest.kt              // 加密/解密测试
├── RecentItemTest.kt         // 最近项测试
├── WikitionaryTest.kt        // 维基词典测试
└── MujingReleaseTest.kt      // 版本信息测试
```

**优先测试项**：
1. `Vocabulary.kt` - 词库加载、保存、序列化
2. `crypt.kt` - 加密解密功能
3. `VocabularyType.kt` - 词库类型判断

---

#### 3. ui 模块（1% 覆盖率）

**当前测试**：`WordScreenTest.kt`（单词学习界面）

**缺少测试的重要组件**（79 个文件，只测试了 1 个）：

**核心对话框**（优先级高）：
```
src/main/kotlin/com/mujingx/ui/dialog/
├── GenerateVocabularyDialog.kt  (3193 行 - 生成词库对话框)
├── EditWordDialog.kt            (1533 行 - 编辑单词对话框)
├── DocumentDialog.kt            (1209 行 - 文档对话框)
├── SettingsDialog.kt            (783 行 - 设置对话框)
└── ColorPicker.kt               (1091 行 - 颜色选择器)
```

**其他重要组件**：
```
src/main/kotlin/com/mujingx/ui/
├── App.kt                        (892 行 - 主应用)
├── wordscreen/
│   ├── WordScreen.kt             (3287 行 - 单词学习界面)
│   └── WordScreenSidebar.kt      (1154 行 - 侧边栏)
├── textscreen/
│   └── TextScreen.kt             (748 行 - 文档阅读)
├── subtitlescreen/
│   └── SubtitleScreen.kt         (1472 行 - 字幕浏览)
└── edit/
    └── EditVocabulary.kt         (1361 行 - 词库编辑)
```

**建议测试文件**：
```
src/test/kotlin/com/mujingx/ui/
├── dialog/
│   ├── SettingsDialogTest.kt        // 设置对话框
│   ├── ColorPickerTest.kt           // 颜色选择器
│   └── DocumentDialogTest.kt         // 文档对话框
├── util/
│   ├── GenerateVocabularyTest.kt    // 词库生成工具
│   └── VideoUtilTest.kt             // 视频工具
└── components/
    └── CustomComponentsTest.kt      // 自定义组件
```

**测试难点**：
- UI 组件测试需要 Compose 测试环境
- 大部分组件需要显示环境
- 部分功能需要文件系统交互

---

### 🟡 P1 - 中优先级（功能重要，已有部分测试）

#### 4. player 模块（50% 覆盖率）

**当前测试**：
- `PlayerStateTest.kt` - 播放器状态
- `danmaku/` - 弹幕系统测试（4个文件）

**缺少测试的文件**：
```
src/main/kotlin/com/mujingx/player/
├── VidePlayer.kt              (1864 行 - 核心播放器)
├── VideoPlayerComponents.kt    (1338 行 - 播放器组件)
├── TimedCaption.kt             (787 行 - 字幕同步)
└── HoverableCaption.kt         (737 行 - 可悬停字幕)
```

**建议测试文件**：
```
src/test/kotlin/com/mujingx/player/
├── VideoPlayerComponentsTest.kt  // 播放器组件测试
├── TimedCaptionTest.kt            // 字幕同步测试
└── HoverableCaptionTest.kt        // 悬停字幕测试
```

---

### 🟢 P2 - 低优先级（已有基础测试）

#### 5. lyric 模块（40% 覆盖率）

**当前测试**：`LyricTest.kt`, `SongLyricTest.kt`

**状态**：基本覆盖，可添加边界情况测试

#### 6. tts 模块（40% 覆盖率）

**当前测试**：`AzureTTSTest.kt`, `TTSFactoryTest.kt`

**缺少测试**：
- macOS TTS 实现
- Windows TTS 实现
- TTS 服务选择逻辑

#### 7. state 模块（40% 覆盖率）

**当前测试**：`GlobalStateTest.kt`, `ScreenTypeTest.kt`

**状态**：基本覆盖，可添加状态转换测试

---

## 测试优先级路线图

### 第一阶段（1-2 周）- 核心 P0 模块

| 优先级 | 模块 | 测试文件 | 预计工作量 |
|--------|------|---------|-----------|
| 1 | ffmpeg | FFmpegUtilTest.kt | 2 天 |
| 2 | data | VocabularyTest.kt, CryptTest.kt | 3 天 |
| 3 | ui | SettingsDialogTest.kt, ColorPickerTest.kt | 3 天 |

### 第二阶段（2-3 周）- 重要 P1 模块

| 优先级 | 模块 | 测试文件 | 预计工作量 |
|--------|------|---------|-----------|
| 4 | player | VideoPlayerComponentsTest.kt | 2 天 |
| 5 | data | WikitionaryTest.kt, VocabularyTypeTest.kt | 2 天 |
| 6 | ui | GenerateVocabularyTest.kt, VideoUtilTest.kt | 3 天 |

### 第三阶段（持续）- 补充测试

| 优先级 | 模块 | 测试文件 | 预计工作量 |
|--------|------|---------|-----------|
| 7 | tts | MacTTSTest.kt, WinTTSTest.kt | 2 天 |
| 8 | state | AppStateTest.kt | 1 天 |
| 9 | ui | 其他对话框测试 | 持续 |

---

## 测试策略建议

### 1. 单元测试优先

对于每个模块，优先测试：
- ✅ 纯函数（无副作用）
- ✅ 数据模型（序列化/反序列化）
- ✅ 工具类（静态方法）
- ⚠️ 有状态类（需要 mock 依赖）

### 2. 集成测试其次

对于复杂的交互：
- 测试完整的用户流程
- 使用真实的文件系统
- 测试外部 API 集成（需要 mock）

### 3. UI 测试最后

UI 组件测试：
- 使用 Compose UI Testing
- 测试用户交互（点击、输入）
- 测试状态变化
- ⚠️ 需要显示环境（可能无法在 CI 中运行）

---

## 测试基础设施建议

### 1. 测试资源管理

```
src/test/resources/
├── videos/          # 测试用视频文件
│   ├── sample.mp4   # 短视频（用于快速测试）
│   └── long.mp4     # 长视频（用于性能测试）
├── vocabularies/    # 测试用词库
│   ├── small.json   # 小词库（10个单词）
│   └── large.json   # 大词库（1000个单词）
└── temp/            # 临时测试文件目录
```

### 2. 测试工具类

创建 `src/test/kotlin/com/mujingx/testutil/`：

```kotlin
// TestHelpers.kt
object TestHelpers {
    fun createTestVocabulary(size: Int): Vocabulary
    fun createTestVideoFile(): File
    fun mockMediaPlayer(): MediaPlayer
}
```

### 3. Mock 外部依赖

对于需要外部依赖的测试：
- VLCJ 播放器 → mock
- TTS 服务 → mock
- 网络请求 → mock
- 文件系统 → 使用临时目录

---

## 目标测试覆盖率

| 阶段 | 目标覆盖率 | 当前 | 差距 |
|------|-----------|------|------|
| 短期（1个月） | 40% | 25% | +15% |
| 中期（3个月） | 60% | 25% | +35% |
| 长期（6个月） | 80% | 25% | +55% |

---

## 总结

**当前状态**：
- ✅ fsrs 模块测试覆盖率优秀（75%+）
- ⚠️ 核心模块（data, ui, ffmpeg）测试覆盖率不足
- 📊 总体覆盖率约 25%

**下一步行动**：
1. 🔴 立即：为 ffmpeg 模块添加测试
2. 🔴 本周：为 data 模块添加核心测试
3. 🔴 本月：为 ui 模块添加关键组件测试

---

*本报告由 Claude Code 自动生成*
