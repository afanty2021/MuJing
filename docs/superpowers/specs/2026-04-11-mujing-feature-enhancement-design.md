# 幕境 (MuJing) 功能增强设计文档

> 日期：2026-04-11
> 版本：1.0.0
> 状态：待审阅
> 项目版本：v2.12.3

## 概述

本文档规划幕境项目 5 项功能增强，借鉴 DashPlayer 项目的架构经验，采用**渐进式分层引入**策略实施。

### 功能清单与优先级

| 优先级 | 功能 | 核心目标 |
|--------|------|----------|
| P0-1 | 引入 Koin DI 框架 | Service 层解耦，为后续功能奠基 |
| P0-2 | 集成 AI 翻译功能 | 多 Provider 翻译服务，覆盖四大场景 |
| P1-1 | 添加国际化支持 | i18n 架构准备 + 字符串抽取 |
| P1-2 | 实现片段管理系统 | 字幕收藏 + 视频截取 + 标签管理 |
| P1-3 | 优化字幕同步精度 | 全局偏移 + 逐句微调 |

### 依赖关系

```
P0-1 (Koin DI)
  ├── P0-2 (AI 翻译) — 依赖 DI 注入 TranslationService
  └── P1-1 (i18n) — 可与 P0-2 并行
       ├── P1-2 (片段管理) — 可与 P1-3 并行
       └── P1-3 (字幕同步) — 可与 P1-2 并行
```

---

## P0-1: Koin DI 框架引入

### 目标

引入 Koin 依赖注入框架，仅覆盖 Service 层，降低服务间耦合度，为 AI 翻译等新功能提供注入基础。

### 设计决策

- **范围**：仅 Service 层和 Repository 层，UI 状态管理（Compose remember）保持不变
- **框架选择**：Koin（轻量、原生 Kotlin DSL、Compose Desktop 支持良好）
- **迁移策略**：渐进式，现有 `object` 单例逐步迁移为 Koin `single`

### 架构分层

```
┌─────────────────────────────────────────────┐
│  UI 层（保持 Compose remember 不变）          │
│  App.kt → rememberAppState()                │
│  WordScreen → rememberWordState()           │
│  SubtitleScreen → rememberSubtitlesState()  │
├─────────────────────────────────────────────┤
│  ↓ koinViewModel() 或 get() 注入             │
├─────────────────────────────────────────────┤
│  Service 层（Koin 管理）                      │
│  TranslationService (接口)                   │
│  FSRSService                                 │
│  DictionaryService                           │
│  ClipService                                 │
│  SubtitleSyncService                         │
├─────────────────────────────────────────────┤
│  ↓ Repository 注入                           │
├─────────────────────────────────────────────┤
│  Data 层（Repository 接口）                   │
│  VocabularyRepository → JSON 词库读写        │
│  ClipRepository → 片段数据存储               │
│  SettingsRepository → 配置持久化             │
└─────────────────────────────────────────────┘
```

### Koin 模块定义

```kotlin
// di/AppModule.kt
val appModule = module {
    single { DictionaryService(get()) }
    single { FSRSService() }
    single { VocabularyRepository() }
    single { SettingsRepository() }
}

// di/TranslationModule.kt
val translationModule = module {
    single<TranslationService> {
        MultiProviderTranslationService(providers = listOf(
            OpenAITranslationProvider(get()),
            YoudaoTranslationProvider(),
            AzureTranslationProvider(get())
        ))
    }
}

// di/ClipModule.kt
val clipModule = module {
    single { ClipService(get(), get()) }
    single { ClipRepository() }
}

// di/SubtitleModule.kt
val subtitleModule = module {
    single { SubtitleSyncService(get()) }
}
```

### 改动范围

- **新增**：`di/AppModule.kt`、`di/TranslationModule.kt`、`di/ClipModule.kt`、`di/SubtitleModule.kt`（4 文件）
- **修改**：`Main.kt`（Koin 初始化）、现有 Service 类（抽取接口 + 构造函数注入，~6 文件）
- **不改动**：UI Composable 函数、AppState/GlobalState 结构、Compose remember 模式

---

## P0-2: AI 翻译功能

### 目标

集成多 Provider AI 翻译服务，覆盖字幕翻译、即时翻译、词库翻译、文档翻译四种场景。

### Provider 架构

```
TranslationService (接口)
  └── MultiProviderTranslationService
        ├── OpenAITranslationProvider (GPT-4o-mini)
        ├── YoudaoTranslationProvider (有道翻译 API)
        └── AzureTranslationProvider (Azure Cognitive Services)
```

**路由策略**：
- 用户可配置默认 Provider
- 主 Provider 失败时自动 fallback 到备用 Provider
- 翻译结果缓存到 SQLite，避免重复 API 调用
- 内置速率限制控制

### 核心接口

```kotlin
// translation/TranslationService.kt
interface TranslationService {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String>
    suspend fun translateBatch(texts: List<String>, sourceLang: String, targetLang: String): Result<List<String>>
}

// translation/TranslationProvider.kt
interface TranslationProvider {
    val name: String
    suspend fun translate(text: String, from: String, to: String): String
    fun isConfigured(): Boolean  // API key 是否已设置
}

// translation/TranslationCache.kt
class TranslationCache(private val repository: TranslationCacheRepository) {
    fun get(text: String, from: String, to: String): String?
    fun put(text: String, from: String, to: String, result: String)
    suspend fun getBatch(texts: List<String>, from: String, to: String): Map<String, String>
}
```

### 四个翻译场景

#### 1. 字幕翻译
- **触发**：字幕浏览界面点击"翻译"按钮
- **流程**：选择字幕文件 → 批量翻译所有条目 → 生成双语字幕（原文+译文同行显示）
- **存储**：翻译结果缓存到 SQLite，关联字幕文件 hash

#### 2. 即时翻译
- **触发**：在任意界面选中文字后弹窗/右键菜单
- **流程**：选中文字 → 调用翻译 API → 浮层显示结果
- **存储**：翻译历史缓存到内存，重启清空

#### 3. 词库翻译
- **触发**：词库生成时自动翻译缺失释义的单词
- **流程**：生成词库 → 检测无翻译单词 → 批量调用 → 补充释义
- **存储**：结果写入词库 JSON + 同步到词典 SQLite

#### 4. 文档翻译
- **触发**：文档阅读界面的翻译工具栏
- **流程**：按段落/页面翻译 → 双语对照显示
- **存储**：翻译结果缓存，关联文档路径+段落 hash

### SQLite 翻译缓存表

```sql
CREATE TABLE translation_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_text TEXT NOT NULL,
    source_lang TEXT NOT NULL,
    target_lang TEXT NOT NULL,
    translated_text TEXT NOT NULL,
    provider TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    UNIQUE(source_text, source_lang, target_lang)
);
```

### 改动范围

- **新增**：~12 文件（translation/ 包 7 文件 + UI 组件 3 文件 + 数据层 2 文件）
- **修改**：~6 文件（SettingsDialog 添加翻译配置入口、SubtitleScreen 添加翻译按钮、WordScreen 即时翻译、GenerateVocabulary 词库翻译集成等）

---

## P1-1: 国际化架构准备

### 目标

建立 i18n 运行时架构，抽取现有 ~70-80 个硬编码中文字符串。本次不进行实际翻译。

### 架构设计

```
resources/i18n/
  ├── zh-CN.json   (主语言，包含所有已抽取的字符串)
  ├── en.json       (占位，仅含 key，值为空或 key 本身)
  └── _keys.json    (自动生成，用于检查缺失 key)
```

### 核心实现

```kotlin
// i18n/I18n.kt
object I18n {
    private var bundles: Map<String, String> = emptyMap()

    fun init(lang: String = "zh-CN") {
        bundles = loadBundle(lang)
    }

    fun t(key: String): String = bundles[key] ?: key

    fun t(key: String, vararg args: Any): String =
        bundles[key]?.format(*args) ?: key
}

// Compose 集成
val LocalI18n = compositionLocalOf { I18n }
```

### Key 命名规范

| 前缀 | 含义 | 示例 |
|------|------|------|
| `common.*` | 通用文本 | `common.confirm`, `common.cancel` |
| `word.*` | 单词学习界面 | `word.action.delete`, `word.action.add` |
| `subtitle.*` | 字幕浏览界面 | `subtitle.translate`, `subtitle.bookmark` |
| `player.*` | 播放器相关 | `player.play`, `player.pause` |
| `settings.*` | 设置对话框 | `settings.language`, `settings.translation` |
| `vocabulary.*` | 词库管理 | `vocabulary.generate`, `vocabulary.link` |
| `error.*` | 错误消息 | `error.file_not_found`, `error.parse_failed` |
| `translation.*` | 翻译功能 | `translation.provider.openai` |
| `clip.*` | 片段管理 | `clip.save`, `clip.tag` |

### 改动范围

- **新增**：3 文件（`I18n.kt`、`zh-CN.json`、`en.json`）
- **修改**：~40 文件（字符串替换 `"中文"` → `I18n.t("key")`）

### 本次不做

- 其他语言的实际翻译
- 语言切换 UI
- 运行时动态切换

---

## P1-2: 片段管理系统

### 目标

实现完整的片段管理系统：字幕片段收藏、视频片段截取、标签分类管理。

### 数据模型

```kotlin
// data/Clip.kt
@Serializable
data class Clip(
    val id: String,               // UUID
    val videoPath: String,         // 源视频路径
    val startTime: Long,           // 起始时间 (ms)
    val endTime: Long,             // 结束时间 (ms)
    var subtitleText: String,      // 字幕原文
    var translatedText: String?,   // AI 翻译（关联 P0-2）
    var note: String?,             // 用户笔记
    val tags: MutableList<String>,
    val createdAt: Long,           // 创建时间
    var videoClipPath: String?     // 截取后的视频路径
)

// data/ClipCollection.kt
@Serializable
data class ClipCollection(
    val name: String,
    val clips: MutableList<Clip>,
    var tags: MutableList<String>  // 所有可用标签
)
```

### 功能模块

#### 1. 字幕片段收藏
- 字幕界面右键/长按 → 收藏选中字幕
- 支持拖选多行字幕批量收藏
- 收藏时可选自动翻译（联动 P0-2）
- 添加标签和笔记

#### 2. 视频片段截取
- 在片段详情中点击"截取"
- 复用现有 FFmpegUtil 提取视频段
- 保存到用户目录 `~/.MuJing/clips/`
- 支持离线回放

#### 3. 标签管理
- 创建/删除自定义标签
- 按标签筛选片段
- 按视频源/时间范围筛选
- 片段复习模式（结合 FSRS 算法）

### SQLite 新增表

```sql
CREATE TABLE clips (
    id TEXT PRIMARY KEY,
    video_path TEXT NOT NULL,
    start_time INTEGER NOT NULL,
    end_time INTEGER NOT NULL,
    subtitle_text TEXT NOT NULL,
    translated_text TEXT,
    note TEXT,
    video_clip_path TEXT,
    collection_id TEXT,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (collection_id) REFERENCES clip_collections(id)
);

CREATE TABLE clip_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    color TEXT
);

CREATE TABLE clip_tag_relations (
    clip_id TEXT NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (clip_id, tag_id),
    FOREIGN KEY (clip_id) REFERENCES clips(id),
    FOREIGN KEY (tag_id) REFERENCES clip_tags(id)
);

CREATE TABLE clip_collections (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    created_at INTEGER NOT NULL
);
```

### 集成点

- **字幕界面**（SubtitleScreen）：添加收藏入口（右键菜单 + 收藏按钮）
- **播放器界面**：添加片段标记快捷键
- **词库界面**（WordScreen）：关联片段播放，查看单词在视频中的使用场景
- **FFmpegUtil**：复用现有 `extractVideoSegment` 能力

### 改动范围

- **新增**：~8 文件（数据模型 2 + Service 1 + Repository 1 + UI 组件 4）
- **修改**：~4 文件（SubtitleScreen、PlayerState、WordScreen、EventBus）

---

## P1-3: 字幕同步精度优化

### 目标

实现全局偏移调整 + 逐句微调的双层字幕同步机制。

### 三层架构

```
全局偏移层 (GlobalOffset)
  → 应用于当前视频的所有字幕
  → 持久化到配置文件，按视频路径隔离

逐句微调层 (PerCaptionOffset)
  → 单条字幕的独立时间微调
  → 持久化到 SQLite，按视频+字幕文件关联

时间映射层 (EffectiveTime)
  → 最终生效时间 = originalTime + globalOffset + perCaptionOffset[index]
  → 播放器和字幕界面统一查询此接口
```

### 核心接口

```kotlin
// subtitle/SubtitleSyncService.kt
interface SubtitleSyncService {
    // 全局偏移
    fun getGlobalOffset(videoPath: String): Long
    fun setGlobalOffset(videoPath: String, offsetMs: Long)

    // 逐句微调
    fun getCaptionOffset(videoPath: String, captionIndex: Int): Long
    fun setCaptionOffset(videoPath: String, captionIndex: Int, offsetMs: Long)

    // 计算最终时间（播放器调用）
    fun getEffectiveTime(videoPath: String, captionIndex: Int, originalStart: Long): Long

    // 批量获取（字幕列表渲染时使用）
    fun getEffectiveTimes(videoPath: String, captions: List<Caption>): List<Long>
}
```

### UI 交互

#### 全局偏移控制
- 按钮组：-500ms / -100ms / [当前值] / +100ms / +500ms / 重置
- 快捷键：`[` / `]` 调整 ±100ms，`Shift+[` / `Shift+]` 调整 ±500ms
- 实时生效，播放中可调整

#### 逐句微调
- 双击字幕条目进入微调模式
- 快捷键：`Alt+←` / `Alt+→` 微调 ±50ms（需选中字幕条）
- 微调值显示在字幕条目右侧
- 支持清除单条微调

### SQLite 新增表

```sql
CREATE TABLE subtitle_global_offset (
    video_path TEXT PRIMARY KEY,
    offset_ms INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);

CREATE TABLE subtitle_caption_offset (
    video_path TEXT NOT NULL,
    subtitle_file_hash TEXT NOT NULL,
    caption_index INTEGER NOT NULL,
    offset_ms INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (video_path, subtitle_file_hash, caption_index)
);
```

### 性能考虑

- **全局偏移**：内存缓存，启动时加载，变更时写 SQLite
- **逐句偏移**：惰性加载，按视频路径分组缓存为 `Map<Int, Long>`
- **字幕渲染**：一次性调用 `getEffectiveTimes()` 计算整页有效时间，避免逐条查询

### 改动范围

- **新增**：~4 文件（SubtitleSyncService 接口 + 实现 + 数据表 + UI 组件）
- **修改**：~3 文件（PlayerState 同步逻辑、SubtitleScreen UI 集成、EventBus 快捷键注册）

---

## 实施阶段规划

### Phase 1: 基础设施（P0-1）
- Koin 依赖添加
- DI 模块定义
- 现有 Service 接口抽取
- Main.kt 初始化 Koin

### Phase 2: AI 翻译（P0-2）
- TranslationService 接口和 Provider 架构
- OpenAI / 有道 / Azure 三个 Provider 实现
- 翻译缓存层
- 四个场景的 UI 集成
- SettingsDialog 翻译配置入口

### Phase 3: 架构准备 + 数据层（P1-1 + P1-2 数据层）
- I18n 运行时核心类
- 字符串抽取（~70-80 个）
- Clip 数据模型和 SQLite 表
- ClipService 和 ClipRepository

### Phase 4: 功能 UI（P1-2 UI + P1-3）
- 片段管理 UI 组件
- 字幕收藏和视频截取集成
- SubtitleSyncService 实现
- 字幕同步 UI 控制面板
- 快捷键注册

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Koin 与 Compose Desktop 兼容性 | 中 | 提前验证 Koin 4.x + Compose 1.9.3 组合 |
| API 调用成本（翻译） | 中 | SQLite 缓存 + 批量接口 + 速率限制 |
| i18n 字符串抽取遗漏 | 低 | Gradle 自动检查任务 |
| FFmpeg 片段截取性能 | 低 | 已有 FFmpegUtil，异步执行 |
| 字幕同步偏移数据丢失 | 低 | SQLite 持久化 + 内存缓存 |

---

## 附录：与 DashPlayer 的借鉴映射

| 幕境新功能 | 借鉴的 DashPlayer 设计 |
|-----------|----------------------|
| Koin DI | Inversify 依赖注入架构 |
| TranslationService 多 Provider | 多翻译 Provider 架构（OpenAI/有道/腾讯） |
| TranslationCache | DashPlayer 的翻译缓存机制 |
| Clip 片段管理 | videoClip + videoLearningClip 表设计 |
| 标签系统 | DashPlayer 的 Tag 系统 |
| SubtitleSyncService | SrtTender 时间映射 + subtitleTimestampAdjustment 表 |
| i18n 架构 | DashPlayer 的 i18n 目录结构 |
