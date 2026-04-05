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

package com.mujingx.state

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class GlobalStateTest {

    private lateinit var globalState: GlobalState

    @BeforeEach
    fun setUp() {
        val defaultData = GlobalData()
        globalState = GlobalState(defaultData)
    }

    @Test
    fun `default global state should use default values`() {
        assertEquals(ScreenType.WORD, globalState.type)
        assertTrue(globalState.isDarkTheme)
        assertFalse(globalState.isFollowSystemTheme)
        assertEquals(0.8F, globalState.audioVolume, 0.01F)
        assertEquals(80F, globalState.videoVolume, 0.01F)
    }

    @Test
    fun `default global state volumes should be in valid range`() {
        assertTrue(globalState.audioVolume in 0F..1F)
        assertTrue(globalState.keystrokeVolume in 0F..1F)
    }

    @Test
    fun `changing type should reflect new value`() {
        globalState.type = ScreenType.SUBTITLES
        assertEquals(ScreenType.SUBTITLES, globalState.type)
    }

    @Test
    fun `changing isDarkTheme should reflect new value`() {
        globalState.isDarkTheme = false
        assertFalse(globalState.isDarkTheme)
    }

    @Test
    fun `changing audioVolume should reflect new value`() {
        globalState.audioVolume = 0.5F
        assertEquals(0.5F, globalState.audioVolume, 0.01F)
    }

    @Test
    fun `default maxSentenceLength should be 25`() {
        assertEquals(25, globalState.maxSentenceLength)
    }

    @Test
    fun `default showInputCount should be true`() {
        assertTrue(globalState.showInputCount)
    }

    @Test
    fun `default autoUpdate should be true`() {
        assertTrue(globalState.autoUpdate)
    }

    @Test
    fun `default bncNum and frqNum should be 1000`() {
        assertEquals(1000, globalState.bncNum)
        assertEquals(1000, globalState.frqNum)
    }

    @Test
    fun `initializing from GlobalData should preserve data`() {
        val customData = GlobalData(
            type = ScreenType.TEXT,
            isDarkTheme = false,
            audioVolume = 0.3F,
            bnc = 500
        )
        val state = GlobalState(customData)
        assertEquals(ScreenType.TEXT, state.type)
        assertFalse(state.isDarkTheme)
        assertEquals(0.3F, state.audioVolume, 0.01F)
        assertEquals(500, state.bncNum)
    }
}
