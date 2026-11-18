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
                current.copy(
                    library = current.library.filterNot { it.gameId == game.id },
                    removedGameIds = (current.removedGameIds + game.id).distinct(),
                    dirtyGameIds = current.dirtyGameIds - game.id,
                )
            } else {
                val old = current.entry(game.id)
                val entry = LibraryEntry(
                    gameId = game.id,
                    status = status.key,
                    rating = rating ?: old?.rating,
                    liked = liked ?: old?.liked ?: false,
                    updatedAt = System.currentTimeMillis(),
                )
                current.copy(
                    library = current.library.filterNot { it.gameId == game.id } + entry,
                    removedGameIds = current.removedGameIds - game.id,
                    dirtyGameIds = (current.dirtyGameIds + game.id).distinct(),
                )
            }
        }
    }

    fun toggleLike(game: Game) {
        mutate { current ->
            val old = current.entry(game.id)
            val entry = if (old == null) {
                LibraryEntry(
                    gameId = game.id,
                    status = PlayStatus.PLAYED.key,
                    liked = true,
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                old.copy(liked = !old.liked, updatedAt = System.currentTimeMillis())
            }
            cacheIn(
                current.copy(
                    library = current.library.filterNot { it.gameId == game.id } + entry,
                    removedGameIds = current.removedGameIds - game.id,
                ),
                game,
            )
        }
    }

    fun addDiary(entry: DiaryEntry) {
        mutate { current ->
            current.copy(
                diary = (current.diary + entry).sortedByDescending { it.playedAt },
                // Re-logging a game gives it a fresh review on the web, so any older
                // deletion tombstone for this game must not delete that review again.
                deletedDiaryKeys = current.deletedDiaryKeys.filterNot { key ->
                    key.substringBefore(':').toLongOrNull() == entry.gameId
                },
                dirtyGameIds = (current.dirtyGameIds + entry.gameId).distinct(),
            )
        }
    }

    fun deleteDiary(id: String) {
        mutate { current ->
            val removed = current.diary.firstOrNull { it.id == id }
            current.copy(
                diary = current.diary.filterNot { it.id == id },
                deletedDiaryKeys = if (removed == null) {
                    current.deletedDiaryKeys
                } else {
                    (current.deletedDiaryKeys + diarySyncKey(removed.gameId, removed.playedAt)).distinct()
                },
            )
        }
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
            val removing = gameId in current.favorites
            val next = when {
                removing -> current.favorites - gameId
                current.favorites.size < 4 -> current.favorites + gameId
                else -> current.favorites
            }
            current.copy(
                favorites = next,
                removedFavoriteIds = if (removing) {
                    (current.removedFavoriteIds + gameId).distinct()
                } else {
                    current.removedFavoriteIds - gameId
                },
            )
        }
    }

    fun setDisplayName(name: String) {
        mutate { current ->
            current.copy(
                displayName = name.trim().ifBlank { "Player One" },
                displayNameDirty = true,
            )
        }
    }

    fun setAvatarUrl(url: String?) {
        mutate { current -> current.copy(avatarUrl = url, avatarDirty = true) }
    }

    fun setAvatarFromCloud(url: String?) {
        mutate { current ->
            if (current.avatarUrl == url) current else current.copy(avatarUrl = url)
        }
    }

    fun markProfileClean() {
        mutate { current ->
            if (!current.displayNameDirty && !current.avatarDirty) {
                current
            } else {
                current.copy(displayNameDirty = false, avatarDirty = false)
            }
        }
    }

    fun clearLibrary() {
        mutate { current ->
            current.copy(
                library = emptyList(),
                diary = emptyList(),
                favorites = emptyList(),
                removedGameIds = (current.removedGameIds + current.library.map { it.gameId }).distinct(),
                removedFavoriteIds = (current.removedFavoriteIds + current.favorites).distinct(),
            )
        }
    }

    /**
     * Merge a cloud snapshot without discarding local entries that have not been
     * pushed yet. Games listed in [pending] are owned by this device until the
     * upload succeeds, so their remote copy is ignored. Everything else adopts
     * the web copy, which keeps a wrong device clock from hiding web edits.
     */
    fun mergeRemoteLibrary(entries: List<LibraryEntry>, pending: Set<Long> = emptySet()) {
        if (entries.isEmpty()) return
        mutate { current ->
            val byId = current.library.associateBy { it.gameId }.toMutableMap()
            entries.forEach { remote ->
                if (remote.gameId in current.removedGameIds) return@forEach
                if (remote.gameId in pending || remote.gameId in current.dirtyGameIds) return@forEach
                val local = byId[remote.gameId]
                byId[remote.gameId] = remote.copy(liked = local?.liked ?: remote.liked)
            }
            current.copy(library = byId.values.toList())
        }
    }

    fun mergeRemoteDiary(entries: List<DiaryEntry>, pending: Set<Long> = emptySet()) {
        if (entries.isEmpty()) return
        mutate { current ->
            val remoteIds = entries.map { it.id }.toSet()
            val byId = current.diary.associateBy { it.id }.toMutableMap()
            // Prefer a locally-created row when a remote log describes the same day,
            // so an earlier pull duplicate is merged instead of kept alongside it.
            // Within a day the newest entry owns the web log.
            val byKey = current.diary
                .sortedWith(
                    compareBy({ if (it.id in remoteIds) 0 else 1 }, { it.playedAt }),
                )
                .associateBy { diarySyncKey(it.gameId, it.playedAt) }
                .toMutableMap()
            entries.forEach { remote ->
                if (remote.gameId in pending || remote.gameId in current.dirtyGameIds) return@forEach
                val key = diarySyncKey(remote.gameId, remote.playedAt)
                if (key in current.deletedDiaryKeys) return@forEach
                val existingById = byId[remote.id]
                val existingByKey = byKey[key]
                when {
                    existingById != null -> {
                        byId[remote.id] = remote.copy(createdAt = existingById.createdAt)
                        byKey[key] = byId.getValue(remote.id)
                    }

                    existingByKey != null -> {
                        // Same game/day is already local but with a locally generated id.
                        // Refresh its content from the cloud without creating a duplicate.
                        val refreshed = existingByKey.copy(
                            gameName = remote.gameName,
                            coverUrl = remote.coverUrl ?: existingByKey.coverUrl,
                            rating = remote.rating,
                            review = remote.review,
                            status = remote.status,
                            isReplay = remote.isReplay,
                            platform = remote.platform,
                            playtimeHours = remote.playtimeHours,
                            completion = remote.completion,
                            tags = remote.tags,
                            createdAt = existingByKey.createdAt,
                        )
                        byId[refreshed.id] = refreshed
                        byKey[key] = refreshed
                    }

                    else -> {
                        byId[remote.id] = remote
                        byKey[key] = remote
                    }
                }
            }
            // Drop a pulled copy when a locally-created row already covers the
            // same game and day (the local row was refreshed above).
            val localDayKeys = byId.values
                .filter { it.id !in remoteIds }
                .map { diarySyncKey(it.gameId, it.playedAt) }
                .toSet()
            val merged = byId.values.filterNot { entry ->
                entry.id in remoteIds &&
                    diarySyncKey(entry.gameId, entry.playedAt) in localDayKeys
            }
            current.copy(diary = merged.sortedByDescending { it.playedAt })
        }
    }

    fun mergeRemoteLists(lists: List<GameList>) {
        if (lists.isEmpty()) return
        mutate { current ->
            val byId = current.lists.associateBy { it.id }.toMutableMap()
            lists.forEach { remote ->
                val local = byId[remote.id]
                byId[remote.id] = remote.copy(createdAt = local?.createdAt ?: remote.createdAt)
            }
            current.copy(lists = byId.values.toList())
        }
    }

    fun mergeRemoteFavorites(ids: List<Long>) {
        if (ids.isEmpty()) return
        mutate { current ->
            val incoming = ids.filterNot { it in current.removedFavoriteIds }
            current.copy(favorites = (current.favorites + incoming).distinct().take(4))
        }
    }

    fun setDisplayNameFromCloud(name: String) {
        val cleaned = name.trim()
        if (cleaned.isBlank()) return
        mutate { current -> current.copy(displayName = cleaned) }
    }

    fun clearRemovedGame(gameId: Long) {
        mutate { current -> current.copy(removedGameIds = current.removedGameIds - gameId) }
    }

    /** Mark games as uploaded so the next pull may adopt their web copy. */
    fun clearDirtyGames(gameIds: Collection<Long>) {
        if (gameIds.isEmpty()) return
        mutate { current ->
            val next = current.dirtyGameIds - gameIds.toSet()
            if (next == current.dirtyGameIds) current else current.copy(dirtyGameIds = next)
        }
    }

    fun clearRemovedFavorite(gameId: Long) {
        mutate { current -> current.copy(removedFavoriteIds = current.removedFavoriteIds - gameId) }
    }

    fun clearDiaryTombstone(gameId: Long, playedAt: Long) {
        mutate { current ->
            current.copy(deletedDiaryKeys = current.deletedDiaryKeys - diarySyncKey(gameId, playedAt))
        }
    }

    /** Cache games supplied by the cloud without triggering a write when nothing changed. */
    fun cacheRemoteGames(games: List<Game>) = cacheGames(games)

    private fun cacheIn(state: PersistedState, game: Game): PersistedState =
        if (state.game(game.id) == game) state
        else state.copy(games = state.games.filterNot { it.id == game.id } + game)
}
