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
        imageResName = "",
        audioToneType = "DEFAULT",
        filePath = filePath,
        isHiRes = true,
        audioQualityBadge = "WAV • 16-bit PCM"
    )
}
