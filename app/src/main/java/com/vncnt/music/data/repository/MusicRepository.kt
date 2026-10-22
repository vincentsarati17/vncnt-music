package com.vncnt.music.data.repository

import android.content.Context
import com.vncnt.music.core.db.AppDatabase
// Companion extension from :core androidMain (AppDatabase is a typealias;
// extensions need an explicit import at each call site).
import com.vncnt.music.core.db.getDatabase
import com.vncnt.music.core.db.TrackEntity
import com.vncnt.music.core.model.Playlist
import com.vncnt.music.core.model.Track
import com.vncnt.music.data.scanner.LocalAudioScanner
import com.vncnt.music.data.scanner.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.musicDao()

    val allTracks: Flow<List<Track>> = dao.getAllTracks().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteTracks: Flow<List<Track>> = dao.getFavoriteTracks().map { list ->
        list.map { it.toDomain() }
    }

    val mostPlayedTracks: Flow<List<Track>> = dao.getMostPlayedTracks(10).map { list ->
        list.map { it.toDomain() }
    }

    val totalTrackCount: Flow<Int> = dao.getTrackCount()

    val allArtists: Flow<List<String>> = dao.getAllArtists()

    val allAlbums: Flow<List<String>> = dao.getAllAlbums()

    fun searchTracks(query: String): Flow<List<Track>> = dao.searchTracks(query).map { list ->
        list.map { it.toDomain() }
    }

    fun getTracksByArtist(artist: String): Flow<List<Track>> = dao.getTracksByArtist(artist).map { list ->
        list.map { it.toDomain() }
    }

    fun getTracksByAlbum(album: String): Flow<List<Track>> = dao.getTracksByAlbum(album).map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getTrackByFilePath(filePath: String): Track? = withContext(Dispatchers.IO) {
        dao.getTrackByFilePath(filePath)?.toDomain()
    }

    suspend fun deleteTrack(trackId: String) = withContext(Dispatchers.IO) {
        dao.deleteTrackById(trackId)
    }

    suspend fun deleteTrackByFilePath(filePath: String) = withContext(Dispatchers.IO) {
        dao.deleteTrackByFilePath(filePath)
    }

    suspend fun initializePreloadedTracksIfEmpty() = withContext(Dispatchers.IO) {
        // Clean up any residual sample/mock tracks from earlier builds so the user's library is 100% genuine
        dao.deleteMockTracks()
    }

    suspend fun toggleFavorite(trackId: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(trackId, !currentStatus)
    }

    suspend fun recordTrackPlayed(track: Track, listenedSeconds: Int, completed: Boolean) = withContext(Dispatchers.IO) {
        dao.incrementPlayCount(track.id, System.currentTimeMillis())
    }

    suspend fun recordTrackSkipped(trackId: String) = withContext(Dispatchers.IO) {
        dao.incrementSkipCount(trackId)
    }

    suspend fun importLocalTrack(
        title: String,
        artist: String,
        durationSec: Int,
        uri: String,
        filePath: String? = null
    ) = withContext(Dispatchers.IO) {
        val id = "imported_${System.currentTimeMillis()}_${(1000..9999).random()}"
        val newTrack = Track(
            id = id,
            title = title,
            artist = artist.ifEmpty { "Local Artist" },
            album = "Local Storage",
            durationSeconds = durationSec.coerceAtLeast(1),
            genre = "Offline Audio",
            imageResName = "",
            audioToneType = "DEFAULT",
            customAudioUri = uri,
            filePath = filePath ?: uri,
            isFavorite = false,
            isHiRes = true,
            audioQualityBadge = "Offline Master • Lossless",
            trackNumber = 1
        )
        dao.insertTrack(TrackEntity.fromDomain(newTrack))
    }

    private val scanner = LocalAudioScanner(context)

    suspend fun scanDeviceAudio(): ScanResult = withContext(Dispatchers.IO) {
        scanner.scanDeviceAudio()
    }

    suspend fun addScannedTracks(tracks: List<Track>) = withContext(Dispatchers.IO) {
        val entities = tracks.map { TrackEntity.fromDomain(it) }
        dao.insertTracks(entities)
    }

    suspend fun importLocalUris(uris: List<android.net.Uri>): Int = withContext(Dispatchers.IO) {
        val tracks = uris.map { uri -> scanner.extractTrackFromUri(uri) }
        dao.insertTracks(tracks.map { TrackEntity.fromDomain(it) })
        tracks.size
    }

    /**
     * Builds dynamic playlists strictly computed from the actual tracks in the user's library.
     *
     * Delegates to shared :core logic (unit-tested there); app types are
     * typealiases of the core types so this is a zero-cost pass-through.
     */
    fun buildPlaylists(tracks: List<Track>): List<Playlist> =
        com.vncnt.music.core.playlists.PlaylistBuilder.buildPlaylists(tracks)

    fun getAvailablePlaylists(): List<Playlist> = buildPlaylists(emptyList())
}
