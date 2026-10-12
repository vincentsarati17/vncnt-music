package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track

/**
 * No-op engine for headless logic tests, Compose previews, and platforms
 * whose native engine is not wired yet. Tracks coarse state so queue and
 * transport logic can be exercised without audio hardware.
 */
class StubAudioEngine : AudioEngine {
    override var listener: AudioEngineListener? = null

    var currentTrack: Track? = null
        private set
    var isPlaying: Boolean = false
        private set
    var positionSeconds: Int = 0
        private set

    override fun playTrack(track: Track, startPositionSeconds: Int) {
        currentTrack = track
        positionSeconds = startPositionSeconds.coerceAtLeast(0)
        isPlaying = true
        listener?.onPosition(positionSeconds, progressOf(track))
        listener?.onPositionMs(positionSeconds * 1000L, positionSeconds, progressOf(track))
    }

    override fun pause() {
        isPlaying = false
    }

    override fun resume() {
        if (currentTrack != null) isPlaying = true
    }

    override fun stop() {
        isPlaying = false
        positionSeconds = 0
    }

    override fun seekTo(seconds: Int) {
        val track = currentTrack ?: return
        positionSeconds = seconds.coerceIn(0, track.durationSeconds)
        listener?.onPosition(positionSeconds, progressOf(track))
        listener?.onPositionMs(positionSeconds * 1000L, positionSeconds, progressOf(track))
    }

    override fun seekToMs(positionMs: Long) {
        val track = currentTrack ?: return
        positionSeconds = (positionMs / 1000L).toInt().coerceIn(0, track.durationSeconds)
        listener?.onPosition(positionSeconds, progressOf(track))
        listener?.onPositionMs(positionMs, positionSeconds, progressOf(track))
    }

    override fun setSoundPreset(preset: SoundPreset) = Unit
    override fun setBassBoost(enabled: Boolean) = Unit
    override fun setVirtualizer(enabled: Boolean) = Unit
    override fun setHiRes(enabled: Boolean) = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun setDucked(duck: Boolean) = Unit
    override fun release() {
        stop()
        currentTrack = null
    }

    private fun progressOf(track: Track): Float {
        val duration = track.durationSeconds.coerceAtLeast(1)
        return (positionSeconds.toFloat() / duration).coerceIn(0f, 1f)
    }
}
