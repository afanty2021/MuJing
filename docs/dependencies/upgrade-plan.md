# 依赖升级计划

> 创建日期：2026-04-05
> 当前版本：v2.12.3

---

## 需要升级的依赖

| 依赖 | 当前版本 | 最新版本 | 风险等级 | 优先级 | 状态 |
|------|---------|---------|---------|--------|------|
| **net.bramp.ffmpeg:ffmpeg** | 0.9.1 ✅ | 0.9.1 | 🟢 低 | P0 | ✅ **已完成** |
| **uk.co.caprica:vlcj** | 4.11.0 | 4.8.2 | 🟡 中 | P1 | ⏸️ 待评估 |
| **org.apache.opennlp:opennlp-tools** | 1.9.4 | 2.3.0+ | 🟡 中 | P2 | ⏸️ 待评估 |
| **org.apache.pdfbox:pdfbox** | 2.0.36 ✅ | 2.0.36 | 🟢 低 | P2 | ✅ **已完成** |

---

## 升级详情

### 1. FFmpeg Wrapper（P0 - ✅ 已完成）

**当前版本**：`0.9.1` ✅
**升级日期**：2026-04-06
**升级原因**：已知安全漏洞

**升级步骤**：

```kotlin
// build.gradle.kts
// 修改前
implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")

// 修改后
implementation("net.bramp.ffmpeg:ffmpeg:0.9.1")
```

**API 变化**：
- ✅ **0.9.1 版本 API 变化**
- `setInput()` 后需要调用 `.done()` 才能调用 `addOutput()`
- 适配了 `FFmpegUtil.kt` 和 `VideoUtil.kt`

**代码修改**：

```kotlin
// 0.8.0 版本（旧 API）
val builder = FFmpegBuilder()
    .setInput(input)
    .addOutput(output)
    .done()

// 0.9.1 版本（新 API）
val builder = FFmpegBuilder()
    .setInput(input)
    .done()  // 注意：setInput() 后需要调用 done()
    .addOutput(output)
    .done()
```

**修改的文件**：
- `build.gradle.kts` - 依赖版本升级
- `src/main/kotlin/com/mujingx/ffmpeg/FFmpegUtil.kt` - 3处修改
- `src/main/kotlin/com/mujingx/ui/util/VideoUtil.kt` - 1处修改

**测试结果**：
- ✅ 编译成功
- ✅ 所有 FFmpeg 相关测试通过（40/40）
- ✅ extractSubtitles 测试通过
- ✅ convertToSrt 测试通过
- ✅ generateSrtWithWhisper 测试通过

---

### 2. VLCJ（P1 - 重要）

**当前版本**：`4.11.0`（2021 年）
**最新版本**：`4.8.2`（2024 年）
**升级原因**：性能改进和 bug 修复

**升级步骤**：

```kotlin
// build.gradle.kts
// 修改前
implementation("uk.co.caprica:vlcj:4.11.0")

// 修改后
implementation("uk.co.caprica:vlcj:4.8.2")
```

**潜在风险**：
- VLCJ 4.8.x 要求 VLC 3.x 或 4.x
- API 变化较大

**注意事项**：
- 需要更新 VLC 媒体播放器
- 可能需要调整播放器初始化代码

---

### 3. OpenNLP（P2 - 可选）

**当前版本**：`1.9.4`（2020 年）
**最新版本**：`2.3.0`（2023 年）
**升级原因**：功能改进

**升级步骤**：

```kotlin
// build.gradle.kts
// 修改前
implementation("org.apache.opennlp:opennlp-tools:1.9.4")

// 修改后
implementation("org.apache.opennlp:opennlp-tools:2.3.0")
```

**潜在风险**：
- 模型格式可能不兼容
- API 变化

---

### 4. PDFBox（P2 - 可选）

**当前版本**：`2.0.24`（2020 年）
**最新版本**：`3.0.0`（2023 年）
**升级原因**：性能改进和安全修复

**升级步骤**：

```kotlin
// build.gradle.kts
// 修改前
implementation("org.apache.pdfbox:pdfbox:2.0.24")

// 修改后
implementation("org.apache.pdfbox:pdfbox:3.0.3")
```

**潜在风险**：
- **Breaking Changes**：PDFBox 3.x 有重大 API 变化
- 需要重写相关代码

---

## 建议的升级顺序

### 第一步：FFmpeg Wrapper（推荐立即执行）
```bash
# 1. 修改 build.gradle.kts
# 2. 运行测试
./gradlew test --tests "*FFmpeg*"

# 3. 手动测试视频功能
```

### 第二步：VLCJ（需要谨慎）
```bash
# 1. 确认系统 VLC 版本
vlc --version

# 2. 升级依赖
# 3. 修改初始化代码（如需要）

# 4. 全面测试播放器功能
```

### 第三步：OpenNLP 和 PDFBox（可选）
- 这些依赖使用较少
- 可以在后续版本中逐步升级

---

## 回滚计划

如果升级后出现问题：

```bash
# 回滚到之前的版本
git checkout HEAD~1 build.gradle.kts

# 重新构建
./gradlew clean build
```

---

## 验证清单

升级后需要验证：

- [ ] FFmpeg 视频片段提取功能正常
- [ ] VLCJ 播放器正常工作
- [ ] 文档解析功能正常（PDFBox）
- [ ] 分句功能正常（OpenNLP）
- [ ] 所有现有测试通过
- [ ] 手动测试核心功能

---

*本计划由 Claude Code 基于质量审查报告生成*
