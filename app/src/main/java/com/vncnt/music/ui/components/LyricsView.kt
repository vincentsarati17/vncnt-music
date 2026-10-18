package com.vncnt.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.data.model.LyricLine
import com.vncnt.music.data.model.LyricsData
import com.vncnt.music.data.model.LyricsSource
import com.vncnt.music.data.model.Track
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.DarkCardBorder
import com.vncnt.music.ui.theme.DarkSurface
import com.vncnt.music.ui.theme.DarkSurfaceVariant
import com.vncnt.music.ui.theme.HairlineBorder
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.StudioMint
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary
import com.vncnt.music.ui.theme.glassBorderBrush
import com.vncnt.music.ui.theme.glassmorphic
import kotlinx.coroutines.delay

@Composable
fun LyricsView(
    track: Track?,
    lyrics: LyricsData?,
    currentPositionMs: Long,
    isLoading: Boolean,
    onSeekToLyric: (Long) -> Unit,
    onFetchOnline: () -> Unit,
    onOpenImportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_view_container"),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = CoralRed,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Searching synchronized lyrics...",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }
            }

            lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainLyrics.isNullOrBlank()) -> {
                EmptyLyricsState(
                    track = track,
                    onFetchOnline = onFetchOnline,
                    onOpenImportDialog = onOpenImportDialog
                )
            }

            lyrics.isSynced -> {
                SyncedLyricsContent(
                    lyrics = lyrics,
                    currentPositionMs = currentPositionMs,
                    onSeekToLyric = onSeekToLyric,
                    onFetchOnline = onFetchOnline,
                    onOpenImportDialog = onOpenImportDialog
                )
            }

            else -> {
                UnsyncedLyricsContent(
                    lyrics = lyrics,
                    onFetchOnline = onFetchOnline,
                    onOpenImportDialog = onOpenImportDialog
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricsContent(
    lyrics: LyricsData,
    currentPositionMs: Long,
    onSeekToLyric: (Long) -> Unit,
    onFetchOnline: () -> Unit,
    onOpenImportDialog: () -> Unit
) {
    val listState = rememberLazyListState()
    var userScrolledManually by remember { mutableStateOf(false) }

    // Detect user manual dragging
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledManually = true
        }
    }

    // Auto reset manual scroll lock after 4 seconds of idle
    LaunchedEffect(userScrolledManually) {
        if (userScrolledManually) {
            delay(4000)
            userScrolledManually = false
        }
    }

    // Identify active lyric line
    val activeLineIndex by remember(lyrics, currentPositionMs) {
        derivedStateOf {
            lyrics.lines.indexOfLast { it.timestampMs <= currentPositionMs }
        }
    }

    // Smooth auto-centering
    LaunchedEffect(activeLineIndex, userScrolledManually) {
        if (!userScrolledManually && activeLineIndex >= 0) {
            val targetIndex = (activeLineIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(
                index = targetIndex,
                scrollOffset = 0
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("synced_lyrics_list"),
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp)
        ) {
            itemsIndexed(
                items = lyrics.lines,
                key = { index, line -> "${line.timestampMs}_$index" }
            ) { index, line ->
                val isActive = index == activeLineIndex
                val isPast = index < activeLineIndex

                LyricLineRow(
                    line = line,
                    isActive = isActive,
                    isPast = isPast,
                    onClick = {
                        userScrolledManually = false
                        onSeekToLyric(line.timestampMs)
                    }
                )
            }
        }

        // Top info badge: Source & format
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceVariant.copy(alpha = 0.85f))
                .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StudioMint)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val sourceLabel = when (lyrics.source) {
                LyricsSource.ONLINE_API -> "SYNCHRONIZED • LRCLIB"
                LyricsSource.SIDECAR_LRC -> "SYNCHRONIZED • .LRC FILE"
                LyricsSource.EMBEDDED_METADATA -> "SYNCHRONIZED • ID3 TAG"
                LyricsSource.USER_IMPORT -> "SYNCHRONIZED • IMPORTED"
                LyricsSource.CACHED_DATABASE -> "SYNCHRONIZED • OFFLINE"
            }
            Text(
                text = sourceLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Refresh",
                tint = TextMuted,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onFetchOnline() }
            )
        }

        // "Sync to Vocal" Floating Button (when user browsed away)
        AnimatedVisibility(
            visible = userScrolledManually && activeLineIndex >= 0,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(CoralRed)
                    .clickable {
                        userScrolledManually = false
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("sync_to_vocal_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Sync",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Follow Vocals",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineRow(
    line: LyricLine,
    isActive: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.04f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "LyricScale"
    )

    val alpha by animateFloatAsState(
        targetValue = when {
            isActive -> 1.0f
            isPast -> 0.38f
            else -> 0.65f
        },
        animationSpec = tween(durationMillis = 300),
        label = "LyricAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = if (isActive) 14.dp else 10.dp)
            .scale(scale)
            .alpha(alpha),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = line.text,
                style = if (isActive) {
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.3).sp,
                        lineHeight = 32.sp
                    )
                } else {
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        lineHeight = 26.sp
                    )
                }
            )

            if (isActive) {
                Text(
                    text = line.timestampFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CoralRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun UnsyncedLyricsContent(
    lyrics: LyricsData,
    onFetchOnline: () -> Unit,
    onOpenImportDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("unsynced_lyrics_content"),
        contentPadding = PaddingValues(top = 32.dp, bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLAIN TEXT LYRICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                TextButton(onClick = onFetchOnline) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Search Synced",
                        tint = CoralRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Search Synced",
                        style = MaterialTheme.typography.labelSmall.copy(color = CoralRed)
                    )
                }
            }
        }

        item {
            Text(
                text = lyrics.plainLyrics ?: lyrics.lines.joinToString("\n") { it.text },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary,
                    lineHeight = 30.sp,
                    fontSize = 17.sp
                )
            )
        }
    }
}

@Composable
private fun EmptyLyricsState(
    track: Track?,
    onFetchOnline: () -> Unit,
    onOpenImportDialog: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .testTag("empty_lyrics_state")
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant)
                .border(1.dp, HairlineBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "No Lyrics",
                tint = TextMuted,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No Lyrics Available",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (track != null) {
                "Synchronized lyrics have not been fetched for \"${track.title}\" yet."
            } else {
                "Select a track to view live lyrics."
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action: Fetch online via LRCLIB
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(CoralRed)
                .clickable { onFetchOnline() }
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("fetch_online_lyrics_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Fetch Online",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Find Lyrics Online (LRCLIB)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action: Import custom .LRC file / text
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                .clickable { onOpenImportDialog() }
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("import_custom_lrc_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Import LRC",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Import or Paste .LRC",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
