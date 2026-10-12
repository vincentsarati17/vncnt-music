package com.example

import com.example.data.lyrics.LrcParser
import com.example.data.model.LyricsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parse valid synchronized lrc content`() {
        val lrc = """
            [ti:Hit the Road Jack]
            [ar:Ray Charles]
            [00:02.50]Hit the road Jack
            [00:05.10]And don't you come back no more
            [00:08.85]No more, no more, no more
        """.trimIndent()

        val result = LrcParser.parse("track_1", lrc, LyricsSource.SIDECAR_LRC)

        assertTrue(result.isSynced)
        assertEquals(3, result.lines.size)
        assertEquals(2500L, result.lines[0].timestampMs)
        assertEquals("Hit the road Jack", result.lines[0].text)
        assertEquals(5100L, result.lines[1].timestampMs)
        assertEquals("And don't you come back no more", result.lines[1].text)
        assertEquals(8850L, result.lines[2].timestampMs)
        assertEquals("0:02", result.lines[0].timestampFormatted)
    }

    @Test
    fun `parse multi timestamp lines`() {
        val lrc = """
            [00:10.00][00:20.00]Repeat chorus
        """.trimIndent()

        val result = LrcParser.parse("track_2", lrc)

        assertTrue(result.isSynced)
        assertEquals(2, result.lines.size)
        assertEquals(10000L, result.lines[0].timestampMs)
        assertEquals("Repeat chorus", result.lines[0].text)
        assertEquals(20000L, result.lines[1].timestampMs)
        assertEquals("Repeat chorus", result.lines[1].text)
    }

    @Test
    fun `parse plain text fallback without timestamps`() {
        val plain = """
            Just a castaway, an island lost at sea, oh
            Another lonely day, with no one here but me, oh
        """.trimIndent()

        val result = LrcParser.parse("track_3", plain)

        assertFalse(result.isSynced)
        assertNotNull(result.plainLyrics)
        assertEquals(2, result.lines.size)
        assertEquals("Just a castaway, an island lost at sea, oh", result.lines[0].text)
    }

    @Test
    fun `parse empty or whitespace returns empty synced false`() {
        val result = LrcParser.parse("track_4", "   ")
        assertFalse(result.isSynced)
        assertTrue(result.lines.isEmpty())
    }
}
