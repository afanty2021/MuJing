/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing.
 *
 * MuJing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MuJing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MuJing. If not, see <https://www.gnu.org/licenses/>.
 */

package com.mujingx.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Vocabulary 序列化测试套件
 *
 * 测试词库、单词、字幕等数据类的序列化和反序列化功能，
 * 包括特殊字符、null 字段、Unicode 字符等边缘情况。
 */
@DisplayName("Vocabulary 序列化测试套件")
class VocabularyTest {

    // 使用与生产代码类似的 Json 配置（测试中关闭 prettyPrint 以便断言匹配紧凑 JSON 格式）
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    @Nested
    @DisplayName("Vocabulary 序列化测试")
    inner class VocabularySerializationTests {

        @Test
        @DisplayName("序列化空词库")
        fun testSerializeEmptyVocabulary() {
            val vocabulary = Vocabulary(
                name = "Test Vocabulary",
                type = VocabularyType.DOCUMENT,
                language = "en",
                size = 0,
                relateVideoPath = "",
                subtitlesTrackId = 0,
                wordList = mutableListOf()
            )

            val jsonString = json.encodeToString(vocabulary)

            assertNotNull(jsonString)
            assertTrue(jsonString.contains("Test Vocabulary"))
            assertTrue(jsonString.contains("DOCUMENT"))
            assertTrue(jsonString.contains("\"size\":0"))
        }

        @Test
        @DisplayName("序列化包含单词的词库")
        fun testSerializeVocabularyWithWords() {
            val vocabulary = Vocabulary(
                name = "Movie Vocabulary",
                type = VocabularyType.MKV,
                language = "en",
                size = 2,
                relateVideoPath = "/path/to/movie.mkv",
                subtitlesTrackId = 1,
                wordList = mutableListOf(
                    Word(value = "hello"),
                    Word(value = "world")
                )
            )

            val jsonString = json.encodeToString(vocabulary)

            assertTrue(jsonString.contains("Movie Vocabulary"))
            assertTrue(jsonString.contains("hello"))
            assertTrue(jsonString.contains("world"))
            assertTrue(jsonString.contains("/path/to/movie.mkv"))
            assertTrue(jsonString.contains("\"subtitlesTrackId\":1"))
        }

        @Test
        @DisplayName("反序列化词库")
        fun testDeserializeVocabulary() {
            val jsonString = """
                {
                    "name": "Test Vocabulary",
                    "type": "DOCUMENT",
                    "language": "en",
                    "size": 1,
                    "relateVideoPath": "/test/path",
                    "subtitlesTrackId": 2,
                    "wordList": [
                        {
                            "value": "test",
                            "usphone": "",
                            "ukphone": "",
                            "definition": "",
                            "translation": "",
                            "pos": "",
                            "collins": 0,
                            "oxford": false,
                            "tag": "",
                            "bnc": 0,
                            "frq": 0,
                            "exchange": "",
                            "externalCaptions": [],
                            "captions": []
                        }
                    ]
                }
            """.trimIndent()

            val vocabulary = json.decodeFromString<Vocabulary>(jsonString)

            assertEquals("Test Vocabulary", vocabulary.name)
            assertEquals(VocabularyType.DOCUMENT, vocabulary.type)
            assertEquals("en", vocabulary.language)
            assertEquals(1, vocabulary.size)
            assertEquals("/test/path", vocabulary.relateVideoPath)
            assertEquals(2, vocabulary.subtitlesTrackId)
            assertEquals(1, vocabulary.wordList.size)
            assertEquals("test", vocabulary.wordList[0].value)
        }

        @Test
        @DisplayName("序列化反序列化往返一致性")
        fun testSerializeDeserializeRoundTrip() {
            val original = Vocabulary(
                name = "Round Trip Test",
                type = VocabularyType.SUBTITLES,
                language = "zh",
                size = 3,
                relateVideoPath = "/video/test.mp4",
                subtitlesTrackId = 0,
                wordList = mutableListOf(
                    Word(value = "apple", definition = "苹果"),
                    Word(value = "banana", definition = "香蕉"),
                    Word(value = "cherry", definition = "樱桃")
                )
            )

            val jsonString = json.encodeToString(original)
            val restored = json.decodeFromString<Vocabulary>(jsonString)

            assertEquals(original.name, restored.name)
            assertEquals(original.type, restored.type)
            assertEquals(original.language, restored.language)
            assertEquals(original.size, restored.size)
            assertEquals(original.relateVideoPath, restored.relateVideoPath)
            assertEquals(original.subtitlesTrackId, restored.subtitlesTrackId)
            assertEquals(original.wordList.size, restored.wordList.size)

            // 验证每个单词
            original.wordList.forEachIndexed { index, word ->
                assertEquals(word.value, restored.wordList[index].value)
                assertEquals(word.definition, restored.wordList[index].definition)
            }
        }

        @Test
        @DisplayName("序列化不同类型的词库")
        fun testSerializeDifferentVocabularyTypes() {
            val types = listOf(
                VocabularyType.DOCUMENT,
                VocabularyType.SUBTITLES,
                VocabularyType.MKV
            )

            types.forEach { type ->
                val vocabulary = Vocabulary(
                    name = "Test $type",
                    type = type,
                    language = "en",
                    size = 0
                )

                val jsonString = json.encodeToString(vocabulary)
                assertTrue(jsonString.contains(type.name))
            }
        }
    }

    @Nested
    @DisplayName("Word 序列化测试")
    inner class WordSerializationTests {

        @Test
        @DisplayName("序列化基本单词")
        fun testSerializeBasicWord() {
            val word = Word(value = "hello")

            val jsonString = json.encodeToString(word)

            assertTrue(jsonString.contains("hello"))
            assertTrue(jsonString.contains("\"usphone\":\"\""), "Missing usphone in: $jsonString")
            assertTrue(jsonString.contains("\"ukphone\":\"\""), "Missing ukphone in: $jsonString")
        }

        @Test
        @DisplayName("序列化包含所有字段的单词")
        fun testSerializeWordWithAllFields() {
            val word = Word(
                value = "example",
                usphone = "ɪɡˈzæmpl",
                ukphone = "ɪɡˈzɑːmpl",
                definition = "例子；榜样",
                translation = "a thing characteristic of its kind",
                pos = "n.",
                collins = 5,
                oxford = true,
                tag = "CET4",
                bnc = 12345,
                frq = 67890,
                exchange = "examples:example"
            )

            val jsonString = json.encodeToString(word)

            assertTrue(jsonString.contains("example"))
            assertTrue(jsonString.contains("ɪɡˈzæmpl"))
            assertTrue(jsonString.contains("例子"))
            assertTrue(jsonString.contains("CET4"))
            assertTrue(jsonString.contains("\"collins\":5"))
            assertTrue(jsonString.contains("\"oxford\":true"))
            assertTrue(jsonString.contains("\"bnc\":12345"))
            assertTrue(jsonString.contains("\"frq\":67890"))
        }

        @Test
        @DisplayName("序列化包含字幕的单词")
        fun testSerializeWordWithCaptions() {
            val word = Word(
                value = "test",
                captions = mutableListOf(
                    Caption("00:00:01", "00:00:02", "This is a test"),
                    Caption("00:00:05", "00:00:06", "Another test")
                )
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals(2, restored.captions.size)
            assertEquals("This is a test", restored.captions[0].content)
            assertEquals("Another test", restored.captions[1].content)
        }

        @Test
        @DisplayName("序列化包含外部字幕的单词")
        fun testSerializeWordWithExternalCaptions() {
            val word = Word(
                value = "hello",
                externalCaptions = mutableListOf(
                    ExternalCaption(
                        relateVideoPath = "/movie.mkv",
                        subtitlesTrackId = 1,
                        subtitlesName = "English",
                        start = "00:00:10",
                        end = "00:00:11",
                        content = "Hello world"
                    )
                )
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals(1, restored.externalCaptions.size)
            assertEquals("/movie.mkv", restored.externalCaptions[0].relateVideoPath)
            assertEquals(1, restored.externalCaptions[0].subtitlesTrackId)
            assertEquals("English", restored.externalCaptions[0].subtitlesName)
            assertEquals("Hello world", restored.externalCaptions[0].content)
        }

        @Test
        @DisplayName("序列化包含特殊字符的单词")
        fun testSerializeWordWithSpecialCharacters() {
            val word = Word(
                value = "emoji测试",
                definition = "包含 emoji 😊 和中文 🎉",
                translation = "Contains emoji 🚀 and symbols @#$%"
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals("emoji测试", restored.value)
            assertEquals("包含 emoji 😊 和中文 🎉", restored.definition)
            assertEquals("Contains emoji 🚀 and symbols @#$%", restored.translation)
        }

        @Test
        @DisplayName("序列化包含空格和换行的单词")
        fun testSerializeWordWithWhitespace() {
            val word = Word(
                value = "test",
                definition = "Line 1\nLine 2\nLine 3",
                translation = "  spaced  out  "
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals("Line 1\nLine 2\nLine 3", restored.definition)
            assertEquals("  spaced  out  ", restored.translation)
        }
    }

    @Nested
    @DisplayName("Caption 序列化测试")
    inner class CaptionSerializationTests {

        @Test
        @DisplayName("序列化基本字幕")
        fun testSerializeCaption() {
            val caption = Caption(
                start = "00:00:01",
                end = "00:00:02",
                content = "Hello world"
            )

            val jsonString = json.encodeToString(caption)
            val restored = json.decodeFromString<Caption>(jsonString)

            assertEquals("00:00:01", restored.start)
            assertEquals("00:00:02", restored.end)
            assertEquals("Hello world", restored.content)
        }

        @Test
        @DisplayName("序列化包含特殊内容的字幕")
        fun testSerializeCaptionWithSpecialContent() {
            val caption = Caption(
                start = "00:00:00",
                end = "00:00:05",
                content = "特殊字符：中文、日本語、한국어、😀"
            )

            val jsonString = json.encodeToString(caption)
            val restored = json.decodeFromString<Caption>(jsonString)

            assertEquals("特殊字符：中文、日本語、한국어、😀", restored.content)
        }

        @Test
        @DisplayName("Caption toString 返回内容")
        fun testCaptionToString() {
            val caption = Caption(
                start = "00:00:01",
                end = "00:00:02",
                content = "Test content"
            )

            assertEquals("Test content", caption.toString())
        }

        @Test
        @DisplayName("序列化包含 HTML 标签的字幕")
        fun testSerializeCaptionWithHtml() {
            val caption = Caption(
                start = "00:00:01",
                end = "00:00:02",
                content = "<i>Hello</i> <b>World</b>"
            )

            val jsonString = json.encodeToString(caption)
            val restored = json.decodeFromString<Caption>(jsonString)

            assertEquals("<i>Hello</i> <b>World</b>", restored.content)
        }
    }

    @Nested
    @DisplayName("ExternalCaption 序列化测试")
    inner class ExternalCaptionSerializationTests {

        @Test
        @DisplayName("序列化基本外部字幕")
        fun testSerializeExternalCaption() {
            val externalCaption = ExternalCaption(
                relateVideoPath = "/path/to/video.mkv",
                subtitlesTrackId = 2,
                subtitlesName = "Chinese",
                start = "00:00:10",
                end = "00:00:11",
                content = "你好世界"
            )

            val jsonString = json.encodeToString(externalCaption)
            val restored = json.decodeFromString<ExternalCaption>(jsonString)

            assertEquals("/path/to/video.mkv", restored.relateVideoPath)
            assertEquals(2, restored.subtitlesTrackId)
            assertEquals("Chinese", restored.subtitlesName)
            assertEquals("00:00:10", restored.start)
            assertEquals("00:00:11", restored.end)
            assertEquals("你好世界", restored.content)
        }

        @Test
        @DisplayName("ExternalCaption toString 返回内容")
        fun testExternalCaptionToString() {
            val externalCaption = ExternalCaption(
                relateVideoPath = "/test.mkv",
                subtitlesTrackId = 1,
                subtitlesName = "English",
                start = "00:00:01",
                end = "00:00:02",
                content = "External content"
            )

            assertEquals("External content", externalCaption.toString())
        }

        @Test
        @DisplayName("序列化包含特殊路径的外部字幕")
        fun testSerializeExternalCaptionWithSpecialPath() {
            val externalCaption = ExternalCaption(
                relateVideoPath = "/路径/包含/中文/视频.mkv",
                subtitlesTrackId = 0,
                subtitlesName = "测试字幕",
                start = "00:00:00",
                end = "00:00:01",
                content = "测试内容"
            )

            val jsonString = json.encodeToString(externalCaption)
            val restored = json.decodeFromString<ExternalCaption>(jsonString)

            assertEquals("/路径/包含/中文/视频.mkv", restored.relateVideoPath)
            assertEquals("测试字幕", restored.subtitlesName)
            assertEquals("测试内容", restored.content)
        }
    }

    @Nested
    @DisplayName("Word equals 和 hashCode 测试")
    inner class WordEqualityTests {

        @Test
        @DisplayName("大小写不敏感的 equals 比较")
        fun testEqualsCaseInsensitive() {
            val word1 = Word(value = "Hello")
            val word2 = Word(value = "hello")
            val word3 = Word(value = "HELLO")
            val word4 = Word(value = "world")

            assertEquals(word1, word2)
            assertEquals(word2, word3)
            assertEquals(word1, word3)
            assertNotEquals(word1, word4)
        }

        @Test
        @DisplayName("hashCode 一致性")
        fun testHashCodeConsistency() {
            val word1 = Word(value = "Test")
            val word2 = Word(value = "test")
            val word3 = Word(value = "TEST")

            // 大小写不敏感的 hashCode 应该相同
            assertEquals(word1.hashCode(), word2.hashCode())
            assertEquals(word2.hashCode(), word3.hashCode())
        }

        @Test
        @DisplayName("hashCode 与 equals 契约")
        fun testHashCodeEqualsContract() {
            val word1 = Word(value = "same")
            val word2 = Word(value = "same")

            // 相等的对象必须有相同的 hashCode
            assertEquals(word1, word2)
            assertEquals(word1.hashCode(), word2.hashCode())
        }

        @Test
        @DisplayName("equals 对非 Word 对象返回 false")
        fun testEqualsWithNonWord() {
            val word = Word(value = "test")

            assertNotEquals(word, null)
            assertNotEquals(word, "test")
            assertNotEquals(word, 123)
        }

        @Test
        @DisplayName("不同字段但相同 value 的单词相等")
        fun testEqualsWithDifferentFields() {
            val word1 = Word(
                value = "test",
                definition = "Definition 1"
            )
            val word2 = Word(
                value = "TEST",
                definition = "Definition 2"
            )

            assertEquals(word1, word2)
        }
    }

    @Nested
    @DisplayName("Word deepCopy 测试")
    inner class WordDeepCopyTests {

        @Test
        @DisplayName("deepCopy 创建新实例")
        fun testDeepCopyCreatesNewInstance() {
            val original = Word(value = "test", definition = "original")
            val copy = original.deepCopy()

            // 不是同一个对象引用
            assertNotSame(original, copy)

            // 但内容相等
            assertEquals(original, copy)
            assertEquals(original.value, copy.value)
            assertEquals(original.definition, copy.definition)
        }

        @Test
        @DisplayName("deepCopy 复制所有字段")
        fun testDeepCopyCopiesAllFields() {
            val original = Word(
                value = "example",
                usphone = "us",
                ukphone = "uk",
                definition = "def",
                translation = "trans",
                pos = "n.",
                collins = 3,
                oxford = true,
                tag = "tag",
                bnc = 100,
                frq = 200,
                exchange = "ex"
            )

            val copy = original.deepCopy()

            assertEquals(original.value, copy.value)
            assertEquals(original.usphone, copy.usphone)
            assertEquals(original.ukphone, copy.ukphone)
            assertEquals(original.definition, copy.definition)
            assertEquals(original.translation, copy.translation)
            assertEquals(original.pos, copy.pos)
            assertEquals(original.collins, copy.collins)
            assertEquals(original.oxford, copy.oxford)
            assertEquals(original.tag, copy.tag)
            assertEquals(original.bnc, copy.bnc)
            assertEquals(original.frq, copy.frq)
            assertEquals(original.exchange, copy.exchange)
        }

        @Test
        @DisplayName("deepCopy 复制 captions 列表")
        fun testDeepCopyCopiesCaptions() {
            val original = Word(
                value = "test",
                captions = mutableListOf(
                    Caption("00:00:01", "00:00:02", "Caption 1"),
                    Caption("00:00:03", "00:00:04", "Caption 2")
                )
            )

            val copy = original.deepCopy()

            assertEquals(original.captions.size, copy.captions.size)
            assertEquals(original.captions[0].content, copy.captions[0].content)
            assertEquals(original.captions[1].content, copy.captions[1].content)
        }

        @Test
        @DisplayName("deepCopy 复制 externalCaptions 列表")
        fun testDeepCopyCopiesExternalCaptions() {
            val original = Word(
                value = "test",
                externalCaptions = mutableListOf(
                    ExternalCaption(
                        relateVideoPath = "/test.mkv",
                        subtitlesTrackId = 1,
                        subtitlesName = "English",
                        start = "00:00:01",
                        end = "00:00:02",
                        content = "External"
                    )
                )
            )

            val copy = original.deepCopy()

            assertEquals(original.externalCaptions.size, copy.externalCaptions.size)
            assertEquals(
                original.externalCaptions[0].content,
                copy.externalCaptions[0].content
            )
        }

        @Test
        @DisplayName("deepCopy 的列表修改不影响原对象")
        fun testDeepCopyListIndependence() {
            val original = Word(
                value = "test",
                captions = mutableListOf(Caption("00:00:01", "00:00:02", "Original"))
            )

            val copy = original.deepCopy()
            copy.captions.add(Caption("00:00:03", "00:00:04", "Added"))

            // 原对象的列表不应被修改
            assertEquals(1, original.captions.size)
            assertEquals(2, copy.captions.size)
        }

        @Test
        @DisplayName("deepCopy 空列表")
        fun testDeepCopyWithEmptyLists() {
            val original = Word(
                value = "test",
                captions = mutableListOf(),
                externalCaptions = mutableListOf()
            )

            val copy = original.deepCopy()

            assertTrue(copy.captions.isEmpty())
            assertTrue(copy.externalCaptions.isEmpty())
        }
    }

    @Nested
    @DisplayName("边缘情况测试")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("null 可选字段处理")
        fun testNullOptionalFields() {
            val word = Word(
                value = "test",
                bnc = null,
                frq = null
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals("test", restored.value)
            // bnc/frq 是 Int? 类型，null 序列化后反序列化仍为 null
            assertNull(restored.bnc)
            assertNull(restored.frq)
        }

        @Test
        @DisplayName("空字符串字段")
        fun testEmptyStrings() {
            val word = Word(
                value = "",
                usphone = "",
                ukphone = "",
                definition = "",
                translation = ""
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals("", restored.value)
            assertEquals("", restored.usphone)
            assertEquals("", restored.ukphone)
            assertEquals("", restored.definition)
            assertEquals("", restored.translation)
        }

        @Test
        @DisplayName("Unicode 字符处理")
        fun testUnicodeCharacters() {
            val vocabulary = Vocabulary(
                name = "测试词库🎉",
                type = VocabularyType.DOCUMENT,
                language = "中文",
                size = 1,
                wordList = mutableListOf(
                    Word(
                        value = "测试",
                        definition = "日本語、한국어、Español",
                        translation = "Various scripts: α, β, γ, δ"
                    )
                )
            )

            val jsonString = json.encodeToString(vocabulary)
            val restored = json.decodeFromString<Vocabulary>(jsonString)

            assertEquals("测试词库🎉", restored.name)
            assertEquals("中文", restored.language)
            assertEquals("测试", restored.wordList[0].value)
            assertEquals("日本語、한국어、Español", restored.wordList[0].definition)
        }

        @Test
        @DisplayName("转义字符处理")
        fun testEscapedCharacters() {
            val word = Word(
                value = "test",
                definition = "Line with \"quotes\" and \\backslash\\",
                translation = "Path: C:\\Users\\test"
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals("Line with \"quotes\" and \\backslash\\", restored.definition)
            assertEquals("Path: C:\\Users\\test", restored.translation)
        }

        @Test
        @DisplayName("超大数值字段")
        fun testLargeNumericValues() {
            val word = Word(
                value = "test",
                collins = Int.MAX_VALUE,
                bnc = Int.MAX_VALUE,
                frq = Int.MAX_VALUE
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals(Int.MAX_VALUE, restored.collins)
            assertEquals(Int.MAX_VALUE, restored.bnc)
            assertEquals(Int.MAX_VALUE, restored.frq)
        }

        @Test
        @DisplayName("非常长的字符串")
        fun testVeryLongStrings() {
            val longString = "a".repeat(10000)
            val word = Word(
                value = "test",
                definition = longString
            )

            val jsonString = json.encodeToString(word)
            val restored = json.decodeFromString<Word>(jsonString)

            assertEquals(10000, restored.definition.length)
            assertEquals(longString, restored.definition)
        }
    }

    @Nested
    @DisplayName("性能测试")
    inner class PerformanceTests {

        @Test
        @DisplayName("大词库序列化性能")
        fun testLargeVocabularySerialization() {
            // 创建包含 1000 个单词的大词库
            val words = (1..1000).map { index ->
                Word(
                    value = "word$index",
                    definition = "Definition $index",
                    captions = mutableListOf(
                        Caption("00:00:0${index % 10}", "00:00:0${(index % 10) + 1}", "Content $index")
                    )
                )
            }.toMutableList()

            val vocabulary = Vocabulary(
                name = "Large Vocabulary",
                type = VocabularyType.DOCUMENT,
                language = "en",
                size = 1000,
                wordList = words
            )

            // 测量序列化时间
            val serializeStart = System.currentTimeMillis()
            val jsonString = json.encodeToString(vocabulary)
            val serializeTime = System.currentTimeMillis() - serializeStart

            // 测量反序列化时间
            val deserializeStart = System.currentTimeMillis()
            val restored = json.decodeFromString<Vocabulary>(jsonString)
            val deserializeTime = System.currentTimeMillis() - deserializeStart

            // 验证数据完整性
            assertEquals(1000, restored.wordList.size)
            assertEquals("word1", restored.wordList[0].value)
            assertEquals("word1000", restored.wordList[999].value)

            // 性能断言（根据实际环境调整）
            println("序列化 1000 个单词耗时: ${serializeTime}ms")
            println("反序列化 1000 个单词耗时: ${deserializeTime}ms")

            // 通常应该在合理时间内完成（例如 < 1 秒）
            assertTrue(serializeTime < 5000, "序列化时间过长: ${serializeTime}ms")
            assertTrue(deserializeTime < 5000, "反序列化时间过长: ${deserializeTime}ms")
        }

        @Test
        @DisplayName("批量序列化性能")
        fun testBatchSerializationPerformance() {
            val words = (1..100).map {
                Word(value = "word$it", definition = "Definition $it")
            }

            val startTime = System.currentTimeMillis()
            words.forEach { word ->
                json.encodeToString(word)
            }
            val totalTime = System.currentTimeMillis() - startTime

            println("序列化 100 个单词总耗时: ${totalTime}ms")
            println("平均每个单词: ${totalTime / 100.0}ms")

            // 性能断言
            assertTrue(totalTime < 1000, "批量序列化时间过长: ${totalTime}ms")
        }
    }

    @Nested
    @DisplayName("MutableVocabulary 测试")
    inner class MutableVocabularyTests {

        @Test
        @DisplayName("MutableVocabulary 序列化方法")
        fun testMutableVocabularySerializeMethod() {
            val vocabulary = Vocabulary(
                name = "Test",
                type = VocabularyType.DOCUMENT,
                language = "en",
                size = 1,
                wordList = mutableListOf(Word(value = "test"))
            )

            val mutableVocabulary = MutableVocabulary(vocabulary)
            val serialized = mutableVocabulary.serializeVocabulary

            assertEquals(vocabulary.name, serialized.name)
            assertEquals(vocabulary.type, serialized.type)
            assertEquals(vocabulary.language, serialized.language)
            assertEquals(vocabulary.size, serialized.size)
            assertEquals(vocabulary.wordList.size, serialized.wordList.size)
        }

        @Test
        @DisplayName("MutableVocabulary 状态更新")
        fun testMutableVocabularyStateUpdate() {
            val vocabulary = Vocabulary(
                name = "Original",
                type = VocabularyType.DOCUMENT,
                language = "en",
                size = 0
            )

            val mutableVocabulary = MutableVocabulary(vocabulary)

            // 修改状态
            mutableVocabulary.name = "Updated"
            mutableVocabulary.size = 10

            assertEquals("Updated", mutableVocabulary.name)
            assertEquals(10, mutableVocabulary.size)
            assertEquals("Original", vocabulary.name) // 原对象不变
        }
    }

    @Nested
    @DisplayName("JSON 格式验证测试")
    inner class JsonFormatTests {

        @Test
        @DisplayName("验证 JSON 格式化输出")
        fun testJsonPrettyPrint() {
            val vocabulary = Vocabulary(
                name = "Pretty Test",
                type = VocabularyType.DOCUMENT,
                language = "en",
                size = 1,
                wordList = mutableListOf(Word(value = "test"))
            )

            val jsonString = json.encodeToString(vocabulary)

            // 验证输出是有效的 JSON（紧凑格式，不包含换行）
            assertTrue(jsonString.startsWith("{"))
            assertTrue(jsonString.contains("\"name\""))
            assertFalse(jsonString.contains("\n"), "Compact JSON should not contain newlines")
        }

        @Test
        @DisplayName("验证默认值编码")
        fun testEncodeDefaults() {
            val word = Word(value = "minimal")

            val jsonString = json.encodeToString(word)

            // 默认值应该被编码
            assertTrue(jsonString.contains("\"usphone\":\"\""))
            assertTrue(jsonString.contains("\"ukphone\":\"\""))
            assertTrue(jsonString.contains("\"definition\":\"\""))
        }

        @Test
        @DisplayName("验证 JSON 字段名")
        fun testJsonFieldNames() {
            val vocabulary = Vocabulary(
                name = "Field Test",
                type = VocabularyType.MKV,
                language = "zh",
                size = 0
            )

            val jsonString = json.encodeToString(vocabulary)

            // 验证字段名正确
            assertTrue(jsonString.contains("\"name\""))
            assertTrue(jsonString.contains("\"type\""))
            assertTrue(jsonString.contains("\"language\""))
            assertTrue(jsonString.contains("\"size\""))
            assertTrue(jsonString.contains("\"relateVideoPath\""))
            assertTrue(jsonString.contains("\"subtitlesTrackId\""))
            assertTrue(jsonString.contains("\"wordList\""))
        }
    }
}
