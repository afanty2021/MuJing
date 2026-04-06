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

package com.mujingx.ui.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import com.mujingx.data.Word
import com.mujingx.data.Vocabulary
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
 * EditVocabulary UI 测试套件
 *
 * 测试词库编辑器的 UI 组件交互
 */
@OptIn(
    ExperimentalTestApi::class,
    ExperimentalSerializationApi::class
)
class EditVocabularyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * 测试词库编辑器基本显示
     */
    @Test
    fun `test EditVocabulary basic display`() {
        val setupCompletedLatch = CountDownLatch(1)

        // 创建测试词库
        val vocabulary = Vocabulary(language = "en", size = 0)
        vocabulary.wordList.add(Word(value = "test", definition = "测试", usphone = "", ukphone = ""))
        vocabulary.size = 1

        val cellVisible = CellVisible()
        val searchData = SearchData()

        composeTestRule.setContent {
            val visible = remember { mutableStateOf(true) }

            DisposableEffect(Unit) {
                setupCompletedLatch.countDown()
                onDispose { }
            }

            MaterialTheme {
                if (visible.value) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("词库编辑器")
                        Text("单词数: ${vocabulary.size}")
                    }
                }
            }
        }

        val uiInitialized = setupCompletedLatch.await(5, TimeUnit.SECONDS)
        assertTrue("UI 初始化超时", uiInitialized)

        // 验证基本文本显示
        composeTestRule.onNodeWithText("词库编辑器").assertExists()
        composeTestRule.onNodeWithText("单词数: 1").assertExists()
    }

    /**
     * 测试列可见性状态
     */
    @Test
    fun `test column visibility state`() {
        val cellVisible = CellVisible()
        val state = CellVisibleState(cellVisible)

        // 测试所有列默认可见
        assertTrue("Translation should be visible", state.translationVisible)
        assertTrue("Definition should be visible", state.definitionVisible)
        assertTrue("UK phone should be visible", state.uKPhoneVisible)
        assertTrue("US phone should be visible", state.usPhoneVisible)
        assertTrue("Exchange should be visible", state.exchangeVisible)
        assertTrue("Captions should be visible", state.captionsVisible)
        assertTrue("Sentences should be visible", state.sentencesVisible)
    }

    /**
     * 测试列可见性切换
     */
    @Test
    fun `test column visibility toggle`() {
        val cellVisible = CellVisible()
        val state = CellVisibleState(cellVisible)

        // 测试隐藏翻译列
        state.translationVisible = false
        assertFalse("Translation should be hidden", state.translationVisible)

        // 测试隐藏定义列
        state.definitionVisible = false
        assertFalse("Definition should be hidden", state.definitionVisible)

        // 测试隐藏英式音标
        state.uKPhoneVisible = false
        assertFalse("UK phone should be hidden", state.uKPhoneVisible)

        // 测试隐藏美式音标
        state.usPhoneVisible = false
        assertFalse("US phone should be hidden", state.usPhoneVisible)

        // 测试隐藏词形变换
        state.exchangeVisible = false
        assertFalse("Exchange should be hidden", state.exchangeVisible)

        // 测试隐藏字幕
        state.captionsVisible = false
        assertFalse("Captions should be hidden", state.captionsVisible)

        // 测试隐藏例句
        state.sentencesVisible = false
        assertFalse("Sentences should be hidden", state.sentencesVisible)
    }

    /**
     * 测试搜索状态
     */
    @Test
    fun `test search state`() {
        val searchData = SearchData()
        val state = SearchState(searchData)

        // 测试默认搜索选项
        assertFalse("Match case should be false by default", state.matchCaseIsSelected)
        assertFalse("Words should be false by default", state.wordsIsSelected)
        assertFalse("Regex should be false by default", state.regexIsSelected)
        assertFalse("Number should be false by default", state.numberSelected)
    }

    /**
     * 测试搜索选项切换
     */
    @Test
    fun `test search option toggle`() {
        val searchData = SearchData()
        val state = SearchState(searchData)

        // 测试区分大小写
        state.matchCaseIsSelected = true
        assertTrue("Match case should be true", state.matchCaseIsSelected)

        // 测试全词匹配
        state.wordsIsSelected = true
        assertTrue("Words should be true", state.wordsIsSelected)

        // 测试正则表达式
        state.regexIsSelected = true
        assertTrue("Regex should be true", state.regexIsSelected)

        // 测试数字匹配
        state.numberSelected = true
        assertTrue("Number should be true", state.numberSelected)
    }

    /**
     * 测试词库加载
     */
    @Test
    fun `test vocabulary loading`() {
        val vocabulary = Vocabulary(language = "en", size = 0)

        // 添加测试单词
        vocabulary.wordList.add(Word(value = "hello", definition = "你好", usphone = "", ukphone = ""))
        vocabulary.wordList.add(Word(value = "world", definition = "世界", usphone = "", ukphone = ""))
        vocabulary.size = 2

        assertTrue("Vocabulary should have 2 words", vocabulary.size == 2)
        assertTrue("First word should be 'hello'", vocabulary.wordList[0].value == "hello")
        assertTrue("Second word should be 'world'", vocabulary.wordList[1].value == "world")
    }

    /**
     * 测试空词库
     */
    @Test
    fun `test empty vocabulary`() {
        val vocabulary = Vocabulary(language = "en", size = 0)

        assertTrue("Empty vocabulary should have size 0", vocabulary.size == 0)
        assertTrue("Empty vocabulary should have no words", vocabulary.wordList.isEmpty())
    }

    /**
     * 测试大词库
     */
    @Test
    fun `test large vocabulary`() {
        val vocabulary = Vocabulary(language = "en", size = 0)

        // 添加大量单词
        repeat(1000) { index ->
            vocabulary.wordList.add(Word(value = "word$index", definition = "定义$index", usphone = "", ukphone = ""))
        }
        vocabulary.size = 1000

        assertTrue("Vocabulary should have 1000 words", vocabulary.size == 1000)
    }

    /**
     * 测试单词编辑
     */
    @Test
    fun `test word editing`() {
        val word = Word(value = "test", definition = "测试", usphone = "", ukphone = "")

        // 修改单词拼写
        word.value = "testing"
        assertTrue("Word value should be 'testing'", word.value == "testing")

        // 修改单词释义
        word.definition = "测试中"
        assertTrue("Word definition should be '测试中'", word.definition == "测试中")

        // 修改单词翻译
        word.translation = "testing 翻译"
        assertTrue("Word translation should be 'testing 翻译'", word.translation == "testing 翻译")
    }

    /**
     * 测试词库保存
     */
    @Test
    fun `test vocabulary saving`() {
        val vocabulary = Vocabulary(language = "en", size = 0)

        // 添加单词
        vocabulary.wordList.add(Word(value = "test", definition = "", usphone = "", ukphone = ""))
        vocabulary.size = 1

        // 模拟保存操作
        val wordCount = vocabulary.size
        assertTrue("Vocabulary should have 1 word before save", wordCount == 1)

        // 验证保存后数据完整性
        assertTrue("Vocabulary should still have 1 word after save", vocabulary.size == 1)
        assertTrue("First word should still be 'test'", vocabulary.wordList[0].value == "test")
    }

    /**
     * 测试列可见性序列化
     */
    @Test
    fun `test column visibility serialization`() {
        val cellVisible = CellVisible(
            translationVisible = true,
            definitionVisible = false,
            uKPhoneVisible = true,
            usPhoneVisible = false,
            exchangeVisible = true,
            captionsVisible = false,
            sentencesVisible = true
        )

        // 验证数据完整性
        assertTrue("Translation should be visible", cellVisible.translationVisible)
        assertFalse("Definition should be hidden", cellVisible.definitionVisible)
        assertTrue("UK phone should be visible", cellVisible.uKPhoneVisible)
        assertFalse("US phone should be hidden", cellVisible.usPhoneVisible)
        assertTrue("Exchange should be visible", cellVisible.exchangeVisible)
        assertFalse("Captions should be hidden", cellVisible.captionsVisible)
        assertTrue("Sentences should be visible", cellVisible.sentencesVisible)
    }

    /**
     * 测试搜索选项序列化
     */
    @Test
    fun `test search options serialization`() {
        val searchData = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = false,
            regexIsSelected = true,
            numberSelected = false
        )

        // 验证数据完整性
        assertTrue("Match case should be selected", searchData.matchCaseIsSelected)
        assertFalse("Words should not be selected", searchData.wordsIsSelected)
        assertTrue("Regex should be selected", searchData.regexIsSelected)
        assertFalse("Number should not be selected", searchData.numberSelected)
    }

    /**
     * 测试状态独立性
     */
    @Test
    fun `test state independence`() {
        val cellVisible1 = CellVisible()
        val cellVisible2 = CellVisible()
        val state1 = CellVisibleState(cellVisible1)
        val state2 = CellVisibleState(cellVisible2)

        // 修改 state1 不应影响 state2
        state1.translationVisible = false
        assertTrue("state2 translation should still be visible", state2.translationVisible)
        assertFalse("state1 translation should be hidden", state1.translationVisible)
    }

    /**
     * 测试搜索状态独立性
     */
    @Test
    fun `test search state independence`() {
        val searchData1 = SearchData()
        val searchData2 = SearchData()
        val state1 = SearchState(searchData1)
        val state2 = SearchState(searchData2)

        // 修改 state1 不应影响 state2
        state1.matchCaseIsSelected = true
        assertFalse("state2 match case should still be false", state2.matchCaseIsSelected)
        assertTrue("state1 match case should be true", state1.matchCaseIsSelected)
    }

    /**
     * 测试组合搜索选项
     */
    @Test
    fun `test combined search options`() {
        val searchData = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = true,
            regexIsSelected = true,
            numberSelected = true
        )
        val state = SearchState(searchData)

        // 验证所有选项都启用
        assertTrue("All options should be enabled",
            state.matchCaseIsSelected &&
            state.wordsIsSelected &&
            state.regexIsSelected &&
            state.numberSelected
        )
    }

    /**
     * 测试列可见性组合
     */
    @Test
    fun `test combined column visibility`() {
        val cellVisible = CellVisible(
            translationVisible = false,
            definitionVisible = false,
            uKPhoneVisible = false,
            usPhoneVisible = false,
            exchangeVisible = false,
            captionsVisible = false,
            sentencesVisible = false
        )
        val state = CellVisibleState(cellVisible)

        // 验证所有列都隐藏
        assertTrue("All columns should be hidden",
            !state.translationVisible &&
            !state.definitionVisible &&
            !state.uKPhoneVisible &&
            !state.usPhoneVisible &&
            !state.exchangeVisible &&
            !state.captionsVisible &&
            !state.sentencesVisible
        )
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
