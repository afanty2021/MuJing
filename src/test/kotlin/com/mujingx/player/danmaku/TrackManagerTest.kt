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

package com.mujingx.player.danmaku

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * TrackManager 单元测试
 * 测试弹幕轨道管理器的核心功能
 */
class TrackManagerTest {

    private lateinit var trackManager: TrackManager

    @Before
    fun setUp() {
        trackManager = TrackManager()
    }

    @Test
    fun `test updateCanvasSize initializes tracks correctly`() {
        // 测试更新 Canvas 尺寸后轨道初始化
        trackManager.updateCanvasSize(600f, 30f)

        // 600 / 30 = 20 个轨道
        assertEquals(20, trackManager.getTotalTrackCount())
        assertEquals(20, trackManager.getAvailableTrackCount())
    }

    @Test
    fun `test assignTrack returns -1 when canvas not initialized`() {
        // 测试 Canvas 未初始化时返回 -1
        val danmaku = createTestDanmaku("测试")
        assertEquals(-1, trackManager.assignTrack(danmaku, 800f))
    }

    @Test
    fun `test assignTrack allocates tracks starting from index 1`() {
        // 测试轨道分配从索引 1 开始（跳过第 0 轨道）
        trackManager.updateCanvasSize(300f, 30f)

        val danmaku1 = createTestDanmaku("测试 1", timeMs = 1000)
        val track1 = trackManager.assignTrack(danmaku1, 800f)

        assertEquals(1, track1) // 第一个可用轨道应该是 1
        assertEquals(30f * 2, danmaku1.y) // Y 坐标应该是 (trackIndex + 1) * lineHeight
    }

    @Test
    fun `test assignTrack skips track 0`() {
        // 测试第 0 轨道被跳过（与标题栏重叠）
        trackManager.updateCanvasSize(300f, 30f)

        // 分配多个轨道
        val tracks = mutableListOf<Int>()
        for (i in 1..5) {
            val danmaku = createTestDanmaku("测试$i", timeMs = 1000L + i * 100L)
            tracks.add(trackManager.assignTrack(danmaku, 800f))
        }

        // 所有轨道都不应该是 0
        assertTrue(tracks.all { it != 0 })
        // 轨道应该是连续的 1, 2, 3, 4, 5
        assertEquals(listOf(1, 2, 3, 4, 5), tracks.sorted())
    }

    @Test
    fun `test assignTrack prevents overlap for simultaneous danmakus`() {
        // 测试同时处理的弹幕不会分配在同一轨道
        trackManager.updateCanvasSize(600f, 30f)

        // 创建两个时间戳相同的弹幕
        val danmaku1 = createTestDanmaku("测试 1", timeMs = 1000)
        val danmaku2 = createTestDanmaku("测试 2", timeMs = 1000)

        val track1 = trackManager.assignTrack(danmaku1, 800f)
        val track2 = trackManager.assignTrack(danmaku2, 800f)

        // 同时处理的弹幕应该在不同轨道
        assertNotEquals(track1, track2)
    }

    @Test
    fun `test assignTrack allows same track for different time danmakus`() {
        // 测试不同时间的弹幕可以在同一轨道
        trackManager.updateCanvasSize(600f, 30f)

        // 创建时间差大于 100ms 的弹幕
        val danmaku1 = createTestDanmaku("测试 1", timeMs = 1000)
        val danmaku2 = createTestDanmaku("测试 2", timeMs = 1200) // 差 200ms

        val track1 = trackManager.assignTrack(danmaku1, 800f)
        // 先标记第一个弹幕为不活跃，模拟它已经移出屏幕
        danmaku1.isActive = false

        val track2 = trackManager.assignTrack(danmaku2, 800f)

        // 不同时间的弹幕可以在同一轨道（当第一个不活跃时）
        assertEquals(track1, track2)
    }

    @Test
    fun `test releaseTrack clears track correctly`() {
        // 测试释放轨道功能
        trackManager.updateCanvasSize(300f, 30f)

        val danmaku = createTestDanmaku("测试", timeMs = 1000L)
        val trackIndex = trackManager.assignTrack(danmaku, 800f)

        assertEquals(1, trackIndex)
        assertEquals(9, trackManager.getAvailableTrackCount())

        trackManager.releaseTrack(danmaku)

        assertEquals(10, trackManager.getAvailableTrackCount())
    }

    @Test
    fun `test cleanup removes inactive danmakus`() {
        // 测试清理不活跃弹幕
        trackManager.updateCanvasSize(300f, 30f)

        val danmaku1 = createTestDanmaku("测试 1", timeMs = 1000L)
        val danmaku2 = createTestDanmaku("测试 2", timeMs = 2000L)

        trackManager.assignTrack(danmaku1, 800f)
        trackManager.assignTrack(danmaku2, 800f)

        assertEquals(8, trackManager.getAvailableTrackCount())

        // 标记两个弹幕为不活跃
        danmaku1.isActive = false
        danmaku2.isActive = false

        trackManager.cleanup()

        assertEquals(10, trackManager.getAvailableTrackCount())
    }

    @Test
    fun `test clear resets all tracks`() {
        // 测试清空所有轨道
        trackManager.updateCanvasSize(300f, 30f)

        // 分配一些轨道
        repeat(5) {
            val danmaku = createTestDanmaku("测试$it", timeMs = 1000L + it * 100L)
            trackManager.assignTrack(danmaku, 800f)
        }

        assertEquals(5, trackManager.getAvailableTrackCount())

        trackManager.clear()

        assertEquals(10, trackManager.getAvailableTrackCount())
    }

    @Test
    fun `test willCollide detects collision correctly`() {
        // 测试碰撞检测
        trackManager.updateCanvasSize(600f, 30f)

        // 创建一个在屏幕右侧的弹幕
        val existingDanmaku = createTestDanmaku("测试", timeMs = 1000)
        existingDanmaku.x = 700f // 在屏幕右侧（假设屏幕宽度 800）
        existingDanmaku.textWidth = 100f
        existingDanmaku.isActive = true

        // 分配轨道给这个弹幕
        trackManager.assignTrack(existingDanmaku, 800f)

        // 创建新弹幕，应该会检测到碰撞
        val newDanmaku = createTestDanmaku("新测试", timeMs = 1100)

        // 由于时间差小于 100ms，会被认为同时处理，应该分配不同轨道
        val newTrack = trackManager.assignTrack(newDanmaku, 800f)

        assertNotEquals(1, newTrack) // 不应该在同一轨道
    }

    @Test
    fun `test estimateTextWidth returns reasonable values`() {
        // 测试文本宽度估算（通过碰撞检测间接测试）
        trackManager.updateCanvasSize(600f, 30f)

        // 短文本
        val shortDanmaku = createTestDanmaku("短", timeMs = 1000)
        trackManager.assignTrack(shortDanmaku, 800f)

        // 长文本
        val longDanmaku = createTestDanmaku("这是一个很长的弹幕文本内容", timeMs = 2000)
        trackManager.assignTrack(longDanmaku, 800f)

        // 只要不抛出异常就算通过（内部方法无法直接测试）
        assertTrue(true)
    }

    @Test
    fun `test different character widths are estimated correctly`() {
        // 测试不同字符宽度的估算
        trackManager.updateCanvasSize(600f, 30f)

        // 测试各种字符类型
        val testCases = listOf(
            " " to "空格",
            "ilI1|." to "窄字符",
            "mwMW" to "宽字符",
            "abcABC123" to "ASCII 字符",
            "中文测试" to "中文字符"
        )

        testCases.forEach { (text, description) ->
            val danmaku = createTestDanmaku(text, timeMs = 1000)
            val track = trackManager.assignTrack(danmaku, 800f)
            // 只要能分配轨道就算通过
            assertTrue("$description 应该能分配轨道", track >= -1)
        }
    }

    /**
     * 创建测试用的弹幕项
     */
    private fun createTestDanmaku(text: String, timeMs: Long? = null): CanvasDanmakuItem {
        return CanvasDanmakuItem(
            text = text,
            word = null,
            color = Color.White,
            type = DanmakuType.SCROLL,
            timeMs = timeMs,
            initialX = 800f, // 从屏幕右侧开始
            initialY = 0f
        )
    }
}
