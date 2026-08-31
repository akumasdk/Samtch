package com.akumasdk.samtch.data.api.gql

import android.util.Log
import com.akumasdk.samtch.data.model.*
import org.json.JSONObject

object TwitchGqlMapper {

    fun mapStreamMetadata(body: String): TwitchStreamMetadata? {
        return try {
            val json = JSONObject(body)
            if (json.has("errors")) {
                val errors = json.optJSONArray("errors")
                Log.e("TwitchGqlMapper", "GQL Response has errors: $errors")
            }
            
            val data = json.optJSONObject("data")
            if (data == null) {
                Log.w("TwitchGqlMapper", "GQL Response has no 'data' object. Full body: $body")
                return null
            }
            
            val userJson = data.optJSONObject("user")
            if (userJson == null) {
                Log.w("TwitchGqlMapper", "GQL user object is null. Data keys: ${data.keys().asSequence().toList()}")
                return null
            }

            val channelLogin = userJson.optString("login")
            val rolesJson = userJson.optJSONObject("roles")
            val streamJson = userJson.optJSONObject("stream")
            
            val stream = if (streamJson != null) {
                val gameJson = streamJson.optJSONObject("game")
                TwitchStream(
                    id = streamJson.optString("id").trim(),
                    title = streamJson.optString("title").trim(),
                    type = streamJson.optString("type").trim(),
                    viewersCount = streamJson.optInt("viewersCount"),
                    previewImageUrl = streamJson.optString("previewImageURL").takeIf { it.isNotEmpty() }
                        ?: streamJson.optString("previewImageUrl").trim().takeIf { it.isNotEmpty() },
                    createdAt = streamJson.optString("createdAt").trim(),
                    game = gameJson?.let { g -> TwitchGame(g.optString("name").trim()) }
                )
            } else null

            val profileImageUrl = userJson.optString("profileImageURL").takeIf { it.isNotBlank() }
                ?: userJson.optString("profileImageUrl").takeIf { it.isNotBlank() }

            val user = TwitchUser(
                id = userJson.optString("id").trim(),
                login = userJson.optString("login").trim(),
                displayName = userJson.optString("displayName").trim().takeIf { it.isNotEmpty() } ?: channelLogin,
                description = userJson.optString("description").trim(),
                profileImageUrl = profileImageUrl,
                createdAt = userJson.optString("createdAt").trim(),
                roles = rolesJson?.let { TwitchRoles(it.optBoolean("isPartner")) },
                stream = stream
            )

            Log.d("TwitchGqlMapper", "Successfully mapped metadata for ${user.displayName} (${user.login}). Live: ${stream != null}")
            TwitchStreamMetadata(user)
        } catch (e: Exception) {
            Log.e("TwitchGqlMapper", "Mapping exception while parsing: $body", e)
            null
        }
    }

    fun mapPlaybackAccessToken(body: String): Pair<String, String>? {
        return try {
            val json = JSONObject(body)
            val data = json.optJSONObject("data")
            val streamPlaybackAccessToken = data?.optJSONObject("streamPlaybackAccessToken")

            val token = streamPlaybackAccessToken?.optString("value")
            val signature = streamPlaybackAccessToken?.optString("signature")

            if (token.isNullOrBlank() || signature.isNullOrBlank()) null
            else Pair(token, signature)
        } catch (_: Exception) {
            null
        }
    }
}
