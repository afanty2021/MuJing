# 拆分超大文件实施规划

> 创建日期：2026-04-06
> 完成日期：2026-04-06
> 状态：已完成（阶段 1 + 阶段 2 已执行，阶段 3 验证通过）
> 关联：quality-review-progress.md P1 任务

---

## 背景

幕境项目有两个超大文件严重影响可维护性：

- **WordScreen.kt** (3,287 行) — `com.mujingx.ui.wordscreen`
- **GenerateVocabularyDialog.kt** (3,211 行) — `com.mujingx.ui.dialog`

## 拆分策略：同包拆分 + 状态参数化

- 所有新文件保持相同 `package` 声明，外部 import 路径零变更
- Compose 组件通过参数传递状态和回调，不引入新状态容器类
- 渐进式执行，每步编译验证

---

## 阶段 1：WordScreen.kt 拆分（3,287 → ~230 行）

### 当前组件分布

| 组件 | 行范围 | 行数 | 类型 |
|------|--------|------|------|
| WordScreen | L119-344 | 226 | 主 Composable |
| Header | L345-426 | 82 | 头部 |
| **MainContent** | **L427-1885** | **1,459** | 核心 (22 remember, 21 mutableStateOf) |
| VocabularyEmpty | L1886-2053 | 168 | 空状态 |
| Morphology | L2054-2230 | 177 | 词形展示 |
| Definition | L2231-2291 | 61 | 释义展示 |
| Translation | L2292-2352 | 61 | 翻译展示 |
| Sentences | L2353-2430 | 78 | 例句展示 |
| Captions | L2431-2704 | 274 | 字幕展示 |
| Caption | L2705-2890 | 186 | 单条字幕 |
| buildAnnotatedString (x6) | L2891-3189 | 298 | 富文本构建 |
| Button 组件 (x7) | L2953-3264 | 311 | 操作按钮 |
| 工具函数 (x5) | L2589-2674, L3277-3287 | ~120 | 纯函数 |

### 拆分步骤

#### Step 1.1 — WordScreenUtils.kt (~120 行)

**优先级最高：纯函数，已被外部引用**

提取函数：
- `replaceSeparator(path: String): String` (L2589)
- `getPlayTripleMap(...)` (L2606)
- `getMediaInfo(...)` (L2637)
- `secondsToString(seconds: Double): String` (L2675)
- `getMediaInfoFromExternalCaption(...)` (L3277)

外部引用（需保持包路径 `com.mujingx.ui.wordscreen`）：
- `getMediaInfo` → EditWordDialog, LinkCaptionDialog
- `secondsToString` → EditWordDialog
- `replaceSeparator` → MiniVideoPlayer

#### Step 1.2 — WordActionButtons.kt (~310 行)

提取 Composable：
- `DeleteButton` (L2953)
- `AddButton` (L3008)
- `EditButton` (L3053)
- `HardButton` (L3087)
- `FamiliarButton` (L3139)
- `BookmarkButton` (L3190)
- `CopyButton` (L3216)

#### Step 1.3 — WordCaptionItem.kt (~250 行)

提取 Composable + 函数：
- `Caption()` (L2705)
- 6 个 `buildAnnotatedString()` 重载 (L2891-2952)

#### Step 1.4 — WordCaptions.kt (~275 行)

提取 Composable：
- `Captions()` (L2431)

#### Step 1.5 — WordInfoDisplay.kt (~375 行)

提取 Composable：
- `Morphology()` (L2054)
- `Definition()` (L2231)
- `Translation()` (L2292)
- `Sentences()` (L2353)

#### Step 1.6 — WordVocabularyEmpty.kt (~168 行)

提取 Composable：
- `VocabularyEmpty()` (L1886)

#### Step 1.7 — WordHeader.kt (~82 行)

提取 Composable：
- `Header()` (L345)

#### Step 1.8 (可选) — WordMainContent.kt (~1,460 行)

提取 Composable：
- `MainContent()` (L427)

**注意**：MainContent 有 21 个 mutableStateOf，内部状态复杂。强行拆分需要引入状态容器类。建议先观察 1.1-1.7 的效果再决定。如果执行此步，WordScreen.kt 降至 ~230 行。

**不执行此步时 WordScreen.kt 约 ~570 行（仍为可接受的单一入口文件）。**

---

## 阶段 2：GenerateVocabularyDialog.kt 拆分（3,211 → ~350 行）

### 当前组件分布

| 组件 | 行范围 | 行数 | 类型 |
|------|--------|------|------|
| **GenerateVocabularyDialog** | **L110-1215** | **1,107** | **主 Composable (36 remember, 25 mutableStateOf)** |
| onCloseRequest | L1216-1226 | 11 | 私有函数 |
| getWordLemma | L1227-1234 | 8 | 公共函数 |
| Summary | L1235-1374 | 139 | 摘要展示 |
| computeSummary | L1375-1403 | 29 | 私有函数 |
| loadSummaryVocabulary | L1404-1425 | 22 | 私有函数 |
| BasicFilter | L1426-1792 | 366 | 基础过滤器 |
| VocabularyFilter | L1793-2015 | 222 | 词库过滤器 |
| SelectedList | L2016-2069 | 53 | 已选列表 |
| SelectFile | L2070-2388 | 318 | 文件选择 |
| filterWords | L2389-2549 | 161 | 纯函数 |
| includeWords | L2550-2694 | 145 | 纯函数 |
| filterSelectVocabulary | L2695-2711 | 17 | 纯函数 |
| includeSelectVocabulary | L2712-2735 | 24 | 纯函数 |
| PreviewWords | L2736-3023 | 287 | 预览面板 |
| TaskList | L3024-3175 | 151 | 任务列表 |
| removeItalicSymbol | L3176-3187 | 12 | 纯函数 |
| removeNewLine | L3188-3205 | 18 | 纯函数 |
| replaceNewLine | L3206-3211 | 8 | 纯函数 |

### 拆分步骤

#### Step 2.1 — VocabularyFilterFunctions.kt (~400 行)

**优先级最高：纯函数，部分已被外部引用**

提取函数：
- `filterWords()` (L2389)
- `includeWords()` (L2550)
- `filterSelectVocabulary()` (L2695)
- `includeSelectVocabulary()` (L2712)
- `removeItalicSymbol()` (L3176)
- `removeNewLine()` (L3188)
- `replaceNewLine()` (L3206)

外部引用：
- `removeItalicSymbol` → VideoUtil
- `removeNewLine` → VideoUtil
- `replaceNewLine` → VideoUtil, FFmpegUtil

#### Step 2.2 — VocabularyTaskList.kt (~152 行)

提取 Composable：
- `TaskList()` (L3024)

#### Step 2.3 — VocabularyPreviewWords.kt (~288 行)

提取 Composable：
- `PreviewWords()` (L2736)

#### Step 2.4 — VocabularySelectFile.kt (~319 行)

提取 Composable：
- `SelectFile()` (L2070)

#### Step 2.5 — VocabularySummary.kt (~190 行)

提取 Composable + 辅助函数：
- `Summary()` (L1235)
- `computeSummary()` (L1375)
- `loadSummaryVocabulary()` (L1404)

#### Step 2.6 — VocabularyBasicFilter.kt (~367 行)

提取 Composable：
- `BasicFilter()` (L1426)

#### Step 2.7 — VocabularyFilterPanel.kt (~223 行)

提取 Composable：
- `VocabularyFilter()` (L1793)

#### Step 2.8 — VocabularySelectedList.kt (~54 行)

提取 Composable：
- `SelectedList()` (L2016)

---

## 阶段 3：验证

每步执行后立即验证：
```bash
./gradlew compileKotlin   # 编译通过
./gradlew test            # 测试通过
./gradlew run             # 运行时验证
```

最终验证清单：
- [ ] 所有外部 import 引用保持不变
- [ ] 项目编译成功
- [ ] 现有单元测试全部通过
- [ ] WordScreen.kt 行数 < 600
- [ ] GenerateVocabularyDialog.kt 行数 < 400

---

## 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 状态变量参数爆炸 | 高 | 中 | MainContent 可暂不拆分 |
| 编译错误 | 中 | 低 | 每步编译验证 |
| 运行时状态丢失 | 低 | 高 | Compose 重组机制保证 |
| Git 合并冲突 | 低 | 低 | 按步骤独立提交 |

---

## 执行顺序建议

```
Phase 1: WordScreen.kt
  Step 1.1 (Utils)      → compile → test → commit
  Step 1.2 (Buttons)    → compile → test → commit
  Step 1.3-1.4 (字幕)   → compile → test → commit
  Step 1.5 (单词详情)   → compile → test → commit
  Step 1.6-1.7 (Header+Empty) → compile → test → commit
  Step 1.8 (MainContent, 可选)

Phase 2: GenerateVocabularyDialog.kt
  Step 2.1 (纯函数)     → compile → test → commit
  Step 2.2-2.3 (预览+任务) → compile → test → commit
  Step 2.4-2.5 (文件+摘要) → compile → test → commit
  Step 2.6-2.8 (过滤器)  → compile → test → commit

Phase 3: 最终验证
  完整编译 + 全量测试 + 运行时验证
```

---

*本规划由 Claude Code 生成，基于代码结构分析和依赖关系梳理。*
