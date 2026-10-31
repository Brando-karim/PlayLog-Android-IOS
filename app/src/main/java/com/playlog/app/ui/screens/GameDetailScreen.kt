package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.data.Game
import com.playlog.app.data.PlayStatus
import com.playlog.app.ui.components.BrandLoader
import com.playlog.app.ui.components.CircleIconButton
import com.playlog.app.ui.components.Cover
import com.playlog.app.ui.components.DiaryRow
import com.playlog.app.ui.components.EmptyState
import com.playlog.app.ui.components.GemScore
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.GlossyPillButton
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.PlaylogDialog
import com.playlog.app.ui.components.RatingBadge
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.components.SectionHeading
import com.playlog.app.ui.components.StatusPill
import com.playlog.app.ui.components.TagRow
import com.playlog.app.ui.components.coverGradientFor
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Obsidian
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.ReadingStyle as BrandReadingStyle
import com.playlog.app.ui.theme.Sage

@Composable
fun GameDetailScreen(
    vm: PlaylogViewModel,
    gameId: Long,
    onBack: () -> Unit,
    onLog: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val cached = snapshot.game(gameId)
    var fetched by remember(gameId) { mutableStateOf<Game?>(null) }
    var loading by remember(gameId) { mutableStateOf(cached == null) }
    var showLists by remember { mutableStateOf(false) }

    LaunchedEffect(gameId, cached) {
        if (cached == null && fetched == null) {
            loading = true
            fetched = vm.loadGame(gameId)
            loading = false
        }
    }

    val game = cached ?: fetched
    if (game == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = "Game", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (loading) {
                    BrandLoader()
                } else {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_warning),
                        title = "Game not found",
                        message = "This entry is no longer in the catalogue.",
                    )
                }
            }
        }
        return
    }

    val entry = snapshot.entry(game.id)
    val status = entry?.playStatus
    val liked = entry?.liked == true
    val favourite = game.id in snapshot.favorites
    val diary = snapshot.diary.filter { it.gameId == game.id }.sortedByDescending { it.playedAt }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            DetailHero(
                game = game,
                status = status,
                favourite = favourite,
                onBack = onBack,
                onFavourite = { vm.toggleFavorite(game.id) },
            )
        }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    RatingBadge(game.rating, game.ratingCount)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Your rating",
                            style = MaterialTheme.typography.labelSmall,
                            color = Sage,
                        )
                        Spacer(Modifier.width(8.dp))
                        GemScore(entry?.rating, gemSize = 12.dp)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlossyPillButton(
                        text = if (diary.isEmpty()) "Log or review" else "Log again",
                        onClick = onLog,
                        modifier = Modifier.weight(1f),
                        icon = painterResource(R.drawable.ic_arrow_right),
                    )
                    GhostPill(
                        text = if (status == PlayStatus.BACKLOG) "In backlog" else "Backlog",
                        onClick = { vm.toggleBacklog(game) },
                        selected = status == PlayStatus.BACKLOG,
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostPill(
                        text = "Add to list",
                        onClick = { showLists = true },
                        icon = painterResource(R.drawable.ic_list_plus),
                    )
                    GhostPill(
                        text = if (liked) "Liked" else "Like",
                        onClick = { vm.toggleLike(game) },
                        selected = liked,
                        icon = painterResource(R.drawable.ic_heart),
                    )
                }

                if (game.summary.isNotBlank()) {
                    Spacer(Modifier.height(28.dp))
                    Kicker("About")
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = game.summary,
                        style = BrandReadingStyle,
                        color = MintWhite.copy(alpha = 0.87f),
                    )
                }

                if (game.genres.isNotEmpty() || game.platforms.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    if (game.genres.isNotEmpty()) {
                        TagRow(game.genres)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (game.platforms.isNotEmpty()) {
                        TagRow(game.platforms)
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }

        if (game.screenshots.isNotEmpty()) {
            item {
                SectionHeading(
                    kicker = "Frames",
                    title = "Screenshots",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(game.screenshots) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(244.dp)
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(14.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        item {
            SectionHeading(
                kicker = "The ledger",
                title = "Your history",
                modifier = Modifier.padding(horizontal = 20.dp),
                action = "Add to list",
                onAction = { showLists = true },
            )
            if (diary.isEmpty()) {
                Text(
                    text = "No entries yet. Log this game to rate it and write a review.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Sage,
                )
                Spacer(Modifier.height(18.dp))
            } else {
                diary.forEach { log ->
                    DiaryRow(
                        entry = log,
                        onClick = onLog,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                        onDelete = { vm.deleteDiary(log.id) },
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            GhostPill(
                text = "Add to a list",
                onClick = { showLists = true },
                icon = painterResource(R.drawable.ic_plus),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
        }
    }

    if (showLists) {
        val lists = snapshot.lists
        PlaylogDialog(
            onDismissRequest = { showLists = false },
            title = "Add to list",
            confirmButton = {
                TextButton(onClick = { showLists = false }) {
                    Text("Done", color = Accent)
                }
            },
        ) {
            if (lists.isEmpty()) {
                Text(
                    text = "You have no lists yet. Create one from the Lists tab.",
                    color = Sage,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                lists.forEach { list ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                vm.addGameToList(list.id, game)
                                showLists = false
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = list.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MintWhite,
                        )
                        Text(
                            text = if (game.id in list.gameIds) "Added" else "Add",
                            color = if (game.id in list.gameIds) Sage else Accent,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHero(
    game: Game,
    status: PlayStatus?,
    favourite: Boolean,
    onBack: () -> Unit,
    onFavourite: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(400.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(coverGradientFor(game.id))),
        )
        val hero = game.backdropUrl ?: game.coverUrl
        if (!hero.isNullOrBlank()) {
            AsyncImage(
                model = hero,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Obsidian.copy(alpha = 0.30f),
                        0.28f to Color.Transparent,
                        0.58f to Obsidian.copy(alpha = 0.76f),
                        1f to Obsidian,
                    ),
                ),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircleIconButton(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Back",
                onClick = onBack,
            )
            CircleIconButton(
                painter = painterResource(
                    if (favourite) R.drawable.ic_heart_fill else R.drawable.ic_heart,
                ),
                contentDescription = if (favourite) "Remove favourite" else "Add favourite",
                onClick = onFavourite,
                accent = favourite,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Cover(
                    game = game,
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(3f / 4f),
                    corner = 10.dp,
                )
                Spacer(Modifier.width(15.dp))
                Column(Modifier.weight(1f)) {
                    if (status != null) {
                        StatusPill(status)
                        Spacer(Modifier.height(9.dp))
                    }
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MintWhite,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOfNotNull(
                            game.year?.toString(),
                            game.platforms.firstOrNull(),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = PaleMint.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
