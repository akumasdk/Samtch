package com.akumasdk.samtch.data.api.gql

import android.util.Log
import com.akumasdk.samtch.data.model.TwitchStreamMetadata
import com.akumasdk.samtch.util.Constants
import com.akumasdk.samtch.util.NetworkUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

/**
 * Service for fetching Twitch stream access tokens via GraphQL
 * Required for direct HLS playback.
 * 
 * Note: Non-HLS functions (Badges, User ID, Stream Metadata) have been migrated to Helix.
 */
object TwitchGqlService {

    private const val TAG = "TwitchGqlService"

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
                suspend fun tryFetch(useRelaxed: Boolean): String? {
                    try {
                        val request = Request.Builder()
                            .url(Constants.Twitch.BASE_URL)
                            .header("User-Agent", Constants.UserAgents.DESKTOP)
                            .build()

                        val response = NetworkUtil.getClient(useRelaxed).newCall(request).execute()
                        val body = response.body.string()

                        if (response.isSuccessful && body.isNotEmpty()) {
                            val regex =
                                """clientId\s*=\s*["']([^"']+)["']|"Client-ID"\s*:\s*["']([^"']+)["']""".toRegex()
                            val match = regex.find(body)
                            val scrapedId = match?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                                ?: match?.groupValues?.get(2)

                            if (!scrapedId.isNullOrBlank()) {
                                Log.d(TAG, "Successfully scraped dynamic Client-ID: $scrapedId (Relaxed: $useRelaxed)")
                                cachedDynamicClientId = scrapedId
                                return scrapedId
                            }
                        }
                    } catch (e: Exception) {
                        val isSslError = e is SSLHandshakeException || e.message?.contains("Handshake", ignoreCase = true) == true || e.cause is SSLHandshakeException
                        if (isSslError && !useRelaxed) {
                            Log.w(TAG, "SSL Handshake failed for Client-ID, retrying with relaxed client...")
                            return tryFetch(true)
                        }
                        Log.e(TAG, "Exception while scraping dynamic Client-ID (Relaxed: $useRelaxed)", e)
                    }
                    return null
                }

                tryFetch(false) ?: Constants.Twitch.CLIENT_ID
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
                
                suspend fun fetchMetadata(token: String?, useRelaxed: Boolean): TwitchStreamMetadata? {
                    return try {
                        val payload = JSONObject().apply {
                            put("operationName", "StreamMetadata")
                            put("query", STREAM_METADATA_QUERY.trimIndent())
                            put("variables", JSONObject().apply {
                                put("login", channelName.lowercase())
                            })
                        }

                        val requestBuilder = Request.Builder()
                            .url(Constants.Twitch.Api.GQL)
                            .post(payload.toString().toRequestBody("application/json".toMediaType()))
                            .addCommonHeaders(clientId)

                        if (!token.isNullOrBlank()) {
                            requestBuilder.header("Client-Integrity", token)
                        }

                        val response = NetworkUtil.getClient(useRelaxed).newCall(requestBuilder.build()).execute()
                        val body = response.body.string()

                        if (response.isSuccessful) {
                            TwitchGqlMapper.mapStreamMetadata(body)
                        } else {
                            Log.e(TAG, "GQL Error for $channelName: ${response.code} body=$body")
                            null
                        }
                    } catch (e: Exception) {
                        val isSslError = e is SSLHandshakeException || e.message?.contains("Handshake", ignoreCase = true) == true || e.cause is SSLHandshakeException
                        if (isSslError && !useRelaxed) {
                            Log.w(TAG, "SSL Handshake failed for StreamMetadata, retrying with relaxed client...")
                            return fetchMetadata(token, true)
                        }
                        Log.e(TAG, "Exception in fetchMetadata for $channelName (Relaxed: $useRelaxed)", e)
                        null
                    }
                }

                // 1. Try without token
                Log.d(TAG, "Fetching metadata for $channelName (Attempt 1: No Token)")
                var metadata = fetchMetadata(null, false)
                
                // 2. Try with token if failed
                if (metadata == null) {
                    val token = cachedIntegrityToken ?: fetchIntegrityToken(clientId)
                    Log.d(TAG, "Fetching metadata for $channelName (Attempt 2: Token present: ${token != null})")
                    metadata = fetchMetadata(token, false)
                }
                
                metadata
            } catch (e: Exception) {
                Log.e(TAG, "Exception in getStreamMetadata for $channelName", e)
                null
            }
        }

    suspend fun getUserId(channelName: String): String? = withContext(Dispatchers.IO) {
        try {
            val clientId = getDynamicClientId()
            val integrityToken = cachedIntegrityToken ?: fetchIntegrityToken(clientId)

            suspend fun fetchId(token: String?, useRelaxed: Boolean): String? {
                return try {
                    val payload = JSONObject().apply {
                        put("operationName", "GetUserId")
                        val variables = JSONObject().apply {
                            put("login", channelName.lowercase())
                        }
                        val extensions = JSONObject().apply {
                            put("persistedQuery", JSONObject().apply {
                                put("version", 1)
                                put("sha256Hash", "e1ed0c80679e44906f47c09a9f4a39039e31d49110196715396d3bb2d48997fd")
                            })
                        }
                        put("variables", variables)
                        put("extensions", extensions)
                    }

                    val requestBuilder = Request.Builder()
                        .url(Constants.Twitch.Api.GQL)
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .addCommonHeaders(clientId)

                    if (!token.isNullOrBlank()) {
                        requestBuilder.header("Client-Integrity", token)
                    }

                    val response = NetworkUtil.getClient(useRelaxed).newCall(requestBuilder.build()).execute()
                    val body = response.body.string()
                    if (response.isSuccessful) {
                        val json = JSONObject(body)
                        json.optJSONObject("data")?.optJSONObject("user")?.optString("id")?.takeIf { it.isNotEmpty() }
                    } else {
                        Log.e(TAG, "GQL GetUserId Error for $channelName: ${response.code} body=$body")
                        null
                    }
                } catch (e: Exception) {
                    val isSslError = e is SSLHandshakeException || e.message?.contains("Handshake", ignoreCase = true) == true || e.cause is SSLHandshakeException
                    if (isSslError && !useRelaxed) {
                        Log.w(TAG, "SSL Handshake failed for GetUserId, retrying with relaxed client...")
                        return fetchId(token, true)
                    }
                    Log.e(TAG, "Error fetching user ID for $channelName (Relaxed: $useRelaxed)", e)
                    null
                }
            }

            var id = fetchId(integrityToken, false)
            if (id == null) {
                delay(500)
                id = fetchId(null, false)
            }
            
            if (id == null) {
                Log.w(TAG, "GQL getUserId failed for $channelName after all attempts")
            }
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user ID for $channelName", e)
            null
        }
    }

    // Stable per app-process run
    private val deviceId: String = UUID.randomUUID().toString().replace("-", "")

    private const val STREAM_METADATA_QUERY = """
        query StreamMetadata(${"$"}login: String!) {
          user(login: ${"$"}login) {
            id
            login
            displayName
            description
            profileImageURL(width: 300)
            stream {
              id
              title
              type
              viewersCount
              createdAt
              previewImageURL(width: 1280, height: 720)
              game {
                id
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
            .header("User-Agent", Constants.UserAgents.DESKTOP)
            .header("Origin", Constants.Twitch.BASE_URL)
            .header("Referer", "${Constants.Twitch.BASE_URL}/")
            .header("Accept", "application/json")
    }

    /**
     * Fetches a new Integrity Token from Twitch.
     */
    private suspend fun fetchIntegrityToken(clientId: String, useRelaxed: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(Constants.Twitch.Api.INTEGRITY)
                    .post("{}".toRequestBody("application/json".toMediaType()))
                    .addCommonHeaders(clientId)
                    .build()

                val response = NetworkUtil.getClient(useRelaxed).newCall(request).execute()
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
                val isSslError = e is SSLHandshakeException || e.message?.contains("Handshake", ignoreCase = true) == true || e.cause is SSLHandshakeException
                if (isSslError && !useRelaxed) {
                    Log.w(TAG, "SSL Handshake failed for integrity token, retrying relaxed...")
                    return@withContext fetchIntegrityToken(clientId, true)
                }
                Log.e(TAG, "Failed to fetch integrity token (Relaxed: $useRelaxed)", e)
            }
            null
        }

    /**
     * Fetch playback access token and signature for a channel
     */
    suspend fun getPlaybackAccessToken(
        channelName: String,
        playerType: String = "site"
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val clientId = getDynamicClientId()

        val firstIntegrity = cachedIntegrityToken ?: fetchIntegrityToken(clientId)
        val first = getPlaybackAccessTokenOnce(channelName, clientId, firstIntegrity, playerType)
        if (first != null) return@withContext first

        cachedIntegrityToken = null
        val secondIntegrity = fetchIntegrityToken(clientId)
        return@withContext getPlaybackAccessTokenOnce(channelName, clientId, secondIntegrity, playerType)
    }

    /**
     * Replicates the exact GQL request from adblock solutions including the persisted query hash.
     */
    private fun createPlaybackAccessTokenPayload(channelName: String, playerType: String): String {
        val platform = if (playerType == "autoplay") "android" else "web"

        val variables = JSONObject().apply {
            put("isLive", true)
            put("login", channelName.lowercase())
            put("isVod", false)
            put("vodID", "")
            put("playerType", playerType)
            put("platform", platform)
        }

        val extensions = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", "ed230aa1e33e07eebb8928504583da78a5173989fadfb1ac94be06a04f3cdbe9")
            })
        }

        return JSONObject().apply {
            put("operationName", "PlaybackAccessToken")
            put("variables", variables)
            put("extensions", extensions)
        }.toString()
    }

    private fun getPlaybackAccessTokenOnce(
        channelName: String,
        clientId: String,
        integrityToken: String?,
        playerType: String,
        useRelaxed: Boolean = false
    ): Pair<String, String>? {
        return try {
            val payload = createPlaybackAccessTokenPayload(channelName, playerType)

            val requestBuilder = Request.Builder()
                .url(Constants.Twitch.Api.GQL)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)

            if (!integrityToken.isNullOrBlank()) {
                requestBuilder.header("Client-Integrity", integrityToken)
            }

            val response = NetworkUtil.getClient(useRelaxed).newCall(requestBuilder.build()).execute()
            val responseBody = response.body.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "GQL Error: ${response.code} body=$responseBody")
                return null
            }

            TwitchGqlMapper.mapPlaybackAccessToken(responseBody)
        } catch (e: Exception) {
            val isSslError = e is SSLHandshakeException || e.message?.contains("Handshake", ignoreCase = true) == true || e.cause is SSLHandshakeException
            if (isSslError && !useRelaxed) {
                Log.w(TAG, "SSL Handshake failed for PlaybackToken, retrying relaxed...")
                return getPlaybackAccessTokenOnce(channelName, clientId, integrityToken, playerType, true)
            }
            Log.e(TAG, "Playback token request exception ($playerType, Relaxed: $useRelaxed)", e)
            null
        }
    }

    fun buildHlsUrl(
        channelName: String, 
        token: String, 
        signature: String
    ): String {
        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val random = (Math.random() * 9999999).toInt()
        val playSessionId = UUID.randomUUID().toString().replace("-", "")
        
        // Base64 encoded ACMB header used by Twitch web player for feature flags/telemetry
        // {"AppVersion":"1.0.0","ClientApp":"twilight","URL":"https://www.twitch.tv/channel"}
        val acmbJson = JSONObject().apply {
            put("AppVersion", "8faed90d-c8f5-46a7-9d7c-4934daaba821") // Current Twitch web build hash
            put("ClientApp", "twilight")
            put("URL", "${Constants.Twitch.BASE_URL}/${channelName.lowercase()}")
        }
        val acmb = android.util.Base64.encodeToString(
            acmbJson.toString().toByteArray(), 
            android.util.Base64.NO_WRAP
        )

        return "${Constants.Twitch.Api.HLS_BASE_V2}${channelName.lowercase()}.m3u8" +
                "?sig=$signature" +
                "&token=$encodedToken" +
                "&allow_source=true" +
                "&allow_audio_only=true" +
                "&low_latency=true" +
                "&fast_bread=true" +
                "&acmb=${URLEncoder.encode(acmb, "UTF-8")}" +
                "&browser_family=chrome" +
                "&browser_version=151.0" +
                "&platform=web" +
                "&play_session_id=$playSessionId" +
                "&player_backend=mediaplayer" +
                "&playlist_include_framerate=true" +
                "&reassignments_supported=true" +
                "&supported_codecs=av1,h265,h264" +
                "&transcode_mode=cbr_v1" +
                "&cdm=wv" +
                "&enable_score=true" +
                "&include_unavailable=true" +
                "&p=$random"
    }
}
