package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.LoadState
import com.playlog.app.PlaylogViewModel
import com.playlog.app.data.Game
import com.playlog.app.data.PlayStatus
import com.playlog.app.ui.components.BrandLoader
import com.playlog.app.ui.components.CircleIconButton
import com.playlog.app.ui.components.Cover
import com.playlog.app.ui.components.DiaryRow
import com.playlog.app.ui.components.GameTile
import com.playlog.app.ui.components.GlassPanel
import com.playlog.app.ui.components.Hairline
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.SectionHeading
import com.playlog.app.ui.components.SkeletonCoverGrid
import com.playlog.app.ui.components.StatCard
import com.playlog.app.ui.components.StatusPill
import com.playlog.app.ui.components.Wordmark
import com.playlog.app.ui.components.todayLabel
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal
import java.time.LocalTime

@Composable
fun HomeScreen(
    vm: PlaylogViewModel,
    onGame: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onDiary: () -> Unit,
    onSettings: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val discover by vm.discover.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    val playing = snapshot.library
        .filter { it.status == PlayStatus.PLAYING.key }
        .mapNotNull { snapshot.game(it.gameId) }
    val recent = snapshot.diary.sortedByDescending { it.playedAt }.take(3)
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 34.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Wordmark()
                CircleIconButton(
                    painter = painterResource(com.playlog.app.R.drawable.ic_settings),
                    contentDescription = "Settings",
                    onClick = onSettings,
                )
            }
        }

        item {
            Spacer(Modifier.height(30.dp))
            Kicker(System.currentTimeMillis().todayLabel())
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    append("$greeting, ")
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = PaleMint)) {
                        append(snapshot.displayName)
                    }
                    append(".")
                },
                style = MaterialTheme.typography.displayMedium,
                color = MintWhite,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "A quiet record of the games you keep.",
                style = MaterialTheme.typography.bodyLarge,
                color = Sage,
            )
            Spacer(Modifier.height(28.dp))
        }

        if (playing.isNotEmpty()) {
            item {
                NowPlayingCard(
                    game = playing.first(),
                    rating = snapshot.entry(playing.first().id)?.rating,
                    onClick = { onGame(playing.first().id) },
                )
                Spacer(Modifier.height(30.dp))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                StatCard("Played", stats.played.toString(), Teal, Modifier.weight(1f))
                StatCard("Playing", stats.playing.toString(), Accent, Modifier.weight(1f))
                StatCard("Backlog", stats.backlog.toString(), Sage, Modifier.weight(1f))
            }
            Spacer(Modifier.height(26.dp))
        }

        if (!vm.igdbConfigured) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(15.dp),
                    onClick = onSettings,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Accent.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(com.playlog.app.R.drawable.ic_database),
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Demo catalogue active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MintWhite,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Add IGDB keys to search the full catalogue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                            )
                        }
                        Icon(
                            painter = painterResource(com.playlog.app.R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }

        item {
            SectionHeading(
                kicker = "The catalogue",
                title = "Popular right now",
                action = "See all",
                onAction = onSeeAll,
            )
        }

        when (val result = discover) {
            is LoadState.Loading, LoadState.Idle -> item {
                SkeletonCoverGrid()
                Spacer(Modifier.height(30.dp))
            }

            is LoadState.Failed -> item {
                GlassPanel {
                    Text(
                        text = "The catalogue is out of reach.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MintWhite,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                    )
                }
                Spacer(Modifier.height(30.dp))
            }

            is LoadState.Ready -> {
                items(result.data.chunked(2)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        row.forEach { game ->
                            GameTile(
                                game = game,
                                onClick = { onGame(game.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(26.dp))
                }
            }
        }

        if (playing.size > 1) {
            item {
                SectionHeading(
                    kicker = "On the shelf",
                    title = "Also in progress",
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playing.drop(1), key = { it.id }) { game ->
                        GameTile(
                            game = game,
                            onClick = { onGame(game.id) },
                            modifier = Modifier.width(126.dp),
                        )
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }

        item {
            SectionHeading(
                kicker = "The ledger",
                title = "From your diary",
                action = if (snapshot.diary.isNotEmpty()) "Open" else null,
                onAction = if (snapshot.diary.isNotEmpty()) onDiary else null,
            )
        }

        if (recent.isEmpty()) {
            item {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDiary,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(com.playlog.app.R.drawable.ic_book),
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Nothing logged yet.",
                                style = MaterialTheme.typography.titleMedium,
                                color = MintWhite,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Open a game and tap Log to start your diary.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Sage,
                            )
                        }
                        Icon(
                            painter = painterResource(com.playlog.app.R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
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
            Hairline()
            Spacer(Modifier.height(18.dp))
            Text(
                text = "PLAYLOG · A LOCAL-FIRST GAME DIARY",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = Sage.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    game: Game,
    rating: Double?,
    onClick: () -> Unit,
) {
    LedgerPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cover(
                game = game,
                modifier = Modifier
                    .width(88.dp)
                    .aspectRatio(3f / 4f),
                corner = 10.dp,
            )
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                Kicker("Now playing")
                Spacer(Modifier.height(6.dp))
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MintWhite,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        game.year?.toString(),
                        game.genres.firstOrNull(),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage,
                    maxLines = 1,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(PlayStatus.PLAYING)
                    if (rating != null) {
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = "★ ${String.format(java.util.Locale.US, "%.1f", rating)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PaleMint,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(com.playlog.app.R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
