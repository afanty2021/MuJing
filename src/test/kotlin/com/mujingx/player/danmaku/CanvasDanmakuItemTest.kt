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
import org.junit.Test

/**
 * CanvasDanmakuItem 单元测试
 * 测试弹幕项数据类的核心功能
 */
class CanvasDanmakuItemTest {

    @Test
    fun `test default values`() {
        // 测试默认值
        val danmaku = CanvasDanmakuItem(text = "测试")

        assertEquals("测试", danmaku.text)
        assertNull(danmaku.word)
        assertEquals(Color.White, danmaku.color)
        assertEquals(DanmakuType.SCROLL, danmaku.type)
        assertEquals(0f, danmaku.startTime, 0.01f)
        assertNull(danmaku.timeMs)
        assertEquals(0f, danmaku.x, 0.01f)
        assertEquals(0f, danmaku.y, 0.01f)
        assertTrue(danmaku.isActive)
        assertFalse(danmaku.isPaused)
        assertEquals(0f, danmaku.textWidth, 0.01f)
    }

    @Test
    fun `test custom values`() {
        // 测试自定义值
        val danmaku = CanvasDanmakuItem(
            text = "自定义弹幕",
            word = null,
            color = Color.Red,
            type = DanmakuType.TOP,
            startTime = 1.5f,
            timeMs = 1500L,
            initialX = 100f,
            initialY = 200f
        )

        assertEquals("自定义弹幕", danmaku.text)
        assertEquals(Color.Red, danmaku.color)
        assertEquals(DanmakuType.TOP, danmaku.type)
        assertEquals(1.5f, danmaku.startTime, 0.01f)
        assertEquals(1500L, danmaku.timeMs)
        assertEquals(100f, danmaku.x, 0.01f)
        assertEquals(200f, danmaku.y, 0.01f)
    }

    @Test
    fun `test updatePosition moves scroll danmaku left`() {
        // 测试更新位置（滚动弹幕向左移动）
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = 800f,
            initialY = 100f
        )

        danmaku.updatePosition(speed = 3f)

        assertEquals(797f, danmaku.x, 0.01f)
        assertEquals(100f, danmaku.y, 0.01f)
    }

    @Test
    fun `test updatePosition does not move when paused`() {
        // 测试暂停时不移动位置
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = 800f
        )

        danmaku.isPaused = true
        danmaku.updatePosition(speed = 3f)

        assertEquals(800f, danmaku.x, 0.01f) // 位置不变
    }

    @Test
    fun `test updatePosition does not move when inactive`() {
        // 测试不活跃时不移动位置
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = 800f
        )

        danmaku.isActive = false
        danmaku.updatePosition(speed = 3f)

        assertEquals(800f, danmaku.x, 0.01f) // 位置不变
    }

    @Test
    fun `test updatePosition static danmaku updates lifetime`() {
        // 测试静止弹幕更新生命周期
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            type = DanmakuType.TOP
        )

        danmaku.setDisplayDuration(3000L)

        // 立即检查应该还是活跃的
        danmaku.updatePosition(speed = 3f)
        // 静止弹幕不会移动，但会检查生命周期
        assertTrue(danmaku.isActive)
    }

    @Test
    fun `test setDisplayDuration sets creation time`() {
        // 测试设置显示时长
        val danmaku = CanvasDanmakuItem(text = "测试")

        val beforeTime = System.currentTimeMillis()
        danmaku.setDisplayDuration(5000L)
        val afterTime = System.currentTimeMillis()

        assertEquals(5000L, danmaku.displayDurationMs)
        assertTrue(danmaku.createdTimeMs >= beforeTime)
        assertTrue(danmaku.createdTimeMs <= afterTime)
    }

    @Test
    fun `test updateStaticDanmakuLifetime deactivates after timeout`() {
        // 测试静止弹幕超时后失活
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            type = DanmakuType.TOP
        )

        danmaku.setDisplayDuration(100L) // 100ms 时长

        // 等待超时
        Thread.sleep(150)

        danmaku.updatePosition(speed = 3f)

        assertFalse(danmaku.isActive)
    }

    @Test
    fun `test isVisible returns true for visible scroll danmaku`() {
        // 测试滚动弹幕可见性
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = 400f
        )
        danmaku.textWidth = 100f

        assertTrue(danmaku.isVisible(canvasWidth = 800f))
    }

    @Test
    fun `test isVisible returns false for off-screen scroll danmaku`() {
        // 测试滚动弹幕离屏时不可见
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = -200f
        )
        danmaku.textWidth = 100f

        assertFalse(danmaku.isVisible(canvasWidth = 800f))
    }

    @Test
    fun `test isVisible returns true for static danmaku within duration`() {
        // 测试静止弹幕在时长内可见
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            type = DanmakuType.TOP
        )

        danmaku.setDisplayDuration(5000L)

        // 立即检查应该可见
        assertTrue(danmaku.isVisible(canvasWidth = 800f))
    }

    @Test
    fun `test isVisible returns false for static danmaku after timeout`() {
        // 测试静止弹幕超时后不可见
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            type = DanmakuType.TOP
        )

        danmaku.setDisplayDuration(50L) // 50ms 时长
        Thread.sleep(100)

        assertFalse(danmaku.isVisible(canvasWidth = 800f))
    }

    @Test
    fun `test isVisible for bottom danmaku`() {
        // 测试底部弹幕可见性
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            type = DanmakuType.BOTTOM
        )

        danmaku.setDisplayDuration(5000L)
        assertTrue(danmaku.isVisible(canvasWidth = 800f))
    }

    @Test
    fun `test isVisible for annotation danmaku`() {
        // 测试标注弹幕可见性
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            type = DanmakuType.ANNOTATION
        )

        danmaku.setDisplayDuration(5000L)
        assertTrue(danmaku.isVisible(canvasWidth = 800f))
    }

    @Test
    fun `test reset resets state`() {
        // 测试重置状态
        val danmaku = CanvasDanmakuItem(
            text = "原始",
            initialX = 100f,
            initialY = 200f
        )

        // 修改一些状态
        danmaku.x = 50f
        danmaku.y = 60f
        danmaku.isActive = false
        danmaku.isPaused = true
        danmaku.textWidth = 100f

        danmaku.reset(
            newText = "新弹幕",
            newType = DanmakuType.TOP,
            newX = 800f,
            newY = 30f
        )

        assertEquals(800f, danmaku.x, 0.01f)
        assertEquals(30f, danmaku.y, 0.01f)
        assertTrue(danmaku.isActive)
        assertFalse(danmaku.isPaused)
        assertEquals(0f, danmaku.textWidth, 0.01f)
        // 注意：text 是不可变属性，不会被 reset 改变
        assertEquals("原始", danmaku.text)
    }

    @Test
    fun `test DanmakuType enum values`() {
        // 测试弹幕类型枚举
        assertEquals(4, DanmakuType.values().size)
        assertEquals(DanmakuType.SCROLL, DanmakuType.valueOf("SCROLL"))
        assertEquals(DanmakuType.TOP, DanmakuType.valueOf("TOP"))
        assertEquals(DanmakuType.BOTTOM, DanmakuType.valueOf("BOTTOM"))
        assertEquals(DanmakuType.ANNOTATION, DanmakuType.valueOf("ANNOTATION"))
    }

    @Test
    fun `test scroll danmaku moves correctly over time`() {
        // 测试滚动弹幕随时间移动
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = 800f
        )

        // 模拟多帧更新
        repeat(10) {
            danmaku.updatePosition(speed = 5f)
        }

        assertEquals(750f, danmaku.x, 0.01f) // 800 - 10*5 = 750
    }

    @Test
    fun `test scroll danmaku visibility boundary`() {
        // 测试滚动弹幕可见性边界
        val danmaku = CanvasDanmakuItem(
            text = "测试",
            initialX = 800f
        )
        danmaku.textWidth = 100f

        // 在屏幕右侧，可见
        assertTrue(danmaku.isVisible(800f))

        // 移动到屏幕左侧
        danmaku.x = -150f
        assertFalse(danmaku.isVisible(800f))

        // 刚好在边界
        danmaku.x = -100f
        danmaku.textWidth = 100f
        // x + textWidth = 0，按实现逻辑边界处仍可见
        assertTrue(danmaku.isVisible(800f))

        // 完全离开屏幕
        danmaku.x = -101f
        assertFalse(danmaku.isVisible(800f))
    }
}
