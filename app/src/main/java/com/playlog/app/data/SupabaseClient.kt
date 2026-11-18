package com.playlog.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class CloudSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: String,
    val email: String,
)

sealed interface AuthOutcome {
    data class Success(val session: CloudSession) : AuthOutcome
    data class NeedsConfirmation(val message: String) : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

class SupabaseException(val code: Int, override val message: String) : Exception(message)

/**
 * Small Supabase REST client for the mobile app. It uses the public anon key
 * only; catalog writes go through owner-scoped database functions.
 */
class SupabaseClient(
    context: Context,
    private val baseUrl: String,
    private val anonKey: String,
) {
    private val prefs = context.applicationContext.getSharedPreferences("playlog_cloud", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val configured: Boolean get() = baseUrl.isNotBlank() && anonKey.isNotBlank()
    val hasSession: Boolean get() = !prefs.getString(KEY_REFRESH, null).isNullOrBlank()
    val storedUserId: String? get() = prefs.getString(KEY_USER_ID, null)
    val storedEmail: String? get() = prefs.getString(KEY_EMAIL, null)

    fun storedSession(): CloudSession? {
        val access = prefs.getString(KEY_ACCESS, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null).orEmpty()
        return CloudSession(access, refresh, prefs.getLong(KEY_EXPIRES, 0L), userId, email)
    }

    suspend fun signIn(email: String, password: String): AuthOutcome = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }
        val response = authRequest("token?grant_type=password", payload)
        if (response.first !in 200..299) {
            return@withContext AuthOutcome.Failure(errorMessage(response.second))
        }
        val session = parseSession(response.second)
        if (session == null) {
            AuthOutcome.Failure(errorMessage(response.second))
        } else {
            saveSession(session)
            AuthOutcome.Success(session)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): AuthOutcome =
        withContext(Dispatchers.IO) {
            val payload = buildJsonObject {
                put("email", email.trim())
                put("password", password)
                put("data", buildJsonObject {
                    put("display_name", displayName.trim())
                })
            }
            val response = authRequest("signup", payload)
            if (response.first !in 200..299) {
                return@withContext AuthOutcome.Failure(errorMessage(response.second))
            }
            val session = parseSession(response.second, allowNested = true)
            if (session != null) {
                saveSession(session)
                AuthOutcome.Success(session)
            } else {
                AuthOutcome.NeedsConfirmation(
                    "Check your email for a confirmation link, then sign in.",
                )
            }
        }

    suspend fun signOut() {
        runCatching {
            if (hasSession) authorized("POST", "auth/v1/logout", null, "return=minimal")
        }
        clearSession()
    }

    suspend fun get(path: String): JsonElement = authorized("GET", "rest/v1/$path")

    suspend fun post(path: String, body: JsonElement, prefer: String = "return=representation"): JsonElement =
        authorized("POST", "rest/v1/$path", body, prefer)

    suspend fun patch(path: String, body: JsonElement, prefer: String = "return=minimal"): JsonElement =
        authorized("PATCH", "rest/v1/$path", body, prefer)

    suspend fun delete(path: String, prefer: String = "return=minimal"): JsonElement =
        authorized("DELETE", "rest/v1/$path", null, prefer)

    suspend fun rpc(name: String, params: JsonElement): JsonElement =
        authorized("POST", "rest/v1/rpc/$name", params, "return=representation")

    /** Upload a binary object to a public Supabase Storage bucket. */
    suspend fun uploadObject(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
    ): JsonElement = withContext(Dispatchers.IO) {
        var session = ensureSession()
            ?: throw SupabaseException(401, "Sign in to upload a profile photo.")
        var response = executeUpload(session, bucket, path, bytes, contentType)
        if (response.first == 401) {
            session = refreshSession()
                ?: throw SupabaseException(401, "Your session expired. Sign in again.")
            response = executeUpload(session, bucket, path, bytes, contentType)
        }
        if (response.first !in 200..299) {
            throw SupabaseException(response.first, errorMessage(response.second))
        }
        if (response.second.isBlank()) JsonNull else json.parseToJsonElement(response.second)
    }

    fun publicObjectUrl(bucket: String, path: String, version: Long? = null): String {
        val base = "${baseUrl.trimEnd('/')}/storage/v1/object/public/$bucket/$path"
        return if (version == null) base else "$base?v=$version"
    }

    private suspend fun authorized(
        method: String,
        path: String,
        body: JsonElement? = null,
        prefer: String? = null,
    ): JsonElement = withContext(Dispatchers.IO) {
        var session = ensureSession()
            ?: throw SupabaseException(401, "Sign in to sync with Playlog.")
        var response = execute(method, path, body, prefer, session)
        if (response.first == 401) {
            session = refreshSession()
                ?: throw SupabaseException(401, "Your session expired. Sign in again.")
            response = execute(method, path, body, prefer, session)
        }
        if (response.first !in 200..299) {
            throw SupabaseException(response.first, errorMessage(response.second))
        }
        if (response.second.isBlank()) JsonNull else json.parseToJsonElement(response.second)
    }

    private suspend fun ensureSession(): CloudSession? {
        val current = storedSession() ?: return null
        if (current.accessToken.isNotBlank() && current.expiresAt > System.currentTimeMillis() + 60_000) {
            return current
        }
        return refreshSession()
    }

    private suspend fun refreshSession(): CloudSession? = withContext(Dispatchers.IO) {
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return@withContext null
        val payload = buildJsonObject { put("refresh_token", refresh) }
        val response = authRequest("token?grant_type=refresh_token", payload)
        if (response.first !in 200..299) {
            clearSession()
            return@withContext null
        }
        val session = parseSession(response.second)
        if (session == null) {
            clearSession()
            null
        } else {
            saveSession(session)
            session
        }
    }

    private fun execute(
        method: String,
        path: String,
        body: JsonElement?,
        prefer: String?,
        session: CloudSession,
    ): Pair<Int, String> {
        val builder = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("Accept", "application/json")
        if (prefer != null) builder.addHeader("Prefer", prefer)
        val requestBody = when {
            body != null -> body.toString().toRequestBody(jsonMedia)
            method == "POST" || method == "PATCH" -> ByteArray(0).toRequestBody(jsonMedia)
            else -> null
        }
        builder.method(method, requestBody)
        http.newCall(builder.build()).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private fun executeUpload(
        session: CloudSession,
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String,
    ): Pair<Int, String> {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/storage/v1/object/$bucket/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer ${session.accessToken}")
            .addHeader("Accept", "application/json")
            .addHeader("x-upsert", "true")
            .addHeader("cache-control", "max-age=3600")
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .build()
        http.newCall(request).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private fun authRequest(path: String, body: JsonElement): Pair<Int, String> {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/auth/v1/$path")
            .addHeader("apikey", anonKey)
            .addHeader("Accept", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        http.newCall(request).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    private fun parseSession(text: String, allowNested: Boolean = false): CloudSession? {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
        val source = if (allowNested) {
            root["session"]?.jsonObject ?: root
        } else {
            root
        }
        val access = source["access_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val refresh = source["refresh_token"]?.jsonPrimitive?.contentOrNull ?: return null
        val expiresIn = source["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L
        val user = source["user"]?.jsonObject ?: root["user"]?.jsonObject
        val userId = user?.get("id")?.jsonPrimitive?.contentOrNull
            ?: prefs.getString(KEY_USER_ID, null)
            ?: return null
        val email = user?.get("email")?.jsonPrimitive?.contentOrNull
            ?: prefs.getString(KEY_EMAIL, null)
            .orEmpty()
        return CloudSession(
            accessToken = access,
            refreshToken = refresh,
            expiresAt = System.currentTimeMillis() + expiresIn * 1000L,
            userId = userId,
            email = email,
        )
    }

    private fun saveSession(session: CloudSession) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putLong(KEY_EXPIRES, session.expiresAt)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    private fun errorMessage(text: String): String {
        val fallback = text.take(220).ifBlank { "Playlog could not reach the cloud." }
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
            ?: return fallback
        fun field(key: String): String? =
            obj[key]?.let { element ->
                when (element) {
                    is JsonPrimitive -> element.contentOrNull
                    is JsonObject -> element["message"]?.jsonPrimitive?.contentOrNull
                    else -> null
                }
            }?.takeIf { it.isNotBlank() }
        return field("msg")
            ?: field("message")
            ?: field("error_description")
            ?: field("error")
            ?: fallback
    }

    private fun buildJsonObject(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject =
        kotlinx.serialization.json.buildJsonObject(block)

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
    }
}
