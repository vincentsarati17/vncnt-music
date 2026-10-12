package com.vncnt.music.core.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

// Runs on JVM via jvmTest (bundled driver, no emulator). Mirrors the Android
// Robolectric DAO suite — both must stay green during migration.
class AppDatabaseTest {

    private fun openDb(): AppDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

    private fun entity(
        id: String,
        filePath: String,
        plays: Int = 0,
        favorite: Boolean = false
    ) = TrackEntity(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationSeconds = 200,
        genre = "Jazz",
        imageResName = "",
        audioToneType = "DEFAULT",
        customAudioUri = null,
        filePath = filePath,
        isFavorite = favorite,
        isHiRes = true,
        audioQualityBadge = "MP3 Offline • 320kbps",
        playCount = plays,
        skipCount = 0,
        lastPlayedTimestamp = 0L,
        trackNumber = 1
    )

    @Test
    fun tracksStoreQueryAndDeleteByFilePath() = runTest {
        val db = openDb()
        try {
            val dao = db.musicDao()
            dao.insertTracks(
                listOf(
                    entity("local_1", "/music/a.flac", plays = 5, favorite = true),
                    entity("local_2", "/music/b.flac", plays = 2)
                )
            )

            assertEquals(2, dao.getTrackCount().first())

            val byPath = dao.getTrackByFilePath("/music/a.flac")
            assertNotNull(byPath)
            assertEquals("local_1", byPath.id)

            assertEquals(2, dao.searchTracks("Title").first().size)
            assertEquals(1, dao.getFavoriteTracks().first().size)

            dao.deleteTrackByFilePath("/music/a.flac")
            assertNull(dao.getTrackByFilePath("/music/a.flac"))
            assertEquals(1, dao.getTrackCount().first())
        } finally {
            db.close()
        }
    }

    @Test
    fun lyricsInsertReadAndDelete() = runTest {
        val db = openDb()
        try {
            val dao = db.lyricsDao()
            dao.insertLyrics(
                LyricsEntity(
                    trackId = "t1",
                    rawLrcContent = "[00:01.00]Hello",
                    isSynced = true,
                    source = "ONLINE_API",
                    fetchedTimestamp = 123L
                )
            )

            val read = dao.getLyricsSync("t1")
            assertNotNull(read)
            assertEquals("[00:01.00]Hello", read.rawLrcContent)

            dao.deleteLyrics("t1")
            assertNull(dao.getLyricsSync("t1"))
        } finally {
            db.close()
        }
    }

    @Test
    fun entityDomainMappingRoundtrip() {
        val domain = entity("x", "/music/x.mp3").toDomain()
        assertEquals("x", domain.id)
        assertEquals("/music/x.mp3", domain.filePath)
        val back = TrackEntity.fromDomain(domain)
        assertEquals("x", back.id)
        assertEquals(200, back.durationSeconds)
    }
}
