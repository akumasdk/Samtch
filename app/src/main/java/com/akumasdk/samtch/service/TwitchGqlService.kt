package com.akumasdk.samtch.service

import android.util.Log
import com.akumasdk.samtch.data.model.TwitchGame
import com.akumasdk.samtch.data.model.TwitchRoles
import com.akumasdk.samtch.data.model.TwitchStream
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.data.model.TwitchUser
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Service for fetching Twitch stream access tokens via GraphQL
 * Required for direct HLS playback
 */
object TwitchGqlService {

    private const val TAG = "TwitchGqlService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedIntegrityToken: String? = null

    @Volatile
    private var cachedDynamicClientId: String? = null

    // Twitch internal endpoint for integrity tokens
    private const val INTEGRITY_URL = "https://gql.twitch.tv/integrity"

    // Add browser-ish headers (helps with Twitch tightening checks)
    private const val ORIGIN = "https://www.twitch.tv"
    private const val REFERER = "https://www.twitch.tv/"

    /**
     * Dynamically scrapes the Twitch Client ID from the homepage.
     */
    private suspend fun getDynamicClientId(): String = withContext(Dispatchers.IO) {
        cachedDynamicClientId?.let { return@withContext it }

        try {
            val request = Request.Builder()
                .url("https://www.twitch.tv")
                .header("User-Agent", Constants.USER_AGENT)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful && body.isNotEmpty()) {
                // Look for clientId="ID" (script block) or "Client-ID":"ID" (JSON/Legacy)
                val regex =
                    """(?:clientId\s*=\s*["']([^"']+)["']|"Client-ID"\s*:\s*["']([^"']+)["'])""".toRegex()
                val match = regex.find(body)
                // The ID could be in group 1 or group 2 depending on which pattern matched
                val scrapedId = match?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                    ?: match?.groupValues?.get(2)

                if (!scrapedId.isNullOrBlank()) {
                    Log.d(TAG, "Successfully scraped dynamic Client-ID: $scrapedId")
                    cachedDynamicClientId = scrapedId
                    return@withContext scrapedId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while scraping dynamic Client-ID", e)
        }

        Constants.TWITCH_GRAPHQL_CLIENT_ID
    }

    // Stable per app-process run (don’t use a constant)
    private val deviceId: String = UUID.randomUUID().toString().replace("-", "")

    private const val PLAYBACK_ACCESS_TOKEN_QUERY = """
        query PlaybackAccessToken(${"$"}login: String!, ${"$"}playerType: String!) {
          streamPlaybackAccessToken(
            channelName: ${"$"}login,
            params: { platform: "web", playerBackend: "mediaplayer", playerType: ${"$"}playerType }
          ) {
            value
            signature
          }
        }
    """

    private const val STREAM_METADATA_QUERY = """
        query StreamMetadata(${"$"}login: String!) {
          user(login: ${"$"}login) {
            id
            login
            displayName
            description
            profileImageURL(width: 300)
            createdAt
            roles {
              isPartner
            }
            stream {
              id
              title
              type
              viewersCount
              previewImageURL(height: 1080, width: 1920)
              createdAt
              game {
                name
              }
            }
          }
        }
    """

    private fun Request.Builder.addCommonHeaders(clientId: String): Request.Builder {
        return this
            .header("Client-Id", clientId)
            .header("X-Device-Id", deviceId)
            .header("User-Agent", Constants.USER_AGENT)
            .header("Origin", ORIGIN)
            .header("Referer", REFERER)
            .header("Accept", "application/json")
    }

    /**
     * Fetches detailed stream and user metadata.
     */
    suspend fun getStreamMetadata(channelName: String): TwitchStreamMetadata? =
        withContext(Dispatchers.IO) {
            try {
                val clientId = getDynamicClientId()
                val payload = JSONObject().apply {
                    put("operationName", "StreamMetadata")
                    put("query", STREAM_METADATA_QUERY.trimIndent())
                    put("variables", JSONObject().apply {
                        put("login", channelName.lowercase())
                    })
                }

                val request = Request.Builder()
                    .url(Constants.TWITCH_GQL_ENDPOINT)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .addCommonHeaders(clientId)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) return@withContext null

                val json = JSONObject(body)
                val data = json.optJSONObject("data")
                val userJson = data?.optJSONObject("user") ?: return@withContext null

                val rolesJson = userJson.optJSONObject("roles")
                val streamJson = userJson.optJSONObject("stream")
                val gameJson = streamJson?.optJSONObject("game")

                val user = TwitchUser(
                    id = userJson.getString("id"),
                    login = userJson.getString("login"),
                    displayName = userJson.getString("displayName"),
                    description = userJson.optString("description"),
                    profileImageUrl = userJson.optString("profileImageURL"),
                    createdAt = userJson.optString("createdAt"),
                    roles = rolesJson?.let { TwitchRoles(it.getBoolean("isPartner")) },
                    stream = streamJson?.let {
                        TwitchStream(
                            id = it.getString("id"),
                            title = it.getString("title"),
                            type = it.optString("type"),
                            viewersCount = it.getInt("viewersCount"),
                            previewImageUrl = it.optString("previewImageURL"),
                            createdAt = it.optString("createdAt"),
                            game = gameJson?.let { g -> TwitchGame(g.getString("name")) }
                        )
                    }
                )

                TwitchStreamMetadata(user)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stream metadata", e)
                null
            }
        }

    /**
     * Fetches a new Integrity Token from Twitch.
     */
    private suspend fun fetchIntegrityToken(clientId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(INTEGRITY_URL)
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .addCommonHeaders(clientId)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.w(TAG, "Integrity token fetch failed: ${response.code} body=$body")
                    return@withContext null
                }

                val json = JSONObject(body)
                val token = json.optString("token").takeIf { it.isNotBlank() }

                if (token != null) {
                    cachedIntegrityToken = token
                    return@withContext token
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch integrity token", e)
            }
            null
        }

    /**
     * Fetch playback access token and signature for a channel
     */
    suspend fun getPlaybackAccessToken(
        channelName: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val clientId = getDynamicClientId()

        val firstIntegrity = cachedIntegrityToken ?: fetchIntegrityToken(clientId)
        val first = getPlaybackAccessTokenOnce(channelName, clientId, firstIntegrity)
        if (first != null) return@withContext first

        cachedIntegrityToken = null
        val secondIntegrity = fetchIntegrityToken(clientId)
        return@withContext getPlaybackAccessTokenOnce(channelName, clientId, secondIntegrity)
    }

    private fun getPlaybackAccessTokenOnce(
        channelName: String,
        clientId: String,
        integrityToken: String?
    ): Pair<String, String>? {
        return try {
            val payload = JSONObject().apply {
                put("operationName", "PlaybackAccessToken")
                put("query", PLAYBACK_ACCESS_TOKEN_QUERY.trimIndent())
                put("variables", JSONObject().apply {
                    put("login", channelName.lowercase())
                    put("playerType", "site")
                })
            }

            val requestBuilder = Request.Builder()
                .url(Constants.TWITCH_GQL_ENDPOINT)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)

            if (!integrityToken.isNullOrBlank()) {
                requestBuilder.header("Client-Integrity", integrityToken)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) return null

            val json = JSONObject(responseBody)
            val data = json.optJSONObject("data")
            val streamPlaybackAccessToken = data?.optJSONObject("streamPlaybackAccessToken")

            val token = streamPlaybackAccessToken?.optString("value")
            val signature = streamPlaybackAccessToken?.optString("signature")

            if (token.isNullOrBlank() || signature.isNullOrBlank()) return null

            Pair(token, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Playback token request exception", e)
            null
        }
    }

    fun buildHlsUrl(channelName: String, token: String, signature: String): String {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val random = (Math.random() * 999999).toInt()

        return "${Constants.TWITCH_HLS_BASE}${channelName.lowercase()}.m3u8" +
                "?sig=$signature" +
                "&token=$encodedToken" +
                "&allow_source=true" +
                "&allow_audio_only=true" +
                "&fast_bread=false" +
                "&p=$random"
    }
}