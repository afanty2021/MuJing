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

class AzureTTSTest {

    @Test
    fun `AzureTTSData default values should initialize correctly`() {
        val data = AzureTTSData()
        assertEquals("", data.subscriptionKey)
        assertEquals("", data.region)
        assertEquals("en-US", data.pronunciationStyle)
        assertEquals("en-US-AvaNeural", data.shortName)
        assertEquals("Ava", data.displayName)
        assertEquals("Female", data.gender)
    }

    @Test
    fun `AzureTTSData custom values should be assigned correctly`() {
        val data = AzureTTSData(
            subscriptionKey = "1234567890abcdef1234567890abcdef",
            region = "eastasia",
            pronunciationStyle = "zh-CN",
            shortName = "zh-CN-XiaoxiaoNeural",
            displayName = "Xiaoxiao",
            gender = "Female"
        )
        assertEquals("1234567890abcdef1234567890abcdef", data.subscriptionKey)
        assertEquals("eastasia", data.region)
        assertEquals("zh-CN", data.pronunciationStyle)
    }

    @Test
    fun `subscriptionKeyIsValid with 32 chars should return true`() {
        val tts = AzureTTS(AzureTTSData(subscriptionKey = "a".repeat(32)))
        assertTrue(tts.subscriptionKeyIsValid())
    }

    @Test
    fun `subscriptionKeyIsValid with non-32 chars should return false`() {
        val tts1 = AzureTTS(AzureTTSData(subscriptionKey = "short"))
        assertFalse(tts1.subscriptionKeyIsValid())

        val tts2 = AzureTTS(AzureTTSData(subscriptionKey = "a".repeat(33)))
        assertFalse(tts2.subscriptionKeyIsValid())

        val tts3 = AzureTTS(AzureTTSData(subscriptionKey = ""))
        assertFalse(tts3.subscriptionKeyIsValid())
    }

    @Test
    fun `regionIsValid with valid regions should return true`() {
        val validRegions = listOf("eastasia", "westeurope", "eastus", "japaneast")
        for (region in validRegions) {
            val tts = AzureTTS(AzureTTSData(region = region))
            assertTrue(tts.regionIsValid(), "Region '$region' should be valid")
        }
    }

    @Test
    fun `regionIsValid with invalid region should return false`() {
        val tts = AzureTTS(AzureTTSData(region = "invalid-region"))
        assertFalse(tts.regionIsValid())
    }

    @Test
    fun `regionIsValid with empty region should return false`() {
        val tts = AzureTTS(AzureTTSData(region = ""))
        assertFalse(tts.regionIsValid())
    }

    @Test
    fun `getAccessToken without valid key should return null`() {
        val tts = AzureTTS(AzureTTSData(
            subscriptionKey = "short",
            region = "eastasia"
        ))
        assertNull(tts.getAccessToken())
    }

    @Test
    fun `Voice data class should map fields correctly`() {
        val voice = Voice(
            DisplayName = "Xiaoxiao",
            ShortName = "zh-CN-XiaoxiaoNeural",
            Locale = "zh-CN",
            Gender = "Female"
        )
        assertEquals("Xiaoxiao", voice.DisplayName)
        assertEquals("zh-CN-XiaoxiaoNeural", voice.ShortName)
        assertEquals("zh-CN", voice.Locale)
        assertEquals("Female", voice.Gender)
    }
}
