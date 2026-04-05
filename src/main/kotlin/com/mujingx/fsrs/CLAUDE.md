# FSRS 模块 - 间隔重复系统

[根目录](../../../../CLAUDE.md) > [src/main/kotlin/com/mujingx](../../) > **fsrs**

## 模块职责

FSRS 模块是幕境应用的学习算法核心，负责：
- **间隔重复算法**：基于 FSRS (Free Spaced Repetition Scheduler) 的智能复习调度
- **学习会话管理**：管理学习进度和记忆状态
- **Anki 集成**：Anki 卡片导入/导出功能
- **压缩算法**：使用 zstd 压缩 Anki 牌组

## 入口与启动

### 核心服务
```kotlin
// FSRSService.kt - FSRS 服务
class FSRSService {
    fun schedule(card: FlashCard, rating: Rating): ScheduledCard
    fun calculateInterval(card: FlashCard): Long
    fun updateDifficulty(card: FlashCard, rating: Rating)
}

// FlashCardManager.kt - 卡片管理器
class FlashCardManager {
    fun createCards(vocabulary: Vocabulary): List<FlashCard>
    fun updateCard(card: FlashCard, rating: Rating)
    fun getDueCards(): List<FlashCard>
    fun getAllCards(): List<FlashCard>
}

// LearningSessionManager.kt - 学习会话管理
class LearningSessionManager {
    fun startSession(cards: List<FlashCard>)
    fun getCurrentCard(): FlashCard?
    fun answerCard(rating: Rating)
    fun endSession()
}
```

### FSRS 算法
```kotlin
// fsrs.kt - FSRS 算法实现
object FSRS {
    // 核心调度函数
    fun schedule(
        card: FlashCard,
        rating: Rating,
        now: Long = System.currentTimeMillis()
    ): ScheduledCard

    // 计算记忆稳定性
    fun calculateStability(
        stability: Double,
        rating: Rating
    ): Double

    // 计算记忆难度
    fun calculateDifficulty(
        difficulty: Double,
        rating: Rating
    ): Double
}
```

## 对外接口

### 卡片调度 API
```kotlin
// 调度卡片
fun schedule(card: FlashCard, rating: Rating): ScheduledCard

// 批量调度
fun scheduleBatch(cards: List<FlashCard>, ratings: List<Rating>): List<ScheduledCard>

// 获取到期卡片
fun getDueCards(before: Long = System.currentTimeMillis()): List<FlashCard>

// 预览下次复习时间
fun previewNextReview(card: FlashCard, rating: Rating): Long
```

### Anki 导入/导出
```kotlin
// 导出为 Anki 牌组
suspend fun exportToAnki(
    cards: List<FlashCard>,
    outputFile: File,
    mediaDir: File? = null
)

// 导入 Anki 牌组
suspend fun importFromAnki(apkgFile: File): List<FlashCard>

// 导出词库为 Anki 格式
suspend fun exportVocabularyToAnki(
    vocabulary: Vocabulary,
    outputFile: File
)
```

## 关键依赖与配置

### 依赖项
```kotlin
// Kotlinx Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

// Rust zstd JNI (本地构建)
implementation(project(":rust-zstd-jni"))

// Apache POI (Excel 导出)
implementation("org.apache.poi:poi:5.4.1")
implementation("org.apache.poi:poi-ooxml:5.4.1")
```

### FSRS 参数
```kotlin
// Models.kt - FSRS 参数模型
data class FSRSParameters(
    val requestRetention: Double = 0.9,  // 期望留存率
    val maximumInterval: Int = 36500,    // 最大间隔（天）
    val w: List<Double> = listOf(        // 权重参数
        0.4, 0.6, 2.4, 5.8, 4.93, 0.94, 0.86, 0.01, 1.49, 0.14, 0.94, 2.18, 0.05, 0.34, 1.26, 0.29, 2.61
    )
)
```

## 数据模型

### FlashCard (记忆卡片)
```kotlin
@Serializable
data class FlashCard(
    val id: String = UUID.randomUUID().toString(),
    val word: String,
    val front: String,           // 正面（单词）
    val back: String,            // 背面（释义）
    val context: String? = null, // 语境

    // FSRS 参数
    val stability: Double = 0.0,     // 记忆稳定性
    val difficulty: Double = 0.0,    // 记忆难度
    val elapsedDays: Int = 0,        // 已过去天数
    val scheduledDays: Int = 0,      // 计划天数
    val reps: Int = 0,               // 重复次数
    val lapses: Int = 0,             // 遗忘次数
    val state: CardState = CardState.NEW,  // 卡片状态

    // 时间戳
    val due: Long = 0L,              // 到期时间
    val lastReview: Long = 0L        // 上次复习时间
)
```

### Rating (评分)
```kotlin
enum class Rating {
    AGAIN,      // 忘记 (1)
    HARD,       // 困难 (2)
    GOOD,       // 良好 (3)
    EASY        // 简单 (4)
}
```

### CardState (卡片状态)
```kotlin
enum class CardState {
    NEW,        // 新卡片
    LEARNING,   // 学习中
    REVIEW,     // 复习中
    RELEARNING  // 重新学习中
}
```

### ScheduledCard (调度的卡片)
```kotlin
data class ScheduledCard(
    val card: FlashCard,
    val reviewTime: Long,      // 下次复习时间
    val interval: Long,        // 间隔时间（天）
    val stability: Double,     // 新的稳定性
    val difficulty: Double     // 新的难度
)
```

## Anki 集成

### Anki 格式支持
```
apkg/
├── collection.anki2       // SQLite 数据库
├── media                  // 媒体文件
│   ├── 0.mp3
│   ├── 1.mp4
│   └── ...
└── _meta.yaml            // 元数据
```

### Apkg 文件结构
```kotlin
// apkg/ApkgFormat.kt - Anki 格式定义
data class ApkgMeta(
    val schemaMod: Long,
    val scm: Long,
    val usn: Long,
    val version: List<Int>
)

// apkg/ApkgParser.kt - Anki 牌组解析器
class ApkgParser {
    fun parse(apkgFile: File): AnkiDeck
    fun extractMedia(apkgFile: File, outputDir: File): Map<String, File>
}

// apkg/ApkgCreator.kt - Anki 牌组创建器
class ApkgCreator {
    fun create(deck: AnkiDeck, outputFile: File)
    fun addMedia(mediaFile: File): String
}
```

### 数据库处理
```kotlin
// apkg/ApkgDatabaseHandler.kt
class ApkgDatabaseHandler(private val dbFile: File) {
    fun createDatabase(schemaVersion: Int = 11)
    fun insertCards(cards: List<FlashCard>)
    fun insertNotes(notes: List<Note>)
    fun insertDeck(deck: Deck)
    fun close()
}
```

## zstd 压缩集成

### Rust JNI 库
```rust
// rust-zstd-jni/src/lib.rs
use jni::JNIEnv;
use jni::objects::{JClass, JByteArray};
use zstd::stream::{encode_all, decode_all};

#[no_mangle]
pub extern "system" fn Java_com_mujingx_fsrs_zstd_ZstdNative_compress(
    env: JNIEnv,
    _class: JClass,
    input: JByteArray
) -> JByteArray {
    // 压缩实现
}

#[no_mangle]
pub extern "system" fn Java_com_mujingx_fsrs_zstd_ZstdNative_decompress(
    env: JNIEnv,
    _class: JClass,
    input: JByteArray
) -> JByteArray {
    // 解压实现
}
```

### Kotlin 调用
```kotlin
// zstd/ZstdNative.kt
object ZstdNative {
    init {
        System.loadLibrary("rust_zstd_jni")
    }

    external fun compress(data: ByteArray): ByteArray
    external fun decompress(data: ByteArray): ByteArray
}
```

## 测试与质量

### 单元测试
- **目录**：`src/test/kotlin/com/mujingx/fsrs/`
- **测试覆盖**：
  - `FSRSTest.kt` - 基础功能测试
  - `FSRSFunctionalTest.kt` - 功能测试
  - `FSRSBusinessLogicTest.kt` - 业务逻辑测试
  - `FSRSParameterTest.kt` - 参数测试
  - `FSRSPerformanceTest.kt` - 性能测试
  - `FSRSEdgeCaseTest.kt` - 边缘情况测试
  - `FSRSUserExperienceTest.kt` - 用户体验测试
  - `FSRSIntegrationTest.kt` - 集成测试
  - `ModelsTest.kt` - 数据模型测试
  - `FSRSTimeUtilsTest.kt` - 时间工具测试

### 测试命令
```bash
# 运行所有 FSRS 测试
./gradlew test --tests "*fsrs*"

# 运行特定测试
./gradlew test --tests "*FSRSFunctionalTest*"

# 运行性能测试
./gradlew test --tests "*FSRSPerformance*"
```

## 常见问题 (FAQ)

### Q: FSRS 算法与 SM-2 有什么区别？
A: FSRS 基于更现代的记忆科学模型，考虑了记忆稳定性和难度两个维度，比 SM-2 更准确。

### Q: 如何调整复习间隔？
A: 修改 `FSRSParameters` 中的 `requestRetention`（期望留存率）和 `maximumInterval`（最大间隔）。

### Q: Anki 导入支持哪些字段？
A: 支持单词、释义、音标、例句、语境、音频、视频等所有字段。

### Q: zstd 压缩率如何？
A: 通常可以达到 3-5 倍的压缩率，大幅减小 Anki 牌组文件大小。

### Q: 如何处理复习遗漏？
A: `getDueCards()` 会获取所有到期卡片，包括过期卡片，用户可以补复习。

## 相关文件清单

### 核心文件
- `FSRSService.kt` - FSRS 服务
- `FlashCardManager.kt` - 卡片管理器
- `LearningSessionManager.kt` - 学习会话管理
- `fsrs.kt` - FSRS 算法实现
- `Models.kt` - 数据模型
- `FSRSTimeUtils.kt` - 时间工具
- `FSRSUsageExample.kt` - 使用示例

### Anki 集成
- `apkg/ApkgFormat.kt` - Anki 格式定义
- `apkg/ApkgParser.kt` - Anki 牌组解析
- `apkg/ApkgCreator.kt` - Anki 牌组创建
- `apkg/ApkgDatabaseHandler.kt` - 数据库处理
- `apkg/ApkgDatabaseCreator.kt` - 数据库创建
- `apkg/ApkgDatabaseParser.kt` - 数据库解析
- `apkg/ApkgMediaParser.kt` - 媒体解析
- `apkg/ApkgMeta.kt` - 元数据
- `apkg/ApkgExample.kt` - 使用示例
- `apkg/ApkgParserExample.kt` - 解析示例

### zstd 压缩
- `zstd/ZstdNative.kt` - zstd JNI 接口

### 测试文件
- `src/test/kotlin/com/mujingx/fsrs/` - 完整测试套件

### Rust 库
- `rust-zstd-jni/Cargo.toml` - Rust 项目配置
- `rust-zstd-jni/src/lib.rs` - Rust 实现

## 变更记录 (Changelog)

### 2026-04-05 - 创建 fsrs 模块文档
- 📚 初始化模块文档
- 🗂️ 记录 FSRS 算法和 Anki 集成
- 🔧 添加 zstd 压缩和测试策略
- 📊 记录数据模型和接口定义

---

**依赖关系**：
- 依赖 `data` 模块（词库数据）
- 被 `ui` 模块依赖（学习界面）
- 使用 `rust-zstd-jni` 模块（压缩）
