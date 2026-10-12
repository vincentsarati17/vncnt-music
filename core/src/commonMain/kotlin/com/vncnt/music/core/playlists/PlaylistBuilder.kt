package com.vncnt.music.core.playlists

import com.vncnt.music.core.model.Playlist
import com.vncnt.music.core.model.Track

/**
 * Pure playlist construction, extracted verbatim from Android
 * MusicRepository.buildPlaylists. Operates only on [Track] — no Context,
 * no database, no I/O — so it compiles in commonMain as-is.
 */
object PlaylistBuilder {

    /**
     * Builds dynamic playlists strictly computed from the actual tracks in the user's library.
     */
    fun buildPlaylists(tracks: List<Track>): List<Playlist> {
        val playlists = mutableListOf<Playlist>()
        val totalSongs = tracks.size
        val totalArtists = tracks.map { it.artist }.distinct().count { it.isNotBlank() }

        // 1. All Offline Tracks
        playlists.add(
            Playlist(
                id = "all_tracks",
                title = "All Offline Tracks",
                subtitle = "$totalSongs songs • $totalArtists artists",
                songCount = totalSongs,
                artistCount = totalArtists,
                bannerDrawableName = "banner_headphones",
                description = "Complete local offline music collection in high-fidelity quality.",
                isSmartCurated = true,
                filterTag = "ALL"
            )
        )

        // 2. Favorites
        val favorites = tracks.filter { it.isFavorite }
        playlists.add(
            Playlist(
                id = "favorites",
                title = "Favorites",
                subtitle = "${favorites.size} loved tracks",
                songCount = favorites.size,
                artistCount = favorites.map { it.artist }.distinct().count { it.isNotBlank() },
                bannerDrawableName = "banner_headphones",
                description = "Your favorite offline tracks.",
                isSmartCurated = true,
                filterTag = "FAVORITES"
            )
        )

        // 3. Heavy Rotation (Most Played)
        val playedTracks = tracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        playlists.add(
            Playlist(
                id = "heavy_rotation",
                title = "Heavy Rotation",
                subtitle = if (playedTracks.isEmpty()) "0 tracks played" else "${playedTracks.size} most played tracks",
                songCount = playedTracks.size,
                artistCount = playedTracks.map { it.artist }.distinct().count { it.isNotBlank() },
                bannerDrawableName = "banner_headphones",
                description = "Your most replayed high-fidelity tracks.",
                isSmartCurated = true,
                filterTag = "TOP"
            )
        )

        // 4. Dynamic Genre Playlists based on actual genres found in the user's library
        val genres = tracks.map { it.genre }
            .filter { it.isNotBlank() && it != "Local Offline" && it != "Offline Audio" }
            .distinct()
            .sorted()

        genres.forEach { genre ->
            val genreTracks = tracks.filter { it.genre.equals(genre, ignoreCase = true) }
            val genreArtists = genreTracks.map { it.artist }.distinct().count { it.isNotBlank() }
            playlists.add(
                Playlist(
                    id = "genre_${genre.lowercase().replace(" ", "_")}",
                    title = "$genre Mix",
                    subtitle = "${genreTracks.size} songs • $genreArtists artists",
                    songCount = genreTracks.size,
                    artistCount = genreArtists,
                    bannerDrawableName = "banner_headphones",
                    description = "All $genre tracks stored on this device.",
                    isSmartCurated = false,
                    filterTag = genre
                )
            )
        }

        return playlists
    }

    fun emptyLibraryPlaylists(): List<Playlist> = buildPlaylists(emptyList())
}
