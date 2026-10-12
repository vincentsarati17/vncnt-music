package com.vncnt.music.core.lyrics

import com.vncnt.music.core.model.LyricLine
import com.vncnt.music.core.model.LyricsData
import com.vncnt.music.core.model.LyricsSource

// Verbatim port of the Android LrcParser — pure Kotlin, no platform APIs.
// Future KMP move: this file relocates to commonMain unchanged.
object LrcParser {
    // Regex matching timestamp patterns like [01:23.45] or [01:23.456] or [01:23]
    private val timestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]""")
    private val offsetRegex = Regex("""\[offset:\s*([+-]?\d+)\s*\]""", RegexOption.IGNORE_CASE)

    fun parse(trackId: String, content: String, source: LyricsSource = LyricsSource.CACHED_DATABASE): LyricsData {
        if (content.isBlank()) {
            return LyricsData(
                trackId = trackId,
                isSynced = false,
                lines = emptyList(),
                plainLyrics = null,
                source = source
            )
        }

        var offsetMs = 0L
        val lines = mutableListOf<LyricLine>()
        var foundAnyTimestamp = false
        val plainLines = mutableListOf<String>()

        val rawLines = content.lines()

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Check for offset tag
            val offsetMatch = offsetRegex.find(trimmed)
            if (offsetMatch != null) {
                offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                continue
            }

            // Find all timestamps in this line
            val matches = timestampRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                foundAnyTimestamp = true
                // The lyric text is everything after the last timestamp match
                val lastMatch = matches.last()
                val text = trimmed.substring(lastMatch.range.last + 1).trim()

                // A single line may have multiple timestamps (e.g. [00:12.00][00:24.00]Lyric)
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val millisStr = match.groupValues[3]
                    val millis = when (millisStr.length) {
                        1 -> (millisStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (millisStr.toLongOrNull() ?: 0L) * 10L
                        3 -> millisStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }

                    val totalMs = (minutes * 60_000L) + (seconds * 1_000L) + millis + offsetMs
                    if (text.isNotEmpty()) {
                        lines.add(LyricLine(timestampMs = totalMs.coerceAtLeast(0L), text = text))
                    }
                }
            } else {
                // Ignore metadata tags like [ti:Title], [ar:Artist], etc.
                if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                    plainLines.add(trimmed)
                }
            }
        }

        return if (foundAnyTimestamp && lines.isNotEmpty()) {
            LyricsData(
                trackId = trackId,
                isSynced = true,
                lines = lines.sortedBy { it.timestampMs },
                plainLyrics = null,
                source = source
            )
        } else {
            // Unsynchronized fallback
            val unsyncedText = plainLines.joinToString("\n")
            val unsyncedLines = plainLines.map { text ->
                LyricLine(timestampMs = 0L, text = text)
            }
            LyricsData(
                trackId = trackId,
                isSynced = false,
                lines = unsyncedLines,
                plainLyrics = unsyncedText,
                source = source
            )
        }
    }
}
