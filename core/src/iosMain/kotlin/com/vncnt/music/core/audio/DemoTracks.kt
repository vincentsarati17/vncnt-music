package com.vncnt.music.core.audio

import com.vncnt.music.core.model.Track

/**
 * Builds the demo Track on the Kotlin side so Swift never constructs shared
 * models across the FFI (a construction path this shell has not proven).
 */
object DemoTracks {

    const val DEMO_DURATION_SECONDS = 24

    fun tone(filePath: String): Track = Track(
        id = "demo-tone",
        title = "Demo Tone",
        artist = "Vncnt Engine",
        album = "Shell Session",
        durationSeconds = DEMO_DURATION_SECONDS,
        genre = "Demo",
        imageResName = "",
        audioToneType = "DEFAULT",
        filePath = filePath,
        isHiRes = true,
        audioQualityBadge = "WAV • 16-bit PCM"
    )

    /** The demo library — built on the Kotlin side so Swift never converts
     *  a Swift array into a Kotlin List across the FFI. */
    fun library(filePath: String): List<Track> = listOf(tone(filePath))
}
