package com.mujingx.lyric

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LyricTest {

    @Test
    fun `default constructor initializes timestamp to 0 and lyric to no data`() {
        val lyric = Lyric()
        assertEquals(0.0, lyric.timestamp, 0.001)
        assertEquals("<no data>", lyric.lyric)
    }

    @Test
    fun `parameterized constructor assigns timestamp and lyric`() {
        val lyric = Lyric(10.5, "Hello World")
        assertEquals(10.5, lyric.timestamp, 0.001)
        assertEquals("Hello World", lyric.lyric)
    }

    @Test
    fun `copy constructor creates object with same values`() {
        val original = Lyric(30.0, "Test lyric")
        val copy = Lyric(original)
        assertEquals(original.timestamp, copy.timestamp, 0.001)
        assertEquals(original.lyric, copy.lyric)
    }

    @Test
    fun `clone returns independent copy`() {
        val original = Lyric(15.0, "Original")
        val cloned = original.clone()
        // Modifying the clone should not affect the original
        cloned.lyric = "Modified"
        assertEquals("Original", original.lyric)
        assertEquals("Modified", cloned.lyric)
    }

    @Test
    fun `clone preserves timestamp`() {
        val original = Lyric(42.5, "Test")
        val cloned = original.clone()
        assertEquals(original.timestamp, cloned.timestamp, 0.001)
        assertEquals(original.lyric, cloned.lyric)
    }

    @Test
    fun `equals returns true for same timestamp and lyric`() {
        val lyric1 = Lyric(10.0, "Same")
        val lyric2 = Lyric(10.0, "Same")
        assertEquals(lyric1, lyric2)
    }

    @Test
    fun `equals returns false for different timestamps`() {
        val lyric1 = Lyric(10.0, "Same")
        val lyric2 = Lyric(20.0, "Same")
        assertNotEquals(lyric1, lyric2)
    }

    @Test
    fun `equals returns false for different lyrics`() {
        val lyric1 = Lyric(10.0, "First")
        val lyric2 = Lyric(10.0, "Second")
        assertNotEquals(lyric1, lyric2)
    }

    @Test
    fun `equals returns false for non-Lyric objects`() {
        val lyric = Lyric(10.0, "Test")
        assertNotEquals(lyric, "not a lyric")
        assertNotEquals(lyric, null)
    }

    @Test
    fun `equals returns false for non-Lyric object even with same toString`() {
        val lyric = Lyric(10.0, "Test")
        // A string that looks like the lyric's toString should not be equal
        assertNotEquals(lyric, "10.00 Test")
    }

    @Test
    fun `toString contains formatted timestamp and lyric text`() {
        val lyric = Lyric(65.5, "Test lyric text")
        val result = lyric.toString()
        assertTrue(result.contains("65.50"), "toString should contain formatted timestamp '65.50'")
        assertTrue(result.contains("Test lyric text"), "toString should contain the lyric text")
    }

    @Test
    fun `toString formats timestamp to two decimal places`() {
        val lyric = Lyric(1.0, "Hello")
        val result = lyric.toString()
        assertTrue(result.startsWith("1.00"), "toString should format timestamp as '1.00'")
    }

    @Test
    fun `zero timestamp works correctly`() {
        val lyric = Lyric(0.0, "Start")
        assertEquals(0.0, lyric.timestamp, 0.001)
    }

    @Test
    fun `negative timestamp is allowed for offset adjustments`() {
        val lyric = Lyric(-5.0, "Negative offset")
        assertEquals(-5.0, lyric.timestamp, 0.001)
    }

    @Test
    fun `empty lyric string works correctly`() {
        val lyric = Lyric(10.0, "")
        assertEquals("", lyric.lyric)
    }

    @Test
    fun `timestamp can be mutated after construction`() {
        val lyric = Lyric(5.0, "Mutable")
        lyric.timestamp = 10.0
        assertEquals(10.0, lyric.timestamp, 0.001)
    }

    @Test
    fun `lyric text can be mutated after construction`() {
        val lyric = Lyric(5.0, "Original")
        lyric.lyric = "Updated"
        assertEquals("Updated", lyric.lyric)
    }

    @Test
    fun `large timestamp value works correctly`() {
        val lyric = Lyric(99999.99, "Long song")
        assertEquals(99999.99, lyric.timestamp, 0.001)
        val result = lyric.toString()
        assertTrue(result.contains("99999.99"), "toString should contain '99999.99' but was: $result")
    }
}
