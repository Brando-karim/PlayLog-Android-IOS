package com.playlog.app.data

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Game(
    val id: Long,
    val name: String,
    val summary: String = "",
    val coverUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val aggregatedRating: Double? = null,
    val ratingCount: Int? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val releaseDate: Long? = null,
    val releaseYear: Int? = null,
    val screenshots: List<String> = emptyList(),
) {
    val year: Int?
        get() = releaseYear ?: releaseDate?.let {
            java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()).year
        }

    val displayRating: Double?
        get() = rating ?: aggregatedRating
}

enum class PlayStatus(val key: String, val label: String) {
    PLAYING("playing", "Playing"),
    PLAYED("played", "Played"),
    BACKLOG("backlog", "Watchlist"),
    DROPPED("dropped", "Dropped");

    companion object {
        fun from(key: String?): PlayStatus? = entries.firstOrNull { it.key == key }
    }
}

/** How much of a game the user finished. Dropped/playing live on [PlayStatus]. */
enum class Completion(val key: String, val label: String) {
    FINISHED("finished", "Finished"),
    COMPLETED("completed", "100%"),
    ENDLESS("endless", "Endless");

    companion object {
        fun from(key: String?): Completion? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Stable key used to match a local entry with a web log. The web stores a date
 * while Android stores an exact timestamp, so both sides match on the local day.
 */
fun diarySyncKey(gameId: Long, playedAt: Long): String {
    val day = java.time.Instant.ofEpochMilli(playedAt)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    return "$gameId:$day"
}

/** Split a comma/newline separated tag field into clean, unique tags. */
fun parseTags(raw: String): List<String> = raw
    .split(',', '\n')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { it.take(24).trim() }
    .filter { it.isNotEmpty() }
    .distinctBy { it.lowercase(Locale.US) }
    .take(8)

/** Hours as a compact label, e.g. "12h" or "12.5h". */
fun formatHours(hours: Double): String = "${formatHoursValue(hours)}h"

/** Hours without the suffix, for inputs and stat values. */
fun formatHoursValue(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toInt().toString()
    else String.format(Locale.US, "%.1f", hours)

@Serializable
data class LibraryEntry(
    val gameId: Long,
    val status: String,
    val rating: Double? = null,
    val liked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val playStatus: PlayStatus? get() = PlayStatus.from(status)
}

@Serializable
data class DiaryEntry(
    val id: String,
    val gameId: Long,
    val gameName: String,
    val coverUrl: String? = null,
    val rating: Double? = null,
    val review: String = "",
    val status: String = PlayStatus.PLAYED.key,
    val isReplay: Boolean = false,
    val platform: String? = null,
    val playtimeHours: Double? = null,
    val completion: String? = null,
    val tags: List<String> = emptyList(),
    val playedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class GameList(
    val id: String,
    val name: String,
    val description: String = "",
    val gameIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class PersistedState(
    val library: List<LibraryEntry> = emptyList(),
    val diary: List<DiaryEntry> = emptyList(),
    val lists: List<GameList> = emptyList(),
    val games: List<Game> = emptyList(),
    val favorites: List<Long> = emptyList(),
    val displayName: String = "Player One",
    val avatarUrl: String? = null,
    /** Local edits that still need to be pushed to the web profile. */
    val displayNameDirty: Boolean = false,
    val avatarDirty: Boolean = false,
    val removedGameIds: List<Long> = emptyList(),
    val deletedDiaryKeys: List<String> = emptyList(),
    val removedFavoriteIds: List<Long> = emptyList(),
    /**
     * Games whose log was edited on this device and not yet uploaded. Sync uses
     * this instead of comparing clocks, so a device with a wrong clock can never
     * overwrite a newer web edit, and a web edit always lands on a clean device.
     */
    val dirtyGameIds: List<Long> = emptyList(),
) {
    fun game(id: Long): Game? = games.firstOrNull { it.id == id }

    fun entry(id: Long): LibraryEntry? = library.firstOrNull { it.gameId == id }

    fun gamesFor(ids: List<Long>): List<Game> = ids.mapNotNull { game(it) }
}

/** Aggregated numbers for the profile screen. */
data class ProfileStats(
    val played: Int,
    val playing: Int,
    val backlog: Int,
    val dropped: Int,
    val liked: Int,
    val reviews: Int,
    val averageRating: Double?,
    val ratingHistogram: Map<Int, Int>,
    val topGenres: List<Pair<String, Int>>,
    val totalHours: Double = 0.0,
)
