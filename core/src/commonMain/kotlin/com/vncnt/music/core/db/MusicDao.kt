package com.vncnt.music.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class GenreCount(val genre: String, val count: Int)
data class ArtistCount(val artist: String, val count: Int)

// Verbatim port of the Android DAO. Room KMP supports Flow + suspend in
// commonMain; SQL is unchanged so queries behave identically.
@Dao
interface MusicDao {
    @Query("SELECT * FROM tracks ORDER BY trackNumber ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY lastPlayedTimestamp DESC, trackNumber ASC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE genre = :genre ORDER BY trackNumber ASC")
    fun getTracksByGenre(genre: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE artist = :artist ORDER BY title ASC")
    fun getTracksByArtist(artist: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE album = :album ORDER BY trackNumber ASC")
    fun getTracksByAlbum(album: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchTracks(query: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE filePath = :filePath LIMIT 1")
    suspend fun getTrackByFilePath(filePath: String): TrackEntity?

    @Query("SELECT DISTINCT artist FROM tracks WHERE artist != '' ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT album FROM tracks WHERE album != '' ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM tracks")
    fun getTrackCount(): Flow<Int>

    @Query("SELECT * FROM tracks ORDER BY playCount DESC LIMIT :limit")
    fun getMostPlayedTracks(limit: Int = 10): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: String)

    @Query("DELETE FROM tracks WHERE filePath = :filePath")
    suspend fun deleteTrackByFilePath(filePath: String)

    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedTimestamp = :timestamp WHERE id = :trackId")
    suspend fun incrementPlayCount(trackId: String, timestamp: Long)

    @Query("UPDATE tracks SET skipCount = skipCount + 1 WHERE id = :trackId")
    suspend fun incrementSkipCount(trackId: String)

    @Query("DELETE FROM tracks WHERE id LIKE 'track_%' OR id LIKE 'scanned_local_%'")
    suspend fun deleteMockTracks()

    @Query("SELECT DISTINCT genre FROM tracks WHERE genre != '' ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>
}
