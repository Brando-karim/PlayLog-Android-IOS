package com.playlog.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.playlog.app.data.CloudState
import com.playlog.app.data.CloudSync
import com.playlog.app.data.Completion
import com.playlog.app.data.DiaryEntry
import com.playlog.app.data.Game
import com.playlog.app.data.GameRepository
import com.playlog.app.data.IgdbClient
import com.playlog.app.data.PlayStatus
import com.playlog.app.data.PlaylogStore
import com.playlog.app.data.ProfileImage
import com.playlog.app.data.ProfileStats
import com.playlog.app.data.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val data: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

data class ProfileEditState(
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class PlaylogViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PlaylogStore(app)
    private val repository = GameRepository(
        store,
        IgdbClient(BuildConfig.IGDB_CLIENT_ID, BuildConfig.IGDB_CLIENT_SECRET),
    )
    private val cloudClient = SupabaseClient(
        app,
        BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_ANON_KEY,
    )
    private val cloud = CloudSync(store, cloudClient, viewModelScope)

    val igdbConfigured: Boolean = repository.igdbConfigured
    val cloudConfigured: Boolean = cloudClient.configured
    val account: StateFlow<CloudState> = cloud.state
    val state = repository.state

    private val _profileEdit = MutableStateFlow(ProfileEditState())
    val profileEdit: StateFlow<ProfileEditState> = _profileEdit.asStateFlow()

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
        platform: String? = null,
        playtimeHours: Double? = null,
        completion: Completion? = null,
        tags: List<String> = emptyList(),
    ) {
        repository.saveLog(
            game = game,
            status = status,
            rating = rating,
            review = review,
            isReplay = isReplay,
            playedAt = playedAt,
            platform = platform,
            playtimeHours = playtimeHours,
            completion = completion,
            tags = tags,
        )
        cloud.scheduleSync()
    }

    fun toggleBacklog(game: Game) {
        repository.toggleBacklog(game)
        cloud.scheduleSync()
    }

    fun toggleLike(game: Game) {
        repository.toggleLike(game)
        cloud.scheduleSync()
    }

    fun setStatus(game: Game, status: PlayStatus?) {
        repository.setStatus(game, status)
        cloud.scheduleSync()
    }

    fun deleteDiary(id: String) {
        val entry = state.value.diary.firstOrNull { it.id == id }
        repository.deleteDiary(id)
        if (entry != null) cloud.deleteRemoteDiary(entry)
        cloud.scheduleSync()
    }

    fun createList(name: String, description: String): String {
        val id = repository.createList(name, description)
        cloud.scheduleSync()
        return id
    }

    fun deleteList(id: String) {
        repository.deleteList(id)
        cloud.deleteRemoteList(id)
    }

    fun addGameToList(listId: String, game: Game) {
        repository.addGameToList(listId, game)
        cloud.scheduleSync()
    }

    fun removeGameFromList(listId: String, gameId: Long) {
        repository.removeGameFromList(listId, gameId)
        cloud.deleteRemoteListItem(listId, gameId)
    }

    fun toggleFavorite(gameId: Long) {
        repository.toggleFavorite(gameId)
        cloud.syncFavorite(gameId, state.value.favorites.contains(gameId))
    }

    fun setDisplayName(name: String) {
        repository.setDisplayName(name)
        cloud.scheduleSync()
    }

    fun resetProfileEditState() {
        _profileEdit.value = ProfileEditState()
    }

    fun saveProfile(name: String, avatarUri: Uri?) {
        val cleaned = name.trim()
        if (cleaned.isEmpty()) {
            _profileEdit.value = ProfileEditState(error = "Enter a display name.")
            return
        }
        if (cleaned.length > 80) {
            _profileEdit.value = ProfileEditState(error = "Display names can be up to 80 characters.")
            return
        }
        if (avatarUri != null && !cloudClient.hasSession) {
            _profileEdit.value = ProfileEditState(error = "Sign in to upload a profile photo.")
            return
        }

        _profileEdit.value = ProfileEditState(busy = true)
        viewModelScope.launch {
            try {
                if (avatarUri != null) {
                    val bytes = withContext(Dispatchers.IO) {
                        ProfileImage.prepare(getApplication(), avatarUri)
                    }
                    cloud.uploadAvatar(bytes)
                }
                repository.setDisplayName(cleaned)
                if (cloudClient.hasSession) {
                    cloud.scheduleSync(200)
                    _profileEdit.value = ProfileEditState(message = "Profile saved and syncing.")
                } else {
                    _profileEdit.value = ProfileEditState(
                        message = "Name saved on this device. Sign in to sync it.",
                    )
                }
            } catch (error: Throwable) {
                _profileEdit.value = ProfileEditState(
                    error = error.message ?: "Could not save your profile.",
                )
            }
        }
    }

    fun removeProfilePhoto() {
        repository.setAvatarUrl(null)
        if (cloudClient.hasSession) cloud.scheduleSync(200)
        _profileEdit.value = ProfileEditState(
            message = if (cloudClient.hasSession) {
                "Profile photo removed and syncing."
            } else {
                "Profile photo removed."
            },
        )
    }

    fun clearLibrary() {
        repository.clearLibrary()
        cloud.clearRemote()
        cloud.scheduleSync()
    }

    fun signIn(email: String, password: String) = cloud.signIn(email, password)

    fun signUp(email: String, password: String, displayName: String) =
        cloud.signUp(email, password, displayName)

    fun signOut() = cloud.signOut()

    fun syncNow() = cloud.syncNow(manual = true)

    fun diaryFor(gameId: Long): List<DiaryEntry> =
        state.value.diary.filter { it.gameId == gameId }.sortedByDescending { it.playedAt }
}
