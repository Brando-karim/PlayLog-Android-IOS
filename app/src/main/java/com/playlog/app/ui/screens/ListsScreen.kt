package com.playlog.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playlog.app.PlaylogViewModel
import com.playlog.app.R
import com.playlog.app.data.Game
import com.playlog.app.data.GameList
import com.playlog.app.ui.components.Cover
import com.playlog.app.ui.components.CoverStack
import com.playlog.app.ui.components.EmptyState
import com.playlog.app.ui.components.GhostPill
import com.playlog.app.ui.components.GlossyPillButton
import com.playlog.app.ui.components.Kicker
import com.playlog.app.ui.components.LedgerPanel
import com.playlog.app.ui.components.MetaBadge
import com.playlog.app.ui.components.PlaylogDialog
import com.playlog.app.ui.components.PlaylogField
import com.playlog.app.ui.components.ScreenHeader
import com.playlog.app.ui.theme.Accent
import com.playlog.app.ui.theme.GlassBorder
import com.playlog.app.ui.theme.MetaStyle
import com.playlog.app.ui.theme.MintWhite
import com.playlog.app.ui.theme.Sage
import com.playlog.app.ui.theme.SerifText

@Composable
fun ListsScreen(
    vm: PlaylogViewModel,
    onList: (String) -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)) {
            Kicker("Curated")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Lists",
                        style = MaterialTheme.typography.displayMedium,
                        color = MintWhite,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${snapshot.lists.size} curated collections",
                        style = MaterialTheme.typography.labelSmall,
                        color = Sage,
                    )
                }
                GlossyPillButton(
                    text = "New",
                    onClick = { showCreate = true },
                    compact = true,
                    icon = painterResource(R.drawable.ic_plus),
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        if (snapshot.lists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = painterResource(R.drawable.ic_list),
                    title = "Make your first list",
                    message = "Group games by mood, year, genre, or your all-time favourites.",
                    action = {
                        GlossyPillButton(
                            text = "Create a list",
                            onClick = { showCreate = true },
                            icon = painterResource(R.drawable.ic_plus),
                        )
                    },
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(snapshot.lists.sortedByDescending { it.createdAt }, key = { it.id }) { list ->
                    ListCard(
                        list = list,
                        games = snapshot.gamesFor(list.gameIds),
                        onClick = { onList(list.id) },
                        onDelete = { vm.deleteList(list.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateListDialog(
            onCreate = { name, description ->
                vm.createList(name, description)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun ListCard(
    list: GameList,
    games: List<Game>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    LedgerPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MetaBadge(
                text = "Private",
                icon = painterResource(R.drawable.ic_lock),
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete list",
                    tint = Sage.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = list.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MintWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (list.description.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = list.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Sage,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            if (games.isEmpty()) {
                Text(
                    text = "Empty list — add some games",
                    style = MaterialTheme.typography.bodySmall,
                    color = Sage,
                    modifier = Modifier.weight(1f),
                )
            } else {
                CoverStack(
                    games = games,
                    coverWidth = 46.dp,
                    count = 3,
                    corner = 6.dp,
                )
                Spacer(Modifier.weight(1f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${list.gameIds.size} ${if (list.gameIds.size == 1) "game" else "games"}",
                    style = MetaStyle,
                    color = Accent,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CreateListDialog(
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    PlaylogDialog(
        onDismissRequest = onDismiss,
        title = "New list",
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), description.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create", color = if (name.isNotBlank()) Accent else Sage)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Sage) }
        },
    ) {
        PlaylogField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
            placeholder = "Quiet games for October",
        )
        Spacer(Modifier.height(12.dp))
        PlaylogField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            placeholder = "An optional note",
            singleLine = false,
            minLines = 3,
            maxLines = 4,
        )
    }
}

@Composable
fun ListDetailScreen(
    vm: PlaylogViewModel,
    listId: String,
    onBack: () -> Unit,
    onAddGames: (String) -> Unit,
    onGame: (Long) -> Unit,
) {
    val snapshot by vm.state.collectAsStateWithLifecycle()
    val list = snapshot.lists.firstOrNull { it.id == listId }

    if (list == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = "List", onBack = onBack)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("List not found", color = Sage)
            }
        }
        return
    }

    val games = snapshot.gamesFor(list.gameIds)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = list.name,
            kicker = "List",
            onBack = onBack,
            trailing = {
                GhostPill(
                    text = "Add",
                    onClick = { onAddGames(list.id) },
                    icon = painterResource(R.drawable.ic_plus),
                )
            },
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
        ) {
            if (list.description.isNotBlank()) {
                item {
                    Text(
                        text = list.description,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = SerifText,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                        color = Sage,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            item {
                Text(
                    text = "${list.gameIds.size} ${if (list.gameIds.size == 1) "game" else "games"}",
                    style = MetaStyle,
                    color = Accent,
                )
                Spacer(Modifier.height(16.dp))
            }

            if (games.isEmpty()) {
                item {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_list),
                        title = "This list is empty",
                        message = "Add games from search to build it out.",
                        action = {
                            GlossyPillButton(
                                text = "Add games",
                                onClick = { onAddGames(list.id) },
                                icon = painterResource(R.drawable.ic_plus),
                            )
                        },
                    )
                }
            } else {
                itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
                    LedgerPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 9.dp),
                        contentPadding = PaddingValues(11.dp),
                        onClick = { onGame(game.id) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = (index + 1).toString().padStart(2, '0'),
                                style = MetaStyle.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
                                color = Accent,
                                modifier = Modifier.width(26.dp),
                            )
                            Cover(
                                game = game,
                                modifier = Modifier
                                    .width(44.dp)
                                    .aspectRatio(3f / 4f),
                                corner = 7.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = game.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = SerifText,
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                    ),
                                    color = MintWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = listOfNotNull(
                                        game.year?.toString(),
                                        game.genres.firstOrNull(),
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Sage,
                                    maxLines = 1,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { vm.removeGameFromList(list.id, game.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Remove from list",
                                    tint = Sage.copy(alpha = 0.75f),
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
