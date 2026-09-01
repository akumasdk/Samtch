package com.akumasdk.samtch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.akumasdk.samtch.data.emote.Emote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsManager {
    private val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
    private val AUDIO_ONLY_BACKGROUND_ENABLED = booleanPreferencesKey("audio_background_v2")
    private val AD_BLOCK_MODE = booleanPreferencesKey("ad_block_mode_is_vaft") // true = VAFT, false = VideoSwap
    private val CHAT_MODE = booleanPreferencesKey("chat_mode_is_native") // true = NATIVE, false = LEGACY
    private val MINI_PLAYER_HINT_SHOWN = booleanPreferencesKey("mini_player_hint_shown")
    private val PLAYER_TOOLTIP_SHOW_COUNT = intPreferencesKey("player_tooltip_show_count")
    private val THEME_MODE = intPreferencesKey("theme_mode") // 0 = DARK, 1 = LIGHT, 2 = SYSTEM
    private val LAST_VERSION_CODE = intPreferencesKey("last_version_code")
    private val KEYBOARD_HEIGHT_PORTRAIT = intPreferencesKey("keyboard_height_portrait")
    private val KEYBOARD_HEIGHT_LANDSCAPE = intPreferencesKey("keyboard_height_landscape")
    private val CHAT_FONT_SIZE = intPreferencesKey("chat_font_size")
    private val CHAT_EMOTE_SIZE = intPreferencesKey("chat_emote_size")
    private val CHAT_BADGE_SIZE = intPreferencesKey("chat_badge_size")
    private val IMMERSIVE_BACKGROUND_ENABLED = booleanPreferencesKey("immersive_background_enabled")
    private val FULLSCREEN_CHAT_RATIO = intPreferencesKey("fullscreen_chat_ratio_v2") // 15 to 50

    // Auth
    private val AUTH_TOKEN = stringPreferencesKey("auth_token")
    private val AUTH_CLIENT_ID = stringPreferencesKey("auth_client_id")
    private val AUTH_USER_NAME = stringPreferencesKey("auth_user_name")
    private val AUTH_USER_ID = stringPreferencesKey("auth_user_id")
    private val AUTH_IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")

    enum class AdBlockMode {
        VAFT, VIDEO_SWAP
    }

    enum class ChatMode {
        NATIVE, LEGACY
    }

    enum class ThemeMode {
        DARK, LIGHT, SYSTEM
    }

    fun isPipEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PIP_ENABLED] ?: true
        }
    }

    suspend fun setPipEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PIP_ENABLED] = enabled
        }
    }

    fun isAudioOnlyBackgroundEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUDIO_ONLY_BACKGROUND_ENABLED] ?: false
        }
    }

    suspend fun setAudioOnlyBackgroundEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUDIO_ONLY_BACKGROUND_ENABLED] = enabled
        }
    }

    fun getAdBlockMode(context: Context): Flow<AdBlockMode> {
        return context.dataStore.data.map { preferences ->
            if (preferences[AD_BLOCK_MODE] ?: true) AdBlockMode.VAFT else AdBlockMode.VIDEO_SWAP
        }
    }

    suspend fun setAdBlockMode(context: Context, mode: AdBlockMode) {
        context.dataStore.edit { preferences ->
            preferences[AD_BLOCK_MODE] = mode == AdBlockMode.VAFT
        }
    }

    fun getChatMode(context: Context): Flow<ChatMode> {
        return context.dataStore.data.map { preferences ->
            if (preferences[CHAT_MODE] ?: true) ChatMode.NATIVE else ChatMode.LEGACY
        }
    }

    suspend fun setChatMode(context: Context, mode: ChatMode) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_MODE] = mode == ChatMode.NATIVE
        }
    }

    fun isMiniPlayerHintShown(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[MINI_PLAYER_HINT_SHOWN] ?: false
        }
    }

    suspend fun setMiniPlayerHintShown(context: Context, shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MINI_PLAYER_HINT_SHOWN] = shown
        }
    }

    fun getPlayerTooltipShowCount(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[PLAYER_TOOLTIP_SHOW_COUNT] ?: 0
        }
    }

    suspend fun incrementPlayerTooltipShowCount(context: Context) {
        context.dataStore.edit { preferences ->
            val current = preferences[PLAYER_TOOLTIP_SHOW_COUNT] ?: 0
            preferences[PLAYER_TOOLTIP_SHOW_COUNT] = current + 1
        }
    }

    fun getThemeMode(context: Context): Flow<ThemeMode> {
        return context.dataStore.data.map { preferences ->
            ThemeMode.entries[preferences[THEME_MODE] ?: ThemeMode.SYSTEM.ordinal]
        }
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.ordinal
        }
    }

    fun getKeyboardHeight(context: Context, isLandscape: Boolean): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            if (isLandscape) {
                preferences[KEYBOARD_HEIGHT_LANDSCAPE] ?: 0
            } else {
                preferences[KEYBOARD_HEIGHT_PORTRAIT] ?: 0
            }
        }
    }

    suspend fun setKeyboardHeight(context: Context, isLandscape: Boolean, height: Int) {
        context.dataStore.edit { preferences ->
            if (isLandscape) {
                preferences[KEYBOARD_HEIGHT_LANDSCAPE] = height
            } else {
                preferences[KEYBOARD_HEIGHT_PORTRAIT] = height
            }
        }
    }

    fun getLastVersionCode(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[LAST_VERSION_CODE] ?: -1
        }
    }

    suspend fun setLastVersionCode(context: Context, versionCode: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_VERSION_CODE] = versionCode
        }
    }

    suspend fun clear(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun getRecentEmotesKey(channel: String) = stringPreferencesKey("recent_emotes_${channel.lowercase()}")

    fun getRecentEmotes(context: Context, channel: String): Flow<List<Emote>> {
        return context.dataStore.data.map { preferences ->
            val json = preferences[getRecentEmotesKey(channel)] ?: return@map emptyList<Emote>()
            try {
                Json.decodeFromString<List<Emote>>(json)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addRecentEmote(context: Context, channel: String, emote: Emote) {
        context.dataStore.edit { preferences ->
            val key = getRecentEmotesKey(channel)
            val currentJson = preferences[key]
            val currentList = if (currentJson != null) {
                try {
                    Json.decodeFromString<List<Emote>>(currentJson).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }

            // Remove if already exists (to move to front)
            currentList.removeAll { it.id == emote.id }
            currentList.add(0, emote)

            // Keep only top 40
            val limitedList = currentList.take(40)
            preferences[key] = Json.encodeToString(limitedList)
        }
    }

    fun getChatFontSize(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[CHAT_FONT_SIZE] ?: 14
        }
    }

    suspend fun setChatFontSize(context: Context, size: Int) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_FONT_SIZE] = size
        }
    }

    fun getChatEmoteSize(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[CHAT_EMOTE_SIZE] ?: 28
        }
    }

    suspend fun setChatEmoteSize(context: Context, size: Int) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_EMOTE_SIZE] = size
        }
    }

    fun getChatBadgeSize(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[CHAT_BADGE_SIZE] ?: 18
        }
    }

    suspend fun setChatBadgeSize(context: Context, size: Int) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_BADGE_SIZE] = size
        }
    }

    fun isImmersiveBackgroundEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IMMERSIVE_BACKGROUND_ENABLED] ?: true
        }
    }

    suspend fun setImmersiveBackgroundEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IMMERSIVE_BACKGROUND_ENABLED] = enabled
        }
    }

    fun getFullscreenChatRatio(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[FULLSCREEN_CHAT_RATIO] ?: 0
        }
    }

    suspend fun setFullscreenChatRatio(context: Context, ratio: Int) {
        context.dataStore.edit { preferences ->
            preferences[FULLSCREEN_CHAT_RATIO] = ratio
        }
    }

    // Auth methods
    fun getAuthToken(context: Context): Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    fun getAuthClientId(context: Context): Flow<String?> = context.dataStore.data.map { it[AUTH_CLIENT_ID] }
    fun getAuthUserName(context: Context): Flow<String?> = context.dataStore.data.map { it[AUTH_USER_NAME] }
    fun getAuthUserId(context: Context): Flow<String?> = context.dataStore.data.map { it[AUTH_USER_ID] }
    fun isLoggedIn(context: Context): Flow<Boolean> = context.dataStore.data.map { it[AUTH_IS_LOGGED_IN] ?: false }

    suspend fun setAuthData(
        context: Context,
        token: String?,
        clientId: String?,
        userName: String?,
        userId: String?,
        isLoggedIn: Boolean
    ) {
        context.dataStore.edit { preferences ->
            if (token != null) preferences[AUTH_TOKEN] = token else preferences.remove(AUTH_TOKEN)
            if (clientId != null) preferences[AUTH_CLIENT_ID] = clientId else preferences.remove(AUTH_CLIENT_ID)
            if (userName != null) preferences[AUTH_USER_NAME] = userName else preferences.remove(AUTH_USER_NAME)
            if (userId != null) preferences[AUTH_USER_ID] = userId else preferences.remove(AUTH_USER_ID)
            preferences[AUTH_IS_LOGGED_IN] = isLoggedIn
        }
    }
}
