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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * CellVisibleState 单元测试
 * 测试词库编辑器列可见性状态管理
 */
@OptIn(ExperimentalSerializationApi::class)
class CellVisibleStateTest {

    private val jsonFormat = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    private lateinit var defaultCellVisible: CellVisible
    private lateinit var cellVisibleState: CellVisibleState

    @Before
    fun setUp() {
        defaultCellVisible = CellVisible()
        cellVisibleState = CellVisibleState(defaultCellVisible)
    }

    @Test
    fun `test default cell visible values`() {
        // 测试默认列可见性值
        assertTrue(cellVisibleState.translationVisible)
        assertTrue(cellVisibleState.definitionVisible)
        assertTrue(cellVisibleState.uKPhoneVisible)
        assertTrue(cellVisibleState.usPhoneVisible)
        assertTrue(cellVisibleState.exchangeVisible)
        assertTrue(cellVisibleState.captionsVisible)
        assertTrue(cellVisibleState.sentencesVisible)
    }

    @Test
    fun `test set translation visible updates state`() {
        // 测试设置翻译列可见性
        cellVisibleState.translationVisible = false
        assertFalse(cellVisibleState.translationVisible)

        cellVisibleState.translationVisible = true
        assertTrue(cellVisibleState.translationVisible)
    }

    @Test
    fun `test set definition visible updates state`() {
        // 测试设置定义列可见性
        cellVisibleState.definitionVisible = false
        assertFalse(cellVisibleState.definitionVisible)
    }

    @Test
    fun `test set UK phone visible updates state`() {
        // 测试设置英式音标列可见性
        cellVisibleState.uKPhoneVisible = false
        assertFalse(cellVisibleState.uKPhoneVisible)
    }

    @Test
    fun `test set US phone visible updates state`() {
        // 测试设置美式音标列可见性
        cellVisibleState.usPhoneVisible = false
        assertFalse(cellVisibleState.usPhoneVisible)
    }

    @Test
    fun `test set exchange visible updates state`() {
        // 测试设置词形变换列可见性
        cellVisibleState.exchangeVisible = false
        assertFalse(cellVisibleState.exchangeVisible)
    }

    @Test
    fun `test set captions visible updates state`() {
        // 测试设置字幕列可见性
        cellVisibleState.captionsVisible = false
        assertFalse(cellVisibleState.captionsVisible)
    }

    @Test
    fun `test set sentences visible updates state`() {
        // 测试设置例句列可见性
        cellVisibleState.sentencesVisible = false
        assertFalse(cellVisibleState.sentencesVisible)
    }

    @Test
    fun `test can hide all columns`() {
        // 测试可以隐藏所有列
        cellVisibleState.translationVisible = false
        cellVisibleState.definitionVisible = false
        cellVisibleState.uKPhoneVisible = false
        cellVisibleState.usPhoneVisible = false
        cellVisibleState.exchangeVisible = false
        cellVisibleState.captionsVisible = false
        cellVisibleState.sentencesVisible = false

        assertFalse(cellVisibleState.translationVisible)
        assertFalse(cellVisibleState.definitionVisible)
        assertFalse(cellVisibleState.uKPhoneVisible)
        assertFalse(cellVisibleState.usPhoneVisible)
        assertFalse(cellVisibleState.exchangeVisible)
        assertFalse(cellVisibleState.captionsVisible)
        assertFalse(cellVisibleState.sentencesVisible)
    }

    @Test
    fun `test can show all columns`() {
        // 测试可以显示所有列
        val state = CellVisibleState(CellVisible(
            translationVisible = false,
            definitionVisible = false,
            uKPhoneVisible = false,
            usPhoneVisible = false,
            exchangeVisible = false,
            captionsVisible = false,
            sentencesVisible = false
        ))

        state.translationVisible = true
        state.definitionVisible = true
        state.uKPhoneVisible = true
        state.usPhoneVisible = true
        state.exchangeVisible = true
        state.captionsVisible = true
        state.sentencesVisible = true

        assertTrue(state.translationVisible)
        assertTrue(state.definitionVisible)
        assertTrue(state.uKPhoneVisible)
        assertTrue(state.usPhoneVisible)
        assertTrue(state.exchangeVisible)
        assertTrue(state.captionsVisible)
        assertTrue(state.sentencesVisible)
    }

    @Test
    fun `test serialization of CellVisible`() {
        // 测试 CellVisible 序列化
        val cellVisible = CellVisible(
            translationVisible = true,
            definitionVisible = false,
            uKPhoneVisible = true,
            usPhoneVisible = false,
            exchangeVisible = true,
            captionsVisible = false,
            sentencesVisible = true
        )

        val json = jsonFormat.encodeToString(cellVisible)
        assertNotNull(json)
        assertTrue(json.contains("\"translationVisible\":true"))
        assertTrue(json.contains("\"definitionVisible\":false"))
        assertTrue(json.contains("\"uKPhoneVisible\":true"))
    }

    @Test
    fun `test deserialization of CellVisible`() {
        // 测试 CellVisible 反序列化
        val json = """
            {
                "translationVisible": false,
                "definitionVisible": true,
                "uKPhoneVisible": false,
                "usPhoneVisible": true,
                "exchangeVisible": false,
                "captionsVisible": true,
                "sentencesVisible": false
            }
        """.trimIndent()

        val cellVisible = Json.decodeFromString<CellVisible>(json)
        assertFalse(cellVisible.translationVisible)
        assertTrue(cellVisible.definitionVisible)
        assertFalse(cellVisible.uKPhoneVisible)
        assertTrue(cellVisible.usPhoneVisible)
        assertFalse(cellVisible.exchangeVisible)
        assertTrue(cellVisible.captionsVisible)
        assertFalse(cellVisible.sentencesVisible)
    }

    @Test
    fun `test serialization roundtrip`() {
        // 测试序列化往返
        val original = CellVisible(
            translationVisible = false,
            definitionVisible = true,
            uKPhoneVisible = false,
            usPhoneVisible = false,
            exchangeVisible = true,
            captionsVisible = true,
            sentencesVisible = false
        )

        val json = jsonFormat.encodeToString(original)
        val restored = Json.decodeFromString<CellVisible>(json)

        assertEquals(original.translationVisible, restored.translationVisible)
        assertEquals(original.definitionVisible, restored.definitionVisible)
        assertEquals(original.uKPhoneVisible, restored.uKPhoneVisible)
        assertEquals(original.usPhoneVisible, restored.usPhoneVisible)
        assertEquals(original.exchangeVisible, restored.exchangeVisible)
        assertEquals(original.captionsVisible, restored.captionsVisible)
        assertEquals(original.sentencesVisible, restored.sentencesVisible)
    }

    @Test
    fun `test empty state serialization`() {
        // 测试空状态序列化（使用默认值）
        val emptyState = CellVisible()
        val json = jsonFormat.encodeToString(emptyState)
        val restored = Json.decodeFromString<CellVisible>(json)

        assertTrue(restored.translationVisible)
        assertTrue(restored.definitionVisible)
        assertTrue(restored.uKPhoneVisible)
        assertTrue(restored.usPhoneVisible)
        assertTrue(restored.exchangeVisible)
        assertTrue(restored.captionsVisible)
        assertTrue(restored.sentencesVisible)
    }

    @Test
    fun `test partial visibility configuration`() {
        // 测试部分可见性配置
        val state = CellVisibleState(CellVisible(
            translationVisible = true,
            definitionVisible = true,
            uKPhoneVisible = false,
            usPhoneVisible = false,
            exchangeVisible = true,
            captionsVisible = false,
            sentencesVisible = true
        ))

        assertTrue(state.translationVisible)
        assertTrue(state.definitionVisible)
        assertFalse(state.uKPhoneVisible)
        assertFalse(state.usPhoneVisible)
        assertTrue(state.exchangeVisible)
        assertFalse(state.captionsVisible)
        assertTrue(state.sentencesVisible)
    }

    @Test
    fun `test only essential columns visible`() {
        // 测试只显示基本列
        val state = CellVisibleState(CellVisible(
            translationVisible = true,
            definitionVisible = false,
            uKPhoneVisible = false,
            usPhoneVisible = false,
            exchangeVisible = false,
            captionsVisible = false,
            sentencesVisible = false
        ))

        assertTrue(state.translationVisible)
        assertFalse(state.definitionVisible)
        assertFalse(state.uKPhoneVisible)
        assertFalse(state.usPhoneVisible)
        assertFalse(state.exchangeVisible)
        assertFalse(state.captionsVisible)
        assertFalse(state.sentencesVisible)
    }

    @Test
    fun `test state independence`() {
        // 测试状态独立性
        val state1 = CellVisibleState(CellVisible())
        val state2 = CellVisibleState(CellVisible())

        state1.translationVisible = false

        assertFalse(state1.translationVisible)
        assertTrue("state2 不应受 state1 影响", state2.translationVisible)
    }

    @Test
    fun `test multiple state updates`() {
        // 测试多次状态更新
        cellVisibleState.translationVisible = false
        cellVisibleState.definitionVisible = false
        cellVisibleState.translationVisible = true

        assertTrue(cellVisibleState.translationVisible)
        assertFalse(cellVisibleState.definitionVisible)
    }

    @Test
    fun `test json with pretty print`() {
        // 测试美化 JSON 输出
        val cellVisible = CellVisible(
            translationVisible = true,
            definitionVisible = false
        )

        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val json = encodeBuilder.encodeToString(cellVisible)

        assertNotNull(json)
        // 美化的 JSON 应该包含换行符
        assertTrue(json.contains("\n"))
    }

    @Test
    fun `test cell visible state with all false`() {
        // 测试所有列都隐藏的状态
        val state = CellVisibleState(CellVisible(
            translationVisible = false,
            definitionVisible = false,
            uKPhoneVisible = false,
            usPhoneVisible = false,
            exchangeVisible = false,
            captionsVisible = false,
            sentencesVisible = false
        ))

        // 验证所有列都是隐藏的
        val allHidden = !state.translationVisible &&
                       !state.definitionVisible &&
                       !state.uKPhoneVisible &&
                       !state.usPhoneVisible &&
                       !state.exchangeVisible &&
                       !state.captionsVisible &&
                       !state.sentencesVisible

        assertTrue("所有列应该都是隐藏的", allHidden)
    }

    @Test
    fun `test cell visible state with all true`() {
        // 测试所有列都显示的状态
        val state = CellVisibleState(CellVisible(
            translationVisible = true,
            definitionVisible = true,
            uKPhoneVisible = true,
            usPhoneVisible = true,
            exchangeVisible = true,
            captionsVisible = true,
            sentencesVisible = true
        ))

        // 验证所有列都是显示的
        val allVisible = state.translationVisible &&
                       state.definitionVisible &&
                       state.uKPhoneVisible &&
                       state.usPhoneVisible &&
                       state.exchangeVisible &&
                       state.captionsVisible &&
                       state.sentencesVisible

        assertTrue("所有列应该都是显示的", allVisible)
    }

    @Test
    fun `test toggle individual column visibility`() {
        // 测试切换单个列的可见性
        // 初始状态
        assertTrue(cellVisibleState.translationVisible)

        // 第一次切换
        cellVisibleState.translationVisible = !cellVisibleState.translationVisible
        assertFalse(cellVisibleState.translationVisible)

        // 第二次切换
        cellVisibleState.translationVisible = !cellVisibleState.translationVisible
        assertTrue(cellVisibleState.translationVisible)
    }
}
