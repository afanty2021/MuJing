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

package com.mujingx.subtitle

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * 字幕同步服务测试
 *
 * 测试字幕时间偏移的各种场景，包括全局偏移和单条字幕偏移。
 */
class SubtitleSyncServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var testDbPath: String
    private lateinit var service: SubtitleSyncService

    private val videoPath = "/path/to/video.mkv"
    private val anotherVideoPath = "/path/to/another/video.mkv"

    /**
     * 每个测试前创建临时数据库和服务实例
     */
    @BeforeEach
    fun setUp() {
        testDbPath = tempDir.resolve("test_subtitle_sync.db").toString()
        service = SubtitleSyncServiceImpl(testDbPath)
    }

    /**
     * 每个测试后清理资源
     */
    @AfterEach
    fun tearDown() {
        // 临时目录会自动清理
    }

    /**
     * 测试：全局偏移默认值为零
     */
    @Test
    fun `global offset defaults to zero`() {
        val offset = service.getGlobalOffset(videoPath)

        assertEquals(0L, offset, "全局偏移量默认值应为 0")
    }

    /**
     * 测试：设置和获取全局偏移
     */
    @Test
    fun `set and get global offset`() {
        service.setGlobalOffset(videoPath, 5000L)

        val offset = service.getGlobalOffset(videoPath)

        assertEquals(5000L, offset, "应返回设置的全局偏移量")

        // 验证不同视频的偏移量独立
        service.setGlobalOffset(anotherVideoPath, -2000L)

        assertEquals(5000L, service.getGlobalOffset(videoPath), "第一个视频的偏移量应保持不变")
        assertEquals(-2000L, service.getGlobalOffset(anotherVideoPath), "第二个视频的偏移量应独立")
    }

    /**
     * 测试：有效时间包含全局偏移
     */
    @Test
    fun `effective time includes global offset`() {
        val originalStart = 10000L // 10 秒
        val captionIndex = 0

        service.setGlobalOffset(videoPath, 5000L)

        val effectiveTime = service.getEffectiveTime(videoPath, captionIndex, originalStart)

        assertEquals(15000L, effectiveTime, "有效时间应包含全局偏移")
    }

    /**
     * 测试：字幕偏移叠加到全局偏移
     */
    @Test
    fun `caption offset adds to global offset`() {
        val originalStart = 10000L // 10 秒
        val captionIndex = 0

        service.setGlobalOffset(videoPath, 2000L)
        service.setCaptionOffset(videoPath, captionIndex, 500L)

        val effectiveTime = service.getEffectiveTime(videoPath, captionIndex, originalStart)

        assertEquals(12500L, effectiveTime, "有效时间 = 原始时间 + 全局偏移 + 字幕偏移")
    }

    /**
     * 测试：清除字幕偏移重置为零
     */
    @Test
    fun `clear caption offset resets to zero`() {
        val originalStart = 10000L // 10 秒
        val captionIndex = 0

        service.setGlobalOffset(videoPath, 2000L)
        service.setCaptionOffset(videoPath, captionIndex, 500L)

        // 清除字幕偏移
        service.clearCaptionOffset(videoPath, captionIndex)

        // 字幕偏移应重置为 0
        val captionOffset = service.getCaptionOffset(videoPath, captionIndex)
        assertEquals(0L, captionOffset, "清除后字幕偏移应为 0")

        // 有效时间只包含全局偏移
        val effectiveTime = service.getEffectiveTime(videoPath, captionIndex, originalStart)
        assertEquals(12000L, effectiveTime, "清除后有效时间只应包含全局偏移")
    }
}
