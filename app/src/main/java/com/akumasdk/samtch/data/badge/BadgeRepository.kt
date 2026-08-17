package com.akumasdk.samtch.data.badge

import android.util.Log
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.api.helix.dto.BadgeSetDto
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object BadgeRepository {
    private const val TAG = "BadgeRepository"

    private val _globalState = MutableStateFlow(GlobalBadgeState())
    val globalState = _globalState.asStateFlow()

    private val _channelStates = ConcurrentHashMap<String, MutableStateFlow<ChannelBadgeState>>()

    fun getChannelState(channelName: String) = _channelStates.getOrPut(channelName.lowercase()) {
        MutableStateFlow(ChannelBadgeState())
    }.asStateFlow()

    suspend fun loadGlobalBadges(context: android.content.Context) = withContext(Dispatchers.IO) {
        val auth = TwitchAuthManager.getAuthState(context)
        if (_globalState.value.isLoaded && _globalState.value.loadedWithAuth == auth.isLoggedIn) return@withContext

        try {
            Log.d(TAG, "Loading global badges. isLoggedIn=${auth.isLoggedIn}")
            val globalBadges = if (auth.isLoggedIn) {
                val badgeResult = HelixApiClient.getGlobalBadges(context)
                val badgeSets = badgeResult.getOrDefault(emptyList())
                Log.d(TAG, "Global badge fetch result size: ${badgeSets.size}")
                mapHelixBadges(badgeSets)
            } else {
                emptyMap()
            }

            _globalState.update { it.copy(
                badges = globalBadges,
                isLoaded = true,
                loadedWithAuth = auth.isLoggedIn
            ) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading global badges", e)
        }
    }

    suspend fun loadChannelBadges(context: android.content.Context, channelName: String, userId: String) = withContext(Dispatchers.IO) {
        val channelLower = channelName.lowercase()
        val stateFlow = _channelStates.getOrPut(channelLower) { MutableStateFlow(ChannelBadgeState()) }
        val auth = TwitchAuthManager.getAuthState(context)
        
        if (stateFlow.value.isLoaded && stateFlow.value.loadedWithAuth == auth.isLoggedIn) return@withContext

        try {
            Log.d(TAG, "Loading channel badges for $channelLower. isLoggedIn=${auth.isLoggedIn}, userId=$userId")
            val channelBadges = if (auth.isLoggedIn) {
                val badgeResult = HelixApiClient.getChannelBadges(context, userId)
                val badgeSets = badgeResult.getOrDefault(emptyList())
                Log.d(TAG, "Channel badge fetch result size for $channelLower: ${badgeSets.size}")
                mapHelixBadges(badgeSets)
            } else {
                emptyMap()
            }

            stateFlow.update { it.copy(
                badges = channelBadges,
                isLoaded = true,
                loadedWithAuth = auth.isLoggedIn
            ) }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading channel badges for $channelLower", e)
        }
    }

    private fun mapHelixBadges(badgeSets: List<BadgeSetDto>): Map<String, Map<String, TwitchBadgeDto>> {
        val result = mutableMapOf<String, MutableMap<String, TwitchBadgeDto>>()
        badgeSets.forEach { setDto ->
            val versions = mutableMapOf<String, TwitchBadgeDto>()
            setDto.versions.forEach { badgeDto ->
                versions[badgeDto.id] = TwitchBadgeDto(
                    setID = setDto.id,
                    version = badgeDto.id,
                    title = badgeDto.title,
                    image1x = badgeDto.imageUrlLow,
                    image2x = badgeDto.imageUrlMedium,
                    image4x = badgeDto.imageUrlHigh
                )
            }
            if (versions.isNotEmpty()) {
                result[setDto.id] = versions
            }
        }
        return result
    }

    fun getBadge(channelName: String, setId: String, version: String): TwitchBadgeDto? {
        val channelLower = channelName.lowercase()
        val channelState = _channelStates[channelLower]?.value
        val globalState = _globalState.value

        return channelState?.displayBadges?.get(setId)?.takeIf { it.version == version }
            ?: channelState?.badges?.get(setId)?.get(version)
            ?: globalState.badges[setId]?.get(version)
    }

    fun getBadgeUrl(channelName: String, setId: String, version: String): String? {
        val channelLower = channelName.lowercase()
        val channelState = _channelStates[channelLower]?.value
        val globalState = _globalState.value

        if (globalState.badges.isEmpty() && (channelState?.badges?.isEmpty() != false)) {
            return null
        }

        val badge = getBadge(channelLower, setId, version)
            
        if (badge == null) {
            Log.d(TAG, "Badge not found: $setId/$version in $channelLower")
            return null
        }

        val url = badge.bestUrl ?: return null
                 
        return if (url.startsWith("http") || url.startsWith("//")) {
            if (url.startsWith("//")) "https:$url" else url
        } else {
            "https://$url"
        }
    }
}
