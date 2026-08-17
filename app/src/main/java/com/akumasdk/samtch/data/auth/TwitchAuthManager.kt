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

    data class AuthState(
        val userName: String? = null,
        val userId: String? = null,
        val authToken: String? = null,
        val clientId: String? = null,
        val isLoggedIn: Boolean = false
    )

    fun getAuthState(context: Context): AuthState {
        return try {
            // 1. Try OAuth from DataStore first
            val oauthToken = runBlocking { SettingsManager.getAuthToken(context).first() }
            val oauthClientId = runBlocking { SettingsManager.getAuthClientId(context).first() }
            val oauthUserName = runBlocking { SettingsManager.getAuthUserName(context).first() }
            val oauthUserId = runBlocking { SettingsManager.getAuthUserId(context).first() }
            val oauthLoggedIn = runBlocking { SettingsManager.isLoggedIn(context).first() }

            if (oauthLoggedIn && !oauthToken.isNullOrEmpty()) {
                return AuthState(oauthUserName, oauthUserId, oauthToken, oauthClientId, true)
            }

            // 2. Passive cookie detection (for UI/Browser identification only)
            // Note: We return isLoggedIn = false here because Helix API calls 
            // require the formal OAuth flow implemented above.
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(Constants.Twitch.BASE_URL) ?: return AuthState()

            val cookieMap = cookies.split(";").associate {
                val pair = it.trim().split("=")
                pair[0] to pair.getOrNull(1)
            }

            val userName = cookieMap["login"]?.lowercase()
            val authToken = cookieMap["auth-token"]

            if (!userName.isNullOrEmpty()) {
                Log.d(TAG, "Detected user via cookies: $userName (Anonymous mode for APIs)")
            }

            return AuthState(userName, null, authToken, Constants.Twitch.CLIENT_ID, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth state", e)
            AuthState()
        }
    }
}
