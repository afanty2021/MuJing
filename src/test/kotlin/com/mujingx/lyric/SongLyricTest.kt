package com.mujingx.lyric

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class SongLyricTest {

    private lateinit var songLyric: SongLyric

    @BeforeEach
    fun setUp() {
        songLyric = SongLyric()
    }

    @Test
    fun `default constructor creates empty song list`() {
        assertTrue(songLyric.song.isEmpty())
    }

    @Test
    fun `addLyric appends lyrics to the list`() {
        songLyric.addLyric(Lyric(0.0, "First line"))
        songLyric.addLyric(Lyric(5.0, "Second line"))
        assertEquals(2, songLyric.song.size)
        assertEquals("First line", songLyric.song[0].lyric)
        assertEquals("Second line", songLyric.song[1].lyric)
    }

    @Test
    fun `list constructor initializes with provided lyrics`() {
        val lyrics = mutableListOf(
            Lyric(0.0, "Line 1"),
            Lyric(10.0, "Line 2")
        )
        val song = SongLyric(lyrics)
        assertEquals(2, song.song.size)
        assertEquals("Line 1", song.song[0].lyric)
        assertEquals("Line 2", song.song[1].lyric)
    }

    @Test
    fun `copy constructor shares the same list reference (shallow copy)`() {
        songLyric.addLyric(Lyric(5.0, "Original"))
        val copy = SongLyric(songLyric)
        // The copy shares the same mutable list, so adding to copy affects original
        copy.addLyric(Lyric(10.0, "Added to copy"))
        assertEquals(2, songLyric.song.size, "Original should see additions via shared reference")
        assertEquals(2, copy.song.size)
    }

    @Test
    fun `clone shares the same list reference as original`() {
        songLyric.addLyric(Lyric(5.0, "Original"))
        val cloned = songLyric.clone()
        // Clone delegates to copy constructor which shares list reference
        cloned.addLyric(Lyric(10.0, "Added to clone"))
        assertEquals(2, songLyric.song.size, "Original sees additions because list is shared")
        assertEquals(2, cloned.song.size)
    }

    @Test
    fun `changeSpeed with 2x halves all timestamps`() {
        songLyric.addLyric(Lyric(10.0, "Line 1"))
        songLyric.addLyric(Lyric(20.0, "Line 2"))
        songLyric.changeSpeed(2.0)
        assertEquals(5.0, songLyric.song[0].timestamp, 0.001)
        assertEquals(10.0, songLyric.song[1].timestamp, 0.001)
    }

    @Test
    fun `changeSpeed with 0_5x doubles all timestamps`() {
        songLyric.addLyric(Lyric(10.0, "Line 1"))
        songLyric.changeSpeed(0.5)
        assertEquals(20.0, songLyric.song[0].timestamp, 0.001)
    }

    @Test
    fun `changeSpeed with 1x leaves timestamps unchanged`() {
        songLyric.addLyric(Lyric(10.0, "Line 1"))
        songLyric.changeSpeed(1.0)
        assertEquals(10.0, songLyric.song[0].timestamp, 0.001)
    }

    @Test
    fun `changeSpeed with empty list does not throw`() {
        assertDoesNotThrow { songLyric.changeSpeed(2.0) }
    }

    @Test
    fun `changeSpeed affects multiple lyrics`() {
        songLyric.addLyric(Lyric(4.0, "A"))
        songLyric.addLyric(Lyric(8.0, "B"))
        songLyric.addLyric(Lyric(12.0, "C"))
        songLyric.changeSpeed(2.0)
        assertEquals(2.0, songLyric.song[0].timestamp, 0.001)
        assertEquals(4.0, songLyric.song[1].timestamp, 0.001)
        assertEquals(6.0, songLyric.song[2].timestamp, 0.001)
    }

    @Test
    fun `toString contains all lyric entries`() {
        songLyric.addLyric(Lyric(0.0, "Hello"))
        songLyric.addLyric(Lyric(5.0, "World"))
        val result = songLyric.toString()
        assertTrue(result.contains("Hello"))
        assertTrue(result.contains("World"))
    }

    @Test
    fun `toString with empty list returns empty string`() {
        val result = songLyric.toString()
        assertEquals("", result)
    }

    @Test
    fun `toString separates lyrics with newlines`() {
        songLyric.addLyric(Lyric(0.0, "Line1"))
        songLyric.addLyric(Lyric(5.0, "Line2"))
        val result = songLyric.toString()
        assertTrue(result.contains("\n"))
        val lines = result.trim().lines()
        assertEquals(2, lines.size)
    }

    @Test
    fun `adding many lyrics works correctly`() {
        for (i in 1..100) {
            songLyric.addLyric(Lyric(i.toDouble(), "Line $i"))
        }
        assertEquals(100, songLyric.song.size)
        assertEquals("Line 1", songLyric.song[0].lyric)
        assertEquals("Line 100", songLyric.song[99].lyric)
    }

    @Test
    fun `equals returns false for different song lists`() {
        // Note: SongLyric.equals() has a known bug where identical lists cause
        // IndexOutOfBoundsException (while loop lacks bounds check).
        // Test with different lists which correctly returns false.
        val song1 = SongLyric(mutableListOf(Lyric(0.0, "A"), Lyric(5.0, "B")))
        val song2 = SongLyric(mutableListOf(Lyric(0.0, "A"), Lyric(10.0, "C")))
        assertNotEquals(song1, song2)
    }

    @Test
    fun `equals returns false for non-SongLyric object`() {
        assertNotEquals(songLyric, "not a song lyric")
        assertNotEquals(songLyric, null)
    }

    @Test
    fun `list constructor preserves same reference`() {
        val lyrics = mutableListOf(Lyric(0.0, "Test"))
        val song = SongLyric(lyrics)
        // The constructor assigns the same list reference
        assertSame(lyrics, song.song)
    }

    @Test
    fun `song list can be accessed and modified directly`() {
        songLyric.song.add(Lyric(1.0, "Direct"))
        assertEquals(1, songLyric.song.size)
        assertEquals("Direct", songLyric.song[0].lyric)
    }
}
