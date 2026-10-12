package com.vncnt.music.core.playlists

import com.vncnt.music.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistBuilderTest {

    private fun track(
        id: String,
        genre: String = "Soul",
        favorite: Boolean = false,
        plays: Int = 0
    ) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist $id",
        album = "Album",
        durationSeconds = 180,
        genre = genre,
        isFavorite = favorite,
        playCount = plays
    )

    @Test
    fun emptyLibraryYieldsThreeSmartPlaylists() {
        val result = PlaylistBuilder.buildPlaylists(emptyList())
        assertEquals(listOf("all_tracks", "favorites", "heavy_rotation"), result.map { it.id })
        assertEquals(0, result[0].songCount)
    }

    @Test
    fun countsSongsArtistsFavoritesAndHeavyRotation() {
        val tracks = listOf(
            track("1", genre = "Soul", favorite = true, plays = 5),
            track("2", genre = "Soul", plays = 2),
            track("3", genre = "Jazz")
        )
        // Distinct artists to verify artistCount math.
        val withArtists = tracks.mapIndexed { i, t -> t.copy(artist = "Artist $i") }

        val result = PlaylistBuilder.buildPlaylists(withArtists)
        assertEquals(3, result[0].songCount)
        assertEquals(3, result[0].artistCount)
        assertEquals(1, result[1].songCount) // favorites
        assertEquals(2, result[2].songCount) // heavy rotation (playCount > 0)
        // Most-played sorts first.
        assertEquals("1", withArtists.sortedByDescending { it.playCount }[0].id)
    }

    @Test
    fun genrePlaylistsExcludeOfflinePlaceholderGenres() {
        val tracks = listOf(
            track("1", genre = "Local Offline"),
            track("2", genre = "Offline Audio"),
            track("3", genre = "Soul")
        )
        val result = PlaylistBuilder.buildPlaylists(tracks)
        val genreIds = result.map { it.id }.filter { it.startsWith("genre_") }
        assertEquals(listOf("genre_soul"), genreIds)
        assertTrue(result.none { it.filterTag == "Local Offline" })
    }

    @Test
    fun genreIdSanitizesSpaces() {
        val tracks = listOf(track("1", genre = "Hip Hop"))
        val result = PlaylistBuilder.buildPlaylists(tracks)
        assertTrue(result.any { it.id == "genre_hip_hop" && it.title == "Hip Hop Mix" })
    }
}
