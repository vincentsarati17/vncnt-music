package com.vncnt.music.core.queue

import com.vncnt.music.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueNavigatorTest {

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "a",
        album = "b",
        durationSeconds = 60,
        genre = "Soul"
    )

    private val queue = listOf(track("1"), track("2"), track("3"))

    @Test
    fun nextAdvancesAndWraps() {
        assertEquals(1, QueueNavigator.nextIndex(queue, "1", false))
        assertEquals(2, QueueNavigator.nextIndex(queue, "2", false))
        assertEquals(0, QueueNavigator.nextIndex(queue, "3", false))
        assertEquals(0, QueueNavigator.nextIndex(queue, "unknown", false))
    }

    @Test
    fun previousRetreatsAndWraps() {
        assertEquals(0, QueueNavigator.previousIndex(queue, "2", false))
        assertEquals(2, QueueNavigator.previousIndex(queue, "1", false))
        assertEquals(2, QueueNavigator.previousIndex(queue, "unknown", false))
    }

    @Test
    fun emptyQueueReturnsNull() {
        assertNull(QueueNavigator.nextIndex(emptyList(), "1", false))
        assertNull(QueueNavigator.previousIndex(emptyList(), "1", false))
        assertNull(QueueNavigator.peekNext(emptyList(), "1"))
    }

    @Test
    fun shuffleDelegatesToInjectedRandom() {
        assertEquals(2, QueueNavigator.nextIndex(queue, "1", true) { 2 })
        assertEquals(0, QueueNavigator.previousIndex(queue, "3", true) { 0 })
    }

    @Test
    fun peekNextPreviewsWithoutMutating() {
        assertEquals("2", QueueNavigator.peekNext(queue, "1")?.id)
        assertEquals("1", QueueNavigator.peekNext(queue, "3")?.id)
        assertEquals("1", QueueNavigator.peekNext(queue, "unknown")?.id)
    }

    @Test
    fun restartThresholdMatchesViewmodel() {
        assertTrue(QueueNavigator.shouldRestartCurrent(4))
        assertFalse(QueueNavigator.shouldRestartCurrent(3))
    }
}
