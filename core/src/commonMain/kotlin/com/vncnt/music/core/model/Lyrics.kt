package com.vncnt.music.core.model

data class LyricLine(
    val timestampMs: Long,
    val text: String
) {
    val timestampFormatted: String
        get() {
            // Manual padding: String.format is JVM-only and unavailable in commonMain.
            val totalSeconds = timestampMs / 1000
            val minutes = totalSeconds / 60
            val seconds = (totalSeconds % 60).toString().padStart(2, '0')
            return "$minutes:$seconds"
        }
}

enum class LyricsSource {
    EMBEDDED_METADATA,
    SIDECAR_LRC,
    ONLINE_API,
    USER_IMPORT,
    CACHED_DATABASE
}

data class LyricsData(
    val trackId: String,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val plainLyrics: String? = null,
    val source: LyricsSource = LyricsSource.CACHED_DATABASE
)
