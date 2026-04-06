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

package com.mujingx.player

import org.junit.Assert.*
import org.junit.Test

/**
 * PlayerState 单元测试
 * 测试播放器状态管理的核心功能
 */
class PlayerStateTest {

    @Test
    fun `test PlayerData default values`() {
        // 测试 PlayerData 的默认值
        val playerData = PlayerData()

        assertFalse(playerData.showSequence)
        assertFalse(playerData.danmakuVisible)
        assertFalse(playerData.autoCopy)
        assertTrue(playerData.autoSpeak)
        assertTrue(playerData.preferredChinese)
        assertFalse(playerData.autoPause)
        assertTrue(playerData.showClose)
    }

    @Test
    fun `test PlayerData custom values`() {
        // 测试 PlayerData 的自定义值
        val playerData = PlayerData(
            showSequence = true,
            danmakuVisible = true,
            autoCopy = true,
            autoSpeak = false,
            preferredChinese = false,
            autoPause = true,
            showClose = false
        )

        assertTrue(playerData.showSequence)
        assertTrue(playerData.danmakuVisible)
        assertTrue(playerData.autoCopy)
        assertFalse(playerData.autoSpeak)
        assertFalse(playerData.preferredChinese)
        assertTrue(playerData.autoPause)
        assertFalse(playerData.showClose)
    }

    @Test
    fun `test RecentVideo equals and hashCode`() {
        // 测试 RecentVideo 的相等性判断
        val video1 = RecentVideo(
            dateTime = "2026-04-05T10:00:00",
            name = "Test Video",
            path = "/path/to/video.mp4",
            lastPlayedTime = "00:01:30"
        )

        val video2 = RecentVideo(
            dateTime = "2026-04-05T11:00:00", // 不同时间
            name = "Test Video",
            path = "/path/to/video.mp4",
            lastPlayedTime = "00:02:00"
        )

        val video3 = RecentVideo(
            dateTime = "2026-04-05T10:00:00",
            name = "Different Video",
            path = "/path/to/video.mp4",
            lastPlayedTime = "00:01:30"
        )

        // 名称和路径相同的视频应该相等
        assertEquals(video1, video2)
        assertEquals(video1.hashCode(), video2.hashCode())

        // 名称不同的视频不应该相等
        assertNotEquals(video1, video3)
    }

    @Test
    fun `test PlaylistItemType enum values`() {
        // 测试播放列表项类型枚举
        assertEquals(PlaylistItemType.DEFAULT, PlaylistItemType.valueOf("DEFAULT"))
        assertEquals(PlaylistItemType.CUSTOM, PlaylistItemType.valueOf("CUSTOM"))
        assertEquals(2, PlaylistItemType.values().size)
    }

    @Test
    fun `test NotificationType enum values`() {
        // 测试通知类型枚举
        assertEquals(NotificationType.INFO, NotificationType.valueOf("INFO"))
        assertEquals(NotificationType.ACTION, NotificationType.valueOf("ACTION"))
        assertEquals(2, NotificationType.values().size)
    }

    @Test
    fun `test PlaylistItem default values`() {
        // 测试 PlaylistItem 的默认值
        val item = PlaylistItem(
            name = "Test",
            path = "/path/test.mp4"
        )

        assertEquals("Test", item.name)
        assertEquals("/path/test.mp4", item.path)
        assertEquals("00:00:00", item.lastPlayedTime)
        assertFalse(item.isCurrentlyPlaying)
        assertEquals(PlaylistItemType.DEFAULT, item.type)
    }

    @Test
    fun `test PlaylistItem custom values`() {
        // 测试 PlaylistItem 的自定义值
        val item = PlaylistItem(
            name = "Custom Test",
            path = "/path/custom.mp4",
            lastPlayedTime = "00:05:30",
            isCurrentlyPlaying = true,
            type = PlaylistItemType.CUSTOM
        )

        assertEquals("Custom Test", item.name)
        assertEquals("/path/custom.mp4", item.path)
        assertEquals("00:05:30", item.lastPlayedTime)
        assertTrue(item.isCurrentlyPlaying)
        assertEquals(PlaylistItemType.CUSTOM, item.type)
    }
}
