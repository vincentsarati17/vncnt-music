package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.volume
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFAudio.setCategory
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_get_main_queue
import kotlin.math.sin

/**
 * AVFoundation-backed engine for iOS. Plays real local files and remote
 * streams through AVPlayer; anything else (notably Android content:// URIs)
 * reports an honest error instead of faking playback.
 *
 * Observers post on the main queue, so listener callbacks are main-safe.
 * DSP presets have no AVAudioUnit counterpart yet and are accepted no-ops.
 * Every method is safe to call before/without playback (nil-guarded) so the
 * engine stays headless-test friendly.
 */
@OptIn(ExperimentalForeignApi::class)
class DarwinAudioEngine : AudioEngine {

    override var listener: AudioEngineListener? = null

    private var player: AVPlayer? = null
    private var currentTrack: Track? = null
    private var currentPositionSeconds: Int = 0
    private var playbackSpeed: Float = 1.0f
    private var ducked: Boolean = false
    private var isPlaying: Boolean = false

    private var timeObserver: Any? = null
    private var endObserver: NSObjectProtocol? = null
    private var tick: Int = 0
    private var audioSessionConfigured: Boolean = false

    // NOTE: no default value here — Kotlin forbids defaults on overrides;
    // the interface default (= 0) still applies to interface-typed callers.
    override fun playTrack(track: Track, startPositionSeconds: Int) {
        stopInternal()
        configureAudioSession()
        currentTrack = track
        currentPositionSeconds = startPositionSeconds.coerceAtLeast(0)
        val url = resolveUrl(track)
        if (url == null) {
            listener?.onError("No playable audio source for \"${track.title}\"")
            return
        }
        val item = AVPlayerItem.playerItemWithURL(url)
        val audioPlayer = requirePlayer()
        audioPlayer.replaceCurrentItemWithPlayerItem(item)
        observeEndOfItem(item)
        observeTime(audioPlayer)
        audioPlayer.volume = if (ducked) 0.25f else 1.0f
        if (startPositionSeconds > 0) {
            audioPlayer.seekToTime(CMTimeMakeWithSeconds(startPositionSeconds.toDouble(), 600))
        }
        isPlaying = true
        audioPlayer.play()
        if (playbackSpeed != 1.0f) {
            audioPlayer.rate = playbackSpeed
        }
        emitPosition()
    }

    override fun pause() {
        isPlaying = false
        player?.pause()
    }

    override fun resume() {
        val audioPlayer = player
        if (audioPlayer == null || audioPlayer.currentItem == null) return
        isPlaying = true
        audioPlayer.play()
        if (playbackSpeed != 1.0f) {
            audioPlayer.rate = playbackSpeed
        }
    }

    override fun stop() {
        stopInternal()
        currentPositionSeconds = 0
        listener?.onPosition(0, 0f)
        listener?.onPositionMs(0L, 0, 0f)
    }

    override fun seekTo(seconds: Int) {
        seekToMs(seconds * 1000L)
    }

    override fun seekToMs(positionMs: Long) {
        currentTrack ?: return
        val durationMs = durationMs()
        val clamped = positionMs.coerceIn(0L, durationMs)
        player?.seekToTime(CMTimeMakeWithSeconds(clamped / 1000.0, 600))
        currentPositionSeconds = (clamped / 1000L).toInt()
        val progress = progressOf(clamped, durationMs)
        listener?.onPosition(currentPositionSeconds, progress)
        listener?.onPositionMs(clamped, currentPositionSeconds, progress)
    }

    override fun setSoundPreset(preset: SoundPreset) {
        // No AVAudioUnitEQ counterpart yet; accepted and ignored (future slice).
    }

    override fun setBassBoost(enabled: Boolean) {
        // See setSoundPreset.
    }

    override fun setVirtualizer(enabled: Boolean) {
        // See setSoundPreset.
    }

    override fun setHiRes(enabled: Boolean) {
        // See setSoundPreset.
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        if (isPlaying) {
            player?.let { it.rate = playbackSpeed }
        }
    }

    override fun setDucked(duck: Boolean) {
        ducked = duck
        player?.let { it.volume = if (duck) 0.25f else 1.0f }
    }

    override fun release() {
        stopInternal()
        currentTrack = null
        listener = null
        player = null
    }

    // -- internals -----------------------------------------------------------

    private fun requirePlayer(): AVPlayer {
        var audioPlayer = player
        if (audioPlayer == null) {
            audioPlayer = AVPlayer()
            player = audioPlayer
        }
        return audioPlayer
    }

    /**
     * Playback category + activation: without it the ringer switch silences
     * playback and background audio is denied, even with the entitlement.
     * Best-effort — shape matches deployed real-world usage exactly
     * (attempt + swallow; no NSError plumbing in KN bindings).
     */
    private fun configureAudioSession() {
        if (audioSessionConfigured) return
        audioSessionConfigured = true
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (t: Throwable) {
            // Playback still functions without a configured session.
        }
    }

    private fun resolveUrl(track: Track): NSURL? {
        val raw = track.customAudioUri ?: track.filePath ?: return null
        if (raw.startsWith("content:")) return null
        return if (raw.startsWith("file://") || raw.startsWith("http://") || raw.startsWith("https://")) {
            NSURL.URLWithString(raw)
        } else {
            NSURL.fileURLWithPath(raw)
        }
    }

    private fun observeTime(audioPlayer: AVPlayer) {
        timeObserver?.let { audioPlayer.removeTimeObserver(it) }
        timeObserver = audioPlayer.addPeriodicTimeObserverForInterval(
            CMTimeMakeWithSeconds(0.3, 600),
            dispatch_get_main_queue()
        ) { time: CValue<CMTime> ->
            val seconds = CMTimeGetSeconds(time)
            if (!seconds.isNaN()) {
                currentPositionSeconds = seconds.toInt()
                val totalMs = durationMs()
                val posMs = (seconds * 1000.0).toLong().coerceIn(0L, totalMs)
                val progress = progressOf(posMs, totalMs)
                listener?.onPosition(currentPositionSeconds, progress)
                listener?.onPositionMs(posMs, currentPositionSeconds, progress)
                emitLevels()
            }
        }
    }

    private fun observeEndOfItem(item: AVPlayerItem) {
        val center = NSNotificationCenter.defaultCenter()
        endObserver?.let { center.removeObserver(it) }
        endObserver = center.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            item,
            NSOperationQueue.mainQueue()
        ) {
            handlePlaybackEnded()
        }
    }

    private fun handlePlaybackEnded() {
        isPlaying = false
        val durMs = durationMs()
        currentPositionSeconds = (durMs / 1000L).toInt()
        listener?.onPosition(currentPositionSeconds, 1f)
        listener?.onPositionMs(durMs, currentPositionSeconds, 1f)
        listener?.onTrackCompleted()
    }

    private fun durationMs(): Long {
        val itemSeconds = player?.currentItem?.duration?.let { CMTimeGetSeconds(it) }
        val seconds = if (itemSeconds != null && !itemSeconds.isNaN() && itemSeconds > 0.0) {
            itemSeconds
        } else {
            currentTrack?.durationSeconds?.toDouble() ?: 0.0
        }
        return (seconds * 1000.0).toLong().coerceAtLeast(1L)
    }

    private fun progressOf(positionMs: Long, durationMs: Long): Float {
        return (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    }

    private fun emitPosition() {
        val durMs = durationMs()
        val posMs = (currentPositionSeconds * 1000L).coerceIn(0L, durMs)
        val progress = progressOf(posMs, durMs)
        listener?.onPosition(currentPositionSeconds, progress)
        listener?.onPositionMs(posMs, currentPositionSeconds, progress)
    }

    private fun emitLevels() {
        tick++
        val amplitudes = List(32) { i ->
            val wobble = sin(i * 0.4 + tick * 0.15).toFloat() * 0.2f
            (0.35f + wobble + (i % 5) * 0.06f).coerceIn(0.08f, 0.9f)
        }
        listener?.onAmplitudes(amplitudes)
    }

    private fun stopInternal() {
        isPlaying = false
        timeObserver?.let { player?.removeTimeObserver(it) }
        timeObserver = null
        endObserver?.let { NSNotificationCenter.defaultCenter().removeObserver(it) }
        endObserver = null
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
    }
}
