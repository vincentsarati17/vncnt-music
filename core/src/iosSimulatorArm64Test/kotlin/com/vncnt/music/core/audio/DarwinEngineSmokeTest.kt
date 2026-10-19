package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Headless engine checks: no audio hardware, runloop, or files needed.
 * The error path is fully synchronous; transport no-ops must never throw.
 */
class DarwinEngineSmokeTest {

    private fun androidOnlyTrack() = Track(
        id = "android-only",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationSeconds = 120,
        genre = "Soul",
        customAudioUri = "content://media/external/audio/1"
    )

    private fun recordingListener(onError: (String) -> Unit): AudioEngineListener =
        object : AudioEngineListener {
            override fun onPosition(positionSeconds: Int, progress: Float) = Unit
            override fun onPositionMs(positionMs: Long, positionSeconds: Int, progress: Float) = Unit
            override fun onAmplitudes(amplitudes: List<Float>) = Unit
            override fun onTrackCompleted() = Unit
            override fun onError(message: String) = onError(message)
        }

    @Test
    fun unplayableSourceReportsErrorSynchronously() {
        val engine: AudioEngine = DarwinAudioEngine()
        var error: String? = null
        engine.listener = recordingListener { error = it }
        engine.playTrack(androidOnlyTrack())
        assertTrue(error?.contains("No playable") == true)
        engine.release()
    }

    @Test
    fun transportCallsAreSafeWithoutPlayback() {
        // Must not throw: no track, no player, no listener required.
        val engine: AudioEngine = DarwinAudioEngine()
        engine.pause()
        engine.resume()
        engine.seekTo(10)
        engine.seekToMs(10_000L)
        engine.setSpeed(1.5f)
        engine.setDucked(true)
        engine.setDucked(false)
        engine.setSoundPreset(SoundPreset.BASS_BOOST)
        engine.setBassBoost(true)
        engine.setVirtualizer(false)
        engine.setHiRes(true)
        engine.stop()
        engine.release()
    }
}
