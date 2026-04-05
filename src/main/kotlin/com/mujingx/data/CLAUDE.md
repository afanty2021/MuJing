# Data 模块 - 数据模型与词典管理

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **data**

## 模块职责

Data 模块是幕境应用的数据层核心，负责：
- **词库数据模型**：定义单词、词库、词汇类型的数据结构
- **词典查询**：集成英语词典数据库，提供单词释义、音标、例句查询
- **数据持久化**：词库的序列化、反序列化和存储管理
- **加密安全**：敏感数据的加密存储

## 入口与启动

### 主要数据模型
```kotlin
// Vocabulary.kt - 词库数据模型
@Serializable
data class Vocabulary(
    val name: String,
    val type: VocabularyType,
    val words: List<Word>,
    val metadata: Metadata? = null
)

// Dictionary.kt - 词典查询接口
class Dictionary {
    fun lookup(word: String): WordDetail?
    fun lookupBatch(words: List<String>): Map<String, WordDetail>
}

// VocabularyType.kt - 词库类型枚举
enum class VocabularyType {
    SUBTITLE,    // 字幕词库
    DOCUMENT,    // 文档词库
    BUILT_IN,    // 内置词库
    CUSTOM       // 自定义词库
}
```

### 数据文件路径
- **用户词库**：`~/.mujing/vocabularies/`
- **词典数据库**：`resources/common/dictionary/ecdict.db`
- **内置词库**：`resources/common/vocabulary/`

## 对外接口

### 词典查询 API
```kotlin
// 单词查询
fun getWordDetail(word: String): WordDetail?

// 批量查询（性能优化）
fun getWordDetails(words: List<String>): Map<String, WordDetail>

// 模糊匹配
fun searchWords(prefix: String): List<String>
```

### 词库管理 API
```kotlin
// 加载词库
fun loadVocabulary(file: File): Vocabulary

// 保存词库
fun saveVocabulary(vocabulary: Vocabulary, file: File)

// 导入/导出
fun exportToAnki(vocabulary: Vocabulary, outputFile: File)
fun importFromAnki(apkgFile: File): Vocabulary
```

## 关键依赖与配置

### 依赖项
```kotlin
// SQLite 数据库
implementation("org.xerial:sqlite-jdbc:3.44.1.0")

// Kotlin 序列化
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

// 加密库（自定义实现）
implementation("com.mujingx.data:crypt")
```

### 词典数据库
- **格式**：SQLite 3
- **表结构**：
  - `words` - 单词表（单词、音标、词性、释义）
  - `examples` - 例句表（英文例句、中文翻译）
  - `inflections` - 词形变化表（时态、复数等）
- **索引**：单词字段建立 B-tree 索引

## 数据模型

### Word (单词)
```kotlin
@Serializable
data class Word(
    val word: String,              // 单词拼写
    val phonetic: String? = null,  // 音标 (IPA)
    val definition: String? = null,// 中文释义
    val translation: String? = null, // 英文释义
    val example: String? = null,   // 例句（英文）
    val exampleTranslation: String? = null, // 例句翻译
    val caption: String? = null,   // 出现的字幕/原文
    val startTime: Long? = null,   // 视频开始时间（毫秒）
    val endTime: Long? = null,     // 视频结束时间（毫秒）
    val inflections: List<String> = emptyList(), // 词形变化
    val difficulty: Int? = null    // 难度等级（1-5）
)
```

### Vocabulary (词库)
```kotlin
@Serializable
data class Vocabulary(
    val name: String,              // 词库名称
    val type: VocabularyType,      // 词库类型
    val words: List<Word>,         // 单词列表
    val source: String? = null,    // 来源（文件名/视频名）
    val createdAt: Long = System.currentTimeMillis(), // 创建时间
    val updatedAt: Long = System.currentTimeMillis(), // 更新时间
    val metadata: Metadata? = null // 元数据
)
```

### Metadata (元数据)
```kotlin
@Serializable
data class Metadata(
    val videoPath: String? = null,    // 视频文件路径
    val subtitlePath: String? = null, // 字幕文件路径
    val duration: Long? = null,       // 视频总时长
    val wordCount: Int = 0,           // 单词总数
    val language: String = "en",      // 语言
    val tags: List<String> = emptyList() // 标签
)
```

## 测试与质量

### 单元测试
- **文件**：`src/test/kotlin/com/mujingx/data/DictionaryTest.kt`
- **覆盖范围**：
  - 词典查询功能
  - 批量查询性能
  - 模糊匹配准确性
  - 词库序列化/反序列化

### 测试命令
```bash
# 运行数据模块测试
./gradlew test --tests "*Dictionary*"

# 测试词典查询性能
./gradlew test --tests "*DictionaryPerformance*"
```

## 常见问题 (FAQ)

### Q: 如何添加新的词典数据源？
A: 在 `Dictionary.kt` 中实现新的查询接口，支持多种词典格式（SQLite、JSON、API）。

### Q: 词库文件损坏如何恢复？
A: 应用会自动备份词库文件到 `~/.mujing/backups/`，可以从备份恢复。

### Q: 如何优化词典查询性能？
A: 使用批量查询 API 而非循环单次查询；确保 SQLite 数据库有适当索引。

### Q: 支持哪些词典格式？
A: 目前支持 ECDICT (SQLite) 格式，计划支持 Stardict、Mdx 等格式。

## 相关文件清单

### 核心文件
- `Vocabulary.kt` - 词库数据模型
- `Dictionary.kt` - 词典查询接口
- `VocabularyType.kt` - 词库类型枚举
- `Wikitionary.kt` - 维基词典集成
- `RecentItem.kt` - 最近使用记录
- `GitHubRelease.kt` - GitHub 发布信息
- `MujingRelease.kt` - 应用版本信息
- `crypt.kt` - 加密工具

### 测试文件
- `src/test/kotlin/com/mujingx/data/DictionaryTest.kt`

### 资源文件
- `resources/common/dictionary/ecdict.db` - 词典数据库
- `resources/common/vocabulary/` - 内置词库

## 变更记录 (Changelog)

### 2026-04-05 - 创建 data 模块文档
- 📚 初始化模块文档
- 🗂️ 记录数据模型和接口定义
- 🔧 添加测试策略和常见问题

---

**依赖关系**：
- 被 `ui` 模块依赖（词库显示和编辑）
- 被 `fsrs` 模块依赖（Anki 导入/导出）
- 被 `player` 模块依赖（字幕显示）
