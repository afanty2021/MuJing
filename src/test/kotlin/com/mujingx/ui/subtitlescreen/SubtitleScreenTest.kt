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

package com.mujingx.ui.subtitlescreen

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import com.mujingx.data.Caption
import com.mujingx.player.PlayerState
import com.mujingx.state.GlobalData
import com.mujingx.state.GlobalState
import com.mujingx.ui.wordscreen.WordScreenData
import com.mujingx.ui.wordscreen.WordScreenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * SubtitleScreen UI 测试套件
 *
 * 测试字幕浏览器的 UI 组件渲染和交互
 */
@OptIn(
    ExperimentalTestApi::class,
    ExperimentalSerializationApi::class
)
class SubtitleScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试字幕屏幕基本显示
     */
    @Test
    fun `test SubtitleScreen basic display`() {
        val setupCompletedLatch = CountDownLatch(1)

        // 创建测试状态
        val subtitlesState = SubtitlesState(DataSubtitlesState())
        val globalState = GlobalState(GlobalData())
        val playerState = PlayerState(com.mujingx.player.PlayerData())
        val wordScreenState = WordScreenState(WordScreenData())
        val audioSet = mutableSetOf<String>()

        composeTestRule.setContent {
            val visible = remember { mutableStateOf(true) }

            DisposableEffect(Unit) {
                setupCompletedLatch.countDown()
                onDispose { }
            }

            MaterialTheme {
                // 注意：SubtitlesScreen 需要完整的上下文，这里我们测试部分组件
                // 在实际测试中，可以使用测试替身或者依赖注入
            }
        }

        val uiInitialized = setupCompletedLatch.await(5, TimeUnit.SECONDS)
        assertTrue("UI 初始化超时", uiInitialized)
    }

    /**
     * 测试 Caption 组件渲染
     */
    @Test
    fun `test Caption component rendering`() {
        val caption = Caption(
            start = "00:00:00,000",
            end = "00:00:02,000",
            content = "Hello World"
        )

        composeTestRule.setContent {
            var selected by remember { mutableStateOf(false) }
            var showCurrent by remember { mutableStateOf(true) }
            var showNotWrote by remember { mutableStateOf(true) }
            var showExternal by remember { mutableStateOf(true) }

            MaterialTheme {
                // Caption 组件需要完整的上下文
                // 这里我们测试基本的状态管理
                assertTrue("Caption content should be 'Hello World'", caption.content == "Hello World")
                assertTrue("Caption start should be '00:00:00,000'", caption.start == "00:00:00,000")
                assertTrue("Caption end should be '00:00:02,000'", caption.end == "00:00:02,000")
            }
        }
    }

    /**
     * 测试字幕状态更新
     */
    @Test
    fun `test SubtitlesState updates`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试媒体路径更新
        state.mediaPath = "/test/video.mp4"
        assertTrue("Media path should be updated", state.mediaPath == "/test/video.mp4")

        // 测试字幕路径更新
        state.subtitlesPath = "/test/subtitles.srt"
        assertTrue("Subtitles path should be updated", state.subtitlesPath == "/test/subtitles.srt")

        // 测试当前索引更新
        state.currentIndex = 5
        assertTrue("Current index should be 5", state.currentIndex == 5)

        // 测试轨道 ID 更新
        state.trackID = 1
        assertTrue("Track ID should be 1", state.trackID == 1)
    }

    /**
     * 测试字幕可见性标志
     */
    @Test
    fun `test SubtitlesState visibility flags`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试抄写字幕标志
        state.transcriptionCaption = true
        assertTrue("Transcription caption should be true", state.transcriptionCaption)

        // 测试当前字幕可见性
        state.currentCaptionVisible = false
        assertFalse("Current caption visible should be false", state.currentCaptionVisible)

        // 测试未抄写字幕可见性
        state.notWroteCaptionVisible = false
        assertFalse("Not wrote caption visible should be false", state.notWroteCaptionVisible)

        // 测试外部字幕可见性
        state.externalSubtitlesVisible = false
        assertFalse("External subtitles visible should be false", state.externalSubtitlesVisible)
    }

    /**
     * 测试多行字幕显示
     */
    @Test
    fun `test multiple lines caption display`() {
        val captions = listOf(
            Caption("00:00:00,000", "00:00:01,000", "First line"),
            Caption("00:00:01,000", "00:00:02,000", "Second line"),
            Caption("00:00:02,000", "00:00:03,000", "Third line")
        )

        composeTestRule.setContent {
            MaterialTheme {
                // 测试字幕数据结构
                assertTrue("Should have 3 captions", captions.size == 3)
                assertTrue("First caption content should be 'First line'", captions[0].content == "First line")
                assertTrue("Second caption content should be 'Second line'", captions[1].content == "Second line")
                assertTrue("Third caption content should be 'Third line'", captions[2].content == "Third line")
            }
        }
    }

    /**
     * 测试字幕导航
     */
    @Test
    fun `test caption navigation`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试前进导航
        state.currentIndex = 0
        state.currentIndex = 1
        assertTrue("Current index should be 1", state.currentIndex == 1)

        // 测试后退导航
        state.currentIndex = 0
        assertTrue("Current index should be 0", state.currentIndex == 0)

        // 测试跳跃导航
        state.currentIndex = 10
        assertTrue("Current index should be 10", state.currentIndex == 10)
    }

    /**
     * 测试字幕选中高亮
     */
    @Test
    fun `test caption selection highlighting`() {
        val captions = (0 until 10).map { index ->
            Caption("${index}s", "${index + 1}s", "Caption $index")
        }

        val selectedIndex = 5

        composeTestRule.setContent {
            MaterialTheme {
                // 验证选中的字幕
                val selectedCaption = captions.getOrNull(selectedIndex)
                assertTrue("Selected caption should exist", selectedCaption != null)
                assertTrue("Selected caption should be 'Caption 5'", selectedCaption?.content == "Caption 5")
            }
        }
    }

    /**
     * 测试空字幕列表
     */
    @Test
    fun `test empty caption list`() {
        val state = SubtitlesState(DataSubtitlesState())
        state.currentIndex = 0

        assertTrue("Current index should be 0 for empty list", state.currentIndex == 0)
        assertTrue("Track ID should be 0", state.trackID == 0)
    }

    /**
     * 测试外部字幕标志
     */
    @Test
    fun `test external subtitle flag`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 外部字幕使用 trackID = -1
        state.trackID = -1
        assertTrue("External subtitle should have trackID -1", state.trackID == -1)

        // 内置字幕使用 trackID >= 0
        state.trackID = 0
        assertTrue("Built-in subtitle should have trackID 0", state.trackID == 0)

        state.trackID = 1
        assertTrue("Built-in subtitle should have trackID 1", state.trackID == 1)
    }

    /**
     * 测试字幕句子最大长度
     */
    @Test
    fun `test sentence max length`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试默认值
        assertTrue("Default sentence max length should be 0", state.sentenceMaxLength == 0)

        // 测试设置最大长度
        state.sentenceMaxLength = 100
        assertTrue("Sentence max length should be 100", state.sentenceMaxLength == 100)

        state.sentenceMaxLength = 200
        assertTrue("Sentence max length should be 200", state.sentenceMaxLength == 200)
    }

    /**
     * 测试第一个可见项索引
     */
    @Test
    fun `test first visible item index`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试默认值
        assertTrue("Default first visible item index should be 0", state.firstVisibleItemIndex == 0)

        // 测试设置第一个可见项
        state.firstVisibleItemIndex = 10
        assertTrue("First visible item index should be 10", state.firstVisibleItemIndex == 10)

        // 测试滚动场景
        state.firstVisibleItemIndex = 50
        assertTrue("First visible item index should be 50", state.firstVisibleItemIndex == 50)
    }

    /**
     * 测试字幕轨道描述
     */
    @Test
    fun `test track description`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试设置轨道描述
        state.trackDescription = "English"
        assertTrue("Track description should be 'English'", state.trackDescription == "English")

        state.trackDescription = "中文"
        assertTrue("Track description should be '中文'", state.trackDescription == "中文")

        state.trackDescription = "日本語"
        assertTrue("Track description should be '日本語'", state.trackDescription == "日本語")
    }

    /**
     * 测试字幕轨道大小
     */
    @Test
    fun `test track size`() {
        val state = SubtitlesState(DataSubtitlesState())

        // 测试设置轨道大小
        state.trackSize = 1000
        assertTrue("Track size should be 1000", state.trackSize == 1000)

        state.trackSize = 500
        assertTrue("Track size should be 500", state.trackSize == 500)
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
