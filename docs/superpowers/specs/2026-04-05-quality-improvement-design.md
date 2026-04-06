# 幕境项目质量提升设计文档

> 日期：2026-04-05
> 状态：已批准
> 基于：`docs/quality-review-2026-04-05.md` 审查报告

---

## 目标

通过 4 个独立任务包，交替进行安全修复与测试补充，将幕境项目从"可用"提升至"可靠"。

### 验收标准

| 指标 | 当前 | 目标 |
|------|------|------|
| SQL 注入风险 | 3 处 | 0 处 |
| 依赖版本冲突 | 1 处（序列化库） | 0 处 |
| 过旧依赖 | 4 个高风险 | 升级至安全版本 |
| 文件操作安全 | 无验证 | 路径验证 + 权限检查 |
| 测试文件数 | 24 个 | ~34 个 |
| 测试覆盖率 | ~15% | ~30%+ |
| 无测试模块 | tts/state/event/lyric | 全部补充测试 |

---

## 总体策略

- **交替进行**：每个任务包包含 1 个安全修复 + 1 个模块测试补充
- **小步迭代**：每个任务包独立可合并，降低风险
- **核心优先**：测试补充从核心基础设施模块（state → event → lyric → tts）逐步推进
- **每个任务包预计 2-3 天**，总计约 8-12 个工作日

---

## 任务包 1：SQL 注入修复 + state 模块测试

### 安全修复：SQL 注入

**问题**：3 处使用字符串拼接构造 SQL 语句

| 文件 | 行号 | 当前写法 |
|------|------|---------|
| `src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseCreator.kt` | ~91 | `"PRAGMA user_version = ${format.schemaVersion}"` |
| `src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseParser.kt` | ~130 | 字符串拼接 SQL |
| `src/main/kotlin/com/mujingx/fsrs/apkg/ApkgDatabaseParser.kt` | ~149 | 字符串拼接 SQL |

**修复方案**：将所有字符串拼接 SQL 改为 `PreparedStatement` 参数化查询。对于 `PRAGMA` 语句（SQLite 不支持参数化），改用白名单验证整数值。

**影响范围**：仅 `fsrs/apkg` 包内的 2 个文件，不影响外部接口。

### 测试补充：state 模块

**目标文件**：
- `src/main/kotlin/com/mujingx/state/AppState.kt`
- `src/main/kotlin/com/mujingx/state/GlobalState.kt`
- `src/main/kotlin/com/mujingx/state/ScreenType.kt`

**新增测试文件**：
- `src/test/kotlin/com/mujingx/state/AppStateTest.kt`
- `src/test/kotlin/com/mujingx/state/GlobalStateTest.kt`

**测试用例**（~30 个）：
- AppState：状态切换、屏幕导航、词库状态管理
- GlobalState：配置读写、持久化、默认值
- ScreenType：枚举完整性、序列化

---

## 任务包 2：依赖版本统一 + event 模块测试

### 安全修复：依赖版本冲突

**问题**：`filekit 0.12.0` 依赖 `kotlinx-serialization-core:1.7.3`，项目声明 `1.9.0`

**修复方案**：
1. 确认 filekit 0.12.0 与 serialization 1.9.0 的兼容性
2. 若兼容：在 `build.gradle.kts` 中添加强制版本解析
3. 若不兼容：升级 filekit 或降级 serialization

**影响范围**：`build.gradle.kts` 依赖声明，可能需要验证序列化功能。

### 测试补充：event 模块

**目标文件**：
- `src/main/kotlin/com/mujingx/event/EventBus.kt`
- `src/main/kotlin/com/mujingx/event/WindowKeyEvent.kt`

**新增测试文件**：
- `src/test/kotlin/com/mujingx/event/EventBusTest.kt`

**测试用例**（~25 个）：
- EventBus：事件发布/订阅、取消订阅、多订阅者
- WindowKeyEvent：快捷键映射、事件传递、边界情况

---

## 任务包 3：过旧依赖升级 + lyric 模块测试

### 安全修复：过旧依赖升级

**升级清单**：

| 依赖 | 当前版本 | 目标版本 | 风险评估 |
|------|---------|---------|---------|
| `net.bramp:ffmpeg` | 0.8.0 | 最新安全版本 | 中（API 可能变化） |
| `org.apache.opennlp:opennlp-tools` | 1.9.4 | 最新稳定版 | 低（向后兼容） |
| `org.apache.pdfbox:pdfbox` | 2.0.24 | 3.x 或最新 2.x | 中（2→3 有破坏性变更） |
| `org.apache.poi:poi` | 5.4.1 | 已较新，检查补丁 | 低 |

**修复方案**：
1. 逐个升级，每次升级后运行全部测试
2. PDFBox 优先升级到 2.x 最新补丁，评估 3.x 迁移成本
3. FFmpeg wrapper 检查 API 兼容性

**影响范围**：`build.gradle.kts` + 可能的 API 调整

### 测试补充：lyric 模块

**目标文件**：
- `src/main/kotlin/com/mujingx/lyric/Lyric.kt`
- `src/main/kotlin/com/mujingx/lyric/SongLyric.kt`
- `src/main/kotlin/com/mujingx/lyric/FileManager.kt`

**新增测试文件**：
- `src/test/kotlin/com/mujingx/lyric/LyricParserTest.kt`
- `src/test/kotlin/com/mujingx/lyric/SongLyricTest.kt`

**测试用例**（~20 个）：
- LRC 格式解析：时间标签、多行歌词、编码处理
- SongLyric：数据模型、时间排序、边界情况
- FileManager：文件读写、异常处理

---

## 任务包 4：文件操作安全加固 + tts 模块测试

### 安全修复：文件操作安全

**问题**：
- 文件操作没有验证路径是否在允许的目录范围内（目录遍历风险）
- 某些文件操作没有检查读写权限
- 临时文件没有正确清理

**修复方案**：
1. 添加路径验证工具函数：确保操作路径在应用数据目录内
2. 文件操作前检查权限（`File.canRead()`/`File.canWrite()`）
3. 临时文件使用 `File.createTempFile()` + `deleteOnExit()`
4. 文件流操作使用 `use {}` 自动关闭

**影响范围**：新增工具类 + 修改文件操作密集的模块

### 测试补充：tts 模块

**目标文件**：
- `src/main/kotlin/com/mujingx/tts/Azure TTS.kt`
- `src/main/kotlin/com/mujingx/tts/MacTTS.kt`
- `src/main/kotlin/com/mujingx/tts/MSTTSpeech.kt`
- `src/main/kotlin/com/mujingx/tts/UbuntuTTS.kt`

**新增测试文件**：
- `src/test/kotlin/com/mujingx/tts/AzureTTSTest.kt`
- `src/test/kotlin/com/mujingx/tts/TTSFactoryTest.kt`

**测试用例**（~25 个）：
- AzureTTS：配置加载/保存、token 获取逻辑、密钥加密/解密、语音列表获取
- TTSFactory：平台检测、TTS 实例创建、降级处理
- Mock 网络请求，不依赖真实 Azure 服务

---

## 约束与风险

### 约束
- 不修改任何用户可见的功能行为
- 不修改公共 API 接口
- 每个任务包可独立合并，不依赖其他任务包

### 风险

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| 依赖升级导致 API 不兼容 | 中 | 高 | 逐个升级，升级后运行全量测试 |
| PDFBox 2→3 破坏性变更 | 高 | 中 | 先升级到 2.x 最新补丁 |
| TTS 测试需要 Mock 平台 API | 中 | 低 | 使用接口抽象 + Mockito |
| PRAGMA 语句无法参数化 | 低 | 低 | 使用白名单验证整数值 |

---

## 不在范围内

以下事项明确排除在本次质量提升之外：

- 架构重构（拆分超大文件、分层架构）
- UI 测试补充
- 代码风格工具（Detekt/Ktlint）引入
- CI/CD 流水线改进
- CHANGELOG / 贡献指南添加
- 性能优化（懒加载、分页）

---

## 执行顺序

```
任务包 1（SQL 注入 + state 测试）
    ↓ 完成并合并
任务包 2（依赖统一 + event 测试）
    ↓ 完成并合并
任务包 3（依赖升级 + lyric 测试）
    ↓ 完成并合并
任务包 4（文件安全 + tts 测试）
    ↓ 完成并合并
验收测试
```

每个任务包完成后运行 `./gradlew test` 确保无回归。
