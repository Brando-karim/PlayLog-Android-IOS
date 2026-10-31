package com.playlog.app.data

import kotlinx.serialization.Serializable

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
    BACKLOG("backlog", "Backlog"),
    DROPPED("dropped", "Dropped");

    companion object {
        fun from(key: String?): PlayStatus? = entries.firstOrNull { it.key == key }
    }
}

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
)
