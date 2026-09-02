package com.akumasdk.samtch.data.api.gql

import android.util.Log
import com.akumasdk.samtch.data.auth.TwitchAuthManager
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwitchGqlService @Inject constructor(
    private val client: OkHttpClient,
    private val authManager: TwitchAuthManager
) {
    companion object {
        private const val TAG = "TwitchGqlService"

        private const val USER_EMOTES_QUERY = """
            query UserEmotes {
              currentUser {
                emoteSets {
                  id
                  emotes {
                    id
                    token
                  }
                }
              }
            }
        """

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
                  previewImageURL(height: 360, width: 640)
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
    }

    @Volatile
    private var cachedIntegrityToken: String? = null

    @Volatile
    private var cachedDynamicClientId: String? = null

    private val clientIdMutex = Mutex()

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
                        val regex = """clientId\s*=\s*["']([^"']+)["']|"Client-ID"\s*:\s*["']([^"']+)["']""".toRegex()
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

    suspend fun getStreamMetadata(channelName: String): TwitchStreamMetadata? = withContext(Dispatchers.IO) {
        try {
            val clientId = getDynamicClientId()
            val integrityToken = cachedIntegrityToken ?: fetchIntegrityToken(clientId)
            
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
            
            if (!integrityToken.isNullOrBlank()) {
                requestBuilder.header("Client-Integrity", integrityToken)
            }

            val response = client.newCall(requestBuilder.build()).execute()
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
            val integrityToken = cachedIntegrityToken ?: fetchIntegrityToken(clientId)

            val fetchId: suspend (String?) -> String? = { token ->
                val payload = JSONObject().apply {
                    put("operationName", "GetUserId")
                    put("query", GET_USER_ID_QUERY.trimIndent())
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

                val response = client.newCall(requestBuilder.build()).execute()
                val body = response.body.string()
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    json.optJSONObject("data")?.optJSONObject("user")?.optString("id")?.takeIf { it.isNotEmpty() }
                } else null
            }

            var id = fetchId(integrityToken)
            if (id == null && integrityToken != null) {
                id = fetchId(null)
            }
            id
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user ID for $channelName", e)
            null
        }
    }

    private val deviceId: String = UUID.randomUUID().toString().replace("-", "")

    private suspend fun Request.Builder.addCommonHeaders(clientId: String): Request.Builder {
        val auth = authManager.getAuthState()
        val builder = this
            .header("Client-Id", clientId)
            .header("X-Device-Id", deviceId)
            .header("User-Agent", Constants.UserAgents.DESKTOP)
            .header("Origin", Constants.Twitch.BASE_URL)
            .header("Referer", "${Constants.Twitch.BASE_URL}/")
            .header("Accept", "application/json")
        
        if (auth.isLoggedIn && !auth.authToken.isNullOrEmpty()) {
            builder.header("Authorization", "OAuth ${auth.authToken}")
        }
        
        return builder
    }

    private suspend fun fetchIntegrityToken(clientId: String): String? = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url(Constants.Twitch.Api.INTEGRITY)
                .post("{}".toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)
            
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body.string()

            if (!response.isSuccessful) return@withContext null

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

    suspend fun getPlaybackAccessToken(channelName: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val clientId = getDynamicClientId()

        val firstIntegrity = cachedIntegrityToken ?: fetchIntegrityToken(clientId)
        val first = getPlaybackAccessTokenOnce(channelName, clientId, firstIntegrity)
        if (first != null) return@withContext first

        cachedIntegrityToken = null
        val secondIntegrity = fetchIntegrityToken(clientId)
        return@withContext getPlaybackAccessTokenOnce(channelName, clientId, secondIntegrity)
    }

    private suspend fun getPlaybackAccessTokenOnce(channelName: String, clientId: String, integrityToken: String?): Pair<String, String>? {
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

    suspend fun getUserEmotes(): List<com.akumasdk.samtch.data.emote.Emote> = withContext(Dispatchers.IO) {
        try {
            val clientId = getDynamicClientId()
            val auth = authManager.getAuthState()
            if (!auth.isLoggedIn) return@withContext emptyList()

            val payload = JSONObject().apply {
                put("operationName", "UserEmotes")
                put("query", USER_EMOTES_QUERY.trimIndent())
            }

            val requestBuilder = Request.Builder()
                .url(Constants.Twitch.Api.GQL)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .addCommonHeaders(clientId)

            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body.string()

            if (!response.isSuccessful) return@withContext emptyList()

            val json = JSONObject(body)
            val emoteSets = json.optJSONObject("data")
                ?.optJSONObject("currentUser")
                ?.optJSONArray("emoteSets") ?: return@withContext emptyList()

            val result = mutableListOf<com.akumasdk.samtch.data.emote.Emote>()
            for (i in 0 until emoteSets.length()) {
                val set = emoteSets.getJSONObject(i)
                val emotes = set.optJSONArray("emotes") ?: continue
                for (j in 0 until emotes.length()) {
                    val e = emotes.getJSONObject(j)
                    val id = e.getString("id")
                    val token = e.getString("token")
                    result.add(com.akumasdk.samtch.data.emote.Emote(
                        id = id,
                        code = token,
                        url = "https://static-cdn.jtvnw.net/emoticons/v2/$id/static/dark/3.0",
                        type = com.akumasdk.samtch.data.emote.EmoteType.TWITCH
                    ))
                }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching GQL user emotes", e)
            emptyList()
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
