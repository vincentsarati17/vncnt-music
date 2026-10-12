package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track

/**
 * Platform-agnostic playback contract. Android implements it with MediaPlayer
 * + AudioTrack synthesis ([HiFiAudioEngine]); iOS will back it with AVPlayer.
 * Callers (ViewModels, widgets, tests) program to this interface and never
 * touch platform engines directly.
 */
interface AudioEngineListener {
    fun onPosition(positionSeconds: Int, progress: Float)
    fun onPositionMs(positionMs: Long, positionSeconds: Int, progress: Float)
    fun onAmplitudes(amplitudes: List<Float>)
    fun onTrackCompleted()
    fun onError(message: String)
}

interface AudioEngine {
    var listener: AudioEngineListener?

    fun playTrack(track: Track, startPositionSeconds: Int = 0)
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(seconds: Int)
    fun seekToMs(positionMs: Long)
    fun setSoundPreset(preset: SoundPreset)
    fun setBassBoost(enabled: Boolean)
    fun setVirtualizer(enabled: Boolean)
    fun setHiRes(enabled: Boolean)
    fun setSpeed(speed: Float)
    fun setDucked(duck: Boolean)
    fun release()
}
