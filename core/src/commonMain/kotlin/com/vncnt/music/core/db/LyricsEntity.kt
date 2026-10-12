package com.vncnt.music.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: String,
    val rawLrcContent: String,
    val isSynced: Boolean,
    val source: String,
    val fetchedTimestamp: Long
)
