package com.vncnt.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vncnt.music.core.model.PlaybackState
import com.vncnt.music.core.model.Track
import com.vncnt.music.ui.components.rememberDrawableResId
import com.vncnt.music.ui.theme.CoralRed
import com.vncnt.music.ui.theme.DarkBackground
import com.vncnt.music.ui.theme.FavoriteHeart
import com.vncnt.music.ui.theme.HiResGold
import com.vncnt.music.ui.theme.TextMuted
import com.vncnt.music.ui.theme.TextPrimary
import com.vncnt.music.ui.theme.TextSecondary
import com.vncnt.music.ui.theme.glassBackgroundBrush
import com.vncnt.music.ui.theme.glassBorderBrush
import com.vncnt.music.ui.theme.glassCoralBorderBrush
import com.vncnt.music.ui.theme.glassmorphic

@Composable
fun FavoritesScreen(
    favoriteTracks: List<Track>,
    playbackState: PlaybackState,
    onTrackClick: (Track) -> Unit,
    onShuffleFavorites: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .testTag("favorites_screen"),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${favoriteTracks.size} loved songs • Offline high-fidelity",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary
                            )
                        )
                    }

                    if (favoriteTracks.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .glassmorphic(
                                    shape = RoundedCornerShape(20.dp),
                                    borderBrush = glassCoralBorderBrush(),
                                    backgroundBrush = glassBackgroundBrush(startAlpha = 0.5f, endAlpha = 0.25f)
                                )
                                .clickable(onClick = onShuffleFavorites)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("fav_shuffle_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = null,
                                    tint = CoralRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Shuffle",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        if (favoriteTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF2C2F3A),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No favorites yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Tap the heart icon on any track to add it here",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextMuted
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            itemsIndexed(favoriteTracks, key = { _, track -> track.id }) { index, track ->
                val isCurrent = playbackState.currentTrack?.id == track.id
                val resId = rememberDrawableResId(context, track.imageResName)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(track) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag("fav_track_${track.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%02d".format(index + 1),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isCurrent) CoralRed else TextMuted,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.width(28.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22242D)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (resId != 0) {
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = track.artist,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCurrent) CoralRed else TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${track.artist}  •  ${track.durationFormatted}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                            if (track.isHiRes) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hi-Res",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = HiResGold,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.testTag("unfav_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Remove Favorite",
                            tint = FavoriteHeart,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
