package com.akumasdk.samtch.data.auth

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object TwitchAuthManager {
    private const val TAG = "TwitchAuthManager"

    private var cachedAuthState: AuthState? = null
    private var lastFetchTime = 0L
    private const val CACHE_DURATION = 2000L // 2 seconds

    data class AuthState(
        val userName: String? = null,
        val userId: String? = null,
        val authToken: String? = null,
        val clientId: String? = null,
        val isLoggedIn: Boolean = false
    )

    @Synchronized
    fun getAuthState(context: Context): AuthState {
        val now = System.currentTimeMillis()
        if (cachedAuthState != null && (now - lastFetchTime) < CACHE_DURATION) {
            return cachedAuthState!!
        }

        val state = try {
            // 1. Try OAuth from DataStore first
            val oauthToken = runBlocking { SettingsManager.getAuthToken(context).first() }
            val oauthClientId = runBlocking { SettingsManager.getAuthClientId(context).first() }
            val oauthUserName = runBlocking { SettingsManager.getAuthUserName(context).first() }
            val oauthUserId = runBlocking { SettingsManager.getAuthUserId(context).first() }
            val oauthLoggedIn = runBlocking { SettingsManager.isLoggedIn(context).first() }

            if (oauthLoggedIn && !oauthToken.isNullOrEmpty()) {
                AuthState(oauthUserName, oauthUserId, oauthToken, oauthClientId, true)
            } else {
                // 2. Passive cookie detection (for UI/Browser identification only)
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(Constants.Twitch.BASE_URL)
                
                if (cookies != null) {
                    val cookieMap = cookies.split(";").associate {
                        val pair = it.trim().split("=")
                        pair[0] to pair.getOrNull(1)
                    }

                    val userName = cookieMap["login"]?.lowercase()
                    val authToken = cookieMap["auth-token"]

                    AuthState(userName, null, authToken, Constants.Twitch.CLIENT_ID, false)
                } else {
                    AuthState()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth state", e)
            AuthState()
        }

        cachedAuthState = state
        lastFetchTime = now
        return state
    }
}
