package com.akumasdk.samtch.data.auth

import android.util.Log
import android.webkit.CookieManager
import com.akumasdk.samtch.util.Constants

object TwitchAuthManager {
    private const val TAG = "TwitchAuthManager"

    @Volatile
    private var validatedClientId: String? = null

    fun setValidatedClientId(id: String) {
        validatedClientId = id
    }

    data class AuthState(
        val userName: String? = null,
        val authToken: String? = null,
        val clientId: String? = null,
        val isLoggedIn: Boolean = false
    )

    fun getAuthState(): AuthState {
        return try {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(Constants.TWITCH_BASE_URL) ?: return AuthState()

            val cookieMap = cookies.split(";").associate {
                val pair = it.trim().split("=")
                pair[0] to pair.getOrNull(1)
            }

            val userName = cookieMap["login"]?.lowercase()
            val authToken = cookieMap["auth-token"]

            val isLoggedIn = !userName.isNullOrEmpty() && !authToken.isNullOrEmpty()
            
            if (isLoggedIn) {
                Log.d(TAG, "Detected logged-in user: $userName")
            }

            AuthState(userName, authToken, validatedClientId, isLoggedIn)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting auth state from cookies", e)
            AuthState()
        }
    }
}
