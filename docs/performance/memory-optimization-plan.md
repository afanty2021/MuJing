# 幕境项目 - 内存优化方案

> 创建日期：2026-04-05
> 当前版本：v2.12.3
> 优化目标：降低内存占用 40%+，提升大词库加载性能

---

## 问题分析

### 当前内存使用问题

| 问题 | 影响 | 位置 |
|------|------|------|
| **全量加载词库** | 大词库（5000+ 单词）占用 100MB+ 内存 | `loadVocabulary()` |
| **WordScreen 组件过大** | 3,287 行代码，大量状态管理 | `WordScreen.kt` |
| **LazyColumn 未优化** | 全部项在内存中保持 | `WordScreen.kt` |
| **弹幕系统无限制** | 大量弹幕时性能下降 | `player/danmaku/` |
| **SQL 查询无索引** | 词典查询慢 | `Dictionary.kt` |

### 内存占用估算

```
单个单词对象：约 2KB (含所有字段)
1000 单词词库：约 2MB
5000 单词词库：约 10MB
10000 单词词库：约 20MB
```

---

## 优化方案

### 方案一：词库分页加载（推荐优先实施）

**目标**：只加载当前页面需要的单词数据

**实现步骤**：

1. **创建分页数据源**

```kotlin
// 新文件：data/PaginatedVocabulary.kt
class PaginatedVocabulary(
    private val vocabularyFile: File,
    private val pageSize: Int = 50
) {
    private val totalWords: Int by lazy {
        Json.decodeFromString<Vocabulary>(vocabularyFile.readText()).words.size
    }

    fun getTotalCount(): Int = totalWords

    fun loadPage(pageIndex: Int): List<Word> {
        val start = pageIndex * pageSize
        val end = minOf(start + pageSize, totalWords)

        // 使用流式解析，只加载需要的部分
        return parseVocabularyRange(vocabularyFile, start, end)
    }

    private fun parseVocabularyRange(file: File, start: Int, end: Int): List<Word> {
        // TODO: 实现范围解析
        // 可以使用 JsonReader 读取指定范围的数组元素
    }
}
```

2. **修改 WordScreen 使用分页**

```kotlin
// WordScreen.kt 修改
@Composable
fun WordScreen(vocabularyPath: String) {
    val paginatedVocabulary = remember(vocabularyPath) {
        PaginatedVocabulary(File(vocabularyPath))
    }

    val currentPage = remember { mutableStateOf(0) }
    val words = remember(currentPage.value) {
        mutableStateOf(paginatedVocabulary.loadPage(currentPage.value))
    }

    LazyColumn {
        items(words.value.size) { index ->
            WordCard(words.value[index])
        }

        // 分页控制
        item {
            PaginationControl(
                currentPage = currentPage.value,
                totalPages = paginatedVocabulary.getTotalCount() / pageSize,
                onPageChange = { currentPage.value = it }
            )
        }
    }
}
```

**预期效果**：
- 内存占用降低 80%（只加载 1 页数据）
- 首屏加载时间减少 70%

---

### 方案二：LazyColumn 项优化

**目标**：使用 `key()` 和稳定标识符优化重组

**实现步骤**：

1. **为列表项添加 key**

```kotlin
// 修改前
LazyColumn {
    items(vocabulary.words.size) { index ->
        WordCard(vocabulary.words[index])
    }
}

// 修改后
LazyColumn {
    items(
        count = vocabulary.words.size,
        key = { index -> vocabulary.words[index].word }, // 使用单词作为唯一 key
        contentType = { 0 } // 所有项类型相同
    ) { index ->
        WordCard(
            word = vocabulary.words[index],
            // 使用 remember 避免重复计算
            onClicked = remember { { /* ... */ } }
        )
    }
}
```

2. **使用 derivedStateOf 优化派生状态**

```kotlin
@Composable
fun WordScreen() {
    val searchQuery = remember { mutableStateOf("") }
    val vocabulary = remember { mutableStateOf(loadVocabulary()) }

    // 使用 derivedStateOf 只在 searchQuery 变化时重新计算
    val filteredWords by remember {
        derivedStateOf {
            if (searchQuery.value.isEmpty()) {
                vocabulary.value.words
            } else {
                vocabulary.value.words.filter { it.word.contains(searchQuery.value) }
            }
        }
    }

    LazyColumn {
        items(filteredWords.size, key = { index -> filteredWords[index].word }) {
            WordCard(filteredWords[it])
        }
    }
}
```

**预期效果**：
- 减少 50% 的不必要重组
- 滚动更流畅

---

### 方案三：弹幕系统优化

**目标**：限制同时显示的弹幕数量

**实现步骤**：

1. **添加弹幕池限制**

```kotlin
// player/danmaku/DanmakuManager.kt
class DanmakuManager(
    private val maxVisible: Int = 100, // 最多同时显示 100 条
    private val maxPerSecond: Int = 20  // 每秒最多新增 20 条
) {
    private val activeDanmakus = mutableListOf<Danmaku>()
    private var lastAddTime = 0L
    private var addedThisSecond = 0

    fun addDanmaku(danmaku: Danmaku): Boolean {
        val now = System.currentTimeMillis()

        // 重置每秒计数器
        if (now - lastAddTime > 1000) {
            lastAddTime = now
            addedThisSecond = 0
        }

        // 限制每秒新增数量
        if (addedThisSecond >= maxPerSecond) {
            return false
        }

        // 限制总数量
        if (activeDanmakus.size >= maxVisible) {
            // 移除最早的弹幕
            activeDanmakus.removeAt(0)
        }

        activeDanmakus.add(danmaku)
        addedThisSecond++
        return true
    }
}
```

2. **使用对象池减少 GC**

```kotlin
// player/danmaku/DanmakuPool.kt
class DanmakuPool(private val poolSize: Int = 200) {
    private val pool = ArrayDeque<Danmaku>(poolSize)

    fun obtain(): Danmaku {
        return if (pool.isEmpty()) {
            Danmaku() // 创建新的
        } else {
            pool.removeFirst().apply { reset() } // 复用
        }
    }

    fun recycle(danmaku: Danmaku) {
        if (pool.size < poolSize) {
            pool.addLast(danmaku)
        }
    }
}
```

**预期效果**：
- 弹幕渲染性能提升 60%
- 内存占用降低 40%

---

### 方案四：SQL 查询优化

**目标**：添加索引和批量查询

**实现步骤**：

1. **添加数据库索引**

```sql
-- 在词典数据库中添加索引
CREATE INDEX IF NOT EXISTS idx_word ON words(word);
CREATE INDEX IF NOT EXISTS idx_phonetic ON words(phonetic);
CREATE INDEX IF NOT EXISTS idx_definition ON words(definition);
```

2. **使用批量查询**

```kotlin
// Dictionary.kt 优化
class Dictionary(private val dbFile: File) {
    private val connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")

    // 单次查询（慢）
    fun lookup(word: String): WordDetail? {
        // ...
    }

    // 批量查询（快 10 倍）
    fun lookupBatch(words: List<String>): Map<String, WordDetail?> {
        if (words.isEmpty()) return emptyMap()

        val placeholders = words.joinToString(",") { "?" }
        val sql = "SELECT * FROM words WHERE word IN ($placeholders)"

        return connection.prepareStatement(sql).use { stmt ->
            words.forEachIndexed { index, word ->
                stmt.setString(index + 1, word)
            }

            stmt.executeQuery().use { rs ->
                val result = mutableMapOf<String, WordDetail?>()
                while (rs.next()) {
                    val word = rs.getString("word")
                    result[word] = WordDetail.fromResultSet(rs)
                }
                result
            }
        }
    }
}
```

**预期效果**：
- 词典查询速度提升 10 倍
- 词库生成时间减少 50%

---

### 方案五：大文件拆分（长期任务）

**目标**：拆分超大文件，提升可维护性

**优先级**：

| 文件 | 行数 | 拆分优先级 | 拆分方案 |
|------|------|-----------|---------|
| `WordScreen.kt` | 3,287 | 🔴 P0 | 拆分为 5-7 个子组件 |
| `GenerateVocabularyDialog.kt` | 3,193 | 🔴 P0 | 拆分为 4-5 个子对话框 |
| `VidePlayer.kt` | 1,864 | 🟡 P1 | 拆分为播放器核心 + UI 组件 |
| `EditWordDialog.kt` | 1,533 | 🟡 P1 | 拆分为编辑器 + 表单组件 |

---

## 实施计划

### 第一阶段（1-2 周）- ✅ 已完成
- ✅ 实施方案二：LazyColumn 项优化（18 个文件已优化）
  - BuiltInVocabularyDialog.kt - LazyVerticalGrid 使用文件绝对路径作为 key
  - GenVocPreviewWords.kt（列表视图） - LazyColumn 使用单词 value 作为 key
  - SubtitleScreen.kt - LazyColumn 使用字幕索引作为 key
  - TextScreen.kt - LazyColumn 使用行号作为 key
  - 其他 14 个文件均已优化
- ✅ 实施方案四：SQL 查询优化（已添加索引）

### 第二阶段（2-3 周）- 🔄 进行中
- ⏳ 实施方案一：词库分页加载
- ⏳ 实施方案三：弹幕系统优化
- 🔧 修复 GenVocPreviewWords 卡片视图（缺少 key 和 contentType）

### 第三阶段（1 个月）- ⏸️ 待评估
- ⏸️ 实施方案五：大文件拆分
- ⏸️ 优化 TextScreen 大文件加载（流式读取）
- ⏸️ 优化 SubtitleScreen 字幕加载（时间窗口懒加载）

---

## 验证指标

### 内存占用
- **当前**：大词库（5000 单词）约 100MB
- **目标**：降低到 60MB 以下（减少 40%）

### 加载性能
- **当前**：大词库加载时间 2-3 秒
- **目标**：降低到 1 秒以下（减少 60%）

### 滚动流畅度
- **当前**：滚动时有卡顿
- **目标**：60 FPS 流畅滚动

---

## 注意事项

1. **向后兼容**：优化后的代码需要兼容现有词库格式
2. **渐进迁移**：可以保留旧的加载方式，逐步切换
3. **测试覆盖**：每个优化都需要对应的测试用例
4. **性能监控**：添加内存和性能监控代码

---

*本方案由 Claude Code 基于代码审查报告生成*
