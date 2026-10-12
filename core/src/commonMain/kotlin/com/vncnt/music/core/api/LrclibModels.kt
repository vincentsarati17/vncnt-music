package com.vncnt.music.core.api

import kotlinx.serialization.Serializable

// Portable LRCLIB DTOs. JSON keys match the API exactly (same names the old
// Moshi client used), so behavior is unchanged. Unknown fields ignored.
@Serializable
data class LrclibTrack(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val albumName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)
