package com.playlog.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Local-first persistence. Everything the user does lives in one JSON document so
 * the app works with no account and no backend.
 */
class PlaylogStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(context.filesDir, "playlog_state.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<PersistedState> = _state.asStateFlow()

    private fun load(): PersistedState = runCatching {
        if (!file.exists()) PersistedState() else json.decodeFromString<PersistedState>(file.readText())
    }.getOrElse { PersistedState() }

    private fun mutate(transform: (PersistedState) -> PersistedState) {
        _state.update(transform)
        scope.launch {
            writeLock.withLock {
                runCatching { file.writeText(json.encodeToString(PersistedState.serializer(), _state.value)) }
            }
        }
    }

    fun cacheGames(games: List<Game>) {
        if (games.isEmpty()) return
        mutate { current ->
            val byId = current.games.associateBy { it.id }.toMutableMap()
            games.forEach { byId[it.id] = it }
            current.copy(games = byId.values.toList())
        }
    }

    fun setStatus(game: Game, status: PlayStatus?, rating: Double? = null, liked: Boolean? = null) {
        mutate { current ->
            cacheIn(current, game)
            if (status == null) {
                current.copy(library = current.library.filterNot { it.gameId == game.id })
            } else {
                val old = current.entry(game.id)
                val entry = LibraryEntry(
                    gameId = game.id,
                    status = status.key,
                    rating = rating ?: old?.rating,
                    liked = liked ?: old?.liked ?: false,
                    updatedAt = System.currentTimeMillis(),
                )
                current.copy(library = current.library.filterNot { it.gameId == game.id } + entry)
            }
        }
    }

    fun toggleLike(game: Game) {
        mutate { current ->
            val old = current.entry(game.id)
            val entry = if (old == null) {
                LibraryEntry(game.id, PlayStatus.PLAYED.key, liked = true)
            } else {
                old.copy(liked = !old.liked, updatedAt = System.currentTimeMillis())
            }
            cacheIn(
                current.copy(library = current.library.filterNot { it.gameId == game.id } + entry),
                game,
            )
        }
    }

    fun addDiary(entry: DiaryEntry) {
        mutate { current ->
            current.copy(diary = (current.diary + entry).sortedByDescending { it.playedAt })
        }
    }

    fun deleteDiary(id: String) {
        mutate { current -> current.copy(diary = current.diary.filterNot { it.id == id }) }
    }

    fun createList(name: String, description: String = ""): String {
        val id = UUID.randomUUID().toString()
        mutate { current ->
            current.copy(lists = current.lists + GameList(id = id, name = name, description = description))
        }
        return id
    }

    fun deleteList(id: String) {
        mutate { current -> current.copy(lists = current.lists.filterNot { it.id == id }) }
    }

    fun addGameToList(listId: String, game: Game) {
        mutate { current ->
            cacheIn(current, game).let { withGame ->
                withGame.copy(
                    lists = withGame.lists.map { list ->
                        if (list.id == listId && game.id !in list.gameIds) {
                            list.copy(gameIds = list.gameIds + game.id)
                        } else {
                            list
                        }
                    },
                )
            }
        }
    }

    fun removeGameFromList(listId: String, gameId: Long) {
        mutate { current ->
            current.copy(
                lists = current.lists.map { list ->
                    if (list.id == listId) list.copy(gameIds = list.gameIds - gameId) else list
                },
            )
        }
    }

    fun toggleFavorite(gameId: Long) {
        mutate { current ->
            val favorites = if (gameId in current.favorites) {
                current.favorites - gameId
            } else {
                current.favorites + gameId
            }
            current.copy(favorites = favorites)
        }
    }

    fun setDisplayName(name: String) {
        mutate { current -> current.copy(displayName = name.trim().ifBlank { "Player One" }) }
    }

    fun clearLibrary() {
        mutate { current ->
            current.copy(library = emptyList(), diary = emptyList(), favorites = emptyList())
        }
    }

    private fun cacheIn(state: PersistedState, game: Game): PersistedState =
        if (state.game(game.id) == game) state
        else state.copy(games = state.games.filterNot { it.id == game.id } + game)
}
