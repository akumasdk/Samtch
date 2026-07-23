package com.akumasdk.samtch.data.mapper

import com.akumasdk.samtch.data.model.*
import org.json.JSONObject

object TwitchGqlMapper {

    fun mapStreamMetadata(body: String): TwitchStreamMetadata? {
        return try {
            val json = JSONObject(body)
            val data = json.optJSONObject("data")
            val userJson = data?.optJSONObject("user") ?: return null

            val rolesJson = userJson.optJSONObject("roles")
            val streamJson = userJson.optJSONObject("stream")
            val gameJson = streamJson?.optJSONObject("game")

            val user = TwitchUser(
                id = userJson.getString("id").trim(),
                login = userJson.getString("login").trim(),
                displayName = userJson.getString("displayName").trim(),
                description = userJson.optString("description").trim(),
                profileImageUrl = userJson.optString("profileImageURL").trim(),
                createdAt = userJson.optString("createdAt").trim(),
                roles = rolesJson?.let { TwitchRoles(it.getBoolean("isPartner")) },
                stream = streamJson?.let {
                    TwitchStream(
                        id = it.getString("id").trim(),
                        title = it.getString("title").trim(),
                        type = it.optString("type").trim(),
                        viewersCount = it.getInt("viewersCount"),
                        previewImageUrl = it.optString("previewImageURL").trim(),
                        createdAt = it.optString("createdAt").trim(),
                        game = gameJson?.let { g -> TwitchGame(g.getString("name").trim()) }
                    )
                }
            )

            TwitchStreamMetadata(user)
        } catch (_: Exception) {
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
