package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import com.example.data.model.SoundPreset
import com.example.data.model.Track
import com.vncnt.music.core.audio.AudioEngine
import com.vncnt.music.core.audio.AudioEngineListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class HiFiAudioEngine(private val context: Context) : AudioEngine {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)

    // Shared contract listener (com.vncnt.music.core.audio.AudioEngine).
    // Replaces the five per-event vars with a single interface dispatch.
    override var listener: AudioEngineListener? = null

    @Volatile private var ducked = false

    private val audioLock = Any()
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var mediaPlayer: MediaPlayer? = null
    @Volatile private var synthesisJob: Job? = null

    @Volatile private var isPlaying = false
    @Volatile private var currentTrack: Track? = null
    @Volatile private var currentPositionSeconds = 0
    @Volatile private var playbackSpeed = 1.0f

    // Hi-Fi Sound Shaping
    @Volatile private var currentPreset = SoundPreset.STUDIO_FLAT
    @Volatile private var bassBoostEnabled = true
    @Volatile private var hiResEnabled = true
    @Volatile private var virtualizerEnabled = true

    // Sample rate
    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(8192)

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (isPlaying && currentTrack != null) {
                val mp = mediaPlayer
                if (mp != null) {
                    try {
                        val posMs = mp.currentPosition.toLong()
                        val durMs = mp.duration.coerceAtLeast(1)
                        currentPositionSeconds = (posMs / 1000L).toInt()
                        val progress = (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                        listener?.onPosition(currentPositionSeconds, progress)
                        listener?.onPositionMs(posMs, currentPositionSeconds, progress)
                    } catch (e: Exception) {
                        // ignore
                    }
                } else {
                    currentPositionSeconds++
                    val duration = currentTrack?.durationSeconds?.coerceAtLeast(1) ?: 1
                    val posMs = currentPositionSeconds * 1000L
                    if (currentPositionSeconds >= duration) {
                        currentPositionSeconds = 0
                        listener?.onPosition(0, 0f)
                        listener?.onPositionMs(0L, 0, 0f)
                        listener?.onTrackCompleted()
                    } else {
                        val progress = currentPositionSeconds.toFloat() / duration.toFloat()
                        listener?.onPosition(currentPositionSeconds, progress)
                        listener?.onPositionMs(posMs, currentPositionSeconds, progress)
                    }
                }
                handler.postDelayed(this, 300)
            }
        }
    }

    override fun playTrack(track: Track, startPositionSeconds: Int = 0) {
        stop()
        currentTrack = track
        currentPositionSeconds = startPositionSeconds
        isPlaying = true

        val targetSource = track.customAudioUri ?: track.filePath
        if (targetSource != null) {
            playFromSource(targetSource, startPositionSeconds)
        } else {
            playSynthesizedTone(track, startPositionSeconds)
        }

        handler.post(tickerRunnable)
    }

    private fun playFromSource(sourceString: String, startPositionSeconds: Int) {
        try {
            val newMp = MediaPlayer().apply {
                // Production: proper audio attributes + wake lock so playback
                // survives screen-off and is routed as music.
                try {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "setAudioAttributes failed", e)
                }
                try {
                    setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                } catch (e: Exception) {
                    Log.w(TAG, "setWakeMode failed (missing WAKE_LOCK?)", e)
                }
                applyDuckVolume(this)
                if (sourceString.startsWith("content://") || sourceString.startsWith("file://")) {
                    setDataSource(context, Uri.parse(sourceString))
                } else if (sourceString.startsWith("/")) {
                    setDataSource(sourceString)
                } else {
                    setDataSource(context, Uri.parse(sourceString))
                }
                setOnPreparedListener { mp ->
                    try {
                        mp.seekTo(startPositionSeconds * 1000)
                        mp.start()
                        this@HiFiAudioEngine.mediaPlayer = mp
                        startVisualizerLoop(mp)
                    } catch (e: Exception) {
                        playSynthesizedTone(currentTrack ?: return@setOnPreparedListener, startPositionSeconds)
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    try { mp.release() } catch (e: Exception) {}
                    if (this@HiFiAudioEngine.mediaPlayer == mp) {
                        this@HiFiAudioEngine.mediaPlayer = null
                    }
                    playSynthesizedTone(currentTrack ?: return@setOnErrorListener true, startPositionSeconds)
                    true
                }
                setOnCompletionListener {
                    this@HiFiAudioEngine.isPlaying = false
                    listener?.onTrackCompleted()
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            // Fallback to local audio synthesis
            try {
                mediaPlayer?.release()
            } catch (ignore: Exception) {}
            mediaPlayer = null
            playSynthesizedTone(currentTrack ?: return, startPositionSeconds)
        }
    }

    private fun playSynthesizedTone(track: Track, startPositionSeconds: Int) {
        currentPositionSeconds = startPositionSeconds
        // Safely stop previous synthesis job before creating new track
        synthesisJob?.cancel()
        synthesisJob = null

        scope.launch(Dispatchers.IO) {
            synchronized(audioLock) {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    // ignore
                }
                audioTrack = null

                try {
                    val newTrack = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()

                    if (newTrack.state == AudioTrack.STATE_INITIALIZED) {
                        newTrack.play()
                        audioTrack = newTrack
                        startSynthesizedPlaybackLoop(track)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startSynthesizedPlaybackLoop(track: Track) {
        synthesisJob = scope.launch {
            val audioBuffer = ShortArray(bufferSize / 2)
            var phase = 0.0
            var bassPhase = 0.0
            var melodyPhase = 0.0
            var beatPhase = 0.0

            // Musical chords and frequencies based on track style
            val chordProgressions = when (track.audioToneType) {
                "RAY_CHARLES_SOUL" -> listOf(
                    doubleArrayOf(174.61, 220.00, 261.63), // F major
                    doubleArrayOf(155.56, 196.00, 233.08), // Eb major
                    doubleArrayOf(138.59, 174.61, 207.65), // Db major
                    doubleArrayOf(130.81, 164.81, 196.00)  // C7
                )
                "AMY_RETRO_JAZZ" -> listOf(
                    doubleArrayOf(146.83, 174.61, 220.00), // D minor
                    doubleArrayOf(174.61, 220.00, 261.63), // F major
                    doubleArrayOf(196.00, 246.94, 293.66), // G minor
                    doubleArrayOf(220.00, 261.63, 329.63)  // A minor
                )
                "CASH_BOOM_CHICKA" -> listOf(
                    doubleArrayOf(130.81, 164.81, 196.00), // C major
                    doubleArrayOf(174.61, 220.00, 261.63), // F major
                    doubleArrayOf(196.00, 246.94, 293.66), // G major
                    doubleArrayOf(130.81, 164.81, 196.00)  // C major
                )
                else -> listOf(
                    doubleArrayOf(220.00, 261.63, 329.63), // Am
                    doubleArrayOf(174.61, 220.00, 261.63), // F
                    doubleArrayOf(130.81, 164.81, 196.00), // C
                    doubleArrayOf(196.00, 246.94, 293.66)  // G
                )
            }

            val bassNotes = when (track.audioToneType) {
                "RAY_CHARLES_SOUL" -> doubleArrayOf(87.31, 77.78, 69.30, 65.41)
                "AMY_RETRO_JAZZ" -> doubleArrayOf(73.42, 87.31, 98.00, 110.00)
                "CASH_BOOM_CHICKA" -> doubleArrayOf(65.41, 87.31, 98.00, 65.41)
                else -> doubleArrayOf(55.00, 43.65, 65.41, 49.00)
            }

            var tick = 0
            val samplesPerChord = (sampleRate * 2.0).toInt()
            val ampArray = FloatArray(32)
            val bufferDurationMs = (((audioBuffer.size / 2) * 1000L) / sampleRate).coerceAtLeast(20L)
            var lastAmplitudeUpdate = 0L

            while (isActive && isPlaying) {
                val loopStartTime = System.currentTimeMillis()
                val trackInstance = audioTrack
                if (trackInstance == null || trackInstance.state != AudioTrack.STATE_INITIALIZED) {
                    break
                }

                val currentChordIndex = ((tick / samplesPerChord) % chordProgressions.size)
                val currentChord = chordProgressions[currentChordIndex]
                val currentBass = bassNotes[currentChordIndex]

                val preset = currentPreset
                val bassMult = if (bassBoostEnabled) preset.bass * 1.35f else preset.bass
                val midMult = preset.mid
                val trebleMult = preset.treble

                var bufferEnergy = 0.0

                for (i in 0 until audioBuffer.size step 2) {
                    val root = currentChord[0]
                    val third = currentChord[1]
                    val fifth = currentChord[2]

                    // Polyphonic harmonic synth
                    val chordWave = (sin(phase * root) + 0.6 * sin(phase * third) + 0.4 * sin(phase * fifth)) / 2.0
                    val bassWave = sin(bassPhase * currentBass) * 0.7 * bassMult
                    val melodyWave = sin(melodyPhase * (root * 2.0)) * 0.3 * trebleMult
                    val acousticTick = cos(beatPhase * 4.0) * 0.08

                    val sampleLeft = ((chordWave * midMult + bassWave + melodyWave + acousticTick) * 0.45).coerceIn(-1.0, 1.0)
                    val sampleRight = if (virtualizerEnabled) {
                        ((chordWave * midMult + bassWave - melodyWave * 0.8 + acousticTick) * 0.45).coerceIn(-1.0, 1.0)
                    } else sampleLeft

                    val sampleShortLeft = (sampleLeft * 32767.0).toInt().toShort()
                    val sampleShortRight = (sampleRight * 32767.0).toInt().toShort()

                    audioBuffer[i] = sampleShortLeft
                    audioBuffer[i + 1] = sampleShortRight

                    phase += (2.0 * PI) / sampleRate
                    bassPhase += (2.0 * PI) / sampleRate
                    melodyPhase += (2.0 * PI) / sampleRate
                    beatPhase += (2.0 * PI) / sampleRate

                    bufferEnergy += sampleLeft * sampleLeft
                    tick++
                }

                // Check again before writing
                if (!isActive || !isPlaying) break

                // Safe write to AudioTrack with comprehensive exception catch
                try {
                    val currentTrackObj = audioTrack ?: break
                    if (currentTrackObj.state == AudioTrack.STATE_INITIALIZED &&
                        currentTrackObj.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        val written = currentTrackObj.write(audioBuffer, 0, audioBuffer.size)
                        if (written < 0) {
                            break
                        }
                    } else {
                        delay(20)
                    }
                } catch (e: IllegalStateException) {
                    // AudioTrack was released on another thread - cleanly exit loop
                    break
                } catch (e: Exception) {
                    // Any other transient error - cleanly exit loop
                    break
                }

                // Safe loop pacing to guarantee we don't hog 100% CPU on headless/virtual audio drivers
                val elapsed = System.currentTimeMillis() - loopStartTime
                if (elapsed < bufferDurationMs - 15) {
                    delay(bufferDurationMs - elapsed)
                }

                // Throttle amplitude updates to ~10 FPS (100ms) to prevent Main Looper starvation
                val now = System.currentTimeMillis()
                if (now - lastAmplitudeUpdate >= 100) {
                    lastAmplitudeUpdate = now
                    val rms = kotlin.math.sqrt(bufferEnergy / (audioBuffer.size / 2)).toFloat().coerceIn(0.1f, 1.0f)
                    for (b in 0 until 32) {
                        val freqFactor = sin((b * 0.3) + (tick * 0.0001)).toFloat() * 0.25f
                        val base = (rms * (0.4f + (b % 4) * 0.15f) + freqFactor).coerceIn(0.08f, 0.98f)
                        ampArray[b] = base
                    }
                    val visualList = ampArray.toList()
                    handler.post {
                        if (isPlaying) {
                            listener?.onAmplitudes(visualList)
                        }
                    }
                }
            }
        }
    }

    private fun startVisualizerLoop(mp: MediaPlayer?) {
        scope.launch {
            val ampArray = FloatArray(32)
            var step = 0
            while (isActive && isPlaying && mediaPlayer != null) {
                step++
                for (b in 0 until 32) {
                    val wobble = sin((b * 0.4) + (step * 0.15)).toFloat() * 0.3f
                    val value = (0.35f + wobble + (b % 5) * 0.1f).coerceIn(0.08f, 0.95f)
                    ampArray[b] = value
                }
                val list = ampArray.toList()
                handler.post {
                    if (isPlaying) {
                        listener?.onAmplitudes(list)
                    }
                }
                delay(100) // 10 FPS rate-limited
            }
        }
    }

    override fun pause() {
        isPlaying = false
        handler.removeCallbacks(tickerRunnable)
        synthesisJob?.cancel()
        synthesisJob = null

        scope.launch(Dispatchers.IO) {
            try {
                synchronized(audioLock) {
                    val trackObj = audioTrack
                    if (trackObj != null && trackObj.state == AudioTrack.STATE_INITIALIZED &&
                        trackObj.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        trackObj.pause()
                    }
                }
                mediaPlayer?.pause()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    override fun resume() {
        if (currentTrack != null) {
            isPlaying = true
            handler.post(tickerRunnable)
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    mp.start()
                    startVisualizerLoop(mp)
                } catch (e: Exception) {
                    playSynthesizedTone(currentTrack!!, currentPositionSeconds)
                }
            } else {
                playSynthesizedTone(currentTrack!!, currentPositionSeconds)
            }
        }
    }

    override fun stop() {
        isPlaying = false
        handler.removeCallbacks(tickerRunnable)
        synthesisJob?.cancel()
        synthesisJob = null

        scope.launch(Dispatchers.IO) {
            synchronized(audioLock) {
                val trackToRelease = audioTrack
                audioTrack = null
                if (trackToRelease != null) {
                    try {
                        if (trackToRelease.state == AudioTrack.STATE_INITIALIZED) {
                            trackToRelease.pause()
                            trackToRelease.flush()
                            trackToRelease.stop()
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                    try {
                        trackToRelease.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            val playerToRelease = mediaPlayer
            mediaPlayer = null
            if (playerToRelease != null) {
                try {
                    playerToRelease.stop()
                    playerToRelease.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    override fun seekTo(seconds: Int) {
        seekToMs(seconds * 1000L)
    }

    override fun seekToMs(ms: Long) {
        val seconds = (ms / 1000L).toInt()
        currentPositionSeconds = seconds
        val duration = currentTrack?.durationSeconds?.coerceAtLeast(1) ?: 1
        val progress = (ms.toFloat() / (duration * 1000f)).coerceIn(0f, 1f)
        listener?.onPosition(seconds, progress)
        listener?.onPositionMs(ms, seconds, progress)

        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.seekTo(ms.toInt())
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    override fun setSoundPreset(preset: SoundPreset) {
        currentPreset = preset
    }

    override fun setBassBoost(enabled: Boolean) {
        bassBoostEnabled = enabled
    }

    override fun setVirtualizer(enabled: Boolean) {
        virtualizerEnabled = enabled
    }

    override fun setHiRes(enabled: Boolean) {
        hiResEnabled = enabled
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed = speed
        mediaPlayer?.let {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                }
            } catch (e: Exception) {
                Log.w(TAG, "setSpeed failed", e)
            }
        }
    }

    /**
     * Transient ducking for navigation prompts / notifications.
     * MediaPlayer uses real volume; synth path applies energy scaling
     * implicitly via [ducked] (kept for future use).
     */
    override fun setDucked(duck: Boolean) {
        ducked = duck
        try {
            mediaPlayer?.let {
                if (duck) it.setVolume(0.25f, 0.25f) else it.setVolume(1.0f, 1.0f)
            }
            audioTrack?.let {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        if (duck) it.setVolume(0.3f) else it.setVolume(1.0f)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "duck AudioTrack failed", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "setDucked failed", e)
        }
    }

    private fun applyDuckVolume(mp: MediaPlayer) {
        try {
            if (ducked) mp.setVolume(0.25f, 0.25f) else mp.setVolume(1.0f, 1.0f)
        } catch (e: Exception) {
            Log.w(TAG, "applyDuckVolume failed", e)
        }
    }

    override fun release() {
        try {
            handler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.w(TAG, "handler cleanup failed", e)
        }
        stop()
    }

    companion object {
        private const val TAG = "HiFiAudioEngine"
    }
}
