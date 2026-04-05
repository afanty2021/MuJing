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
import org.junit.jupiter.api.Assertions.*

class ScreenTypeTest {

    @Test
    fun `ScreenType should contain all expected screen types`() {
        val expected = setOf("WORD", "SUBTITLES", "TEXT")
        val actual = ScreenType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `ScreenType WORD is the default screen`() {
        assertNotNull(ScreenType.WORD)
        assertEquals("WORD", ScreenType.WORD.name)
    }

    @Test
    fun `ScreenType valueOf should return correct enum`() {
        assertEquals(ScreenType.WORD, ScreenType.valueOf("WORD"))
        assertEquals(ScreenType.SUBTITLES, ScreenType.valueOf("SUBTITLES"))
        assertEquals(ScreenType.TEXT, ScreenType.valueOf("TEXT"))
    }

    @Test
    fun `ScreenType valueOf invalid value should throw`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScreenType.valueOf("INVALID")
        }
    }

    @Test
    fun `ScreenType should have 3 values`() {
        assertEquals(3, ScreenType.values().size)
    }
}
