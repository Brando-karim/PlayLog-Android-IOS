package com.playlog.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal IGDB v4 client using Twitch client-credentials OAuth.
 * Docs: https://api-docs.igdb.com/
 */
class IgdbClient(
    private val clientId: String,
    private val clientSecret: String,
) {

    val isConfigured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile private var token: String? = null
    @Volatile private var tokenExpiry: Long = 0L

    private suspend fun accessToken(): String = withContext(Dispatchers.IO) {
        val cached = token
        if (cached != null && System.currentTimeMillis() < tokenExpiry) return@withContext cached

        val url = "https://id.twitch.tv/oauth2/token" +
            "?client_id=${clientId.trim()}" +
            "&client_secret=${clientSecret.trim()}" +
            "&grant_type=client_credentials"
        val request = Request.Builder().url(url).post(ByteArray(0).toRequestBody(null)).build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("IGDB auth failed (${response.code}): ${body.take(160)}")
            val obj = json.parseToJsonElement(body).jsonObject
            val value = obj["access_token"]?.jsonPrimitive?.contentOrNull
                ?: error("IGDB auth response had no access_token")
            val expiresIn = obj["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 5_000_000L
            token = value
            tokenExpiry = System.currentTimeMillis() + (expiresIn - 120) * 1000
            value
        }
    }

    private suspend fun query(body: String): List<Game> = withContext(Dispatchers.IO) {
        val auth = accessToken()
        val request = Request.Builder()
            .url("https://api.igdb.com/v4/games")
            .addHeader("Client-ID", clientId.trim())
            .addHeader("Authorization", "Bearer $auth")
            .post(body.toRequestBody("text/plain".toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("IGDB query failed (${response.code}): ${text.take(200)}")
            val array = json.parseToJsonElement(text) as? JsonArray ?: return@use emptyList()
            array.mapNotNull { runCatching { it.jsonObject.toGame() }.getOrNull() }
        }
    }

    suspend fun popular(limit: Int = 30): List<Game> = query(
        """
        where total_rating_count != null;
        sort total_rating_count desc;
        $FIELDS
        limit $limit;
        """.trimIndent(),
    )

    suspend fun search(term: String, limit: Int = 30): List<Game> {
        val safe = term.replace("\\", "").replace("\"", "")
        if (safe.isBlank()) return emptyList()
        return query(
            """
            search "$safe";
            $FIELDS
            limit $limit;
            """.trimIndent(),
        )
    }

    suspend fun byIds(ids: List<Long>): List<Game> {
        if (ids.isEmpty()) return emptyList()
        return query(
            """
            where id = (${ids.joinToString(",")});
            $FIELDS
            limit 100;
            """.trimIndent(),
        )
    }

    private fun JsonObject.toGame(): Game {
        fun string(key: String): String? =
            this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

        fun number(key: String): Double? =
            this[key]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

        fun names(key: String): List<String> =
            (this[key] as? JsonArray).orEmpty()
                .mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }

        fun image(key: String, size: String): String? {
            val url = (this[key] as? JsonObject)?.get("url")?.jsonPrimitive?.contentOrNull
                ?: return null
            return "https:" + url.replace("t_thumb", size).replace("t_cover_small", size)
        }

        val screenshots = (this["screenshots"] as? JsonArray).orEmpty()
            .mapNotNull { shot ->
                shot.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                    ?.replace("t_thumb", "t_1080p")
                    ?.let { "https:$it" }
            }

        val artworks = (this["artworks"] as? JsonArray).orEmpty()
            .mapNotNull { art ->
                art.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                    ?.replace("t_thumb", "t_1080p")
                    ?.let { "https:$it" }
            }

        return Game(
            id = this["id"]?.jsonPrimitive?.long ?: error("missing id"),
            name = string("name") ?: "Unknown game",
            summary = string("summary").orEmpty(),
            coverUrl = image("cover", "t_cover_big"),
            backdropUrl = artworks.firstOrNull() ?: screenshots.firstOrNull(),
            rating = number("rating"),
            aggregatedRating = number("aggregated_rating"),
            ratingCount = number("rating_count")?.toInt()
                ?: number("total_rating_count")?.toInt(),
            genres = names("genres"),
            platforms = names("platforms"),
            releaseDate = number("first_release_date")?.toLong(),
            screenshots = screenshots.take(6),
        )
    }

    private companion object {
        const val FIELDS =
            "fields name,summary,rating,aggregated_rating,rating_count,total_rating_count," +
                "first_release_date,cover.url,genres.name,platforms.name,screenshots.url,artworks.url;"
    }
}
