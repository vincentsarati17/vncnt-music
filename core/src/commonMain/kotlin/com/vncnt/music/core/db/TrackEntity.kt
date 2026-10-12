package com.vncnt.music.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vncnt.music.core.model.Track

// Canonical library entity. Verbatim port of the Android entity — same table,
// columns, and indices, so existing installs upgrade without migration.
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["artist"]),
        Index(value = ["title"]),
        Index(value = ["filePath"])
    ]
)
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val genre: String,
    val imageResName: String,
    val audioToneType: String,
    val customAudioUri: String?,
    val filePath: String?,
    val isFavorite: Boolean,
    val isHiRes: Boolean,
    val audioQualityBadge: String,
    val playCount: Int,
    val skipCount: Int,
    val lastPlayedTimestamp: Long,
    val trackNumber: Int
) {
    fun toDomain(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationSeconds = durationSeconds,
        genre = genre,
        imageResName = imageResName,
        audioToneType = audioToneType,
        customAudioUri = customAudioUri,
        filePath = filePath,
        isFavorite = isFavorite,
        isHiRes = isHiRes,
        audioQualityBadge = audioQualityBadge,
        playCount = playCount,
        skipCount = skipCount,
        lastPlayedTimestamp = lastPlayedTimestamp,
        trackNumber = trackNumber
    )

    companion object {
        fun fromDomain(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationSeconds = track.durationSeconds,
            genre = track.genre,
            imageResName = track.imageResName,
            audioToneType = track.audioToneType,
            customAudioUri = track.customAudioUri,
            filePath = track.filePath,
            isFavorite = track.isFavorite,
            isHiRes = track.isHiRes,
            audioQualityBadge = track.audioQualityBadge,
            playCount = track.playCount,
            skipCount = track.skipCount,
            lastPlayedTimestamp = track.lastPlayedTimestamp,
            trackNumber = track.trackNumber
        )
    }
}
