package com.playlog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.LoadState
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.ui.components.BrandLoader
import com.playlog.app.ui.components.CircleIconButton
import com.playlog.app.ui.components.EmptyState
import com.playlog.app.ui.components.FilterPill
import com.playlog.app.ui.components.GameTile
import com.playlog.app.ui.components.GlassPanel
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.components.SkeletonCoverGrid
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MetaStyle
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage

private enum class DiscoverSort(val label: String) {
    POPULAR("Popular"), TOP("Top rated"), NEWEST("Newest")
}

@Composable
fun DiscoverScreen(
    vm: PlaylogViewModel,
    onGame: (Long) -> Unit,
    onSearch: () -> Unit,
) {
    val discover by vm.discover.collectAsStateWithLifecycle()
    var sort by remember { mutableStateOf(DiscoverSort.POPULAR) }

    val games = (discover as? LoadState.Ready)?.data.orEmpty().let { list ->
        when (sort) {
            DiscoverSort.POPULAR -> list
            DiscoverSort.TOP -> list.sortedByDescending { it.displayRating ?: 0.0 }
            DiscoverSort.NEWEST -> list.sortedByDescending { it.releaseDate ?: 0L }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
            Kicker("The catalogue")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Discover",
                style = MaterialTheme.typography.displayMedium,
                color = MintWhite,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Find something worth keeping.",
                style = MaterialTheme.typography.bodyLarge,
                color = Sage,
            )
            Spacer(Modifier.height(20.dp))
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp),
                onClick = onSearch,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = Sage,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Text(
                        text = "Search games, studios, genres…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Sage,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Accent.copy(alpha = 0.10f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DiscoverSort.entries.size) { index ->
                    val option = DiscoverSort.entries[index]
                    FilterPill(
                        text = option.label,
                        selected = sort == option,
                        onClick = { sort = option },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        Box(Modifier.fillMaxSize()) {
            when (val state = discover) {
                is LoadState.Loading, LoadState.Idle -> {
                    Box(Modifier.padding(horizontal = 20.dp)) { SkeletonCoverGrid() }
                }

                is LoadState.Failed -> EmptyState(
                    icon = painterResource(R.drawable.ic_warning),
                    title = "The catalogue is out of reach",
                    message = state.message,
                    modifier = Modifier.padding(top = 30.dp),
                )

                is LoadState.Ready -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(games, key = { it.id }) { game ->
                        GameTile(game = game, onClick = { onGame(game.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    vm: PlaylogViewModel,
    initialListId: String?,
    onBack: () -> Unit,
    onGame: (Long) -> Unit,
    onAddedToList: () -> Unit,
) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val result by vm.search.collectAsStateWithLifecycle()
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val list = initialListId?.let { id -> snapshot.lists.firstOrNull { it.id == id } }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Search",
            kicker = "The catalogue",
            onBack = onBack,
        )
        Spacer(Modifier.height(14.dp))
        PlaylogField(
            value = query,
            onValueChange = vm::search,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .focusRequester(focusRequester),
            placeholder = "Title, studio, or genre…",
            leadingIcon = painterResource(R.drawable.ic_search),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )

        if (list != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .background(Accent.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .border(1.dp, Accent.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_list_plus),
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Tap a game to add it to ${list.name}",
                    style = MaterialTheme.typography.labelLarge,
                    color = com.playlog.app.ui.theme.PaleMint,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        when (val state = result) {
            LoadState.Idle -> {
                EmptyState(
                    icon = painterResource(R.drawable.ic_sparkle),
                    title = "Find your next game",
                    message = if (vm.igdbConfigured) {
                        "Search the full IGDB catalogue by title, studio, or genre."
                    } else {
                        "Demo mode: searching the bundled catalogue. Add IGDB keys in Settings for the full database."
                    },
                    modifier = Modifier.padding(top = 24.dp),
                )
            }

            LoadState.Loading -> BrandLoader()

            is LoadState.Failed -> EmptyState(
                icon = painterResource(R.drawable.ic_warning),
                title = "Search failed",
                message = state.message,
            )

            is LoadState.Ready -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_search),
                        title = "Nothing found",
                        message = "No games match “$query”. Try a different title or studio.",
                    )
                } else {
                    Text(
                        text = "${state.data.size} results",
                        style = MetaStyle,
                        color = Sage,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        items(state.data, key = { it.id }) { game ->
                            GameTile(
                                game = game,
                                onClick = {
                                    if (initialListId != null) {
                                        vm.addGameToList(initialListId, game)
                                        onAddedToList()
                                    } else {
                                        onGame(game.id)
                                    }
                                },
                                badge = if (list != null && game.id in list.gameIds) "In list" else null,
                            )
                        }
                    }
                }
            }
        }
    }
}
