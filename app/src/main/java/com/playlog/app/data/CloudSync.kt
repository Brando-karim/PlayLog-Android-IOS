package com.playlog.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

data class CloudState(
    val configured: Boolean = true,
    val signedIn: Boolean = false,
    val email: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val lastSync: Long? = null,
)

/**
 * Two-way sync between the local JSON ledger and the Playlog web database.
 *
 * The web schema keeps one log per profile+game, so the newest local diary entry
 * for each game becomes that log's review. Local entries keep their own history.
 */
class CloudSync(
    private val store: PlaylogStore,
    private val client: SupabaseClient,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val _state = MutableStateFlow(
        CloudState(
            configured = client.configured,
            signedIn = client.hasSession,
            email = client.storedEmail,
        ),
    )
    val state: StateFlow<CloudState> = _state.asStateFlow()

    private var debounceJob: Job? = null
    private var running = false

    init {
        if (client.hasSession) scheduleSync(1_200)
    }

    fun signIn(email: String, password: String) {
        if (!client.configured) {
            _state.update { it.copy(error = "Cloud sync is not configured in this build.") }
            return
        }
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Enter your email and password.") }
            return
        }
        scope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            when (val outcome = client.signIn(email, password)) {
                is AuthOutcome.Success -> {
                    _state.update {
                        it.copy(
                            signedIn = true,
                            email = outcome.session.email,
                            busy = false,
                            message = "Signed in. Syncing your ledger…",
                        )
                    }
                    syncNow(manual = false)
                }

                is AuthOutcome.Failure -> _state.update {
                    it.copy(busy = false, signedIn = false, error = outcome.message)
                }

                is AuthOutcome.NeedsConfirmation -> _state.update {
                    it.copy(busy = false, message = outcome.message)
                }
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        if (!client.configured) {
            _state.update { it.copy(error = "Cloud sync is not configured in this build.") }
            return
        }
        if (email.isBlank() || password.length < 6) {
            _state.update { it.copy(error = "Enter an email and a password of at least 6 characters.") }
            return
        }
        scope.launch {
            _state.update { it.copy(busy = true, error = null, message = null) }
            when (val outcome = client.signUp(email, password, displayName)) {
                is AuthOutcome.Success -> {
                    _state.update {
                        it.copy(
                            signedIn = true,
                            email = outcome.session.email,
                            busy = false,
                            message = "Account created. Syncing your ledger…",
                        )
                    }
                    syncNow(manual = false)
                }

                is AuthOutcome.Failure -> _state.update {
                    it.copy(busy = false, signedIn = false, error = outcome.message)
                }

                is AuthOutcome.NeedsConfirmation -> _state.update {
                    it.copy(busy = false, message = outcome.message)
                }
            }
        }
    }

    fun signOut() {
        scope.launch {
            _state.update { it.copy(busy = true) }
            runCatching { client.signOut() }
            _state.update {
                it.copy(
                    signedIn = false,
                    email = null,
                    busy = false,
                    lastSync = null,
                    message = "Signed out. Your local ledger stays on this device.",
                    error = null,
                )
            }
        }
    }

    fun scheduleSync(delayMillis: Long = 1_500) {
        if (!client.hasSession) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMillis)
            syncNow(manual = false)
        }
    }

    fun syncNow(manual: Boolean = true) {
        if (!client.hasSession) {
            _state.update { it.copy(signedIn = false, error = "Sign in to sync with Playlog.") }
            return
        }
        if (running) return
        running = true
        scope.launch {
            _state.update {
                it.copy(
                    busy = true,
                    error = null,
                    message = if (manual) "Syncing…" else it.message,
                )
            }
            try {
                performSync()
                _state.update {
                    it.copy(
                        busy = false,
                        signedIn = true,
                        email = client.storedEmail,
                        lastSync = System.currentTimeMillis(),
                        message = if (manual) "Synced just now." else "Your ledger is up to date.",
                    )
                }
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        busy = false,
                        signedIn = client.hasSession,
                        error = error.message ?: "Sync failed. Try again.",
                    )
                }
            } finally {
                running = false
            }
        }
    }

    fun syncFavorite(gameId: Long, favorite: Boolean) {
        if (!client.hasSession) return
        val userId = client.storedUserId ?: return
        scope.launch {
            runCatching {
                if (!favorite) {
                    client.delete("profile_favorites?profile_id=eq.$userId&game_id=eq.$gameId")
                    store.clearRemovedFavorite(gameId)
                    return@runCatching
                }
                val entry = store.state.value.entry(gameId)
                if (entry?.status != PlayStatus.PLAYED.key) return@runCatching
                val remote = fetchFavorites(userId)
                if (remote.any { it.gameId == gameId }) return@runCatching
                val freePosition = (1..4).firstOrNull { position ->
                    remote.none { it.position == position }
                } ?: return@runCatching
                val game = store.state.value.game(gameId) ?: return@runCatching
                upsertGames(listOf(game))
                client.post(
                    "profile_favorites",
                    buildJsonObject {
                        put("profile_id", userId)
                        put("game_id", gameId)
                        put("position", freePosition)
                    },
                )
            }
        }
    }

    fun deleteRemoteList(listId: String) {
        if (!client.hasSession) return
        scope.launch {
            runCatching { client.delete("game_lists?id=eq.$listId") }
        }
    }

    fun deleteRemoteListItem(listId: String, gameId: Long) {
        if (!client.hasSession) return
        scope.launch {
            runCatching { client.delete("list_items?list_id=eq.$listId&game_id=eq.$gameId") }
        }
    }

    fun deleteRemoteDiary(entry: DiaryEntry) {
        if (!client.hasSession) return
        // The web keeps one review per game. Only clear it when this was the last
        // local entry for that game; otherwise the newest remaining entry owns it.
        if (store.state.value.diary.any { it.gameId == entry.gameId }) return
        val userId = client.storedUserId ?: return
        scope.launch {
            runCatching {
                val logs = fetchLogs(userId).filter { it.gameId == entry.gameId }
                logs.forEach { log -> client.delete("reviews?game_log_id=eq.${log.id}") }
            }
        }
    }

    fun clearRemote() {
        if (!client.hasSession) return
        val userId = client.storedUserId ?: return
        scope.launch {
            runCatching {
                client.delete("game_logs?profile_id=eq.$userId")
                client.delete("game_lists?profile_id=eq.$userId")
                client.delete("profile_favorites?profile_id=eq.$userId")
            }
        }
    }

    /** Upload a prepared square WebP and store its cache-busted public URL locally. */
    suspend fun uploadAvatar(bytes: ByteArray): String {
        val userId = client.storedUserId
            ?: throw SupabaseException(401, "Sign in to upload a profile photo.")
        val path = "$userId/avatar.webp"
        client.uploadObject(PROFILE_BUCKET, path, bytes, "image/webp")
        val url = client.publicObjectUrl(PROFILE_BUCKET, path, System.currentTimeMillis())
        store.setAvatarUrl(url)
        return url
    }

    private suspend fun performSync() {
        if (!client.configured) throw SupabaseException(0, "Cloud sync is not configured in this build.")
        val userId = client.storedUserId ?: throw SupabaseException(401, "Sign in to sync with Playlog.")

        // Process local deletions first so a pull cannot resurrect them.
        store.state.value.removedGameIds.forEach { gameId ->
            runCatching {
                client.delete("game_logs?profile_id=eq.$userId&game_id=eq.$gameId")
                store.clearRemovedGame(gameId)
            }
        }
        store.state.value.removedFavoriteIds.forEach { gameId ->
            runCatching {
                client.delete("profile_favorites?profile_id=eq.$userId&game_id=eq.$gameId")
                store.clearRemovedFavorite(gameId)
            }
        }
        store.state.value.deletedDiaryKeys.forEach { key ->
            val gameId = key.substringBefore(':').toLongOrNull() ?: return@forEach
            // A newer local entry for this game now owns the web review.
            if (store.state.value.diary.any { it.gameId == gameId }) return@forEach
            runCatching {
                fetchLogs(userId)
                    .filter { it.gameId == gameId }
                    .forEach { log -> client.delete("reviews?game_log_id=eq.${log.id}") }
            }
        }

        val remoteLogs = fetchLogs(userId)
        val remoteLists = fetchLists(userId)
        val remoteItems = fetchListItems(remoteLists.map { it.id })
        val remoteFavorites = fetchFavorites(userId)
        val remoteProfile = fetchProfile(userId)

        val catalogIds = (
            remoteLogs.map { it.gameId } +
                remoteFavorites.map { it.gameId } +
                remoteItems.map { it.gameId } +
                store.state.value.lists.flatMap { it.gameIds }
            ).distinct()
        store.cacheRemoteGames(fetchGames(catalogIds).map { it.toGame() })

        val snapshot = store.state.value

        // A profile edit made on this device wins until it has been pushed. Only
        // the changed fields are written, so a local name edit never clears a
        // web avatar that this device has not touched.
        val nameDirty = snapshot.displayNameDirty
        val avatarDirty = snapshot.avatarDirty
        if (nameDirty || avatarDirty) {
            runCatching {
                client.patch(
                    "profiles?id=eq.$userId",
                    buildJsonObject {
                        if (nameDirty) {
                            put("display_name", snapshot.displayName.trim().ifBlank { "Player One" })
                        }
                        if (avatarDirty) {
                            put("avatar_url", snapshot.avatarUrl?.let { JsonPrimitive(it) } ?: JsonNull)
                        }
                    },
                )
                store.markProfileClean()
            }
        }
        if (!nameDirty) {
            val remoteName = remoteProfile?.displayName?.trim().orEmpty()
            if (remoteName.isNotBlank()) store.setDisplayNameFromCloud(remoteName)
        }
        if (!avatarDirty) {
            store.setAvatarFromCloud(remoteProfile?.avatarUrl)
        }

        val remoteByGame = remoteLogs.associateBy { it.gameId }
        val dirtyGames = snapshot.dirtyGameIds.toSet()
        snapshot.library.forEach { entry ->
            val latest = snapshot.diary
                .filter { it.gameId == entry.gameId }
                .maxByOrNull { it.playedAt }
            val remote = remoteByGame[entry.gameId]
            // Upload only local edits plus games the web has never seen. A device
            // whose clock runs fast must not overwrite a newer web edit.
            if (remote == null || entry.gameId in dirtyGames) {
                val game = snapshot.game(entry.gameId) ?: return@forEach
                pushLog(entry, latest, game)
                store.clearDirtyGames(listOf(entry.gameId))
            }
        }

        val listGames = snapshot.lists
            .flatMap { it.gameIds }
            .distinct()
            .mapNotNull { snapshot.game(it) }
        if (listGames.isNotEmpty()) upsertGames(listGames)

        val remoteListsById = remoteLists.associateBy { it.id }
        val remoteItemsByList = remoteItems.groupBy { it.listId }
        snapshot.lists.forEach { list ->
            runCatching {
                pushList(
                    userId = userId,
                    list = list,
                    remote = remoteListsById[list.id],
                    remoteItems = remoteItemsByList[list.id].orEmpty(),
                    snapshot = snapshot,
                )
            }
        }

        val favoriteGames = snapshot.favorites
            .take(4)
            .distinct()
            .mapNotNull { snapshot.game(it) }
        if (favoriteGames.isNotEmpty()) upsertGames(favoriteGames)

        val remoteFavoritesByGame = remoteFavorites.map { it.gameId }.toSet()
        val playedIds = snapshot.library
            .filter { it.status == PlayStatus.PLAYED.key }
            .map { it.gameId }
            .toSet()
        val freePositions = (1..4)
            .filter { position -> remoteFavorites.none { it.position == position } }
            .toMutableList()
        snapshot.favorites.take(4).distinct().forEach { gameId ->
            if (gameId !in remoteFavoritesByGame && gameId in playedIds && freePositions.isNotEmpty()) {
                val position = freePositions.removeAt(0)
                runCatching {
                    client.post(
                        "profile_favorites",
                        buildJsonObject {
                            put("profile_id", userId)
                            put("game_id", gameId)
                            put("position", position)
                        },
                    )
                }
            }
        }

        pullRemote(userId)
    }

    private suspend fun pullRemote(userId: String) {
        val logs = fetchLogs(userId)
        val reviews = fetchReviews(logs.map { it.id })
        val lists = fetchLists(userId)
        val items = fetchListItems(lists.map { it.id })
        val favorites = fetchFavorites(userId)
        val profile = fetchProfile(userId)
        val games = fetchGames(
            (logs.map { it.gameId } + items.map { it.gameId } + favorites.map { it.gameId }).distinct(),
        )
        val gamesById = games.associateBy { it.id }
        store.cacheRemoteGames(games.map { it.toGame() })

        val reviewByLog = reviews.associateBy { it.gameLogId }
        // Games with unpushed local edits stay local until their upload succeeds.
        val pending = store.state.value.dirtyGameIds.toSet()
        val library = logs.map { log ->
            LibraryEntry(
                gameId = log.gameId,
                status = log.status.toMobileStatus(),
                rating = log.rating,
                liked = false,
                updatedAt = log.updatedAtMillis(),
            )
        }
        store.mergeRemoteLibrary(library, pending)

        // Watchlist/backlog rows are library entries, not diary entries. Only pull
        // played/playing/dropped logs (or logs that actually carry a review) into
        // the Android diary so the journal does not fill up with unplayed games.
        val diary = logs
            .filter { log ->
                log.status != "backlog" &&
                    log.status != "wishlist" ||
                    reviewByLog[log.id]?.body?.isNotBlank() == true
            }
            .map { log ->
                val review = reviewByLog[log.id]
                val game = gamesById[log.gameId]
                DiaryEntry(
                    id = log.id,
                    gameId = log.gameId,
                    gameName = game?.title ?: "Game ${log.gameId}",
                    coverUrl = game?.coverUrl,
                    rating = log.rating,
                    review = review?.body.orEmpty(),
                    status = log.status.toMobileStatus(),
                    isReplay = log.replay,
                    platform = log.platform?.ifBlank { null },
                    playtimeHours = log.playtimeHours,
                    completion = log.completion?.ifBlank { null },
                    tags = log.tags.orEmpty(),
                    playedAt = log.finishedAtMillis() ?: log.updatedAtMillis(),
                    createdAt = log.createdAtMillis(),
                )
            }
        store.mergeRemoteDiary(diary, pending)

        val itemsByList = items.groupBy { it.listId }
        store.mergeRemoteLists(
            lists.map { list ->
                GameList(
                    id = list.id,
                    name = list.name,
                    description = list.description.orEmpty(),
                    gameIds = itemsByList[list.id]
                        .orEmpty()
                        .sortedBy { it.position }
                        .map { it.gameId },
                    createdAt = list.createdAtMillis(),
                )
            },
        )
        store.mergeRemoteFavorites(favorites.map { it.gameId })

        val cloudName = profile?.displayName?.trim().orEmpty()
        if (!store.state.value.displayNameDirty && cloudName.isNotBlank()) {
            store.setDisplayNameFromCloud(cloudName)
        }
        if (!store.state.value.avatarDirty) {
            store.setAvatarFromCloud(profile?.avatarUrl)
        }
    }

    private suspend fun pushLog(entry: LibraryEntry, diary: DiaryEntry?, game: Game) {
        val releaseDate = game.releaseDate
            ?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            ?: game.releaseYear?.let { "$it-01-01" }
        val finishedAt = diary?.playedAt
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
        val review = diary?.review?.trim().orEmpty()

        client.rpc(
            "save_mobile_game_log",
            buildJsonObject {
                put("p_game_id", game.id)
                put("p_title", game.name)
                put("p_cover_url", game.coverUrl)
                put("p_release_date", releaseDate)
                put("p_genres", buildJsonArray { game.genres.forEach { add(it) } })
                put("p_platforms", buildJsonArray { game.platforms.forEach { add(it) } })
                put("p_summary", game.summary.ifBlank { null })
                put("p_status", entry.status)
                put("p_rating", entry.rating?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_finished_at", finishedAt)
                put("p_replay", diary?.isReplay ?: false)
                put("p_review_body", review.ifBlank { null })
                put("p_is_public", true)
                put("p_update_review", review.isNotBlank())
                put("p_platform", diary?.platform?.ifBlank { null })
                put("p_playtime_hours", diary?.playtimeHours?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_completion", diary?.completion?.ifBlank { null })
                put("p_tags", buildJsonArray { diary?.tags?.forEach { add(it) } })
                // A library-only sync (e.g. watchlist toggle) must not clear details
                // that were set on another device.
                put("p_update_details", diary != null)
            },
        )
    }

    private suspend fun pushList(
        userId: String,
        list: GameList,
        remote: RemoteGameList?,
        remoteItems: List<RemoteListItem>,
        snapshot: PersistedState,
    ) {
        if (remote == null) {
            client.post(
                "game_lists",
                buildJsonObject {
                    put("id", list.id)
                    put("profile_id", userId)
                    put("name", list.name)
                    put("description", list.description.ifBlank { null })
                    put("visibility", "private")
                },
            )
        } else {
            client.patch(
                "game_lists?id=eq.${list.id}",
                buildJsonObject {
                    put("name", list.name)
                    put("description", list.description.ifBlank { null })
                },
            )
        }

        val existing = remoteItems.map { it.gameId }.toSet()
        var position = (remoteItems.maxOfOrNull { it.position } ?: -1) + 1
        list.gameIds.forEach { gameId ->
            if (gameId !in existing && snapshot.game(gameId) != null) {
                runCatching {
                    client.post(
                        "list_items",
                        buildJsonObject {
                            put("list_id", list.id)
                            put("game_id", gameId)
                            put("position", position)
                        },
                    )
                }
                position += 1
            }
        }
    }

    private suspend fun upsertGames(games: List<Game>) {
        val distinct = games.distinctBy { it.id }
        if (distinct.isEmpty()) return
        client.rpc(
            "upsert_mobile_games",
            buildJsonObject {
                put(
                    "p_games",
                    buildJsonArray {
                        distinct.forEach { game ->
                            val releaseDate = game.releaseDate
                                ?.let {
                                    Instant.ofEpochSecond(it)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toString()
                                }
                                ?: game.releaseYear?.let { "$it-01-01" }
                            add(
                                buildJsonObject {
                                    put("id", game.id)
                                    put("title", game.name)
                                    put("cover_url", game.coverUrl)
                                    put("release_date", releaseDate)
                                    put("genres", buildJsonArray { game.genres.forEach { add(it) } })
                                    put("platforms", buildJsonArray { game.platforms.forEach { add(it) } })
                                    put("summary", game.summary.ifBlank { null })
                                },
                            )
                        }
                    },
                )
            },
        )
    }

    private suspend fun fetchLogs(userId: String): List<RemoteGameLog> =
        parseList(
            client.get(
                "game_logs?select=id,game_id,status,rating,finished_at,replay,platform,playtime_hours,completion,tags,created_at,updated_at" +
                    "&profile_id=eq.$userId&order=updated_at.desc",
            ),
        )

    private suspend fun fetchReviews(logIds: List<String>): List<RemoteReview> {
        if (logIds.isEmpty()) return emptyList()
        return logIds.chunked(80).flatMap { chunk ->
            parseList(
                client.get(
                    "reviews?select=id,game_log_id,body,is_public,created_at" +
                        "&game_log_id=in.(${chunk.joinToString(",")})",
                ),
            )
        }
    }

    private suspend fun fetchLists(userId: String): List<RemoteGameList> =
        parseList(
            client.get(
                "game_lists?select=id,name,description,visibility,created_at" +
                    "&profile_id=eq.$userId&order=created_at.asc",
            ),
        )

    private suspend fun fetchListItems(listIds: List<String>): List<RemoteListItem> {
        if (listIds.isEmpty()) return emptyList()
        return listIds.chunked(50).flatMap { chunk ->
            parseList(
                client.get(
                    "list_items?select=list_id,game_id,position" +
                        "&list_id=in.(${chunk.joinToString(",")})&order=position.asc",
                ),
            )
        }
    }

    private suspend fun fetchFavorites(userId: String): List<RemoteFavorite> =
        parseList(
            client.get(
                "profile_favorites?select=game_id,position" +
                    "&profile_id=eq.$userId&order=position.asc",
            ),
        )

    private suspend fun fetchProfile(userId: String): RemoteProfile? =
        parseList<RemoteProfile>(
            client.get("profiles?select=display_name,avatar_url&id=eq.$userId"),
        ).firstOrNull()

    private suspend fun fetchGames(gameIds: List<Long>): List<RemoteGame> {
        if (gameIds.isEmpty()) return emptyList()
        return gameIds.chunked(80).flatMap { chunk ->
            parseList(
                client.get(
                    "games?select=id,title,cover_url,release_date,genres,platforms,summary" +
                        "&id=in.(${chunk.joinToString(",")})",
                ),
            )
        }
    }

    private inline fun <reified T> parseList(element: JsonElement): List<T> =
        runCatching { json.decodeFromString<List<T>>(element.toString()) }.getOrDefault(emptyList())

    private companion object {
        const val PROFILE_BUCKET = "playlog-profile-images"
    }
}

private fun String.toMobileStatus(): String =
    if (this == "wishlist") PlayStatus.BACKLOG.key else this

// PostgREST renders timestamptz values as "2026-11-14T17:33:46.3+00:00", which
// Instant.parse rejects, so accept both offset and Z forms.
private fun String?.isoMillis(): Long = this?.let { value ->
    runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(value).toEpochMilli() }
        .getOrNull()
} ?: 0L

private fun String?.dateMillis(): Long? =
    this?.let {
        runCatching {
            LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }.getOrNull()
    }

@Serializable
private data class RemoteGameLog(
    val id: String,
    @SerialName("game_id") val gameId: Long,
    val status: String,
    val rating: Double? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    val replay: Boolean = false,
    val platform: String? = null,
    @SerialName("playtime_hours") val playtimeHours: Double? = null,
    val completion: String? = null,
    val tags: List<String>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun updatedAtMillis(): Long = updatedAt.isoMillis()
    fun createdAtMillis(): Long = (createdAt ?: updatedAt).isoMillis()
    fun finishedAtMillis(): Long? = finishedAt.dateMillis()
}

@Serializable
private data class RemoteReview(
    val id: String,
    @SerialName("game_log_id") val gameLogId: String,
    val body: String,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class RemoteGameList(
    val id: String,
    val name: String,
    val description: String? = null,
    val visibility: String = "private",
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun createdAtMillis(): Long = createdAt.isoMillis()
}

@Serializable
private data class RemoteListItem(
    @SerialName("list_id") val listId: String,
    @SerialName("game_id") val gameId: Long,
    val position: Int = 0,
)

@Serializable
private data class RemoteFavorite(
    @SerialName("game_id") val gameId: Long,
    val position: Int = 0,
)

@Serializable
private data class RemoteProfile(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
private data class RemoteGame(
    val id: Long,
    val title: String,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val genres: List<String>? = null,
    val platforms: List<String>? = null,
    val summary: String? = null,
) {
    fun toGame(): Game {
        val millis = releaseDate?.let {
            runCatching {
                LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
        }
        return Game(
            id = id,
            name = title,
            summary = summary.orEmpty(),
            coverUrl = coverUrl,
            genres = genres.orEmpty(),
            platforms = platforms.orEmpty(),
            releaseDate = millis?.let { it / 1000 },
            releaseYear = releaseDate?.take(4)?.toIntOrNull(),
        )
    }
}
