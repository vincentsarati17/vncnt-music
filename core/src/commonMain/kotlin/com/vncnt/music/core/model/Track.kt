package com.vncnt.music.core.model

// Canonical Track model. Platform layers (Room entities, scanner output,
// notification metadata) map to/from this type. No Android imports allowed.
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val genre: String,
    val imageResName: String = "",
    val audioToneType: String = "RAY_CHARLES_SOUL",
    val customAudioUri: String? = null,
    val filePath: String? = null,
    val isFavorite: Boolean = false,
    val isHiRes: Boolean = true,
    val audioQualityBadge: String = "FLAC 24-bit/96kHz",
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val trackNumber: Int = 1
) {
    val durationFormatted: String
        get() {
            // Manual padding: String.format is JVM-only and unavailable in commonMain.
            val minutes = durationSeconds / 60
            val seconds = (durationSeconds % 60).toString().padStart(2, '0')
            return "$minutes:$seconds"
        }
}
