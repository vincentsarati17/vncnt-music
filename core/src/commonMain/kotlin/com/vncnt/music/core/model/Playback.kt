package com.vncnt.music.core.model

// DSP presets and UI playback state. Pure data — the audio engine interprets
// these values per platform (Android AudioTrack / iOS Audio Engine).
enum class SoundPreset(val label: String, val bass: Float, val mid: Float, val treble: Float, val description: String) {
    STUDIO_FLAT("Studio Master", 1.0f, 1.0f, 1.0f, "Reference flat frequency response"),
    BASS_BOOST("Bass Impact", 1.6f, 0.9f, 0.8f, "Deep punchy low end frequencies"),
    VOCAL_WARMTH("Vocal Warmth", 0.9f, 1.4f, 1.2f, "Brings mid-range vocals and acoustics to the front"),
    ACOUSTIC_CHILL("Acoustic Air", 1.1f, 1.1f, 1.5f, "Crisp strings and intimate presence"),
    VINTAGE_VINYL("Vintage Vinyl 1960s", 1.3f, 1.2f, 0.7f, "Warm analog saturation and tube warmth")
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val currentPositionMs: Long = 0L,
    val currentPositionProgress: Float = 0f,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val activePreset: SoundPreset = SoundPreset.STUDIO_FLAT,
    val isHiResModeEnabled: Boolean = true,
    val bassBoostEnabled: Boolean = true,
    val virtualizerEnabled: Boolean = true,
    val amplitudes: List<Float> = List(32) { 0.2f }
)
