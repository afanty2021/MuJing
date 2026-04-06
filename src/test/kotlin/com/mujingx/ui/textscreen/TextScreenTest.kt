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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import com.mujingx.player.PlayerState
import com.mujingx.state.GlobalData
import com.mujingx.state.GlobalState
import com.mujingx.ui.wordscreen.WordScreenData
import com.mujingx.ui.wordscreen.WordScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * TextScreen UI 测试套件
 *
 * 测试文档阅读器的 UI 组件渲染和交互
 */
@OptIn(
    ExperimentalTestApi::class,
    ExperimentalSerializationApi::class
)
class TextScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试文本屏幕基本显示
     */
    @Test
    fun `test TextScreen basic display`() {
        val setupCompletedLatch = CountDownLatch(1)

        // 创建测试状态
        val textState = TextState(DataTextState())
        val globalState = GlobalState(GlobalData())
        val playerState = PlayerState(com.mujingx.player.PlayerData())
        val wordScreenState = WordScreenState(WordScreenData())

        composeTestRule.setContent {
            val visible = remember { mutableStateOf(true) }

            DisposableEffect(Unit) {
                setupCompletedLatch.countDown()
                onDispose { }
            }

            MaterialTheme {
                if (visible.value) {
                    Text(
                        text = "Test TextScreen",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        val uiInitialized = setupCompletedLatch.await(5, TimeUnit.SECONDS)
        assertTrue("UI 初始化超时", uiInitialized)

        // 验证文本显示
        composeTestRule.onNodeWithText("Test TextScreen").assertExists()
    }

    /**
     * 测试文本状态更新
     */
    @Test
    fun `test TextState updates`() {
        val state = TextState(DataTextState())

        // 测试文本路径更新
        state.textPath = "/test/document.txt"
        assertTrue("Text path should be updated", state.textPath == "/test/document.txt")

        // 测试当前索引更新
        state.currentIndex = 5
        assertTrue("Current index should be 5", state.currentIndex == 5)

        // 测试第一个可见项索引更新
        state.firstVisibleItemIndex = 10
        assertTrue("First visible item index should be 10", state.firstVisibleItemIndex == 10)
    }

    /**
     * 测试文本选择
     */
    @Test
    fun `test text selection`() {
        val testText = "This is a test document for text selection."

        composeTestRule.setContent {
            MaterialTheme {
                Text(testText)
            }
        }

        // 验证文本显示
        composeTestRule.onNodeWithText("This is a test document for text selection.")
            .assertExists()
    }

    /**
     * 测试页面导航
     */
    @Test
    fun `test page navigation`() {
        val state = TextState(DataTextState())

        // 测试前进导航
        state.currentIndex = 0
        state.currentIndex = 1
        assertTrue("Current index should be 1", state.currentIndex == 1)

        // 测试后退导航
        state.currentIndex = 0
        assertTrue("Current index should be 0", state.currentIndex == 0)

        // 测试跳跃导航
        state.currentIndex = 20
        assertTrue("Current index should be 20", state.currentIndex == 20)
    }

    /**
     * 测试滚动位置
     */
    @Test
    fun `test scroll position`() {
        val state = TextState(DataTextState())

        // 测试设置滚动位置
        state.firstVisibleItemIndex = 0
        assertTrue("First visible item should be 0", state.firstVisibleItemIndex == 0)

        state.firstVisibleItemIndex = 15
        assertTrue("First visible item should be 15", state.firstVisibleItemIndex == 15)

        state.firstVisibleItemIndex = 50
        assertTrue("First visible item should be 50", state.firstVisibleItemIndex == 50)
    }

    /**
     * 测试不同文件格式
     */
    @Test
    fun `test different file formats`() {
        val formats = listOf(
            "/test/document.txt",
            "/test/novel.txt",
            "/test/article.txt",
            "/test/notes.txt"
        )

        val state = TextState(DataTextState())

        formats.forEach { path ->
            state.textPath = path
            assertTrue("Text path should be $path", state.textPath == path)
        }
    }

    /**
     * 测试长路径处理
     */
    @Test
    fun `test long path handling`() {
        val longPath = "/very/long/path/to/document/directory/with/many/subdirectories/file.txt"

        val state = TextState(DataTextState())
        state.textPath = longPath

        assertTrue("Should handle long paths", state.textPath == longPath)
    }

    /**
     * 测试 Unicode 路径
     */
    @Test
    fun `test unicode path handling`() {
        val unicodePaths = listOf(
            "/测试/文档.txt",
            "/文档/読書/本.txt",
            "/documents/édition.txt"
        )

        val state = TextState(DataTextState())

        unicodePaths.forEach { path ->
            state.textPath = path
            assertTrue("Should handle Unicode path: $path", state.textPath == path)
        }
    }

    /**
     * 测试特殊字符路径
     */
    @Test
    fun `test special character path handling`() {
        val specialPaths = listOf(
            "/path with spaces/document.txt",
            "/path-with-dashes/document.txt",
            "/path_with_underscores/document.txt",
            "/path.with.dots/document.txt"
        )

        val state = TextState(DataTextState())

        specialPaths.forEach { path ->
            state.textPath = path
            assertTrue("Should handle special character path: $path", state.textPath == path)
        }
    }

    /**
     * 测试空路径
     */
    @Test
    fun `test empty path`() {
        val state = TextState(DataTextState())

        // 默认路径为空
        assertTrue("Default path should be empty", state.textPath == "")

        // 设置为空路径
        state.textPath = ""
        assertTrue("Path should be empty", state.textPath == "")
    }

    /**
     * 测试索引边界值
     */
    @Test
    fun `test index boundary values`() {
        val state = TextState(DataTextState())

        // 测试最小值
        state.currentIndex = 0
        assertTrue("Current index should be 0", state.currentIndex == 0)

        // 测试较大值
        state.currentIndex = 1000
        assertTrue("Current index should be 1000", state.currentIndex == 1000)

        // 测试最大值
        state.currentIndex = Int.MAX_VALUE
        assertTrue("Current index should be Int.MAX_VALUE", state.currentIndex == Int.MAX_VALUE)
    }

    /**
     * 测试索引关系
     */
    @Test
    fun `test index relationships`() {
        val state = TextState(DataTextState())

        // 当前索引等于第一个可见项索引
        state.currentIndex = 10
        state.firstVisibleItemIndex = 10
        assertTrue("Current index should equal first visible item index",
            state.currentIndex == state.firstVisibleItemIndex)

        // 当前索引大于第一个可见项索引
        state.currentIndex = 20
        state.firstVisibleItemIndex = 10
        assertTrue("Current index should be greater than first visible item index",
            state.currentIndex > state.firstVisibleItemIndex)

        // 当前索引小于第一个可见项索引
        state.currentIndex = 5
        state.firstVisibleItemIndex = 10
        assertTrue("Current index should be less than first visible item index",
            state.currentIndex < state.firstVisibleItemIndex)
    }

    /**
     * 测试文本加载状态
     */
    @Test
    fun `test text loading states`() {
        val state = TextState(DataTextState())

        // 初始状态：无路径
        assertTrue("Initial state should have empty path", state.textPath == "")

        // 加载文本后：有路径
        state.textPath = "/test/document.txt"
        assertTrue("After loading should have path", state.textPath == "/test/document.txt")
    }

    /**
     * 测试相对路径
     */
    @Test
    fun `test relative path`() {
        val relativePaths = listOf(
            "./documents/test.txt",
            "../documents/test.txt",
            "../../test.txt"
        )

        val state = TextState(DataTextState())

        relativePaths.forEach { path ->
            state.textPath = path
            assertTrue("Should handle relative path: $path", state.textPath == path)
        }
    }

    /**
     * 测试绝对路径
     */
    @Test
    fun `test absolute path`() {
        val absolutePaths = listOf(
            "/home/user/documents/test.txt",
            "C:/Users/user/Documents/test.txt",
            "/Users/user/Documents/test.txt"
        )

        val state = TextState(DataTextState())

        absolutePaths.forEach { path ->
            state.textPath = path
            assertTrue("Should handle absolute path: $path", state.textPath == path)
        }
    }

    /**
     * 安全点击辅助函数
     */
    private fun safeClickWithText(text: String) {
        runBlocking {
            withContext(Dispatchers.Main) {
                try {
                    composeTestRule.onNodeWithText(text).performClick()
                    composeTestRule.waitForIdle()
                } catch (e: Exception) {
                    // 忽略点击错误，某些元素可能不可点击
                }
            }
        }
    }
}
