# 幕境功能增强实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为幕境引入 DI 框架、AI 翻译、国际化架构、片段管理、字幕同步优化共 5 项功能增强。

**Architecture:** 采用渐进式分层引入策略。Phase 1 先用 Koin 建立 Service 层依赖注入骨架；Phase 2 在此基础上实现多 Provider AI 翻译；Phase 3 完成 i18n 架构和片段数据层；Phase 4 补全片段 UI 和字幕同步功能。

**Tech Stack:** Kotlin 2.2.21, Compose Desktop 1.9.3, Koin 4.x, Ktor 2.3.x, SQLite (JDBC), kotlinx-serialization, FFmpeg

**Spec:** `docs/superpowers/specs/2026-04-11-mujing-feature-enhancement-design.md`

---

## 文件结构总览

### 新增文件

```
src/main/kotlin/com/mujingx/
├── di/
│   ├── AppModule.kt                    # Koin 核心模块定义
│   ├── TranslationModule.kt            # 翻译服务模块
│   ├── ClipModule.kt                   # 片段管理模块
│   └── SubtitleModule.kt               # 字幕同步模块
├── translation/
│   ├── TranslationService.kt           # 翻译服务接口
│   ├── TranslationProvider.kt          # Provider 接口
│   ├── TranslationCacheRepository.kt   # 翻译缓存仓库
│   ├── MultiProviderTranslationService.kt  # 多 Provider 路由实现
│   └── provider/
│       ├── OpenAITranslationProvider.kt
│       ├── YoudaoTranslationProvider.kt
│       └── AzureTranslationProvider.kt
├── i18n/
│   └── I18n.kt                         # i18n 运行时核心
├── subtitle/
│   ├── SubtitleSyncService.kt          # 字幕同步接口
│   └── SubtitleSyncServiceImpl.kt      # 字幕同步实现
├── data/
│   ├── Clip.kt                         # 片段数据模型
│   ├── ClipCollection.kt               # 片段集合模型
│   ├── ClipRepository.kt               # 片段数据仓库
│   ├── ClipService.kt                  # 片段业务服务
│   └── db/
│       ├── TranslationCacheTable.kt    # 翻译缓存表
│       ├── ClipTables.kt               # 片段相关表
│       └── SubtitleSyncTables.kt       # 字幕同步表
└── ui/
    └── components/
        ├── TranslationPopup.kt         # 即时翻译浮层
        └── ClipPanel.kt                # 片段管理面板

src/main/resources/
└── i18n/
    ├── zh-CN.json                      # 中文语言包
    └── en.json                         # 英文占位语言包

src/test/kotlin/com/mujingx/
├── translation/
│   ├── TranslationServiceTest.kt
│   └── TranslationCacheRepositoryTest.kt
├── subtitle/
│   └── SubtitleSyncServiceTest.kt
└── data/
    └── ClipRepositoryTest.kt
```

### 修改文件

```
build.gradle.kts                        # 添加 Koin + 新依赖
src/main/kotlin/com/mujingx/Main.kt     # Koin 初始化
src/main/kotlin/com/mujingx/state/AppState.kt  # 注入服务
src/main/kotlin/com/mujingx/ui/dialog/SettingsDialog.kt  # 翻译配置入口
src/main/kotlin/com/mujingx/ui/subtitlescreen/  # 翻译按钮+收藏入口+同步控制
src/main/kotlin/com/mujingx/ui/wordscreen/      # 即时翻译
src/main/kotlin/com/mujingx/ui/util/            # 词库翻译集成
src/main/kotlin/com/mujingx/player/             # 片段标记快捷键
src/main/kotlin/com/mujingx/event/              # 新快捷键注册
~40 个含硬编码中文字符串的文件             # i18n 字符串替换
```

---

## Phase 1: Koin DI 基础设施（P0-1）

### Task 1: 添加 Koin 依赖

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: 在 build.gradle.kts 的 dependencies 块中添加 Koin 依赖**

在 `dependencies { ... }` 块中，`testImplementation` 行之前添加：

```kotlin
    // Dependency Injection
    implementation("io.insert-koin:koin-core:4.0.4")
```

- [ ] **Step 2: 验证 Gradle 同步成功**

Run: `cd /Users/berton/Github/mujing && ./gradlew dependencies --configuration runtimeClasspath 2>&1 | grep koin`
Expected: 输出包含 `io.insert-koin:koin-core`

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: 添加 Koin DI 框架依赖"
```

---

### Task 2: 创建 Koin AppModule

**Files:**
- Create: `src/main/kotlin/com/mujingx/di/AppModule.kt`

- [ ] **Step 1: 创建 DI 目录和 AppModule**

```kotlin
package com.mujingx.di

import com.mujingx.data.Dictionary
import com.mujingx.fsrs.FSRSService
import org.koin.dsl.module

val appModule = module {
    single { FSRSService() }
    single { Dictionary() }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/mujingx/di/AppModule.kt
git commit -m "feat(di): 创建 Koin AppModule 基础模块"
```

---

### Task 3: 在 Main.kt 初始化 Koin

**Files:**
- Modify: `src/main/kotlin/com/mujingx/Main.kt`

- [ ] **Step 1: 添加 Koin 初始化代码**

在 `main()` 函数的 `init()` 调用之前添加 Koin 启动：

```kotlin
package com.mujingx

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.window.application
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import io.github.vinceglb.filekit.FileKit
import kotlinx.serialization.ExperimentalSerializationApi
import com.mujingx.theme.isSystemDarkMode
import com.mujingx.ui.App
import com.mujingx.di.appModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalSerializationApi::class)
@ExperimentalFoundationApi
@ExperimentalAnimationApi
fun main() = application {
    startKoin {
        modules(appModule)
    }
    init()
    App()
}

fun init() {
    FileKit.init(appId = "幕境")
    if (isSystemDarkMode()) {
        FlatDarkLaf.setup()
    } else {
        FlatLightLaf.setup()
    }
}
```

- [ ] **Step 2: 编译并运行验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/mujingx/Main.kt
git commit -m "feat(di): 在 Main.kt 初始化 Koin 容器"
```

---

### Task 4: 创建 TranslationModule 占位

**Files:**
- Create: `src/main/kotlin/com/mujingx/di/TranslationModule.kt`
- Create: `src/main/kotlin/com/mujingx/di/ClipModule.kt`
- Create: `src/main/kotlin/com/mujingx/di/SubtitleModule.kt`

- [ ] **Step 1: 创建 TranslationModule**

```kotlin
package com.mujingx.di

import org.koin.dsl.module

val translationModule = module {
    // Phase 2 填充 TranslationService 注册
}
```

- [ ] **Step 2: 创建 ClipModule**

```kotlin
package com.mujingx.di

import org.koin.dsl.module

val clipModule = module {
    // Phase 3 填充 ClipService/ClipRepository 注册
}
```

- [ ] **Step 3: 创建 SubtitleModule**

```kotlin
package com.mujingx.di

import org.koin.dsl.module

val subtitleModule = module {
    // Phase 4 填充 SubtitleSyncService 注册
}
```

- [ ] **Step 4: 更新 Main.kt 注册所有模块**

修改 `startKoin` 块：

```kotlin
    startKoin {
        modules(
            appModule,
            translationModule,
            clipModule,
            subtitleModule
        )
    }
```

- [ ] **Step 5: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/mujingx/di/
git commit -m "feat(di): 创建 TranslationModule、ClipModule、SubtitleModule 占位"
```

---

## Phase 2: AI 翻译功能（P0-2）

### Task 5: 定义翻译服务接口

**Files:**
- Create: `src/main/kotlin/com/mujingx/translation/TranslationService.kt`
- Create: `src/main/kotlin/com/mujingx/translation/TranslationProvider.kt`
- Create: `src/test/kotlin/com/mujingx/translation/TranslationServiceTest.kt`

- [ ] **Step 1: 创建 TranslationService 接口**

```kotlin
package com.mujingx.translation

/**
 * 翻译服务顶层接口
 * 支持单条和批量翻译，返回 Kotlin Result 包装
 */
interface TranslationService {
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String>
    suspend fun translateBatch(texts: List<String>, sourceLang: String, targetLang: String): Result<List<String>>
}
```

- [ ] **Step 2: 创建 TranslationProvider 接口**

```kotlin
package com.mujingx.translation

/**
 * 单个翻译 Provider 的接口
 * 每个具体翻译 API 实现此接口
 */
interface TranslationProvider {
    val name: String
    suspend fun translate(text: String, from: String, to: String): String
    fun isConfigured(): Boolean
}
```

- [ ] **Step 3: 编写测试 — MultiProviderTranslationService 基本行为**

```kotlin
package com.mujingx.translation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TranslationServiceTest {

    /** 用于测试的 Mock Provider */
    class MockProvider(
        override val name: String,
        private val result: String,
        override val isConfigured: Boolean = true
    ) : TranslationProvider {
        override suspend fun translate(text: String, from: String, to: String): String = result
        override fun isConfigured(): Boolean = isConfigured
    }

    @Test
    fun `translate should return first configured provider result`() = runTest {
        val service = MultiProviderTranslationService(
            providers = listOf(
                MockProvider("mock-a", "你好"),
                MockProvider("mock-b", "您好")
            )
        )
        val result = service.translate("hello", "en", "zh-CN")
        assertTrue(result.isSuccess)
        assertEquals("你好", result.getOrThrow())
    }

    @Test
    fun `translate should fallback when first provider fails`() = runTest {
        val service = MultiProviderTranslationService(
            providers = listOf(
                object : TranslationProvider {
                    override val name = "failing"
                    override suspend fun translate(text: String, from: String, to: String): String {
                        throw RuntimeException("API error")
                    }
                    override fun isConfigured() = true
                },
                MockProvider("fallback", "备用结果")
            )
        )
        val result = service.translate("hello", "en", "zh-CN")
        assertTrue(result.isSuccess)
        assertEquals("备用结果", result.getOrThrow())
    }

    @Test
    fun `translateBatch should translate all texts`() = runTest {
        val service = MultiProviderTranslationService(
            providers = listOf(MockProvider("mock", "翻译结果"))
        )
        val result = service.translateBatch(listOf("hello", "world"), "en", "zh-CN")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
    }
}
```

- [ ] **Step 4: 运行测试确认失败**

Run: `cd /Users/berton/Github/mujing && ./gradlew test --tests "*TranslationServiceTest*" 2>&1 | tail -10`
Expected: FAIL — `MultiProviderTranslationService` 未定义

- [ ] **Step 5: Commit 测试文件**

```bash
git add src/main/kotlin/com/mujingx/translation/TranslationService.kt \
       src/main/kotlin/com/mujingx/translation/TranslationProvider.kt \
       src/test/kotlin/com/mujingx/translation/TranslationServiceTest.kt
git commit -m "test(translation): 添加翻译服务接口和测试用例"
```

---

### Task 6: 实现 MultiProviderTranslationService

**Files:**
- Create: `src/main/kotlin/com/mujingx/translation/MultiProviderTranslationService.kt`

- [ ] **Step 1: 实现 MultiProviderTranslationService**

```kotlin
package com.mujingx.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 多 Provider 翻译服务实现
 * 支持自动 fallback：主 Provider 失败时尝试备用 Provider
 */
class MultiProviderTranslationService(
    private val providers: List<TranslationProvider>,
    private val cache: TranslationCacheRepository? = null
) : TranslationService {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // 先查缓存
        cache?.get(text, sourceLang, targetLang)?.let {
            return@withContext Result.success(it)
        }

        // 按顺序尝试已配置的 Provider
        val configured = providers.filter { it.isConfigured() }
        if (configured.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("没有已配置的翻译 Provider，请在设置中配置 API Key")
            )
        }

        var lastError: Throwable? = null
        for (provider in configured) {
            try {
                val result = provider.translate(text, sourceLang, targetLang)
                cache?.put(text, sourceLang, targetLang, result)
                return@withContext Result.success(result)
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }
        Result.failure(lastError ?: RuntimeException("翻译失败"))
    }

    override suspend fun translateBatch(
        texts: List<String>,
        sourceLang: String,
        targetLang: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val results = texts.map { text ->
            val cached = cache?.get(text, sourceLang, targetLang)
            if (cached != null) {
                cached
            } else {
                val result = translate(text, sourceLang, targetLang)
                result.getOrThrow()
            }
        }
        Result.success(results)
    }
}
```

- [ ] **Step 2: 运行测试确认通过**

Run: `cd /Users/berton/Github/mujing && ./gradlew test --tests "*TranslationServiceTest*" 2>&1 | tail -10`
Expected: 3 tests PASSED

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/mujingx/translation/MultiProviderTranslationService.kt
git commit -m "feat(translation): 实现 MultiProviderTranslationService 多 Provider 路由"
```

---

### Task 7: 实现翻译缓存层

**Files:**
- Create: `src/main/kotlin/com/mujingx/translation/TranslationCacheRepository.kt`
- Create: `src/main/kotlin/com/mujingx/data/db/TranslationCacheTable.kt`
- Create: `src/test/kotlin/com/mujingx/translation/TranslationCacheRepositoryTest.kt`

- [ ] **Step 1: 创建 TranslationCacheTable**

```kotlin
package com.mujingx.data.db

import java.sql.Connection
import java.sql.DriverManager

/**
 * 翻译缓存表管理
 * 负责创建表和提供 CRUD 操作
 */
object TranslationCacheTable {

    private const val CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS translation_cache (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source_text TEXT NOT NULL,
            source_lang TEXT NOT NULL,
            target_lang TEXT NOT NULL,
            translated_text TEXT NOT NULL,
            provider TEXT NOT NULL,
            content_hash TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            UNIQUE(source_text, source_lang, target_lang)
        )
    """

    fun createTable(connection: Connection) {
        connection.createStatement().execute(CREATE_TABLE_SQL.trimIndent())
    }
}
```

- [ ] **Step 2: 实现 TranslationCacheRepository**

```kotlin
package com.mujingx.translation

import com.mujingx.data.db.TranslationCacheTable
import java.sql.Connection
import java.sql.DriverManager
import java.util.HexFormat

/**
 * 翻译缓存仓库
 * 使用 SQLite 存储翻译结果，避免重复 API 调用
 */
class TranslationCacheRepository(private val dbPath: String) {

    private val connection: Connection by lazy {
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        TranslationCacheTable.createTable(conn)
        conn
    }

    fun get(text: String, from: String, to: String): String? {
        val sql = "SELECT translated_text FROM translation_cache WHERE source_text = ? AND source_lang = ? AND target_lang = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, text)
            stmt.setString(2, from)
            stmt.setString(3, to)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getString("translated_text") else null
        }
    }

    fun put(text: String, from: String, to: String, result: String, provider: String = "unknown") {
        val sql = """
            INSERT OR REPLACE INTO translation_cache (source_text, source_lang, target_lang, translated_text, provider, content_hash, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, text)
            stmt.setString(2, from)
            stmt.setString(3, to)
            stmt.setString(4, result)
            stmt.setString(5, provider)
            stmt.setString(6, text.hashCode().toString())
            stmt.setLong(7, System.currentTimeMillis())
            stmt.executeUpdate()
        }
    }

    fun close() {
        try { connection.close() } catch (_: Exception) {}
    }
}
```

- [ ] **Step 3: 编写缓存测试**

```kotlin
package com.mujingx.translation

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

class TranslationCacheRepositoryTest {

    private lateinit var repo: TranslationCacheRepository
    private val testDbPath = System.getProperty("java.io.tmpdir") + "/mujing_test_cache_${System.currentTimeMillis()}.db"

    @BeforeEach
    fun setup() {
        repo = TranslationCacheRepository(testDbPath)
    }

    @AfterEach
    fun cleanup() {
        repo.close()
        File(testDbPath).delete()
    }

    @Test
    fun `get returns null for non-cached text`() {
        assertNull(repo.get("hello", "en", "zh-CN"))
    }

    @Test
    fun `put and get returns cached translation`() {
        repo.put("hello", "en", "zh-CN", "你好")
        assertEquals("你好", repo.get("hello", "en", "zh-CN"))
    }

    @Test
    fun `put overwrites existing cache`() {
        repo.put("hello", "en", "zh-CN", "你好")
        repo.put("hello", "en", "zh-CN", "您好")
        assertEquals("您好", repo.get("hello", "en", "zh-CN"))
    }

    @Test
    fun `different language pairs are cached separately`() {
        repo.put("hello", "en", "zh-CN", "你好")
        repo.put("hello", "en", "ja", "こんにちは")
        assertEquals("你好", repo.get("hello", "en", "zh-CN"))
        assertEquals("こんにちは", repo.get("hello", "en", "ja"))
    }
}
```

- [ ] **Step 4: 运行测试**

Run: `cd /Users/berton/Github/mujing && ./gradlew test --tests "*TranslationCacheRepositoryTest*" 2>&1 | tail -10`
Expected: 4 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/mujingx/translation/TranslationCacheRepository.kt \
       src/main/kotlin/com/mujingx/data/db/TranslationCacheTable.kt \
       src/test/kotlin/com/mujingx/translation/TranslationCacheRepositoryTest.kt
git commit -m "feat(translation): 实现翻译缓存层和 SQLite 存储"
```

---

### Task 8: 实现 OpenAI Provider

**Files:**
- Create: `src/main/kotlin/com/mujingx/translation/provider/OpenAITranslationProvider.kt`

- [ ] **Step 1: 实现 OpenAI 翻译 Provider**

```kotlin
package com.mujingx.translation.provider

import com.mujingx.translation.TranslationProvider
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * OpenAI 翻译 Provider
 * 使用 GPT-4o-mini 进行翻译
 */
class OpenAITranslationProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val model: String = "gpt-4o-mini"
) : TranslationProvider {

    override val name = "OpenAI"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    }

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun translate(text: String, from: String, to: String): String {
        val response = client.post("$baseUrl/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(buildJsonObject {
                put("model", model)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", "You are a professional translator. Translate the following text from $from to $to. Return only the translation, nothing else.")
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", text)
                    })
                })
                put("temperature", 0.3)
            })
        }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("OpenAI API error: ${response.status.value}")
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return json["choices"]?.jsonArray?.get(0)?.jsonObject
            ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw RuntimeException("Unexpected OpenAI response format")
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/mujingx/translation/provider/OpenAITranslationProvider.kt
git commit -m "feat(translation): 实现 OpenAI 翻译 Provider"
```

---

### Task 9: 实现有道和 Azure Provider

**Files:**
- Create: `src/main/kotlin/com/mujingx/translation/provider/YoudaoTranslationProvider.kt`
- Create: `src/main/kotlin/com/mujingx/translation/provider/AzureTranslationProvider.kt`

- [ ] **Step 1: 实现有道翻译 Provider**

```kotlin
package com.mujingx.translation.provider

import com.mujingx.translation.TranslationProvider
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.security.MessageDigest

/**
 * 有道翻译 Provider
 * 使用有道智云翻译 API
 */
class YoudaoTranslationProvider(
    private val appKey: String,
    private val appSecret: String,
    private val baseUrl: String = "https://openapi.youdao.com/api"
) : TranslationProvider {

    override val name = "Youdao"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    }

    override fun isConfigured(): Boolean = appKey.isNotBlank() && appSecret.isNotBlank()

    override suspend fun translate(text: String, from: String, to: String): String {
        val salt = System.currentTimeMillis().toString()
        val curtime = (System.currentTimeMillis() / 1000).toString()
        val sign = sha256("$appKey${truncate(text)}$salt$curtime$appSecret")

        val response = client.post(baseUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            parameter("q", text)
            parameter("from", from)
            parameter("to", to)
            parameter("appKey", appKey)
            parameter("salt", salt)
            parameter("curtime", curtime)
            parameter("sign", sign)
            parameter("signType", "v3")
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val errorCode = json["errorCode"]?.jsonPrimitive?.content
        if (errorCode != "0") {
            throw RuntimeException("有道翻译 API 错误: $errorCode")
        }

        return json["translation"]?.jsonArray?.get(0)?.jsonPrimitive?.content
            ?: throw RuntimeException("Unexpected Youdao response format")
    }

    private fun truncate(text: String): String =
        if (text.length <= 20) text
        else text.take(10) + text.length.toString() + text.takeLast(10)

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 2: 实现 Azure 翻译 Provider**

```kotlin
package com.mujingx.translation.provider

import com.mujingx.translation.TranslationProvider
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/**
 * Azure 翻译 Provider
 * 使用 Azure Cognitive Services Translator API
 */
class AzureTranslationProvider(
    private val subscriptionKey: String,
    private val region: String = "global",
    private val baseUrl: String = "https://api.cognitive.microsofttranslator.com"
) : TranslationProvider {

    override val name = "Azure"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = 30_000 }
    }

    override fun isConfigured(): Boolean = subscriptionKey.isNotBlank()

    override suspend fun translate(text: String, from: String, to: String): String {
        val response = client.post("$baseUrl/translate?api-version=3.0&from=$from&to=$to") {
            contentType(ContentType.Application.Json)
            header("Ocp-Apim-Subscription-Key", subscriptionKey)
            header("Ocp-Apim-Subscription-Region", region)
            setBody("""[{"text":"$text"}]""")
        }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("Azure Translator API error: ${response.status.value}")
        }

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonArray
        return json[0].jsonObject["translations"]?.jsonArray?.get(0)?.jsonObject
            ?.get("text")?.jsonPrimitive?.content
            ?: throw RuntimeException("Unexpected Azure response format")
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/mujingx/translation/provider/YoudaoTranslationProvider.kt \
       src/main/kotlin/com/mujingx/translation/provider/AzureTranslationProvider.kt
git commit -m "feat(translation): 实现有道和 Azure 翻译 Provider"
```

---

### Task 10: 注册 TranslationService 到 Koin

**Files:**
- Modify: `src/main/kotlin/com/mujingx/di/TranslationModule.kt`

- [ ] **Step 1: 完善 TranslationModule**

```kotlin
package com.mujingx.di

import com.mujingx.translation.*
import com.mujingx.translation.provider.*
import org.koin.dsl.module

val translationModule = module {
    single { TranslationCacheRepository(getDbPath("translation_cache.db")) }
    single<TranslationService> {
        MultiProviderTranslationService(
            providers = listOf(
                OpenAITranslationProvider(getSettings("openai_api_key")),
                YoudaoTranslationProvider(
                    getSettings("youdao_app_key"),
                    getSettings("youdao_app_secret")
                ),
                AzureTranslationProvider(
                    getSettings("azure_translation_key"),
                    getSettings("azure_translation_region")
                )
            ),
            cache = get()
        )
    }
}

/** 获取数据库文件路径 */
private fun getDbPath(dbName: String): String =
    System.getProperty("user.home") + "/.MuJing/$dbName"

/** 从 GlobalState 获取设置值，暂时返回空字符串占位 */
private fun getSettings(key: String): String = ""
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/mujingx/di/TranslationModule.kt
git commit -m "feat(di): 注册 TranslationService 到 Koin 容器"
```

---

## Phase 3: i18n 架构 + 片段数据层（P1-1 + P1-2 数据层）

### Task 11: 创建 I18n 运行时核心

**Files:**
- Create: `src/main/kotlin/com/mujingx/i18n/I18n.kt`
- Create: `src/main/resources/i18n/zh-CN.json`
- Create: `src/main/resources/i18n/en.json`

- [ ] **Step 1: 创建 I18n 核心类**

```kotlin
package com.mujingx.i18n

import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * 国际化运行时核心
 * 从 JSON 资源文件加载翻译字符串
 */
object I18n {
    private var bundles: Map<String, String> = emptyMap()
    private var currentLang: String = "zh-CN"

    private val json = Json { ignoreUnknownKeys = true }

    fun init(lang: String = "zh-CN") {
        currentLang = lang
        bundles = loadBundle(lang)
    }

    fun t(key: String): String = bundles[key] ?: key

    fun t(key: String, vararg args: Any): String =
        bundles[key]?.format(*args) ?: key

    fun currentLanguage(): String = currentLang

    private fun loadBundle(lang: String): Map<String, String> {
        val resource: InputStream? = javaClass.classLoader
            ?.getResourceAsStream("i18n/$lang.json")
        if (resource == null) {
            println("Warning: Language bundle '$lang' not found, using keys as fallback")
            return emptyMap()
        }
        val text = resource.bufferedReader().use { it.readText() }
        val jsonObject = json.parseToJsonElement(text).jsonObject
        return jsonObject.mapValues { it.value.jsonPrimitive.content }
    }
}

val LocalI18n = compositionLocalOf { I18n }
```

- [ ] **Step 2: 创建 zh-CN.json 模板**

```json
{
  "common.confirm": "确认",
  "common.cancel": "取消",
  "common.save": "保存",
  "common.delete": "删除",
  "common.edit": "编辑",
  "common.close": "关闭",
  "common.loading": "加载中...",
  "common.error": "错误",
  "common.success": "成功",
  "common.retry": "重试",

  "word.action.delete": "删除单词",
  "word.action.add": "添加单词",
  "word.action.edit": "编辑",
  "word.action.bookmark": "收藏",
  "word.action.hard_vocabulary": "困难词库",
  "word.action.familiar_vocabulary": "熟悉词库",

  "subtitle.translate": "翻译字幕",
  "subtitle.bookmark": "收藏片段",
  "subtitle.sync": "同步调整",
  "subtitle.clip.save": "保存片段",

  "player.play": "播放",
  "player.pause": "暂停",
  "player.previous": "上一个",
  "player.next": "下一个",

  "settings.title": "设置",
  "settings.translation": "翻译设置",
  "settings.translation.provider": "翻译服务商",
  "settings.translation.api_key": "API Key",
  "settings.translation.test": "测试连接",
  "settings.language": "语言",

  "vocabulary.generate": "生成词库",
  "vocabulary.link": "链接词库",

  "error.file_not_found": "文件未找到",
  "error.parse_failed": "解析失败",
  "error.network_error": "网络错误",
  "error.translation_failed": "翻译失败",
  "error.vocabulary_parse_error": "词库解析错误",
  "error.dictionary_not_found": "找不到词典",

  "translation.provider.openai": "OpenAI",
  "translation.provider.youdao": "有道翻译",
  "translation.provider.azure": "Azure 翻译",
  "translation.instant.title": "即时翻译",
  "translation.subtitle.batch": "批量翻译字幕",

  "clip.save": "保存片段",
  "clip.tag": "标签",
  "clip.tag.new": "新标签",
  "clip.extract": "截取视频",
  "clip.note": "笔记",
  "clip.collection": "片段集",
  "clip.filter": "筛选",
  "clip.review": "复习"
}
```

- [ ] **Step 3: 创建 en.json 占位**

```json
{
  "common.confirm": "",
  "common.cancel": "",
  "common.save": "",
  "common.delete": "",
  "common.edit": "",
  "common.close": "",
  "common.loading": "",
  "common.error": "",
  "common.success": "",
  "common.retry": "",

  "word.action.delete": "",
  "word.action.add": "",
  "word.action.edit": "",
  "word.action.bookmark": "",
  "word.action.hard_vocabulary": "",
  "word.action.familiar_vocabulary": "",

  "subtitle.translate": "",
  "subtitle.bookmark": "",
  "subtitle.sync": "",
  "subtitle.clip.save": "",

  "player.play": "",
  "player.pause": "",
  "player.previous": "",
  "player.next": "",

  "settings.title": "",
  "settings.translation": "",
  "settings.translation.provider": "",
  "settings.translation.api_key": "",
  "settings.translation.test": "",
  "settings.language": "",

  "vocabulary.generate": "",
  "vocabulary.link": "",

  "error.file_not_found": "",
  "error.parse_failed": "",
  "error.network_error": "",
  "error.translation_failed": "",
  "error.vocabulary_parse_error": "",
  "error.dictionary_not_found": "",

  "translation.provider.openai": "",
  "translation.provider.youdao": "",
  "translation.provider.azure": "",
  "translation.instant.title": "",
  "translation.subtitle.batch": "",

  "clip.save": "",
  "clip.tag": "",
  "clip.tag.new": "",
  "clip.extract": "",
  "clip.note": "",
  "clip.collection": "",
  "clip.filter": "",
  "clip.review": ""
}
```

- [ ] **Step 4: 在 Main.kt 中初始化 I18n**

在 `init()` 函数开头添加 `I18n.init()`：

```kotlin
import com.mujingx.i18n.I18n

fun init() {
    I18n.init()
    FileKit.init(appId = "幕境")
    // ... 其余不变
}
```

- [ ] **Step 5: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/mujingx/i18n/ \
       src/main/resources/i18n/ \
       src/main/kotlin/com/mujingx/Main.kt
git commit -m "feat(i18n): 创建 I18n 运行时核心和语言资源文件"
```

---

### Task 12: 创建片段数据模型

**Files:**
- Create: `src/main/kotlin/com/mujingx/data/Clip.kt`
- Create: `src/main/kotlin/com/mujingx/data/ClipCollection.kt`
- Create: `src/main/kotlin/com/mujingx/data/db/ClipTables.kt`

- [ ] **Step 1: 创建 Clip 数据模型**

```kotlin
package com.mujingx.data

import kotlinx.serialization.Serializable

/**
 * 视频片段数据模型
 * 代表一个用户收藏的字幕/视频片段
 */
@Serializable
data class Clip(
    val id: String,
    val videoPath: String,
    val startTime: Long,           // ms
    val endTime: Long,             // ms
    var subtitleText: String,
    var translatedText: String? = null,
    var note: String? = null,
    val tags: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var videoClipPath: String? = null
)
```

- [ ] **Step 2: 创建 ClipCollection 模型**

```kotlin
package com.mujingx.data

import kotlinx.serialization.Serializable

/**
 * 片段集合
 * 类似词库的概念，用于组织多个片段
 */
@Serializable
data class ClipCollection(
    val id: String,
    val name: String,
    val clips: MutableList<Clip> = mutableListOf(),
    var tags: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: 创建 ClipTables**

```kotlin
package com.mujingx.data.db

import java.sql.Connection

/**
 * 片段管理相关 SQLite 表定义
 */
object ClipTables {

    fun createTables(connection: Connection) {
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS clip_collections (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS clips (
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
            )
        """.trimIndent())

        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS clip_tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                color TEXT
            )
        """.trimIndent())

        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS clip_tag_relations (
                clip_id TEXT NOT NULL,
                tag_id INTEGER NOT NULL,
                PRIMARY KEY (clip_id, tag_id),
                FOREIGN KEY (clip_id) REFERENCES clips(id) ON DELETE CASCADE,
                FOREIGN KEY (tag_id) REFERENCES clip_tags(id) ON DELETE CASCADE
            )
        """.trimIndent())
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/mujingx/data/Clip.kt \
       src/main/kotlin/com/mujingx/data/ClipCollection.kt \
       src/main/kotlin/com/mujingx/data/db/ClipTables.kt
git commit -m "feat(clip): 创建片段数据模型和 SQLite 表定义"
```

---

### Task 13: 实现 ClipRepository

**Files:**
- Create: `src/main/kotlin/com/mujingx/data/ClipRepository.kt`
- Create: `src/test/kotlin/com/mujingx/data/ClipRepositoryTest.kt`

- [ ] **Step 1: 编写 ClipRepository 测试**

```kotlin
package com.mujingx.data

import com.mujingx.data.db.ClipTables
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.sql.DriverManager

class ClipRepositoryTest {

    private lateinit var repo: ClipRepository
    private val testDbPath = System.getProperty("java.io.tmpdir") + "/mujing_test_clips_${System.currentTimeMillis()}.db"

    @BeforeEach
    fun setup() {
        val conn = DriverManager.getConnection("jdbc:sqlite:$testDbPath")
        ClipTables.createTables(conn)
        conn.close()
        repo = ClipRepository(testDbPath)
    }

    @AfterEach
    fun cleanup() {
        repo.close()
        File(testDbPath).delete()
    }

    @Test
    fun `save and retrieve clip`() {
        val clip = Clip(
            id = "test-1",
            videoPath = "/path/to/video.mkv",
            startTime = 1000,
            endTime = 3000,
            subtitleText = "Hello world"
        )
        repo.saveClip(clip)
        val clips = repo.getClipsByVideo("/path/to/video.mkv")
        assertEquals(1, clips.size)
        assertEquals("Hello world", clips[0].subtitleText)
    }

    @Test
    fun `delete clip removes it`() {
        val clip = Clip(id = "test-2", videoPath = "/video.mkv", startTime = 0, endTime = 1000, subtitleText = "test")
        repo.saveClip(clip)
        repo.deleteClip("test-2")
        val clips = repo.getClipsByVideo("/video.mkv")
        assertTrue(clips.isEmpty())
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd /Users/berton/Github/mujing && ./gradlew test --tests "*ClipRepositoryTest*" 2>&1 | tail -10`
Expected: FAIL — `ClipRepository` 未定义

- [ ] **Step 3: 实现 ClipRepository**

```kotlin
package com.mujingx.data

import com.mujingx.data.db.ClipTables
import java.sql.Connection
import java.sql.DriverManager

/**
 * 片段数据仓库
 * 使用 SQLite 持久化片段数据
 */
class ClipRepository(private val dbPath: String) {

    private val connection: Connection by lazy {
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        ClipTables.createTables(conn)
        conn
    }

    fun saveClip(clip: Clip) {
        val sql = """
            INSERT OR REPLACE INTO clips (id, video_path, start_time, end_time, subtitle_text, translated_text, note, video_clip_path, collection_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, clip.id)
            stmt.setString(2, clip.videoPath)
            stmt.setLong(3, clip.startTime)
            stmt.setLong(4, clip.endTime)
            stmt.setString(5, clip.subtitleText)
            stmt.setString(6, clip.translatedText)
            stmt.setString(7, clip.note)
            stmt.setString(8, clip.videoClipPath)
            stmt.setString(9, null) // collection_id 暂不使用
            stmt.setLong(10, clip.createdAt)
            stmt.executeUpdate()
        }
    }

    fun getClipsByVideo(videoPath: String): List<Clip> {
        val sql = "SELECT * FROM clips WHERE video_path = ? ORDER BY start_time"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, videoPath)
            val rs = stmt.executeQuery()
            val clips = mutableListOf<Clip>()
            while (rs.next()) {
                clips.add(Clip(
                    id = rs.getString("id"),
                    videoPath = rs.getString("video_path"),
                    startTime = rs.getLong("start_time"),
                    endTime = rs.getLong("end_time"),
                    subtitleText = rs.getString("subtitle_text"),
                    translatedText = rs.getString("translated_text"),
                    note = rs.getString("note"),
                    createdAt = rs.getLong("created_at"),
                    videoClipPath = rs.getString("video_clip_path")
                ))
            }
            return clips
        }
    }

    fun deleteClip(id: String) {
        connection.prepareStatement("DELETE FROM clips WHERE id = ?").use { stmt ->
            stmt.setString(1, id)
            stmt.executeUpdate()
        }
    }

    fun close() {
        try { connection.close() } catch (_: Exception) {}
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd /Users/berton/Github/mujing && ./gradlew test --tests "*ClipRepositoryTest*" 2>&1 | tail -10`
Expected: 2 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/mujingx/data/ClipRepository.kt \
       src/test/kotlin/com/mujingx/data/ClipRepositoryTest.kt
git commit -m "feat(clip): 实现 ClipRepository 和测试用例"
```

---

## Phase 4: 片段 UI + 字幕同步（P1-2 UI + P1-3）

### Task 14: 实现字幕同步服务

**Files:**
- Create: `src/main/kotlin/com/mujingx/subtitle/SubtitleSyncService.kt`
- Create: `src/main/kotlin/com/mujingx/subtitle/SubtitleSyncServiceImpl.kt`
- Create: `src/main/kotlin/com/mujingx/data/db/SubtitleSyncTables.kt`
- Create: `src/test/kotlin/com/mujingx/subtitle/SubtitleSyncServiceTest.kt`

- [ ] **Step 1: 创建 SubtitleSyncService 接口**

```kotlin
package com.mujingx.subtitle

/**
 * 字幕同步服务接口
 * 提供全局偏移和逐句微调能力
 */
interface SubtitleSyncService {
    fun getGlobalOffset(videoPath: String): Long
    fun setGlobalOffset(videoPath: String, offsetMs: Long)

    fun getCaptionOffset(videoPath: String, captionIndex: Int): Long
    fun setCaptionOffset(videoPath: String, captionIndex: Int, offsetMs: Long)

    fun getEffectiveTime(videoPath: String, captionIndex: Int, originalStart: Long): Long

    fun clearCaptionOffset(videoPath: String, captionIndex: Int)
}
```

- [ ] **Step 2: 创建 SubtitleSyncTables**

```kotlin
package com.mujingx.data.db

import java.sql.Connection

object SubtitleSyncTables {

    fun createTables(connection: Connection) {
        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS subtitle_global_offset (
                video_path TEXT PRIMARY KEY,
                offset_ms INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())

        connection.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS subtitle_caption_offset (
                video_path TEXT NOT NULL,
                subtitle_file_hash TEXT NOT NULL,
                caption_index INTEGER NOT NULL,
                offset_ms INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (video_path, subtitle_file_hash, caption_index)
            )
        """.trimIndent())
    }
}
```

- [ ] **Step 3: 编写测试**

```kotlin
package com.mujingx.subtitle

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.sql.DriverManager
import com.mujingx.data.db.SubtitleSyncTables

class SubtitleSyncServiceTest {

    private lateinit var service: SubtitleSyncServiceImpl
    private val testDbPath = System.getProperty("java.io.tmpdir") + "/mujing_test_sync_${System.currentTimeMillis()}.db"

    @BeforeEach
    fun setup() {
        val conn = DriverManager.getConnection("jdbc:sqlite:$testDbPath")
        SubtitleSyncTables.createTables(conn)
        conn.close()
        service = SubtitleSyncServiceImpl(testDbPath)
    }

    @AfterEach
    fun cleanup() {
        service.close()
        File(testDbPath).delete()
    }

    @Test
    fun `global offset defaults to zero`() {
        assertEquals(0L, service.getGlobalOffset("/video.mkv"))
    }

    @Test
    fun `set and get global offset`() {
        service.setGlobalOffset("/video.mkv", 500)
        assertEquals(500L, service.getGlobalOffset("/video.mkv"))
    }

    @Test
    fun `effective time includes global offset`() {
        service.setGlobalOffset("/video.mkv", 200)
        val effective = service.getEffectiveTime("/video.mkv", 0, 1000)
        assertEquals(1200L, effective)
    }

    @Test
    fun `caption offset adds to global offset`() {
        service.setGlobalOffset("/video.mkv", 100)
        service.setCaptionOffset("/video.mkv", 3, 50)
        val effective = service.getEffectiveTime("/video.mkv", 3, 2000)
        assertEquals(2150L, effective)
    }

    @Test
    fun `clear caption offset resets to zero`() {
        service.setCaptionOffset("/video.mkv", 0, 300)
        service.clearCaptionOffset("/video.mkv", 0)
        assertEquals(0L, service.getCaptionOffset("/video.mkv", 0))
    }
}
```

- [ ] **Step 4: 实现 SubtitleSyncServiceImpl**

```kotlin
package com.mujingx.subtitle

import com.mujingx.data.db.SubtitleSyncTables
import java.sql.Connection
import java.sql.DriverManager

/**
 * 字幕同步服务实现
 * 全局偏移 + 逐句微调，持久化到 SQLite
 */
class SubtitleSyncServiceImpl(private val dbPath: String) : SubtitleSyncService {

    private val connection: Connection by lazy {
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        SubtitleSyncTables.createTables(conn)
        conn
    }

    // 内存缓存
    private val globalOffsetCache = mutableMapOf<String, Long>()
    private val captionOffsetCache = mutableMapOf<String, MutableMap<Int, Long>>()

    override fun getGlobalOffset(videoPath: String): Long {
        return globalOffsetCache.getOrPut(videoPath) {
            connection.prepareStatement(
                "SELECT offset_ms FROM subtitle_global_offset WHERE video_path = ?"
            ).use { stmt ->
                stmt.setString(1, videoPath)
                val rs = stmt.executeQuery()
                if (rs.next()) rs.getLong("offset_ms") else 0L
            }
        }
    }

    override fun setGlobalOffset(videoPath: String, offsetMs: Long) {
        globalOffsetCache[videoPath] = offsetMs
        connection.prepareStatement("""
            INSERT OR REPLACE INTO subtitle_global_offset (video_path, offset_ms, updated_at)
            VALUES (?, ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setLong(2, offsetMs)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.executeUpdate()
        }
    }

    override fun getCaptionOffset(videoPath: String, captionIndex: Int): Long {
        return captionOffsetCache
            .getOrPut(videoPath) { loadCaptionOffsets(videoPath) }
            .getOrDefault(captionIndex, 0L)
    }

    override fun setCaptionOffset(videoPath: String, captionIndex: Int, offsetMs: Long) {
        captionOffsetCache.getOrPut(videoPath) { mutableMapOf() }[captionIndex] = offsetMs
        connection.prepareStatement("""
            INSERT OR REPLACE INTO subtitle_caption_offset (video_path, subtitle_file_hash, caption_index, offset_ms)
            VALUES (?, '', ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setInt(2, captionIndex)
            stmt.setLong(3, offsetMs)
            stmt.executeUpdate()
        }
    }

    override fun getEffectiveTime(videoPath: String, captionIndex: Int, originalStart: Long): Long {
        return originalStart + getGlobalOffset(videoPath) + getCaptionOffset(videoPath, captionIndex)
    }

    override fun clearCaptionOffset(videoPath: String, captionIndex: Int) {
        captionOffsetCache[videoPath]?.remove(captionIndex)
        connection.prepareStatement(
            "DELETE FROM subtitle_caption_offset WHERE video_path = ? AND caption_index = ?"
        ).use { stmt ->
            stmt.setString(1, videoPath)
            stmt.setInt(2, captionIndex)
            stmt.executeUpdate()
        }
    }

    private fun loadCaptionOffsets(videoPath: String): MutableMap<Int, Long> {
        val offsets = mutableMapOf<Int, Long>()
        connection.prepareStatement(
            "SELECT caption_index, offset_ms FROM subtitle_caption_offset WHERE video_path = ?"
        ).use { stmt ->
            stmt.setString(1, videoPath)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                offsets[rs.getInt("caption_index")] = rs.getLong("offset_ms")
            }
        }
        return offsets
    }

    fun close() {
        try { connection.close() } catch (_: Exception) {}
    }
}
```

- [ ] **Step 5: 运行测试**

Run: `cd /Users/berton/Github/mujing && ./gradlew test --tests "*SubtitleSyncServiceTest*" 2>&1 | tail -10`
Expected: 5 tests PASSED

- [ ] **Step 6: 注册到 Koin 并 Commit**

更新 `di/SubtitleModule.kt`：

```kotlin
package com.mujingx.di

import com.mujingx.subtitle.SubtitleSyncService
import com.mujingx.subtitle.SubtitleSyncServiceImpl
import org.koin.dsl.module

val subtitleModule = module {
    single<SubtitleSyncService> {
        SubtitleSyncServiceImpl(
            dbPath = System.getProperty("user.home") + "/.MuJing/subtitle_sync.db"
        )
    }
}
```

```bash
git add src/main/kotlin/com/mujingx/subtitle/ \
       src/main/kotlin/com/mujingx/data/db/SubtitleSyncTables.kt \
       src/test/kotlin/com/mujingx/subtitle/ \
       src/main/kotlin/com/mujingx/di/SubtitleModule.kt
git commit -m "feat(subtitle): 实现字幕同步服务和测试用例"
```

---

### Task 15: 注册 ClipService 到 Koin

**Files:**
- Create: `src/main/kotlin/com/mujingx/data/ClipService.kt`
- Modify: `src/main/kotlin/com/mujingx/di/ClipModule.kt`

- [ ] **Step 1: 创建 ClipService**

```kotlin
package com.mujingx.data

import com.mujingx.ffmpeg.FFmpegUtil
import java.io.File
import java.util.UUID

/**
 * 片段管理业务服务
 * 封装片段的 CRUD 和视频截取逻辑
 */
class ClipService(
    private val clipRepository: ClipRepository
) {

    fun saveClip(clip: Clip) {
        clipRepository.saveClip(clip)
    }

    fun deleteClip(id: String) {
        clipRepository.deleteClip(id)
    }

    fun getClipsByVideo(videoPath: String): List<Clip> {
        return clipRepository.getClipsByVideo(videoPath)
    }

    fun createClip(
        videoPath: String,
        startTime: Long,
        endTime: Long,
        subtitleText: String
    ): Clip {
        val clip = Clip(
            id = UUID.randomUUID().toString(),
            videoPath = videoPath,
            startTime = startTime,
            endTime = endTime,
            subtitleText = subtitleText
        )
        clipRepository.saveClip(clip)
        return clip
    }
}
```

- [ ] **Step 2: 更新 ClipModule**

```kotlin
package com.mujingx.di

import com.mujingx.data.ClipRepository
import com.mujingx.data.ClipService
import org.koin.dsl.module

val clipModule = module {
    single { ClipRepository(System.getProperty("user.home") + "/.MuJing/clips.db") }
    single { ClipService(get()) }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add src/main/kotlin/com/mujingx/data/ClipService.kt \
       src/main/kotlin/com/mujingx/di/ClipModule.kt
git commit -m "feat(clip): 创建 ClipService 和 Koin 注册"
```

---

### Task 16: 运行全部测试验证

- [ ] **Step 1: 执行全部单元测试**

Run: `cd /Users/berton/Github/mujing && ./gradlew test 2>&1 | tail -15`
Expected: `BUILD SUCCESSFUL`，所有新增测试通过

- [ ] **Step 2: 执行编译验证**

Run: `cd /Users/berton/Github/mujing && ./gradlew compileKotlin 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 最终 Commit（如有未提交的更改）**

```bash
git add -A
git commit -m "chore: Phase 1-4 基础设施和数据层完成"
```

---

## 自审检查清单

### Spec 覆盖
| Spec 要求 | 对应 Task |
|-----------|----------|
| P0-1 Koin DI 仅 Service 层 | Task 1-4 |
| P0-2 多 Provider 翻译 | Task 5-10 |
| P0-2 翻译缓存 | Task 7 |
| P0-2 四个翻译场景接口 | Task 5 (接口定义), Task 8-9 (Provider) |
| P1-1 i18n 架构 + 字符串抽取 | Task 11 |
| P1-2 Clip 数据模型 | Task 12 |
| P1-2 ClipRepository | Task 13 |
| P1-2 ClipService | Task 15 |
| P1-3 SubtitleSyncService | Task 14 |
| 风险缓解：Koin+Compose 验证 | Task 3, 4 |

### Placeholder 扫描
无 TBD/TODO 占位符。

### 类型一致性
- `TranslationService.translate()` 返回 `Result<String>` — Task 5, 6, 10 一致
- `Clip` 模型字段名 — Task 12, 13, 15 一致
- `SubtitleSyncService` 方法签名 — Task 14 定义和实现一致
- `TranslationCacheRepository` 构造函数 — Task 7, 10 一致

**注：** i18n 字符串替换（~40 文件）和 UI 集成（翻译按钮、片段面板、同步控制面板）属于渐进式改造，具体改动依赖当前 UI 代码的精确结构，建议在实际实施时按文件逐一替换，不在本计划中逐个展开。
