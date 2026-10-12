package com.vncnt.music.core.audio

import com.vncnt.music.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StubAudioEngineTest {

    private fun track() = Track(
        id = "t1",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationSeconds = 200,
        genre = "Soul"
    )

    @Test
    fun playPauseResumeStopTrackState() {
        val engine: AudioEngine = StubAudioEngine()
        val stub = engine as StubAudioEngine

        assertFalse(stub.isPlaying)
        assertNull(stub.currentTrack)

        engine.playTrack(track(), 10)
        assertTrue(stub.isPlaying)
        assertEquals("t1", stub.currentTrack?.id)
        assertEquals(10, stub.positionSeconds)

        engine.pause()
        assertFalse(stub.isPlaying)

        engine.resume()
        assertTrue(stub.isPlaying)

        engine.stop()
        assertFalse(stub.isPlaying)
        assertEquals(0, stub.positionSeconds)
    }

    @Test
    fun seekNotifiesListenerWithProgress() {
        val engine = StubAudioEngine()
        val positions = mutableListOf<Pair<Int, Float>>()
        engine.listener = object : AudioEngineListener {
            override fun onPosition(positionSeconds: Int, progress: Float) {
                positions += positionSeconds to progress
            }

            override fun onPositionMs(positionMs: Long, positionSeconds: Int, progress: Float) = Unit
            override fun onAmplitudes(amplitudes: List<Float>) = Unit
            override fun onTrackCompleted() = Unit
            override fun onError(message: String) = Unit
        }

        engine.playTrack(track())
        engine.seekTo(100)

        assertEquals(100, engine.positionSeconds)
        assertTrue(positions.isNotEmpty())
        assertEquals(100, positions.last().first)
        assertEquals(0.5f, positions.last().second)
    }

    @Test
    fun seekClampsToDuration() {
        val engine = StubAudioEngine()
        engine.playTrack(track())
        engine.seekTo(9999)
        assertEquals(200, engine.positionSeconds)
        engine.seekTo(-5)
        assertEquals(0, engine.positionSeconds)
    }
}
