package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Production audio-focus wrapper.
 *
 * - Requests [AudioManager.AUDIOFOCUS_GAIN] before playback.
 * - Maps focus changes to pause / duck / resume callbacks so the engine
 *   behaves correctly during calls, navigation prompts, and other apps.
 * - Works back to minSdk 24 (uses AudioFocusRequest on O+).
 */
class AudioFocusHelper(
    context: Context,
    private val onPauseForLoss: () -> Unit = {},
    private val onResumeAfterGain: () -> Unit = {},
    private val onDuck: (duck: Boolean) -> Unit = {}
) {

    private val appContext = context.applicationContext
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Stored as Any to avoid class-verification issues on API 24-25 where
    // AudioFocusRequest does not exist. Only touched on O+.
    private var focusRequest: Any? = null

    @Suppress("DEPRECATION")
    private val legacyListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleFocusChange(focusChange)
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleFocusChange(focusChange)
    }

    private fun handleFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost ($focusChange) -> pausing")
                onPauseForLoss()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus duck")
                onDuck(true)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained -> unduck/resume hook")
                onDuck(false)
                onResumeAfterGain()
            }
        }
    }

    /** @return true when focus was granted (or already held). */
    fun requestFocus(): Boolean {
        return try {
            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest = buildFocusRequestOreo()
                @Suppress("NewApi")
                audioManager.requestAudioFocus(focusRequest as AudioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    legacyListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ||
                res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED
        } catch (e: Exception) {
            Log.w(TAG, "requestFocus failed", e)
            // Fail open: allow playback even if focus API throws on odd OEM ROMs.
            true
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun buildFocusRequestOreo(): AudioFocusRequest {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
    }

    fun abandonFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (focusRequest as? AudioFocusRequest)?.let {
                    @Suppress("NewApi")
                    audioManager.abandonAudioFocusRequest(it)
                }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(legacyListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "abandonFocus failed", e)
        }
    }

    companion object {
        private const val TAG = "AudioFocusHelper"
    }
}
