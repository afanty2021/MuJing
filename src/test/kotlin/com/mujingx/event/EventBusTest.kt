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

package com.mujingx.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class EventBusTest {

    private lateinit var eventBus: EventBus
    private lateinit var testScope: CoroutineScope

    @BeforeEach
    fun setUp() {
        eventBus = EventBus()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterEach
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `post and receive should deliver same event`() = runBlocking {
        var received: Any? = null

        val job = testScope.launch {
            eventBus.events.collect { event ->
                received = event
                testScope.cancel()
            }
        }

        // 等待 collector 就绪
        kotlinx.coroutines.delay(100)
        eventBus.post("test-event")

        withTimeout(2000L) {
            while (received == null) {
                kotlinx.coroutines.delay(10)
            }
        }

        assertEquals("test-event", received)
        job.cancel()
    }

    @Test
    fun `post multiple events should deliver in order`() = runBlocking {
        val received = CopyOnWriteArrayList<Any>()

        val job = testScope.launch {
            eventBus.events.collect { event ->
                received.add(event)
                if (received.size >= 3) testScope.cancel()
            }
        }

        // 等待 collector 就绪
        kotlinx.coroutines.delay(100)
        eventBus.post("event-1")
        eventBus.post("event-2")
        eventBus.post("event-3")

        withTimeout(1000L) {
            while (received.size < 3) {
                kotlinx.coroutines.delay(10)
            }
        }

        assertEquals(3, received.size)
        assertEquals("event-1", received[0])
        assertEquals("event-2", received[1])
        assertEquals("event-3", received[2])
        job.cancel()
    }

    @Test
    fun `post enum event should deliver correctly`() = runBlocking {
        var received: PlayerEventType? = null

        val job = testScope.launch {
            eventBus.events.collect { event ->
                if (event is PlayerEventType) {
                    received = event
                    testScope.cancel()
                }
            }
        }

        // 等待 collector 就绪
        kotlinx.coroutines.delay(100)
        eventBus.post(PlayerEventType.PLAY)

        withTimeout(1000L) {
            while (received == null) {
                kotlinx.coroutines.delay(10)
            }
        }

        assertEquals(PlayerEventType.PLAY, received)
        job.cancel()
    }

    @Test
    fun `PlayerEventType should contain all expected player events`() {
        val expected = setOf(
            "PLAY", "ESC", "FULL_SCREEN", "CLOSE_PLAYER",
            "DIRECTION_LEFT", "DIRECTION_RIGHT", "DIRECTION_UP", "DIRECTION_DOWN",
            "PREVIOUS_CAPTION", "NEXT_CAPTION", "REPEAT_CAPTION", "AUTO_PAUSE",
            "TOGGLE_FIRST_CAPTION", "TOGGLE_SECOND_CAPTION"
        )
        val actual = PlayerEventType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `WordScreenEventType should contain all expected word screen events`() {
        val expected = setOf(
            "NEXT_WORD", "PREVIOUS_WORD", "OPEN_SIDEBAR",
            "SHOW_WORD", "SHOW_PRONUNCIATION", "SHOW_LEMMA",
            "SHOW_DEFINITION", "SHOW_TRANSLATION", "SHOW_SENTENCES",
            "SHOW_SUBTITLES", "PLAY_AUDIO", "OPEN_VOCABULARY",
            "DELETE_WORD", "ADD_TO_FAMILIAR", "ADD_TO_DIFFICULT",
            "COPY_WORD", "PLAY_FIRST_CAPTION", "PLAY_SECOND_CAPTION",
            "PLAY_THIRD_CAPTION", "FOCUS_ON_WORD"
        )
        val actual = WordScreenEventType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `EventBus should be instantiable with events flow`() {
        val bus = EventBus()
        assertNotNull(bus)
        assertNotNull(bus.events)
    }

    @Test
    fun `first should receive the posted event`() = runBlocking {
        val job = testScope.launch {
            val event = eventBus.events.first()
            assertEquals("hello", event)
        }
        kotlinx.coroutines.delay(50)
        eventBus.post("hello")
        job.join()
    }
}
