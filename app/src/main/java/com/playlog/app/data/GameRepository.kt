package com.playlog.app.data

import kotlinx.coroutines.flow.StateFlow

class GameRepository(
    private val store: PlaylogStore,
    private val igdb: IgdbClient,
) {
    val state: StateFlow<PersistedState> = store.state
    val igdbConfigured: Boolean = igdb.isConfigured

    suspend fun popular(): List<Game> {
        val remote = if (igdbConfigured) {
            runCatching { igdb.popular() }.getOrNull().orEmpty()
        } else {
            emptyList()
        }
        val result = remote.ifEmpty { DemoCatalog.games }
        store.cacheGames(result)
        return result
    }

    suspend fun search(term: String): List<Game> {
        val query = term.trim()
        if (query.isEmpty()) return emptyList()
        val remote = if (igdbConfigured) {
            runCatching { igdb.search(query) }.getOrNull().orEmpty()
        } else {
            emptyList()
        }
        if (remote.isNotEmpty()) {
            store.cacheGames(remote)
            return remote
        }
        val local = DemoCatalog.search(query)
        store.cacheGames(local)
        return local
    }

    suspend fun game(id: Long): Game? {
        store.state.value.game(id)?.let { return it }
        if (!igdbConfigured) return DemoCatalog.games.firstOrNull { it.id == id }
        val loaded = runCatching { igdb.byIds(listOf(id)).firstOrNull() }.getOrNull() ?: return null
        store.cacheGames(listOf(loaded))
        return loaded
    }

    fun saveLog(
        game: Game,
        status: PlayStatus,
        rating: Double?,
        review: String,
        isReplay: Boolean,
        playedAt: Long,
    ) {
        store.cacheGames(listOf(game))
        store.setStatus(game, status, rating)
        val entry = DiaryEntry(
            id = java.util.UUID.randomUUID().toString(),
            gameId = game.id,
            gameName = game.name,
            coverUrl = game.coverUrl,
            rating = rating,
            review = review.trim(),
            status = status.key,
            isReplay = isReplay,
            playedAt = playedAt,
        )
        store.addDiary(entry)
    }

    fun toggleBacklog(game: Game) {
        val current = store.state.value.entry(game.id)?.playStatus
        if (current == PlayStatus.BACKLOG) {
            store.setStatus(game, null)
        } else {
            store.setStatus(game, PlayStatus.BACKLOG)
        }
    }

    fun toggleLike(game: Game) = store.toggleLike(game)

    fun setStatus(game: Game, status: PlayStatus?) = store.setStatus(game, status)

    fun deleteDiary(id: String) = store.deleteDiary(id)

    fun createList(name: String, description: String): String = store.createList(name, description)

    fun deleteList(id: String) = store.deleteList(id)

    fun addGameToList(listId: String, game: Game) = store.addGameToList(listId, game)

    fun removeGameFromList(listId: String, gameId: Long) = store.removeGameFromList(listId, gameId)

    fun toggleFavorite(gameId: Long) = store.toggleFavorite(gameId)

    fun setDisplayName(name: String) = store.setDisplayName(name)

    fun clearLibrary() = store.clearLibrary()

    fun stats(): ProfileStats = statsOf(store.state.value)

    private fun statsOf(snapshot: PersistedState): ProfileStats {
        val played = snapshot.library.count { it.status == PlayStatus.PLAYED.key }
        val playing = snapshot.library.count { it.status == PlayStatus.PLAYING.key }
        val backlog = snapshot.library.count { it.status == PlayStatus.BACKLOG.key }
        val dropped = snapshot.library.count { it.status == PlayStatus.DROPPED.key }
        val liked = snapshot.library.count { it.liked }

        val rated = snapshot.library.mapNotNull { it.rating }
        val histogram = rated.groupingBy { it.toInt().coerceIn(1, 5) }.eachCount()

        val genreCounts = snapshot.library
            .mapNotNull { entry -> snapshot.game(entry.gameId) }
            .flatMap { it.genres }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        return ProfileStats(
            played = played,
            playing = playing,
            backlog = backlog,
            dropped = dropped,
            liked = liked,
            reviews = snapshot.diary.count { it.review.isNotBlank() },
            averageRating = if (rated.isEmpty()) null else rated.average(),
            ratingHistogram = histogram,
            topGenres = genreCounts,
        )
    }
}
