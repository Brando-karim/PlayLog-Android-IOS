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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.data.CloudState
import com.playlog.app.data.DiaryEntry
import com.playlog.app.data.Game
import com.playlog.app.data.GameList
import com.playlog.app.data.PersistedState
import com.playlog.app.data.PlayStatus
import com.playlog.app.data.ProfileStats
import com.playlog.app.data.formatHoursValue
import com.playlog.app.ui.components.CircleIconButton
import com.playlog.app.ui.components.Cover
import com.playlog.app.ui.components.DiaryRow
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.Hairline
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.ProfileAvatar
import com.playlog.app.ui.components.SectionHeading
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Obsidian
import com.playlog.app.ui.theme.PaleMint
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.Teal
import java.util.Locale

private enum class ProfileTab(val label: String) {
    Profile("Profile"),
    Diary("Diary"),
    Lists("Lists"),
    Watchlist("Watchlist"),
}

@Composable
fun ProfileScreen(
    vm: PlaylogViewModel,
    onGame: (Long) -> Unit,
    onDiary: () -> Unit,
    onList: (String) -> Unit,
    onLists: () -> Unit,
    onLibrary: () -> Unit,
    onAccount: () -> Unit,
    onSettings: () -> Unit,
    onEditProfile: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val account by vm.account.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(ProfileTab.Profile) }

    Column(Modifier.fillMaxSize()) {
        ProfileHeader(
            displayName = snapshot.displayName,
            avatarUrl = snapshot.avatarUrl,
            gameCount = snapshot.library.size,
            diaryCount = snapshot.diary.size,
            account = account,
            selectedTab = tab,
            onTab = { tab = it },
            onEdit = onEditProfile,
            onAccount = onAccount,
            onSettings = onSettings,
        )

        Box(Modifier.weight(1f)) {
            when (tab) {
                ProfileTab.Profile -> ProfileOverview(
                    snapshot = snapshot,
                    stats = stats,
                    onGame = onGame,
                    onLibrary = onLibrary,
                    onDiary = onDiary,
                    onTab = { tab = it },
                )

                ProfileTab.Diary -> DiaryTab(
                    snapshot = snapshot,
                    onGame = onGame,
                    onDiary = onDiary,
                )

                ProfileTab.Lists -> ListsTab(
                    snapshot = snapshot,
                    onList = onList,
                    onLists = onLists,
                )

                ProfileTab.Watchlist -> WatchlistTab(
                    snapshot = snapshot,
                    onGame = onGame,
                    onLibrary = onLibrary,
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    displayName: String,
    avatarUrl: String?,
    gameCount: Int,
    diaryCount: Int,
    account: CloudState,
    selectedTab: ProfileTab,
    onTab: (ProfileTab) -> Unit,
    onEdit: () -> Unit,
    onAccount: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Kicker("Player profile")
            Spacer(Modifier.weight(1f))
            CircleIconButton(
                painter = painterResource(R.drawable.ic_database),
                contentDescription = "Account and sync",
                onClick = onAccount,
                size = 34.dp,
                iconSize = 15.dp,
                tint = if (account.signedIn) Accent else Sage,
            )
            Spacer(Modifier.width(7.dp))
            CircleIconButton(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                onClick = onSettings,
                size = 34.dp,
                iconSize = 15.dp,
                tint = Sage,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(name = displayName, avatarUrl = avatarUrl, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MintWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(4.dp))
                    CircleIconButton(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "Edit profile",
                        onClick = onEdit,
                        size = 28.dp,
                        iconSize = 12.dp,
                        tint = Sage,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$gameCount ${if (gameCount == 1) "game" else "games"} · " +
                        "$diaryCount ${if (diaryCount == 1) "diary entry" else "diary entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.clickable(onClick = onAccount),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .background(
                                when {
                                    !account.signedIn -> Sage
                                    account.error != null -> Color(0xFFE58C8C)
                                    else -> Accent
                                },
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = when {
                            !account.signedIn -> "Sign in to sync with the web"
                            account.busy -> "Syncing your ledger…"
                            account.error != null -> "Sync needs attention"
                            else -> "Synced with Playlog"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            account.error != null -> Color(0xFFE58C8C)
                            account.signedIn -> Accent
                            else -> Sage
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth()) {
            ProfileTab.entries.forEach { option ->
                val selected = option == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTab(option) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp,
                        ),
                        color = if (selected) Accent else Sage,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(2.dp)
                            .background(if (selected) Accent else Color.Transparent, CircleShape),
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Hairline()
    }
}

@Composable
private fun ProfileOverview(
    snapshot: PersistedState,
    stats: ProfileStats,
    onGame: (Long) -> Unit,
    onLibrary: () -> Unit,
    onDiary: () -> Unit,
    onTab: (ProfileTab) -> Unit,
) {
    val favorites = snapshot.favorites.take(4).distinct().mapNotNull { snapshot.game(it) }
    val recent = snapshot.diary.sortedByDescending { it.playedAt }.take(4)
    val maxHistogram = (stats.ratingHistogram.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val maxGenre = (stats.topGenres.firstOrNull()?.second ?: 1).coerceAtLeast(1)

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp),
    ) {
        item {
            SectionHeading(kicker = "Ledger", title = "Your numbers")
            Spacer(Modifier.height(9.dp))
            LedgerPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                StatRow("Games", snapshot.library.size.toString(), onClick = onLibrary)
                Hairline()
                StatRow("Diary", snapshot.diary.size.toString(), onClick = { onTab(ProfileTab.Diary) })
                if (stats.totalHours > 0) {
                    Hairline()
                    StatRow("Hours", formatHoursValue(stats.totalHours))
                }
                Hairline()
                StatRow("Reviews", stats.reviews.toString(), onClick = { onTab(ProfileTab.Diary) })
                Hairline()
                StatRow("Watchlist", stats.backlog.toString(), onClick = { onTab(ProfileTab.Watchlist) })
                Hairline()
                StatRow("Lists", snapshot.lists.size.toString(), onClick = { onTab(ProfileTab.Lists) })
                Hairline()
                StatRow("Likes", stats.liked.toString(), onClick = onLibrary)
                Hairline()
                StatRow("Favourites", favorites.size.toString())
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            SectionHeading(
                kicker = "Favourites",
                title = "Kept close",
                action = if (favorites.isNotEmpty()) "Library" else null,
                onAction = if (favorites.isNotEmpty()) onLibrary else null,
            )
            Spacer(Modifier.height(11.dp))
            if (favorites.isEmpty()) {
                EmptyShelf(
                    title = "No favourites yet",
                    message = "Tap the heart on a game to pin it to this shelf.",
                    actionLabel = "Open library",
                    onAction = onLibrary,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    lazyItems(favorites, key = { it.id }) { game ->
                        Cover(
                            game = game,
                            modifier = Modifier
                                .width(104.dp)
                                .aspectRatio(3f / 4f)
                                .clickable { onGame(game.id) },
                            corner = 12.dp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            SectionHeading(
                kicker = "Recent",
                title = "Recent activity",
                action = if (recent.isNotEmpty()) "More" else null,
                onAction = if (recent.isNotEmpty()) onDiary else null,
            )
            Spacer(Modifier.height(11.dp))
            if (recent.isEmpty()) {
                EmptyShelf(
                    title = "Nothing logged yet",
                    message = "Your latest diary entries will appear here as posters.",
                    actionLabel = "Open diary",
                    onAction = onDiary,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    lazyItems(recent, key = { it.id }) { entry ->
                        ActivityTile(
                            entry = entry,
                            game = snapshot.game(entry.gameId),
                            onClick = { onGame(entry.gameId) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        if (stats.ratingHistogram.isNotEmpty()) {
            item {
                SectionHeading(kicker = "Statistics", title = "Rating spread")
                Spacer(Modifier.height(11.dp))
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
                Spacer(Modifier.height(28.dp))
            }
        }

        if (stats.topGenres.isNotEmpty()) {
            item {
                SectionHeading(kicker = "Patterns", title = "Top genres")
                Spacer(Modifier.height(11.dp))
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
                                overflow = TextOverflow.Ellipsis,
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
                                            Brush.horizontalGradient(
                                                listOf(Teal, Accent.copy(alpha = 0.8f)),
                                            ),
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
                Spacer(Modifier.height(28.dp))
            }
        }

        item {
            Text(
                text = "Playlog is a personal game diary. Sign in to keep your ledger on every device.",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
                color = Sage.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DiaryTab(
    snapshot: PersistedState,
    onGame: (Long) -> Unit,
    onDiary: () -> Unit,
) {
    val diary = snapshot.diary.sortedByDescending { it.playedAt }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Kicker("Journal")
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${diary.size} ${if (diary.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MintWhite,
                    )
                }
                GhostPill(text = "Full diary", onClick = onDiary)
            }
            Spacer(Modifier.height(16.dp))
        }
        if (diary.isEmpty()) {
            item {
                EmptyShelf(
                    title = "Your diary is empty",
                    message = "Log a game and your entries will collect here.",
                    actionLabel = "Open diary",
                    onAction = onDiary,
                )
            }
        } else {
            lazyItems(diary, key = { it.id }) { entry ->
                DiaryRow(
                    entry = entry,
                    onClick = { onGame(entry.gameId) },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun ListsTab(
    snapshot: PersistedState,
    onList: (String) -> Unit,
    onLists: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Kicker("Curated")
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${snapshot.lists.size} ${if (snapshot.lists.size == 1) "list" else "lists"}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MintWhite,
                    )
                }
                GhostPill(text = "All lists", onClick = onLists)
            }
            Spacer(Modifier.height(16.dp))
        }
        if (snapshot.lists.isEmpty()) {
            item {
                EmptyShelf(
                    title = "No lists yet",
                    message = "Build a shelf for a mood, a genre, or a rainy weekend.",
                    actionLabel = "Open lists",
                    onAction = onLists,
                )
            }
        } else {
            lazyItems(snapshot.lists, key = { it.id }) { list ->
                ListSummaryCard(
                    list = list,
                    snapshot = snapshot,
                    onClick = { onList(list.id) },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun WatchlistTab(
    snapshot: PersistedState,
    onGame: (Long) -> Unit,
    onLibrary: () -> Unit,
) {
    val watchlist = snapshot.library
        .filter { it.status == PlayStatus.BACKLOG.key }
        .sortedByDescending { it.updatedAt }
        .mapNotNull { snapshot.game(it.gameId) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Kicker("Watchlist")
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${watchlist.size} ${if (watchlist.size == 1) "game" else "games"} waiting",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MintWhite,
                    )
                }
                GhostPill(text = "Add", onClick = onLibrary)
            }
            Spacer(Modifier.height(4.dp))
        }
        if (watchlist.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyShelf(
                    title = "Your watchlist is empty",
                    message = "Add a game from its page and it will wait here until you press play.",
                    actionLabel = "Open library",
                    onAction = onLibrary,
                )
            }
        } else {
            gridItems(watchlist, key = { it.id }) { game ->
                Column {
                    Cover(
                        game = game,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clickable { onGame(game.id) },
                        corner = 10.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = game.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Sage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTile(
    entry: DiaryEntry,
    game: Game?,
    onClick: () -> Unit,
) {
    Column(Modifier.width(104.dp)) {
        Box {
            if (game != null) {
                Cover(
                    game = game,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clickable(onClick = onClick),
                    corner = 11.dp,
                )
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Obsidian),
                )
            }
            if (entry.review.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Obsidian.copy(alpha = 0.86f))
                        .border(1.dp, GlassBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book),
                        contentDescription = "Review",
                        tint = Accent,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = if (entry.rating != null) Accent else Sage.copy(alpha = 0.6f),
                modifier = Modifier.size(9.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = entry.rating?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.rating != null) Accent else Sage,
            )
            if (entry.review.isNotBlank()) {
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Review",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = Sage,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.gameName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MintWhite.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ListSummaryCard(
    list: GameList,
    snapshot: PersistedState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val games = list.gameIds.mapNotNull { snapshot.game(it) }
    LedgerPanel(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MintWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (list.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = list.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Sage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${games.size} ${if (games.size == 1) "game" else "games"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                )
            }
            if (games.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    games.take(3).forEach { game ->
                        Cover(
                            game = game,
                            modifier = Modifier
                                .width(34.dp)
                                .aspectRatio(3f / 4f),
                            corner = 6.dp,
                            showSheen = false,
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Sage.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MintWhite,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = Accent,
        )
        Spacer(Modifier.width(5.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Sage.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun EmptyShelf(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    LedgerPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(17.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MintWhite,
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Sage,
        )
        Spacer(Modifier.height(13.dp))
        GhostPill(text = actionLabel, onClick = onAction)
    }
}
