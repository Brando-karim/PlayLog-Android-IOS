package com.playlog.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playlog.app.data.DiaryEntry
import com.playlog.app.data.Game
import com.playlog.app.data.GameRepository
import com.playlog.app.data.IgdbClient
import com.playlog.app.data.PlayStatus
import com.playlog.app.data.PlaylogStore
import com.playlog.app.data.ProfileStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

class PlaylogViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PlaylogStore(app)
    private val repository = GameRepository(
        store,
        IgdbClient(BuildConfig.IGDB_CLIENT_ID, BuildConfig.IGDB_CLIENT_SECRET),
    )

    val igdbConfigured: Boolean = repository.igdbConfigured
    val state = repository.state

    val stats: StateFlow<ProfileStats> = repository.state
        .map { repository.stats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.stats())

    private val _discover = MutableStateFlow<LoadState<List<Game>>>(LoadState.Loading)
    val discover: StateFlow<LoadState<List<Game>>> = _discover.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _search = MutableStateFlow<LoadState<List<Game>>>(LoadState.Idle)
    val search: StateFlow<LoadState<List<Game>>> = _search.asStateFlow()

    private var searchJob: Job? = null

    init {
        refreshDiscover()
    }

    fun refreshDiscover() {
        _discover.value = LoadState.Loading
        viewModelScope.launch {
            runCatching { repository.popular() }
                .onSuccess { _discover.value = LoadState.Ready(it) }
                .onFailure { _discover.value = LoadState.Failed(it.message ?: "Could not load games") }
        }
    }

    fun search(term: String) {
        _searchQuery.value = term
        searchJob?.cancel()
        if (term.isBlank()) {
            _search.value = LoadState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(280)
            _search.value = LoadState.Loading
            runCatching { repository.search(term) }
                .onSuccess { _search.value = LoadState.Ready(it) }
                .onFailure { _search.value = LoadState.Failed(it.message ?: "Search failed") }
        }
    }

    suspend fun loadGame(id: Long): Game? = repository.game(id)

    fun game(id: Long): Game? = state.value.game(id)

    fun saveLog(
        game: Game,
        status: PlayStatus,
        rating: Double?,
        review: String,
        isReplay: Boolean,
        playedAt: Long,
    ) {
        repository.saveLog(game, status, rating, review, isReplay, playedAt)
    }

    fun toggleBacklog(game: Game) = repository.toggleBacklog(game)

    fun toggleLike(game: Game) = repository.toggleLike(game)

    fun setStatus(game: Game, status: PlayStatus?) = repository.setStatus(game, status)

    fun deleteDiary(id: String) = repository.deleteDiary(id)

    fun createList(name: String, description: String) = repository.createList(name, description)

    fun deleteList(id: String) = repository.deleteList(id)

    fun addGameToList(listId: String, game: Game) = repository.addGameToList(listId, game)

    fun removeGameFromList(listId: String, gameId: Long) = repository.removeGameFromList(listId, gameId)

    fun toggleFavorite(gameId: Long) = repository.toggleFavorite(gameId)

    fun setDisplayName(name: String) = repository.setDisplayName(name)

    fun clearLibrary() = repository.clearLibrary()

    fun diaryFor(gameId: Long): List<DiaryEntry> =
        state.value.diary.filter { it.gameId == gameId }.sortedByDescending { it.playedAt }
}
