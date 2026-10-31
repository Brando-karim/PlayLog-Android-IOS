package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.ui.components.CircleIconButton
import com.playlog.app.ui.components.DiaryRow
import com.playlog.app.ui.components.GameTile
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.Monogram
import com.playlog.app.ui.components.PlaylogDialog
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.SectionHeading
import com.playlog.app.ui.components.StatCard
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal
import java.util.Locale

@Composable
fun ProfileScreen(
    vm: PlaylogViewModel,
    onGame: (Long) -> Unit,
    onDiary: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    val favorites = snapshot.gamesFor(snapshot.favorites)
    val recent = snapshot.diary.sortedByDescending { it.playedAt }.take(3)
    val maxHistogram = (stats.ratingHistogram.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val maxGenre = (stats.topGenres.firstOrNull()?.second ?: 1).coerceAtLeast(1)

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 34.dp),
    ) {
        item {
            Kicker("Player profile")
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Monogram(name = snapshot.displayName, size = 78.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = snapshot.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MintWhite,
                            maxLines = 2,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(6.dp))
                        CircleIconButton(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = "Edit name",
                            onClick = { editing = true },
                            size = 30.dp,
                            iconSize = 13.dp,
                            tint = Sage,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Local-first player",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${snapshot.library.size} games tracked",
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("Played", stats.played.toString(), Teal, Modifier.weight(1f))
                StatCard("Playing", stats.playing.toString(), Accent, Modifier.weight(1f))
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("Backlog", stats.backlog.toString(), Sage, Modifier.weight(1f))
                StatCard("Liked", stats.liked.toString(), PaleMint, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            LedgerPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 17.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SummaryItem(
                        label = "Average",
                        value = stats.averageRating?.let {
                            String.format(Locale.US, "%.2f", it)
                        } ?: "—",
                        accent = Accent,
                        modifier = Modifier.weight(1f),
                    )
                    VerticalRule()
                    SummaryItem(
                        label = "Reviews",
                        value = stats.reviews.toString(),
                        accent = MintWhite,
                        modifier = Modifier.weight(1f),
                    )
                    VerticalRule()
                    SummaryItem(
                        label = "Dropped",
                        value = stats.dropped.toString(),
                        accent = MintWhite,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(30.dp))
        }

        if (favorites.isNotEmpty()) {
            item {
                SectionHeading(
                    kicker = "Favourites",
                    title = "Kept close",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    items(favorites, key = { it.id }) { game ->
                        GameTile(
                            game = game,
                            onClick = { onGame(game.id) },
                            modifier = Modifier.width(118.dp),
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        if (stats.ratingHistogram.isNotEmpty()) {
            item {
                SectionHeading(
                    kicker = "Statistics",
                    title = "Rating spread",
                )
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    for (star in 5 downTo 1) {
                        val count = stats.ratingHistogram[star] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = star.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = PaleMint,
                                modifier = Modifier.width(13.dp),
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_star),
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(9.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MintWhite.copy(alpha = 0.055f)),
                            ) {
                                if (count > 0) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(count.toFloat() / maxHistogram)
                                            .height(6.dp)
                                            .background(
                                                Brush.horizontalGradient(listOf(Accent, Teal)),
                                                RoundedCornerShape(50),
                                            ),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Sage,
                                modifier = Modifier.width(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }

        if (stats.topGenres.isNotEmpty()) {
            item {
                SectionHeading(
                    kicker = "Statistics",
                    title = "Top genres",
                )
                LedgerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 15.dp),
                ) {
                    stats.topGenres.take(5).forEach { (genre, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MintWhite.copy(alpha = 0.9f),
                                modifier = Modifier.width(108.dp),
                                maxLines = 1,
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MintWhite.copy(alpha = 0.055f)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(count.toFloat() / maxGenre)
                                        .height(6.dp)
                                        .background(
                                            Brush.horizontalGradient(listOf(Teal, Accent.copy(alpha = 0.8f))),
                                            RoundedCornerShape(50),
                                        ),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Sage,
                                modifier = Modifier.width(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }

        item {
            SectionHeading(
                kicker = "Recent",
                title = "Recent activity",
                action = if (snapshot.diary.isNotEmpty()) "Diary" else null,
                onAction = if (snapshot.diary.isNotEmpty()) onDiary else null,
            )
        }

        if (recent.isEmpty()) {
            item {
                Text(
                    text = "Nothing logged yet. Your recent entries will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sage,
                )
                Spacer(Modifier.height(26.dp))
            }
        } else {
            items(recent, key = { it.id }) { entry ->
                DiaryRow(
                    entry = entry,
                    onClick = { onGame(entry.gameId) },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }

        item {
            Text(
                text = "Playlog is a personal game diary. Data stays on this device.",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = Sage.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (editing) {
        var name by remember { mutableStateOf(snapshot.displayName) }
        PlaylogDialog(
            onDismissRequest = { editing = false },
            title = "Display name",
            confirmButton = {
                TextButton(onClick = {
                    vm.setDisplayName(name)
                    editing = false
                }) { Text("Save", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("Cancel", color = Sage) }
            },
        ) {
            PlaylogField(
                value = name,
                onValueChange = { name = it },
                label = "Name",
                singleLine = true,
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                letterSpacing = 1.1.sp,
            ),
            color = Sage,
        )
    }
}

@Composable
private fun VerticalRule() {
    Box(
        Modifier
            .width(1.dp)
            .height(38.dp)
            .background(GlassBorder),
    )
}
