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

package com.mujingx.tts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TTSFactoryTest {

    @Test
    fun `AzureTTSData serialization defaults should be empty strings`() {
        val data = AzureTTSData()
        assertEquals("", data.subscriptionKey)
        assertEquals("", data.region)
    }

    @Test
    fun `AzureTTSData different voice configs should be distinguishable`() {
        val enUs = AzureTTSData(
            pronunciationStyle = "en-US",
            shortName = "en-US-AvaNeural",
            displayName = "Ava",
            gender = "Female"
        )
        val zhCn = AzureTTSData(
            pronunciationStyle = "zh-CN",
            shortName = "zh-CN-XiaoxiaoNeural",
            displayName = "Xiaoxiao",
            gender = "Female"
        )
        assertNotEquals(enUs.shortName, zhCn.shortName)
        assertNotEquals(enUs.pronunciationStyle, zhCn.pronunciationStyle)
    }

    @Test
    fun `AzureTTS mutable properties should reflect new values after update`() {
        val tts = AzureTTS(AzureTTSData())
        tts.subscriptionKey = "a".repeat(32)
        tts.region = "eastasia"
        assertTrue(tts.subscriptionKeyIsValid())
        assertTrue(tts.regionIsValid())
    }

    @Test
    fun `AzureTTS switching pronunciation style should update correctly`() {
        val tts = AzureTTS(AzureTTSData())
        tts.pronunciationStyle = "ja-JP"
        tts.shortName = "ja-JP-NanamiNeural"
        assertEquals("ja-JP", tts.pronunciationStyle)
        assertEquals("ja-JP-NanamiNeural", tts.shortName)
    }

    @Test
    fun `PlayerEventType enum should contain PLAY`() {
        // Verify cross-module enum access (no class loading issues)
        val playEvent = com.mujingx.event.PlayerEventType.PLAY
        assertNotNull(playEvent)
        assertEquals("PLAY", playEvent.name)
    }
}
