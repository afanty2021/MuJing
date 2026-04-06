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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.awt.Rectangle
import java.io.File

/**
 * SubtitlesState 单元测试
 * 测试字幕状态管理类的核心功能
 */
@OptIn(ExperimentalSerializationApi::class)
class SubtitlesStateTest {

    private val jsonFormat = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    private lateinit var defaultState: DataSubtitlesState
    private lateinit var subtitlesState: SubtitlesState

    @Before
    fun setUp() {
        defaultState = DataSubtitlesState()
        subtitlesState = SubtitlesState(defaultState)
    }

    @Test
    fun `test default state values`() {
        // 测试默认状态值
        assertEquals("", subtitlesState.mediaPath)
        assertEquals("", subtitlesState.subtitlesPath)
        assertEquals(0, subtitlesState.trackID)
        assertEquals("", subtitlesState.trackDescription)
        assertEquals(0, subtitlesState.trackSize)
        assertEquals(0, subtitlesState.currentIndex)
        assertEquals(0, subtitlesState.firstVisibleItemIndex)
        assertEquals(0, subtitlesState.sentenceMaxLength)
        assertFalse(subtitlesState.transcriptionCaption)
        assertTrue(subtitlesState.currentCaptionVisible)
        assertTrue(subtitlesState.notWroteCaptionVisible)
        assertTrue(subtitlesState.externalSubtitlesVisible)
    }

    @Test
    fun `test set media path updates state`() {
        // 测试设置媒体路径
        val testPath = "/path/to/video.mp4"
        subtitlesState.mediaPath = testPath
        assertEquals(testPath, subtitlesState.mediaPath)
    }

    @Test
    fun `test set subtitles path updates state`() {
        // 测试设置字幕路径
        val testPath = "/path/to/subtitles.srt"
        subtitlesState.subtitlesPath = testPath
        assertEquals(testPath, subtitlesState.subtitlesPath)
    }

    @Test
    fun `test set track ID updates state`() {
        // 测试设置轨道 ID
        subtitlesState.trackID = 1
        assertEquals(1, subtitlesState.trackID)
    }

    @Test
    fun `test set track ID to -1 for external subtitles`() {
        // 测试设置轨道 ID 为 -1（外部字幕）
        subtitlesState.trackID = -1
        assertEquals(-1, subtitlesState.trackID)
    }

    @Test
    fun `test set current index updates state`() {
        // 测试设置当前索引
        subtitlesState.currentIndex = 5
        assertEquals(5, subtitlesState.currentIndex)
    }

    @Test
    fun `test set first visible item index updates state`() {
        // 测试设置第一个可见项索引
        subtitlesState.firstVisibleItemIndex = 10
        assertEquals(10, subtitlesState.firstVisibleItemIndex)
    }

    @Test
    fun `test set sentence max length updates state`() {
        // 测试设置句子最大长度
        subtitlesState.sentenceMaxLength = 100
        assertEquals(100, subtitlesState.sentenceMaxLength)
    }

    @Test
    fun `test toggle transcription caption`() {
        // 测试切换抄写字幕显示
        subtitlesState.transcriptionCaption = true
        assertTrue(subtitlesState.transcriptionCaption)

        subtitlesState.transcriptionCaption = false
        assertFalse(subtitlesState.transcriptionCaption)
    }

    @Test
    fun `test toggle current caption visible`() {
        // 测试切换当前字幕可见性
        subtitlesState.currentCaptionVisible = false
        assertFalse(subtitlesState.currentCaptionVisible)

        subtitlesState.currentCaptionVisible = true
        assertTrue(subtitlesState.currentCaptionVisible)
    }

    @Test
    fun `test toggle not wrote caption visible`() {
        // 测试切换未抄写字幕可见性
        subtitlesState.notWroteCaptionVisible = false
        assertFalse(subtitlesState.notWroteCaptionVisible)

        subtitlesState.notWroteCaptionVisible = true
        assertTrue(subtitlesState.notWroteCaptionVisible)
    }

    @Test
    fun `test toggle external subtitles visible`() {
        // 测试切换外部字幕可见性
        subtitlesState.externalSubtitlesVisible = false
        assertFalse(subtitlesState.externalSubtitlesVisible)

        subtitlesState.externalSubtitlesVisible = true
        assertTrue(subtitlesState.externalSubtitlesVisible)
    }

    @Test
    fun `test set video bounds`() {
        // 测试设置视频边界（使用 Rectangle）
        val testRectangle = Rectangle(100, 200, 1280, 720)
        subtitlesState.videoBounds = testRectangle

        assertEquals(100, subtitlesState.videoBounds.x)
        assertEquals(200, subtitlesState.videoBounds.y)
        assertEquals(1280, subtitlesState.videoBounds.width)
        assertEquals(720, subtitlesState.videoBounds.height)
    }

    @Test
    fun `test serialization of DataSubtitlesState`() {
        // 测试 DataSubtitlesState 序列化
        val state = DataSubtitlesState(
            videoPath = "/test/video.mp4",
            subtitlesPath = "/test/subtitles.srt",
            trackID = 2,
            trackDescription = "English",
            trackSize = 1000,
            currentIndex = 5,
            firstVisibleItemIndex = 3,
            sentenceMaxLength = 80,
            transcriptionCaption = true,
            currentCaptionVisible = false,
            notWroteCaptionVisible = true,
            externalSubtitlesVisible = false,
            videoX = 100,
            videoY = 100,
            videoWidth = 1920,
            videoHeight = 1080
        )

        val json = jsonFormat.encodeToString(state)
        assertNotNull(json)
        assertTrue(json.contains("\"videoPath\":\"/test/video.mp4\""))
        assertTrue(json.contains("\"trackID\":2"))
        assertTrue(json.contains("\"currentIndex\":5"))
    }

    @Test
    fun `test deserialization of DataSubtitlesState`() {
        // 测试 DataSubtitlesState 反序列化
        val json = """
            {
                "videoPath": "/test/video.mp4",
                "subtitlesPath": "/test/subtitles.srt",
                "trackID": 2,
                "trackDescription": "English",
                "trackSize": 1000,
                "currentIndex": 5,
                "firstVisibleItemIndex": 3,
                "sentenceMaxLength": 80,
                "transcriptionCaption": true,
                "currentCaptionVisible": false,
                "notWroteCaptionVisible": true,
                "externalSubtitlesVisible": false,
                "videoX": 100,
                "videoY": 100,
                "videoWidth": 1920,
                "videoHeight": 1080
            }
        """.trimIndent()

        val state = Json.decodeFromString<DataSubtitlesState>(json)
        assertEquals("/test/video.mp4", state.videoPath)
        assertEquals("/test/subtitles.srt", state.subtitlesPath)
        assertEquals(2, state.trackID)
        assertEquals("English", state.trackDescription)
        assertEquals(1000, state.trackSize)
        assertEquals(5, state.currentIndex)
        assertEquals(3, state.firstVisibleItemIndex)
        assertEquals(80, state.sentenceMaxLength)
        assertTrue(state.transcriptionCaption)
        assertFalse(state.currentCaptionVisible)
        assertTrue(state.notWroteCaptionVisible)
        assertFalse(state.externalSubtitlesVisible)
        assertEquals(100, state.videoX)
        assertEquals(100, state.videoY)
        assertEquals(1920, state.videoWidth)
        assertEquals(1080, state.videoHeight)
    }

    @Test
    fun `test serialization roundtrip`() {
        // 测试序列化往返
        val original = DataSubtitlesState(
            videoPath = "/test/video.mkv",
            subtitlesPath = "/test/sub.srt",
            trackID = 1,
            trackDescription = "测试字幕",
            trackSize = 500,
            currentIndex = 10,
            firstVisibleItemIndex = 5
        )

        val json = jsonFormat.encodeToString(original)
        val restored = Json.decodeFromString<DataSubtitlesState>(json)

        assertEquals(original.videoPath, restored.videoPath)
        assertEquals(original.subtitlesPath, restored.subtitlesPath)
        assertEquals(original.trackID, restored.trackID)
        assertEquals(original.trackDescription, restored.trackDescription)
        assertEquals(original.trackSize, restored.trackSize)
        assertEquals(original.currentIndex, restored.currentIndex)
        assertEquals(original.firstVisibleItemIndex, restored.firstVisibleItemIndex)
    }

    @Test
    fun `test empty state serialization`() {
        // 测试空状态序列化
        val emptyState = DataSubtitlesState()
        val json = jsonFormat.encodeToString(emptyState)
        val restored = Json.decodeFromString<DataSubtitlesState>(json)

        assertEquals("", restored.videoPath)
        assertEquals("", restored.subtitlesPath)
        assertEquals(0, restored.trackID)
        assertEquals(0, restored.currentIndex)
    }

    @Test
    fun `test state with unicode characters`() {
        // 测试包含 Unicode 字符的状态
        val state = DataSubtitlesState(
            videoPath = "/测试/视频.mp4",
            trackDescription = "中文字幕 🎬",
            currentIndex = 1
        )

        val json = jsonFormat.encodeToString(state)
        val restored = Json.decodeFromString<DataSubtitlesState>(json)

        assertEquals("/测试/视频.mp4", restored.videoPath)
        assertEquals("中文字幕 🎬", restored.trackDescription)
    }

    @Test
    fun `test state with special path characters`() {
        // 测试包含特殊路径字符的状态
        val state = DataSubtitlesState(
            videoPath = "C:/Users/Test/[Video]/video.mkv",
            subtitlesPath = "/home/user/sub's subtitles.srt"
        )

        val json = jsonFormat.encodeToString(state)
        val restored = Json.decodeFromString<DataSubtitlesState>(json)

        assertEquals("C:/Users/Test/[Video]/video.mkv", restored.videoPath)
        assertEquals("/home/user/sub's subtitles.srt", restored.subtitlesPath)
    }

    @Test
    fun `test track ID zero means first track`() {
        // 测试轨道 ID 为 0 表示第一个轨道
        val state = SubtitlesState(DataSubtitlesState(trackID = 0))
        assertEquals(0, state.trackID)
        // trackID = 0 表示使用第一个内置轨道
    }

    @Test
    fun `test negative track ID values`() {
        // 测试负数轨道 ID 值
        val state = SubtitlesState(DataSubtitlesState(trackID = -5))
        assertEquals(-5, state.trackID)
        // 负数表示使用外部字幕
    }

    @Test
    fun `test large index values`() {
        // 测试大索引值
        val state = SubtitlesState(DataSubtitlesState(
            currentIndex = Int.MAX_VALUE,
            firstVisibleItemIndex = Int.MAX_VALUE - 10
        ))

        assertEquals(Int.MAX_VALUE, state.currentIndex)
        assertEquals(Int.MAX_VALUE - 10, state.firstVisibleItemIndex)
    }

    @Test
    fun `test zero sentence max length`() {
        // 测试句子最大长度为 0
        subtitlesState.sentenceMaxLength = 0
        assertEquals(0, subtitlesState.sentenceMaxLength)
    }

    @Test
    fun `test all visibility flags default to true`() {
        // 测试所有可见性标志默认为 true
        val state = SubtitlesState(DataSubtitlesState())

        assertTrue("当前字幕默认可见", state.currentCaptionVisible)
        assertTrue("未抄写字幕默认可见", state.notWroteCaptionVisible)
        assertTrue("外部字幕默认可见", state.externalSubtitlesVisible)
    }

    @Test
    fun `test can set all visibility flags independently`() {
        // 测试可以独立设置所有可见性标志
        subtitlesState.currentCaptionVisible = false
        subtitlesState.notWroteCaptionVisible = true
        subtitlesState.externalSubtitlesVisible = false

        assertFalse(subtitlesState.currentCaptionVisible)
        assertTrue(subtitlesState.notWroteCaptionVisible)
        assertFalse(subtitlesState.externalSubtitlesVisible)
    }
}
