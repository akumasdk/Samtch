package com.akumasdk.samtch.util

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.akumasdk.samtch.data.settings.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.net.URI

object TwitchUrlUtil {
    fun ensureMobileUrl(url: String): String {
        val uri = try { URI(url) } catch (_: Exception) { return url }
        if (uri.host == "www.twitch.tv" || uri.host == Constants.Twitch.DOMAIN) {
            return url.replaceFirst(uri.host ?: "", "m.twitch.tv")
        }
        return url
    }

    fun isSafeExplorationUrl(url: String?): Boolean {
        if (url.isNullOrEmpty() || url.contains(Constants.ABOUT_BLANK)) return true
        
        val uri = try { URI(url) } catch (_: Exception) { return false }
        if (uri.host != null && !uri.host.contains(Constants.Twitch.DOMAIN)) return true

        val path = uri.path ?: "/"
        val segments = path.split("/").filter { it.isNotEmpty() }

        // Root/Home cases
        if (segments.isEmpty() || path == "/" || path == "/home" || path == "/home/") return true
        
        // Exploration zones
        val safeRoots = setOf(
            "directory", "search", "following", "browse", "p", 
            "settings", "inventory", "wallet", "drops", "turbo", 
            "friends", "activity", "bits", "about", "jobs", "security"
        )
        
        if (safeRoots.contains(segments[0].lowercase())) return true
        
        // Everything else (single segment usernames or /username/home) is "Unsafe"
        return false
    }

    fun isPlayableChannel(channelMatch: String?, currentUser: String?): Boolean {
        if (channelMatch == null) return false
        if (currentUser == null) return true // If not logged in, all channels are playable
        
        // The current user should never trigger the player
        return !channelMatch.equals(currentUser, ignoreCase = true)
    }

    fun extractChannelFromUrl(url: String?): String? {
        val uri = try {
            if (url == null) return null
            val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
            URI(cleanUrl)
        } catch (_: Exception) {
            return null
        }

        if (uri.host != null && !uri.host.contains(Constants.Twitch.DOMAIN)) return null

        val path = uri.path ?: return null
        val segments = path.split("/").filter { it.isNotEmpty() }

        if (segments.isEmpty()) return null

        val channelCandidate = segments[0].trim()

        val excludedNames = setOf(
            "directory", "search", "videos", "clips", "events",
            "esports", "music", "about", "jobs", "security",
            "p", "settings", "subscriptions", "inventory", "wallet",
            "drops", "turbo", "friends", "popout", "embed", "home",
            "activity", "bits", "browse", "following"
        )

        if (excludedNames.any { it.equals(channelCandidate, ignoreCase = true) }) {
            return null
        }

        return channelCandidate
    }

    fun getCurrentUser(context: Context): String? {
        return try {
            // 1. Try OAuth from DataStore first
            val oauthUserName = runBlocking { SettingsManager.getAuthUserName(context).first() }
            if (!oauthUserName.isNullOrEmpty()) {
                return oauthUserName
            }

            // 2. Fallback to cookies
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie(Constants.Twitch.BASE_URL) ?: return null

            // The login cookie contains the username: login=username;
            val loginCookie = cookies.split(";").find { it.trim().startsWith("login=") }
            val username = loginCookie?.split("=")?.getOrNull(1)?.trim()?.lowercase()
            
            if (!username.isNullOrEmpty()) {
                Log.d("TwitchUrlUtil", "Detected logged-in user from cookies: $username")
                username
            } else null
        } catch (e: Exception) {
            Log.e("TwitchUrlUtil", "Error getting user", e)
            null
        }
    }

    fun isGlobalHome(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val uri = try { URI(url) } catch (_: Exception) { return false }
        val path = uri.path ?: "/"
        return path == "/home" || path == "/home/"
    }

    fun isBrowserRoot(url: String?): Boolean {
        if (url.isNullOrEmpty()) return true
        val uri = try { URI(url) } catch (_: Exception) { return false }
        val path = uri.path ?: ""
        return path == "/" || path == "" || path == "/home" || path == "/home/"
    }
}
