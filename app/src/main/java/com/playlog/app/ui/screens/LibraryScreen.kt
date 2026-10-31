package com.playlog.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.data.PlayStatus
import com.playlog.app.ui.components.EmptyState
import com.playlog.app.ui.components.FilterPill
import com.playlog.app.ui.components.GameTile
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.GlossyPillButton
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage

private enum class LibraryFilter(val label: String) {
    ALL("All"), PLAYING("Playing"), PLAYED("Played"), BACKLOG("Backlog"), DROPPED("Dropped"), LIKED("Liked")
}

@Composable
fun LibraryScreen(
    vm: PlaylogViewModel,
    onGame: (Long) -> Unit,
    onDiscover: () -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(LibraryFilter.ALL) }

    val items = snapshot.library
        .filter { entry ->
            when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.PLAYING -> entry.status == PlayStatus.PLAYING.key
                LibraryFilter.PLAYED -> entry.status == PlayStatus.PLAYED.key
                LibraryFilter.BACKLOG -> entry.status == PlayStatus.BACKLOG.key
                LibraryFilter.DROPPED -> entry.status == PlayStatus.DROPPED.key
                LibraryFilter.LIKED -> entry.liked
            }
        }
        .sortedByDescending { it.updatedAt }
        .mapNotNull { entry -> snapshot.game(entry.gameId)?.let { it to entry } }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
            Kicker("Collection")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.displayMedium,
                    color = MintWhite,
                )
                Text(
                    text = "${snapshot.library.size} games",
                    style = MaterialTheme.typography.labelSmall,
                    color = Sage,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Everything you have kept, in one quiet place.",
                style = MaterialTheme.typography.bodyMedium,
                color = Sage,
            )
            Spacer(Modifier.height(18.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LibraryFilter.entries.size) { index ->
                    val option = LibraryFilter.entries[index]
                    FilterPill(
                        text = option.label,
                        selected = filter == option,
                        onClick = { filter = option },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_gamepad),
                    title = if (snapshot.library.isEmpty()) "Your shelves are empty" else "Nothing here yet",
                    message = if (snapshot.library.isEmpty()) {
                        "Log or backlog a game and it will appear in your library."
                    } else {
                        "No games match this filter."
                    },
                    action = {
                        if (snapshot.library.isEmpty()) {
                            GlossyPillButton(
                                text = "Find games",
                                onClick = onDiscover,
                                icon = painterResource(R.drawable.ic_arrow_right),
                            )
                        } else {
                            GhostPill(
                                text = "Show all",
                                onClick = { filter = LibraryFilter.ALL },
                            )
                        }
                    },
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(items, key = { it.first.id }) { (game, entry) ->
                    GameTile(
                        game = game,
                        onClick = { onGame(game.id) },
                        badge = entry.playStatus?.label,
                        rating = entry.rating,
                    )
                }
            }
        }
    }
}
