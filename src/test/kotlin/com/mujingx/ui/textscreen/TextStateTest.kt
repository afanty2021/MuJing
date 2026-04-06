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

package com.mujingx.ui.textscreen

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * TextState 单元测试
 * 测试文本状态管理类的核心功能
 */
@OptIn(ExperimentalSerializationApi::class)
class TextStateTest {

    private val jsonFormat = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    private lateinit var defaultState: DataTextState
    private lateinit var textState: TextState

    @Before
    fun setUp() {
        defaultState = DataTextState()
        textState = TextState(defaultState)
    }

    @Test
    fun `test default state values`() {
        // 测试默认状态值
        assertEquals("", textState.textPath)
        assertEquals(0, textState.currentIndex)
        assertEquals(0, textState.firstVisibleItemIndex)
    }

    @Test
    fun `test set text path updates state`() {
        // 测试设置文本路径
        val testPath = "/path/to/document.txt"
        textState.textPath = testPath
        assertEquals(testPath, textState.textPath)
    }

    @Test
    fun `test set current index updates state`() {
        // 测试设置当前索引
        textState.currentIndex = 5
        assertEquals(5, textState.currentIndex)
    }

    @Test
    fun `test set first visible item index updates state`() {
        // 测试设置第一个可见项索引
        textState.firstVisibleItemIndex = 10
        assertEquals(10, textState.firstVisibleItemIndex)
    }

    @Test
    fun `test serialization of DataTextState`() {
        // 测试 DataTextState 序列化
        val state = DataTextState(
            textPath = "/test/document.txt",
            currentIndex = 10,
            firstVisibleItemIndex = 5
        )

        val json = jsonFormat.encodeToString(state)
        assertNotNull(json)
        assertTrue(json.contains("\"textPath\":\"/test/document.txt\""))
        assertTrue(json.contains("\"currentIndex\":10"))
        assertTrue(json.contains("\"firstVisibleItemIndex\":5"))
    }

    @Test
    fun `test deserialization of DataTextState`() {
        // 测试 DataTextState 反序列化
        val json = """
            {
                "textPath": "/test/document.txt",
                "currentIndex": 10,
                "firstVisibleItemIndex": 5
            }
        """.trimIndent()

        val state = Json.decodeFromString<DataTextState>(json)
        assertEquals("/test/document.txt", state.textPath)
        assertEquals(10, state.currentIndex)
        assertEquals(5, state.firstVisibleItemIndex)
    }

    @Test
    fun `test serialization roundtrip`() {
        // 测试序列化往返
        val original = DataTextState(
            textPath = "/test/novel.txt",
            currentIndex = 20,
            firstVisibleItemIndex = 15
        )

        val json = jsonFormat.encodeToString(original)
        val restored = Json.decodeFromString<DataTextState>(json)

        assertEquals(original.textPath, restored.textPath)
        assertEquals(original.currentIndex, restored.currentIndex)
        assertEquals(original.firstVisibleItemIndex, restored.firstVisibleItemIndex)
    }

    @Test
    fun `test empty state serialization`() {
        // 测试空状态序列化
        val emptyState = DataTextState()
        val json = jsonFormat.encodeToString(emptyState)
        val restored = Json.decodeFromString<DataTextState>(json)

        assertEquals("", restored.textPath)
        assertEquals(0, restored.currentIndex)
        assertEquals(0, restored.firstVisibleItemIndex)
    }

    @Test
    fun `test state with unicode characters`() {
        // 测试包含 Unicode 字符的状态
        val state = DataTextState(
            textPath = "/测试/文档.txt",
            currentIndex = 1
        )

        val json = jsonFormat.encodeToString(state)
        val restored = Json.decodeFromString<DataTextState>(json)

        assertEquals("/测试/文档.txt", restored.textPath)
        assertEquals(1, restored.currentIndex)
    }

    @Test
    fun `test state with special path characters`() {
        // 测试包含特殊路径字符的状态
        val state = DataTextState(
            textPath = "C:/Users/Test/Documents/[novel].txt"
        )

        val json = jsonFormat.encodeToString(state)
        val restored = Json.decodeFromString<DataTextState>(json)

        assertEquals("C:/Users/Test/Documents/[novel].txt", restored.textPath)
    }

    @Test
    fun `test different file extensions`() {
        // 测试不同文件扩展名
        val extensions = listOf(".txt", ".md", ".rtf", ".log")

        extensions.forEach { ext ->
            val state = DataTextState(textPath = "/test/document$ext")
            val json = jsonFormat.encodeToString(state)
            val restored = Json.decodeFromString<DataTextState>(json)

            assertEquals("/test/document$ext", restored.textPath)
        }
    }

    @Test
    fun `test large index values`() {
        // 测试大索引值
        val state = TextState(DataTextState(
            currentIndex = Int.MAX_VALUE,
            firstVisibleItemIndex = Int.MAX_VALUE - 100
        ))

        assertEquals(Int.MAX_VALUE, state.currentIndex)
        assertEquals(Int.MAX_VALUE - 100, state.firstVisibleItemIndex)
    }

    @Test
    fun `test negative index values`() {
        // 测试负数索引值（虽然不应该在正常使用中出现）
        val state = TextState(DataTextState(
            currentIndex = -1,
            firstVisibleItemIndex = -5
        ))

        assertEquals(-1, state.currentIndex)
        assertEquals(-5, state.firstVisibleItemIndex)
    }

    @Test
    fun `test state with relative path`() {
        // 测试相对路径
        val state = DataTextState(textPath = "./documents/test.txt")

        val json = jsonFormat.encodeToString(state)
        val restored = Json.decodeFromString<DataTextState>(json)

        assertEquals("./documents/test.txt", restored.textPath)
    }

    @Test
    fun `test state with absolute path`() {
        // 测试绝对路径
        val paths = listOf(
            "/home/user/documents/test.txt",
            "C:/Users/user/Documents/test.txt",
            "/Users/user/Documents/test.txt"
        )

        paths.forEach { path ->
            val state = DataTextState(textPath = path)
            assertEquals(path, state.textPath)
        }
    }

    @Test
    fun `test current index can be zero`() {
        // 测试当前索引可以为 0
        textState.currentIndex = 0
        assertEquals(0, textState.currentIndex)
    }

    @Test
    fun `test first visible item index can be zero`() {
        // 测试第一个可见项索引可以为 0
        textState.firstVisibleItemIndex = 0
        assertEquals(0, textState.firstVisibleItemIndex)
    }

    @Test
    fun `test current index can equal first visible item index`() {
        // 测试当前索引可以等于第一个可见项索引
        textState.currentIndex = 10
        textState.firstVisibleItemIndex = 10

        assertEquals(textState.currentIndex, textState.firstVisibleItemIndex)
    }

    @Test
    fun `test current index can be greater than first visible item index`() {
        // 测试当前索引可以大于第一个可见项索引
        textState.currentIndex = 20
        textState.firstVisibleItemIndex = 10

        assertTrue(textState.currentIndex > textState.firstVisibleItemIndex)
    }

    @Test
    fun `test current index can be less than first visible item index`() {
        // 测试当前索引可以小于第一个可见项索引（滚动后）
        textState.currentIndex = 5
        textState.firstVisibleItemIndex = 10

        assertTrue(textState.currentIndex < textState.firstVisibleItemIndex)
    }

    @Test
    fun `test state with empty path`() {
        // 测试空路径
        val state = TextState(DataTextState(textPath = ""))
        assertEquals("", state.textPath)
    }

    @Test
    fun `test state with whitespace path`() {
        // 测试包含空格的路径
        val state = DataTextState(textPath = "/path with spaces/document.txt")
        assertEquals("/path with spaces/document.txt", state.textPath)
    }

    @Test
    fun `test state preserves path encoding`() {
        // 测试路径编码保留
        val paths = listOf(
            "/path/to/file%20name.txt",
            "/path/to/file%2Fwith%2Fslashes.txt",
            "/中文/路径/文档.txt"
        )

        paths.forEach { path ->
            val state = DataTextState(textPath = path)
            val json = jsonFormat.encodeToString(state)
            val restored = Json.decodeFromString<DataTextState>(json)

            assertEquals(path, restored.textPath)
        }
    }

    @Test
    fun `test minimal json representation`() {
        // 测试最小的 JSON 表示
        val state = DataTextState()
        val json = jsonFormat.encodeToString(state)

        // 最小 JSON 应该包含所有字段，即使是默认值
        assertTrue(json.contains("textPath"))
        assertTrue(json.contains("currentIndex"))
        assertTrue(json.contains("firstVisibleItemIndex"))
    }

    @Test
    fun `test state immutability after creation`() {
        // 测试创建后的状态不可变性（通过 DataTextState）
        val dataState = DataTextState(
            textPath = "/test.txt",
            currentIndex = 5
        )

        // DataTextState 本身是不可变的
        assertEquals("/test.txt", dataState.textPath)
        assertEquals(5, dataState.currentIndex)

        // 创建新的状态
        val newState = DataTextState(
            textPath = dataState.textPath,
            currentIndex = dataState.currentIndex + 1
        )

        assertEquals("/test.txt", newState.textPath)
        assertEquals(6, newState.currentIndex)
        // 原始状态不受影响
        assertEquals(5, dataState.currentIndex)
    }
}
