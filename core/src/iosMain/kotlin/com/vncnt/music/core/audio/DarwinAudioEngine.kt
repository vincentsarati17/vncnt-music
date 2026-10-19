package com.vncnt.music.core.audio

import com.vncnt.music.core.model.SoundPreset
import com.vncnt.music.core.model.Track
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioUnitEQ
import platform.AVFAudio.AVAudioUnitEQFilterParameters
import platform.AVFAudio.rate
import platform.AVFAudio.sampleRate
import platform.AVFAudio.setActive
import platform.AVFAudio.startAndReturnError
import platform.AVFAudio.stop
import platform.AVFoundation.AVAssetExportPresetAppleM4A
import platform.AVFoundation.AVAssetExportSession
import platform.AVFoundation.AVFileTypeAppleM4A
import platform.AVFoundation.AVURLAsset
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.Foundation.setValue
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.log10
import kotlin.math.sin

/**
 * AVAudioEngine-backed engine for iOS. The graph is
 * player → AVAudioUnitEQ → mainMixer, so DSP presets are real filter
 * bands (see setSoundPreset). Transport uses scheduleSegment (seek is an
 * explicit startingFrame), position comes from player render time, and
 * end-of-track from the schedule completion handler. ipod-library:// assets
 * are exported to a temp m4a first (AVAudioFile needs a plain file URL);
 * Android content:// URIs and remote streams report an honest error instead
 * of faking playback.
 *
 * Observers post on the main queue, so listener callbacks are main-safe.
 * Every method is safe to call before/without playback (nil-guarded) so the
 * engine stays headless-test friendly.
 */
@OptIn(ExperimentalForeignApi::class)
class DarwinAudioEngine : AudioEngine {

    override var listener: AudioEngineListener? = null

    private var engine: AVAudioEngine? = null
    private var player: AVAudioPlayerNode? = null
    private var eq: AVAudioUnitEQ? = null
    private var currentFile: AVAudioFile? = null
    private var currentTrack: Track? = null
    private var currentPositionSeconds: Int = 0
    private var currentDurationMs: Long = 1L
    private var playbackSpeed: Float = 1.0f
    private var ducked: Boolean = false
    private var isPlaying: Boolean = false

    private var engineStarted: Boolean = false
    private var startFrames: Long = 0L
    private var loadId: Int = 0
    private var timer: NSTimer? = null
    private var tick: Int = 0
    private var audioSessionConfigured: Boolean = false
    private var currentPreset: SoundPreset = SoundPreset.STUDIO_FLAT
    private var bassBoostEnabled: Boolean = false
    private var virtualizerEnabled: Boolean = false
    private var hiResEnabled: Boolean = true

    // NOTE: no default value here — Kotlin forbids defaults on overrides;
    // the interface default (= 0) still applies to interface-typed callers.
    override fun playTrack(track: Track, startPositionSeconds: Int) {
        stopInternal()
        configureAudioSession()
        currentTrack = track
        currentFile = null
        currentPositionSeconds = startPositionSeconds.coerceAtLeast(0)
        currentDurationMs = (track.durationSeconds.toLong() * 1000L).coerceAtLeast(1L)
        val url = resolveUrl(track)
        if (url == null) {
            listener?.onError("No playable audio source for \"${track.title}\"")
            return
        }
        val myLoad = ++loadId
        val scheme = url.scheme ?: ""
        when {
            scheme == "ipod-library" -> exportAndPlay(url, track, startPositionSeconds, myLoad)
            scheme == "http" || scheme == "https" -> listener?.onError(
                "Remote streams are not playable in this iOS build yet."
            )
            else -> openAndPlay(url, track, startPositionSeconds, myLoad)
        }
    }

    override fun pause() {
        isPlaying = false
        player?.pause()
    }

    override fun resume() {
        val audioPlayer = player ?: return
        if (currentTrack == null || currentFile == null) return
        isPlaying = true
        audioPlayer.play()
        audioPlayer.rate = playbackSpeed
        startTimerIfNeeded()
        emitPosition()
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
        val track = currentTrack ?: return
        // Seek reuses the already-open file: device-library exports are
        // unlinked after open, so the handle is the only source.
        val file = currentFile ?: return
        val clamped = positionMs.coerceIn(0L, currentDurationMs)
        currentPositionSeconds = (clamped / 1000L).toInt()
        val wasPlaying = isPlaying
        val rate = sampleRateOf(file, track)
        scheduleAndPlay(file, (clamped.toDouble() / 1000.0 * rate).toLong(), rate, wasPlaying)
        emitPosition()
    }

    override fun setSoundPreset(preset: SoundPreset) {
        currentPreset = preset
        applyEq()
    }

    override fun setBassBoost(enabled: Boolean) {
        bassBoostEnabled = enabled
        applyEq()
    }

    override fun setVirtualizer(enabled: Boolean) {
        virtualizerEnabled = enabled
        applyEq()
    }

    override fun setHiRes(enabled: Boolean) {
        // iOS renders through the system mixer at float32; there is no
        // bit-depth switch to flip. Stored for state parity with Android.
        hiResEnabled = enabled
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2.0f)
        player?.rate = playbackSpeed
    }

    override fun setDucked(duck: Boolean) {
        ducked = duck
        engine?.mainMixerNode?.outputVolume = if (duck) 0.25f else 1.0f
    }

    override fun release() {
        stopInternal()
        engine?.stop()
        engineStarted = false
        currentTrack = null
        currentFile = null
        listener = null
        engine = null
        player = null
        eq = null
    }

    // -- internals -----------------------------------------------------------

    private fun requireGraph(): Pair<AVAudioEngine, AVAudioPlayerNode> {
        var audioEngine = engine
        var audioPlayer = player
        if (audioEngine == null || audioPlayer == null) {
            audioEngine = AVAudioEngine()
            audioPlayer = AVAudioPlayerNode()
            val eqNode = AVAudioUnitEQ(4u)
            audioEngine.attachNode(audioPlayer)
            audioEngine.attachNode(eqNode)
            audioEngine.connect(audioPlayer, eqNode, null)
            audioEngine.connect(eqNode, audioEngine.mainMixerNode, null)
            // enableRate has no Kotlin binding name in this KN version; KVC
            // sets the ObjC property directly (it is KVC-compliant).
            audioPlayer.setValue(true, "enableRate")
            audioEngine.mainMixerNode.outputVolume = if (ducked) 0.25f else 1.0f
            engine = audioEngine
            player = audioPlayer
            eq = eqNode
            applyEq()
        }
        return audioEngine to audioPlayer
    }

    /**
     * Maps the shared 3-band preset multipliers onto four parametric EQ
     * bands (Android parity: bassBoost multiplies the bass multiplier by
     * 1.35). The band objects expose no Kotlin property names in this KN
     * build, so values go through KVC (frequency/gain/bandwidth are
     * KVC-compliant ObjC properties). Virtualizer is approximated with a
     * presence lift — iOS has no stereo-phase virtualizer node.
     */
    private fun applyEq() {
        val eqNode = eq ?: return
        val bandsList = eqNode.bands ?: return
        if (bandsList.size < 4) return
        val bassMult = if (bassBoostEnabled) currentPreset.bass * 1.35f else currentPreset.bass
        setBand(bandsList, 0, 100f, toDb(bassMult), 1.0f)
        setBand(bandsList, 1, 1000f, toDb(currentPreset.mid), 1.0f)
        setBand(bandsList, 2, 8000f, toDb(currentPreset.treble), 1.0f)
        setBand(bandsList, 3, 3500f, if (virtualizerEnabled) 1.5f else 0f, 2.0f)
    }

    private fun setBand(bandsList: List<*>, index: Int, frequency: Float, gainDb: Float, bandwidth: Float) {
        val band = bandsList.getOrNull(index) as? AVAudioUnitEQFilterParameters ?: return
        band.setValue(frequency, "frequency")
        band.setValue(gainDb, "gain")
        band.setValue(bandwidth, "bandwidth")
    }

    private fun toDb(multiplier: Float): Float = (20.0 * log10(multiplier.toDouble())).toFloat()

    private fun ensureStarted(): Boolean {
        val (audioEngine, _) = requireGraph()
        if (!engineStarted) {
            val ok = audioEngine.startAndReturnError(null)
            if (!ok) {
                listener?.onError("The audio engine failed to start.")
                return false
            }
            engineStarted = true
        }
        return true
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
        // ipod-library:// is the device-library asset URL (MPMediaItem).
        return if (raw.startsWith("file://") || raw.startsWith("http://") ||
            raw.startsWith("https://") || raw.startsWith("ipod-library://")
        ) {
            NSURL.URLWithString(raw)
        } else {
            NSURL.fileURLWithPath(raw)
        }
    }

    private fun openAndPlay(url: NSURL, track: Track, startPosition: Int, myLoad: Int) {
        val file = try {
            AVAudioFile(url, null)
        } catch (t: Throwable) {
            listener?.onError("Could not open \"${track.title}\" for playback.")
            return
        }
        playOpenedFile(file, track, startPosition, myLoad)
    }

    /**
     * AVAudioFile only reads plain file URLs; device-library assets are
     * exported to a temp m4a first. DRM-protected items fail the export and
     * get an honest error.
     */
    private fun exportAndPlay(url: NSURL, track: Track, startPosition: Int, myLoad: Int) {
        val exporter = try {
            AVAssetExportSession(AVURLAsset(url, null), AVAssetExportPresetAppleM4A)
        } catch (t: Throwable) {
            null
        }
        if (exporter == null) {
            listener?.onError("Could not prepare \"${track.title}\" from the device library.")
            return
        }
        val tempUrl = NSURL.fileURLWithPath(NSTemporaryDirectory() + "vncnt-device-export.m4a")
        exporter.outputURL = tempUrl
        exporter.outputFileType = AVFileTypeAppleM4A
        exporter.exportAsynchronouslyWithCompletionHandler {
            dispatch_async(dispatch_get_main_queue()) {
                if (myLoad != loadId) return@dispatch_async
                openAndPlay(tempUrl, track, startPosition, myLoad)
                NSFileManager.defaultManager().removeItemAtURL(tempUrl, null)
            }
        }
    }

    private fun playOpenedFile(file: AVAudioFile, track: Track, startPosition: Int, myLoad: Int) {
        currentFile = file
        val rate = sampleRateOf(file, track)
        val startFrame = (startPosition.coerceAtLeast(0).toLong() * rate).toLong()
        scheduleAndPlay(file, startFrame, rate, true)
    }

    private fun sampleRateOf(file: AVAudioFile, track: Track): Double {
        val frames = file.length
        val seconds = track.durationSeconds.coerceAtLeast(1).toDouble()
        return if (frames > 0L) frames.toDouble() / seconds else 44100.0
    }

    private fun scheduleAndPlay(file: AVAudioFile, fromFrame: Long, rate: Double, beginPlaying: Boolean) {
        if (!ensureStarted()) return
        val (_, audioPlayer) = requireGraph()
        val myLoad = ++loadId
        startFrames = fromFrame.coerceAtLeast(0L)
        val count = (file.length - startFrames).coerceAtLeast(0L).toUInt()
        audioPlayer.stop()
        audioPlayer.scheduleSegment(file, startFrames, count, null) {
            dispatch_async(dispatch_get_main_queue()) {
                if (myLoad == loadId) handlePlaybackEnded()
            }
        }
        audioPlayer.rate = playbackSpeed
        if (beginPlaying) {
            isPlaying = true
            audioPlayer.play()
            startTimerIfNeeded()
            emitPosition()
        }
    }

    private fun handlePlaybackEnded() {
        isPlaying = false
        timer?.invalidate()
        timer = null
        val durMs = currentDurationMs
        currentPositionSeconds = (durMs / 1000L).toInt()
        listener?.onPosition(currentPositionSeconds, 1f)
        listener?.onPositionMs(durMs, currentPositionSeconds, 1f)
        listener?.onTrackCompleted()
    }

    private fun startTimerIfNeeded() {
        if (timer != null) return
        timer = NSTimer.scheduledTimerWithTimeInterval(0.3, true) {
            onTimerTick()
        }
    }

    private fun onTimerTick() {
        if (!isPlaying) return
        val audioPlayer = player ?: return
        val nodeTime = audioPlayer.lastRenderTime ?: return
        val playerTime = audioPlayer.playerTimeForNodeTime(nodeTime) ?: return
        val rate = playerTime.sampleRate
        if (rate <= 0.0) return
        val frames = startFrames + playerTime.sampleTime
        val posMs = (frames / rate * 1000.0).toLong().coerceIn(0L, currentDurationMs)
        currentPositionSeconds = (posMs / 1000L).toInt()
        val progress = progressOf(posMs, currentDurationMs)
        listener?.onPosition(currentPositionSeconds, progress)
        listener?.onPositionMs(posMs, currentPositionSeconds, progress)
        emitLevels()
    }

    private fun progressOf(positionMs: Long, durationMs: Long): Float {
        return (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    }

    private fun emitPosition() {
        val durMs = currentDurationMs
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
        loadId++
        timer?.invalidate()
        timer = null
        player?.stop()
    }
}
