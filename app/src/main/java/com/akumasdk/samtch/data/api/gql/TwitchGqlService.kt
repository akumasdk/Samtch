package com.akumasdk.samtch.data.api.gql

import android.util.Log
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * Required for direct HLS playback.
 * 
 * Note: Non-HLS functions (Badges, User ID, Stream Metadata) have been migrated to Helix.
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

    private val clientIdMutex = Mutex()

    /**
     * Dynamically scrapes the Twitch Client ID from the homepage.
     */
    suspend fun getDynamicClientId(): String {
        cachedDynamicClientId?.let { return it }

        return clientIdMutex.withLock {
            cachedDynamicClientId?.let { return@withLock it }

            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(Constants.Twitch.BASE_URL)
                        .header("User-Agent", Constants.UserAgents.DESKTOP)
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body.string()

                    if (response.isSuccessful && body.isNotEmpty()) {
                        val regex =
                            """clientId\s*=\s*["']([^"']+)["']|"Client-ID"\s*:\s*["']([^"']+)["']""".toRegex()
                        val match = regex.find(body)
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

                Constants.Twitch.CLIENT_ID
            }
        }
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
                    .url(Constants.Twitch.Api.GQL)
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .addCommonHeaders(clientId)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body.string()

                if (!response.isSuccessful) return@withContext null

                TwitchGqlMapper.mapStreamMetadata(body)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stream metadata", e)
                null
            }
        }

    suspend fun getUserId(channelName: String): String? = withContext(Dispatchers.IO) {
        try {
            val clientId = getDynamicClientId()
            val payload = JSONObject().apply {
                put("operationName", "GetUserId")
                put("query", GET_USER_ID_QUERY.trimIndent())
                put("variables", JSONObject().apply {
                    put("login", channelName.lowercase())
                })
            }

            val request = Request.Builder()
                .url(Constants.Twitch.Api.GQL)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body.string()
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(body)
            json.optJSONObject("data")?.optJSONObject("user")?.optString("id")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user ID", e)
            null
        }
    }

    // Stable per app-process run
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
              previewImageURL(height: 480, width: 853)
              createdAt
              game {
                name
              }
            }
          }
        }
    """

    private const val GET_USER_ID_QUERY = """
        query GetUserId(${"$"}login: String!) {
          user(login: ${"$"}login) {
            id
          }
        }
    """

    private fun Request.Builder.addCommonHeaders(clientId: String): Request.Builder {
        return this
            .header("Client-Id", clientId)
            .header("X-Device-Id", deviceId)
            .header("User-Agent", Constants.UserAgents.DESKTOP)
            .header("Origin", Constants.Twitch.BASE_URL)
            .header("Referer", "${Constants.Twitch.BASE_URL}/")
            .header("Accept", "application/json")
    }

    /**
     * Fetches a new Integrity Token from Twitch.
     */
    private suspend fun fetchIntegrityToken(clientId: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(Constants.Twitch.Api.INTEGRITY)
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .addCommonHeaders(clientId)
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body.string()

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
                .url(Constants.Twitch.Api.GQL)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)

            if (!integrityToken.isNullOrBlank()) {
                requestBuilder.header("Client-Integrity", integrityToken)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body.string()

            if (!response.isSuccessful) return null

            TwitchGqlMapper.mapPlaybackAccessToken(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Playback token request exception", e)
            null
        }
    }

    fun buildHlsUrl(channelName: String, token: String, signature: String): String {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val random = (Math.random() * 999999).toInt()

        return "${Constants.Twitch.Api.HLS_BASE}${channelName.lowercase()}.m3u8" +
                "?sig=$signature" +
                "&token=$encodedToken" +
                "&allow_source=true" +
                "&allow_audio_only=true" +
                "&fast_bread=false" +
                "&p=$random"
    }
}
