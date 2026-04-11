/*
 * MuJing: 幕境 - Contextual Vocabulary Learning
 * Copyright (C) 2025 MuJing Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mujingx.data

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ClipRepository 测试
 *
 * 测试 Clip 数据仓库的基本功能：
 * - 保存和查询片段
 * - 删除片段
 */
class ClipRepositoryTest {

    private lateinit var repository: ClipRepository
    private lateinit var tempDbFile: File

    /**
     * 设置测试环境
     *
     * 创建临时 SQLite 数据库文件用于测试。
     */
    @BeforeEach
    fun setup() {
        // 创建临时数据库文件
        tempDbFile = File.createTempFile("clip_repository_test_", ".db")
        repository = ClipRepository(tempDbFile.absolutePath)
    }

    /**
     * 清理测试环境
     *
     * 关闭数据库连接并删除临时文件。
     */
    @AfterEach
    fun cleanup() {
        repository.close()
        if (tempDbFile.exists()) {
            tempDbFile.delete()
        }
    }

    @Test
    fun `save and retrieve clip`() {
        // Arrange
        val clip = Clip(
            id = "test-clip-1",
            videoPath = "/path/to/video.mp4",
            startTime = 1000L,
            endTime = 5000L,
            subtitleText = "Hello, world!",
            translatedText = "你好，世界！",
            note = "Test note",
            tags = mutableListOf("greeting", "test"),
            createdAt = System.currentTimeMillis(),
            videoClipPath = "/path/to/clip.mp4"
        )

        // Act
        repository.saveClip(clip)
        val retrievedClips = repository.getClipsByVideo("/path/to/video.mp4")

        // Assert
        assertEquals(1, retrievedClips.size, "应该返回 1 个片段")
        val retrievedClip = retrievedClips[0]
        assertEquals(clip.id, retrievedClip.id, "片段 ID 应该匹配")
        assertEquals(clip.videoPath, retrievedClip.videoPath, "视频路径应该匹配")
        assertEquals(clip.startTime, retrievedClip.startTime, "开始时间应该匹配")
        assertEquals(clip.endTime, retrievedClip.endTime, "结束时间应该匹配")
        assertEquals(clip.subtitleText, retrievedClip.subtitleText, "字幕文本应该匹配")
        assertEquals(clip.translatedText, retrievedClip.translatedText, "翻译文本应该匹配")
        assertEquals(clip.note, retrievedClip.note, "备注应该匹配")
        assertEquals(clip.videoClipPath, retrievedClip.videoClipPath, "片段路径应该匹配")
    }

    @Test
    fun `delete clip removes it`() {
        // Arrange
        val clip = Clip(
            id = "test-clip-2",
            videoPath = "/path/to/video2.mp4",
            startTime = 2000L,
            endTime = 6000L,
            subtitleText = "Test subtitle",
            translatedText = null,
            note = null,
            tags = mutableListOf(),
            createdAt = System.currentTimeMillis(),
            videoClipPath = null
        )

        // Act
        repository.saveClip(clip)
        repository.deleteClip(clip.id)
        val retrievedClips = repository.getClipsByVideo("/path/to/video2.mp4")

        // Assert
        assertTrue(retrievedClips.isEmpty(), "删除后列表应该为空")
    }
}
