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

/**
 * SearchState 单元测试
 * 测试词库编辑器搜索状态管理
 */
@OptIn(ExperimentalSerializationApi::class)
class SearchStateTest {

    private val jsonFormat = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    private lateinit var defaultSearchData: SearchData
    private lateinit var searchState: SearchState

    @Before
    fun setUp() {
        defaultSearchData = SearchData()
        searchState = SearchState(defaultSearchData)
    }

    @Test
    fun `test default search state values`() {
        // 测试默认搜索状态值
        assertFalse(searchState.matchCaseIsSelected)
        assertFalse(searchState.wordsIsSelected)
        assertFalse(searchState.regexIsSelected)
        assertFalse(searchState.numberSelected)
    }

    @Test
    fun `test set match case selected updates state`() {
        // 测试设置区分大小写选项
        searchState.matchCaseIsSelected = true
        assertTrue(searchState.matchCaseIsSelected)

        searchState.matchCaseIsSelected = false
        assertFalse(searchState.matchCaseIsSelected)
    }

    @Test
    fun `test set words selected updates state`() {
        // 测试设置全词匹配选项
        searchState.wordsIsSelected = true
        assertTrue(searchState.wordsIsSelected)
    }

    @Test
    fun `test set regex selected updates state`() {
        // 测试设置正则表达式选项
        searchState.regexIsSelected = true
        assertTrue(searchState.regexIsSelected)
    }

    @Test
    fun `test set number selected updates state`() {
        // 测试设置数字匹配选项
        searchState.numberSelected = true
        assertTrue(searchState.numberSelected)
    }

    @Test
    fun `test can enable all options`() {
        // 测试可以启用所有选项
        searchState.matchCaseIsSelected = true
        searchState.wordsIsSelected = true
        searchState.regexIsSelected = true
        searchState.numberSelected = true

        assertTrue(searchState.matchCaseIsSelected)
        assertTrue(searchState.wordsIsSelected)
        assertTrue(searchState.regexIsSelected)
        assertTrue(searchState.numberSelected)
    }

    @Test
    fun `test can disable all options`() {
        // 测试可以禁用所有选项
        val state = SearchState(SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = true,
            regexIsSelected = true,
            numberSelected = true
        ))

        state.matchCaseIsSelected = false
        state.wordsIsSelected = false
        state.regexIsSelected = false
        state.numberSelected = false

        assertFalse(state.matchCaseIsSelected)
        assertFalse(state.wordsIsSelected)
        assertFalse(state.regexIsSelected)
        assertFalse(state.numberSelected)
    }

    @Test
    fun `test serialization of SearchData`() {
        // 测试 SearchData 序列化
        val searchData = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = false,
            regexIsSelected = true,
            numberSelected = false
        )

        val json = jsonFormat.encodeToString(searchData)
        assertNotNull(json)
        assertTrue(json.contains("\"matchCaseIsSelected\":true"))
        assertTrue(json.contains("\"wordsIsSelected\":false"))
        assertTrue(json.contains("\"regexIsSelected\":true"))
    }

    @Test
    fun `test deserialization of SearchData`() {
        // 测试 SearchData 反序列化
        val json = """
            {
                "matchCaseIsSelected": false,
                "wordsIsSelected": true,
                "regexIsSelected": false,
                "numberSelected": true
            }
        """.trimIndent()

        val searchData = Json.decodeFromString<SearchData>(json)
        assertFalse(searchData.matchCaseIsSelected)
        assertTrue(searchData.wordsIsSelected)
        assertFalse(searchData.regexIsSelected)
        assertTrue(searchData.numberSelected)
    }

    @Test
    fun `test serialization roundtrip`() {
        // 测试序列化往返
        val original = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = true,
            regexIsSelected = false,
            numberSelected = false
        )

        val json = jsonFormat.encodeToString(original)
        val restored = Json.decodeFromString<SearchData>(json)

        assertEquals(original.matchCaseIsSelected, restored.matchCaseIsSelected)
        assertEquals(original.wordsIsSelected, restored.wordsIsSelected)
        assertEquals(original.regexIsSelected, restored.regexIsSelected)
        assertEquals(original.numberSelected, restored.numberSelected)
    }

    @Test
    fun `test empty state serialization`() {
        // 测试空状态序列化（使用默认值）
        val emptyState = SearchData()
        val json = jsonFormat.encodeToString(emptyState)
        val restored = Json.decodeFromString<SearchData>(json)

        assertFalse(restored.matchCaseIsSelected)
        assertFalse(restored.wordsIsSelected)
        assertFalse(restored.regexIsSelected)
        assertFalse(restored.numberSelected)
    }

    @Test
    fun `test partial search configuration`() {
        // 测试部分搜索配置
        val state = SearchState(SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = false,
            regexIsSelected = true,
            numberSelected = false
        ))

        assertTrue(state.matchCaseIsSelected)
        assertFalse(state.wordsIsSelected)
        assertTrue(state.regexIsSelected)
        assertFalse(state.numberSelected)
    }

    @Test
    fun `test match case only configuration`() {
        // 测试仅区分大小写配置
        val state = SearchState(SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = false,
            regexIsSelected = false,
            numberSelected = false
        ))

        assertTrue(state.matchCaseIsSelected)
        assertFalse(state.wordsIsSelected)
        assertFalse(state.regexIsSelected)
        assertFalse(state.numberSelected)
    }

    @Test
    fun `test words only configuration`() {
        // 测试仅全词匹配配置
        val state = SearchState(SearchData(
            matchCaseIsSelected = false,
            wordsIsSelected = true,
            regexIsSelected = false,
            numberSelected = false
        ))

        assertFalse(state.matchCaseIsSelected)
        assertTrue(state.wordsIsSelected)
        assertFalse(state.regexIsSelected)
        assertFalse(state.numberSelected)
    }

    @Test
    fun `test regex only configuration`() {
        // 测试仅正则表达式配置
        val state = SearchState(SearchData(
            matchCaseIsSelected = false,
            wordsIsSelected = false,
            regexIsSelected = true,
            numberSelected = false
        ))

        assertFalse(state.matchCaseIsSelected)
        assertFalse(state.wordsIsSelected)
        assertTrue(state.regexIsSelected)
        assertFalse(state.numberSelected)
    }

    @Test
    fun `test number only configuration`() {
        // 测试仅数字匹配配置
        val state = SearchState(SearchData(
            matchCaseIsSelected = false,
            wordsIsSelected = false,
            regexIsSelected = false,
            numberSelected = true
        ))

        assertFalse(state.matchCaseIsSelected)
        assertFalse(state.wordsIsSelected)
        assertFalse(state.regexIsSelected)
        assertTrue(state.numberSelected)
    }

    @Test
    fun `test state independence`() {
        // 测试状态独立性
        val state1 = SearchState(SearchData())
        val state2 = SearchState(SearchData())

        state1.matchCaseIsSelected = true

        assertTrue(state1.matchCaseIsSelected)
        assertFalse("state2 不应受 state1 影响", state2.matchCaseIsSelected)
    }

    @Test
    fun `test multiple state updates`() {
        // 测试多次状态更新
        searchState.matchCaseIsSelected = true
        searchState.wordsIsSelected = false
        searchState.matchCaseIsSelected = false

        assertFalse(searchState.matchCaseIsSelected)
        assertFalse(searchState.wordsIsSelected)
    }

    @Test
    fun `test toggle match case option`() {
        // 测试切换区分大小写选项
        // 初始状态
        assertFalse(searchState.matchCaseIsSelected)

        // 第一次切换
        searchState.matchCaseIsSelected = !searchState.matchCaseIsSelected
        assertTrue(searchState.matchCaseIsSelected)

        // 第二次切换
        searchState.matchCaseIsSelected = !searchState.matchCaseIsSelected
        assertFalse(searchState.matchCaseIsSelected)
    }

    @Test
    fun `test toggle words option`() {
        // 测试切换全词匹配选项
        searchState.wordsIsSelected = !searchState.wordsIsSelected
        assertTrue(searchState.wordsIsSelected)

        searchState.wordsIsSelected = !searchState.wordsIsSelected
        assertFalse(searchState.wordsIsSelected)
    }

    @Test
    fun `test toggle regex option`() {
        // 测试切换正则表达式选项
        searchState.regexIsSelected = !searchState.regexIsSelected
        assertTrue(searchState.regexIsSelected)

        searchState.regexIsSelected = !searchState.regexIsSelected
        assertFalse(searchState.regexIsSelected)
    }

    @Test
    fun `test toggle number option`() {
        // 测试切换数字匹配选项
        searchState.numberSelected = !searchState.numberSelected
        assertTrue(searchState.numberSelected)

        searchState.numberSelected = !searchState.numberSelected
        assertFalse(searchState.numberSelected)
    }

    @Test
    fun `test search data with all options enabled`() {
        // 测试所有选项启用的搜索数据
        val searchData = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = true,
            regexIsSelected = true,
            numberSelected = true
        )

        val json = jsonFormat.encodeToString(searchData)
        val restored = Json.decodeFromString<SearchData>(json)

        assertTrue(restored.matchCaseIsSelected)
        assertTrue(restored.wordsIsSelected)
        assertTrue(restored.regexIsSelected)
        assertTrue(restored.numberSelected)
    }

    @Test
    fun `test search data with all options disabled`() {
        // 测试所有选项禁用的搜索数据
        val searchData = SearchData(
            matchCaseIsSelected = false,
            wordsIsSelected = false,
            regexIsSelected = false,
            numberSelected = false
        )

        val json = jsonFormat.encodeToString(searchData)
        val restored = Json.decodeFromString<SearchData>(json)

        assertFalse(restored.matchCaseIsSelected)
        assertFalse(restored.wordsIsSelected)
        assertFalse(restored.regexIsSelected)
        assertFalse(restored.numberSelected)
    }

    @Test
    fun `test json with pretty print`() {
        // 测试美化 JSON 输出
        val searchData = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = false
        )

        val encodeBuilder = Json {
            prettyPrint = true
            encodeDefaults = true
        }
        val json = encodeBuilder.encodeToString(searchData)

        assertNotNull(json)
        // 美化的 JSON 应该包含换行符
        assertTrue(json.contains("\n"))
    }

    @Test
    fun `test combined search options`() {
        // 测试组合搜索选项
        val state = SearchState(SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = true
        ))

        assertTrue("应该同时启用区分大小写和全词匹配", state.matchCaseIsSelected && state.wordsIsSelected)
    }

    @Test
    fun `test exclusive search options`() {
        // 测试互斥搜索选项（正则表达式通常与其他选项互斥）
        val state1 = SearchState(SearchData(
            regexIsSelected = true,
            wordsIsSelected = true
        ))

        // 虽然代码允许同时启用，但实际使用时可能有互斥逻辑
        assertTrue(state1.regexIsSelected)
        assertTrue(state1.wordsIsSelected)
    }

    @Test
    fun `test search data immutability`() {
        // 测试 SearchData 不可变性
        val original = SearchData(
            matchCaseIsSelected = true,
            wordsIsSelected = false
        )

        // 创建新的 SearchData
        val modified = original.copy(
            wordsIsSelected = true
        )

        // 原始数据不应改变
        assertTrue(original.matchCaseIsSelected)
        assertFalse(original.wordsIsSelected)

        // 新数据应该反映修改
        assertTrue(modified.matchCaseIsSelected)
        assertTrue(modified.wordsIsSelected)
    }
}
