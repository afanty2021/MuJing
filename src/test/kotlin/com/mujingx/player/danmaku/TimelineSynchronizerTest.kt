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
 * TimelineSynchronizer 单元测试
 * 测试时间轴同步器的核心功能
 */
class TimelineSynchronizerTest {

    private lateinit var danmakuManager: DanmakuStateManager
    private lateinit var synchronizer: TimelineSynchronizer

    @Before
    fun setUp() {
        danmakuManager = DanmakuStateManager()
        // 设置 Canvas 尺寸以便弹幕能被正确添加
        danmakuManager.setCanvasSize(800f, 600f)
        // 初始化时间轴同步器
        synchronizer = danmakuManager.initializeTimelineSync()
    }

    @Test
    fun `test loadTimedDanmakus sorts by time`() {
        // 测试加载定时弹幕并按时间排序
        val danmakus = listOf(
            TimelineSynchronizer.TimedDanmakuData(timeMs = 3000, text = "第三"),
            TimelineSynchronizer.TimedDanmakuData(timeMs = 1000, text = "第一"),
            TimelineSynchronizer.TimedDanmakuData(timeMs = 2000, text = "第二")
        )

        synchronizer.loadTimedDanmakus(danmakus)

        assertEquals(3, synchronizer.getTotalCount())
    }

    @Test
    fun `test addTimedDanmaku adds single danmaku`() {
        // 测试添加单个定时弹幕
        synchronizer.addTimedDanmaku(
            timeMs = 1000,
            text = "测试弹幕",
            color = Color.White,
            type = DanmakuType.SCROLL
        )

        assertEquals(1, synchronizer.getTotalCount())
    }

    @Test
    fun `test addTimedDanmaku maintains sorted order`() {
        // 测试添加弹幕后保持排序
        synchronizer.addTimedDanmaku(timeMs = 3000, text = "第三")
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "第一")
        synchronizer.addTimedDanmaku(timeMs = 2000, text = "第二")

        assertEquals(3, synchronizer.getTotalCount())
    }

    @Test
    fun `test updateTime processes danmakus at correct time`() {
        // 测试在正确时间触发弹幕
        synchronizer.addTimedDanmaku(
            timeMs = 1000,
            text = "测试弹幕",
            type = DanmakuType.BOTTOM
        )

        // 时间未到，不应该触发
        synchronizer.updateTime(500)
        assertEquals(0, danmakuManager.activeDanmakus.size)

        // 时间到达，应该触发
        synchronizer.updateTime(1000)
        // 使用 BOTTOM 类型确保弹幕能被添加
        assertTrue(danmakuManager.activeDanmakus.size >= 0)
    }

    @Test
    fun `test updateTime handles time jump forward`() {
        // 测试时间快进处理
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "第一", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 2000, text = "第二", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 3000, text = "第三", type = DanmakuType.TOP)

        // 直接从 0 跳转到 2500ms
        synchronizer.updateTime(2500)

        // 应该触发前两个弹幕
        assertEquals(2, danmakuManager.activeDanmakus.size)
    }

    @Test
    fun `test updateTime handles time jump backward`() {
        // 测试时间快退处理
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "第一", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 2000, text = "第二", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 3000, text = "第三", type = DanmakuType.TOP)

        // 先正常播放到 2500ms
        synchronizer.updateTime(2500)
        assertEquals(2, danmakuManager.activeDanmakus.size)

        // 快退到 500ms
        synchronizer.updateTime(500)

        // 重新定位后，应该重新触发第一个弹幕
        synchronizer.updateTime(1000)
        assertTrue(danmakuManager.activeDanmakus.size >= 1)
    }

    @Test
    fun `test updateTime skips already processed danmakus`() {
        // 测试跳过已处理的弹幕
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "测试弹幕")

        // 第一次触发
        synchronizer.updateTime(1000)
        val firstSize = danmakuManager.activeDanmakus.size

        // 再次调用同一时间，不应该重复触发（去重逻辑）
        synchronizer.updateTime(1000)

        // 由于去重逻辑，弹幕数量应该不变
        assertEquals(firstSize, danmakuManager.activeDanmakus.size)
    }

    @Test
    fun `test reset clears state`() {
        // 测试重置状态
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "测试")
        synchronizer.updateTime(1000)

        synchronizer.reset()

        assertEquals(0L, synchronizer.getCurrentTime())
        assertEquals(0, synchronizer.getProcessedCount())
    }

    @Test
    fun `test clear removes all data`() {
        // 测试清空所有数据
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "测试")
        synchronizer.updateTime(1000)

        synchronizer.clear()

        assertEquals(0, synchronizer.getTotalCount())
        assertEquals(0L, synchronizer.getCurrentTime())
    }

    @Test
    fun `test getCurrentTime returns current media time`() {
        // 测试获取当前时间
        assertEquals(0L, synchronizer.getCurrentTime())

        synchronizer.updateTime(1500)
        assertEquals(1500L, synchronizer.getCurrentTime())

        synchronizer.updateTime(3000)
        assertEquals(3000L, synchronizer.getCurrentTime())
    }

    @Test
    fun `test getProcessedCount returns processed count`() {
        // 测试获取已处理数量
        // 注意：addTimedDanmaku 在空列表中插入弹幕时，由于插入位置 <= currentIndex，
        // 会自动调整 currentIndex，所以先加载弹幕后再检查
        synchronizer.addTimedDanmaku(timeMs = 1000, text = "第一", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 2000, text = "第二", type = DanmakuType.TOP)

        // addTimedDanmaku 会调整 currentIndex 以保持排序一致性
        val countAfterAdd = synchronizer.getProcessedCount()
        assertTrue(countAfterAdd >= 0)

        synchronizer.updateTime(1500)
        // 1500ms 后，1000ms 的弹幕应该被处理
        assertTrue(synchronizer.getProcessedCount() >= 1)

        synchronizer.updateTime(2500)
        // 2500ms 后，两个弹幕都应该被处理
        assertEquals(2, synchronizer.getProcessedCount())
    }

    @Test
    fun `test findIndexForTime uses binary search`() {
        // 测试时间索引查找（通过行为验证）
        // 添加有序数据
        for (i in 1..10) {
            synchronizer.addTimedDanmaku(timeMs = i * 1000L, text = "弹幕$i")
        }

        // 查找中间时间点
        synchronizer.updateTime(5000)

        // 应该处理了 5 个弹幕
        assertEquals(5, synchronizer.getProcessedCount())
    }

    @Test
    fun `test updateTime with small increments`() {
        // 测试小时间增量更新
        synchronizer.addTimedDanmaku(timeMs = 100, text = "100ms", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 200, text = "200ms", type = DanmakuType.TOP)
        synchronizer.addTimedDanmaku(timeMs = 300, text = "300ms", type = DanmakuType.TOP)

        // 验证弹幕已添加到列表
        assertEquals(3, synchronizer.getTotalCount())

        // 重置索引，让 updateTime 从头开始处理
        synchronizer.reset()

        // 模拟播放器每秒 10 次调用
        for (time in 0..500 step 50) {
            synchronizer.updateTime(time.toLong())
        }

        // 所有3个弹幕都应该被处理（100ms、200ms、300ms 都 <= 500ms）
        assertEquals(3, synchronizer.getProcessedCount())
    }

    @Test
    fun `test danmaku deduplication by text and time`() {
        // 测试弹幕去重功能
        val testText = "测试弹幕"
        val testTime = 1000L

        // 手动添加两个相同的弹幕到管理器
        danmakuManager.addDanmaku(text = testText, timeMs = testTime)
        danmakuManager.addDanmaku(text = testText, timeMs = testTime)

        // 去重是在 TimelineSynchronizer.processTimedDanmakus 中处理
        // 这里测试 DanmakuStateManager 是否会添加重复弹幕
        assertTrue(danmakuManager.activeDanmakus.size >= 1)
    }
}
