package com.akumasdk.samtch.data.auth

import android.util.Log
import android.webkit.CookieManager
import com.akumasdk.samtch.data.settings.SettingsManager
import com.akumasdk.samtch.util.Constants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TwitchAuthManager @Inject constructor(
    private val settingsManager: SettingsManager
) {
    companion object {
        private const val TAG = "TwitchAuthManager"
    }

    data class AuthState(
        val userName: String? = null,
        val userId: String? = null,
        val authToken: String? = null,
        val clientId: String? = null,
        val isLoggedIn: Boolean = false
    )

    val authStateFlow: Flow<AuthState> = combine(
        settingsManager.getAuthToken(),
        settingsManager.getAuthClientId(),
        settingsManager.getAuthUserName(),
        settingsManager.getAuthUserId(),
        settingsManager.isLoggedIn()
    ) { token, clientId, userName, userId, isLoggedIn ->
        if (isLoggedIn && !token.isNullOrEmpty()) {
            AuthState(userName, userId, token, clientId, true)
        } else {
            // Passive cookie detection (for UI/Browser identification only)
            detectFromCookies()
        }
    }

    suspend fun getAuthState(): AuthState = authStateFlow.first()

    private fun detectFromCookies(): AuthState {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(Constants.Twitch.BASE_URL) ?: return AuthState()

            val cookieMap = cookies.split(";").associate {
                val pair = it.trim().split("=")
                pair[0] to pair.getOrNull(1)
            }

            val userName = cookieMap["login"]?.lowercase()
            val authToken = cookieMap["auth-token"]

            AuthState(userName, null, authToken, Constants.Twitch.CLIENT_ID, false)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting from cookies", e)
            AuthState()
        }
    }
}
