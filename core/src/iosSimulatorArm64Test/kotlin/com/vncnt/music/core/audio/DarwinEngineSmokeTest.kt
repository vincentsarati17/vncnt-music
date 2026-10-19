package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioUnitEQ
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)

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

    /**
     * Compile probes for Kotlin/Native AVFAudio member names. Any miss
     * surfaces as a CI annotation with the exact compiler error, which is
     * how the engine's binding spellings get settled without a local Mac.
     */
    @Test
    fun avAudioBindingProbes() {
        val node = AVAudioPlayerNode()
        node.enableRate = true
        node.isEnableRate = true
        val eq = AVAudioUnitEQ(4u)
        val band = eq.bands[0]
        band.frequency = 100f
        band.gain = 2f
        band.bandwidth = 1.0f
        assertTrue(band.frequency == 100f)
    }
}
