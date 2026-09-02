package com.akumasdk.samtch.data.badge

import android.util.Log
import com.akumasdk.samtch.data.api.helix.HelixApiClient
import com.akumasdk.samtch.data.auth.TwitchAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BadgeRepository @Inject constructor(
    private val helixApiClient: HelixApiClient,
    private val authManager: TwitchAuthManager
) {
    companion object {
        private const val TAG = "BadgeRepository"
    }

    private val _globalState = MutableStateFlow(GlobalBadgeState())
    val globalState = _globalState.asStateFlow()

    private val _channelStates = ConcurrentHashMap<String, MutableStateFlow<ChannelBadgeState>>()

    fun getChannelState(channelName: String) = _channelStates.getOrPut(channelName.lowercase()) {
        MutableStateFlow(ChannelBadgeState())
    }.asStateFlow()

    suspend fun loadGlobalBadges(force: Boolean = false) = withContext(Dispatchers.IO) {
        val auth = authManager.authStateFlow.first()
        if (!force && _globalState.value.isLoaded && _globalState.value.loadedWithAuth == auth.isLoggedIn) return@withContext
        
        Log.d(TAG, "Fetching global badges...")
        helixApiClient.getGlobalBadges().onSuccess { badgeSets ->
            val badgeMap = badgeSets.associate { set ->
                set.id to set.versions.associate { v ->
                    v.id to TwitchBadgeDto(
                        setID = set.id,
                        version = v.id,
                        title = v.title,
                        image1x = v.imageUrlLow,
                        image2x = v.imageUrlMedium,
                        image4x = v.imageUrlHigh
                    )
                }
            }
            _globalState.update { it.copy(badges = badgeMap, isLoaded = true, loadedWithAuth = auth.isLoggedIn) }
            Log.d(TAG, "Global badges loaded: ${badgeMap.size} sets")
        }.onFailure { 
            Log.e(TAG, "Failed to load global badges", it)
            _globalState.update { it.copy(isLoaded = true, loadedWithAuth = auth.isLoggedIn) }
        }
    }

    suspend fun loadChannelBadges(channelName: String, broadcasterId: String, force: Boolean = false) = withContext(Dispatchers.IO) {
        val channelLower = channelName.lowercase()
        val stateFlow = _channelStates.getOrPut(channelLower) { MutableStateFlow(ChannelBadgeState()) }
        val auth = authManager.authStateFlow.first()
        
        if (!force && stateFlow.value.isLoaded && stateFlow.value.loadedWithAuth == auth.isLoggedIn) return@withContext

        Log.d(TAG, "Fetching channel badges for $channelName...")
        helixApiClient.getChannelBadges(broadcasterId).onSuccess { badgeSets ->
            val badgeMap = badgeSets.associate { set ->
                set.id to set.versions.associate { v ->
                    v.id to TwitchBadgeDto(
                        setID = set.id,
                        version = v.id,
                        title = v.title,
                        image1x = v.imageUrlLow,
                        image2x = v.imageUrlMedium,
                        image4x = v.imageUrlHigh
                    )
                }
            }
            stateFlow.update { it.copy(badges = badgeMap, isLoaded = true, loadedWithAuth = auth.isLoggedIn) }
            Log.d(TAG, "Channel badges loaded for $channelName: ${badgeMap.size} sets")
        }.onFailure { 
            Log.e(TAG, "Failed to load channel badges for $channelName", it)
            stateFlow.update { it.copy(isLoaded = true, loadedWithAuth = auth.isLoggedIn) }
        }
    }

    fun getBadge(channelName: String, setId: String, versionId: String): TwitchBadgeDto? {
        val channelBadges = _channelStates[channelName.lowercase()]?.value?.badges
        val globalBadges = _globalState.value.badges
        
        return channelBadges?.get(setId)?.get(versionId) ?: globalBadges[setId]?.get(versionId)
    }

    fun clearCache() {
        Log.d(TAG, "Clearing badge cache")
        _globalState.update { GlobalBadgeState() }
        _channelStates.values.forEach { it.update { ChannelBadgeState() } }
    }
}
