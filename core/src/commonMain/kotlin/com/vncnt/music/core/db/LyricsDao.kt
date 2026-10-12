package com.vncnt.music.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Query("SELECT * FROM track_lyrics WHERE trackId = :trackId LIMIT 1")
    fun getLyricsForTrack(trackId: String): Flow<LyricsEntity?>

    @Query("SELECT * FROM track_lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyricsSync(trackId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM track_lyrics WHERE trackId = :trackId")
    suspend fun deleteLyrics(trackId: String)

    @Query("DELETE FROM track_lyrics")
    suspend fun clearAllLyrics()
}
