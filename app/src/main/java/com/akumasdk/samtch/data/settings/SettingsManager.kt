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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
    private val AD_BLOCK_MODE = booleanPreferencesKey("ad_block_mode_is_vaft")
    private val CHAT_MODE = booleanPreferencesKey("chat_mode_is_native")
    private val MINI_PLAYER_HINT_SHOWN = booleanPreferencesKey("mini_player_hint_shown")
    private val PLAYER_TOOLTIP_SHOW_COUNT = intPreferencesKey("player_tooltip_show_count")
    private val THEME_MODE = intPreferencesKey("theme_mode")
    private val LAST_VERSION_CODE = intPreferencesKey("last_version_code")
    private val KEYBOARD_HEIGHT_PORTRAIT = intPreferencesKey("keyboard_height_portrait")
    private val KEYBOARD_HEIGHT_LANDSCAPE = intPreferencesKey("keyboard_height_landscape")
    private val CHAT_FONT_SIZE = intPreferencesKey("chat_font_size")
    private val CHAT_EMOTE_SIZE = intPreferencesKey("chat_emote_size")
    private val CHAT_BADGE_SIZE = intPreferencesKey("chat_badge_size")
    private val IMMERSIVE_BACKGROUND_ENABLED = booleanPreferencesKey("immersive_background_enabled")
    private val FULLSCREEN_CHAT_RATIO = intPreferencesKey("fullscreen_chat_ratio_v2")
    private val THIRD_PARTY_EMOTES_ENABLED = booleanPreferencesKey("third_party_emotes_enabled")
    private val EMOTE_QUALITY = intPreferencesKey("emote_quality_v1")

    // Auth
    private val AUTH_TOKEN = stringPreferencesKey("auth_token")
    private val AUTH_CLIENT_ID = stringPreferencesKey("auth_client_id")
    private val AUTH_USER_NAME = stringPreferencesKey("auth_user_name")
    private val AUTH_USER_ID = stringPreferencesKey("auth_user_id")
    private val AUTH_IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")

    enum class AdBlockMode { VAFT, VIDEO_SWAP }
    enum class ChatMode { NATIVE, LEGACY }
    enum class ThemeMode { DARK, LIGHT, SYSTEM }
    enum class EmoteQuality(
        val twitchScale: String,
        val bttvScale: String,
        val ffzScale: String,
        val sevenTvFile: String
    ) {
        LOW("1.0", "1x", "1", "1x.webp"),
        MEDIUM("2.0", "2x", "2", "2x.webp"),
        HIGH("3.0", "3x", "4", "4x.webp")
    }

    private val dataStore get() = context.dataStore

    fun isPipEnabled(): Flow<Boolean> = dataStore.data.map { it[PIP_ENABLED] ?: true }
    suspend fun setPipEnabled(enabled: Boolean) = dataStore.edit { it[PIP_ENABLED] = enabled }


    fun getAdBlockMode(): Flow<AdBlockMode> = dataStore.data.map { if (it[AD_BLOCK_MODE] ?: true) AdBlockMode.VAFT else AdBlockMode.VIDEO_SWAP }
    suspend fun setAdBlockMode(mode: AdBlockMode) = dataStore.edit { it[AD_BLOCK_MODE] = mode == AdBlockMode.VAFT }

    fun getChatMode(): Flow<ChatMode> = dataStore.data.map { if (it[CHAT_MODE] ?: true) ChatMode.NATIVE else ChatMode.LEGACY }
    suspend fun setChatMode(mode: ChatMode) = dataStore.edit { it[CHAT_MODE] = mode == ChatMode.NATIVE }

    fun isMiniPlayerHintShown(): Flow<Boolean> = dataStore.data.map { it[MINI_PLAYER_HINT_SHOWN] ?: false }
    suspend fun setMiniPlayerHintShown(shown: Boolean) = dataStore.edit { it[MINI_PLAYER_HINT_SHOWN] = shown }

    fun getPlayerTooltipShowCount(): Flow<Int> = dataStore.data.map { it[PLAYER_TOOLTIP_SHOW_COUNT] ?: 0 }
    suspend fun incrementPlayerTooltipShowCount() = dataStore.edit {
        val current = it[PLAYER_TOOLTIP_SHOW_COUNT] ?: 0
        it[PLAYER_TOOLTIP_SHOW_COUNT] = current + 1
    }

    fun getThemeMode(): Flow<ThemeMode> = dataStore.data.map { ThemeMode.entries[it[THEME_MODE] ?: ThemeMode.SYSTEM.ordinal] }
    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[THEME_MODE] = mode.ordinal }

    fun getKeyboardHeight(isLandscape: Boolean): Flow<Int> = dataStore.data.map { 
        if (isLandscape) it[KEYBOARD_HEIGHT_LANDSCAPE] ?: 0 else it[KEYBOARD_HEIGHT_PORTRAIT] ?: 0 
    }
    suspend fun setKeyboardHeight(isLandscape: Boolean, height: Int) = dataStore.edit {
        if (isLandscape) it[KEYBOARD_HEIGHT_LANDSCAPE] = height else it[KEYBOARD_HEIGHT_PORTRAIT] = height
    }

    fun getLastVersionCode(): Flow<Int> = dataStore.data.map { it[LAST_VERSION_CODE] ?: -1 }
    suspend fun setLastVersionCode(versionCode: Int) = dataStore.edit { it[LAST_VERSION_CODE] = versionCode }

    suspend fun clear() = dataStore.edit { it.clear() }

    private fun getRecentEmotesKey(channel: String) = stringPreferencesKey("recent_emotes_${channel.lowercase()}")

    fun getRecentEmotes(channel: String): Flow<List<Emote>> = dataStore.data.map { preferences ->
        val jsonStr = preferences[getRecentEmotesKey(channel)] ?: return@map emptyList()
        try { json.decodeFromString<List<Emote>>(jsonStr) } catch (_: Exception) { emptyList() }
    }

    suspend fun addRecentEmote(channel: String, emote: Emote) = dataStore.edit { preferences ->
        val key = getRecentEmotesKey(channel)
        val currentList = preferences[key]?.let {
            try { json.decodeFromString<List<Emote>>(it).toMutableList() } catch (_: Exception) { mutableListOf() }
        } ?: mutableListOf()

        currentList.removeAll { it.id == emote.id }
        currentList.add(0, emote)
        preferences[key] = json.encodeToString(currentList.take(40))
    }

    fun getChatFontSize(): Flow<Int> = dataStore.data.map { it[CHAT_FONT_SIZE] ?: 14 }
    suspend fun setChatFontSize(size: Int) = dataStore.edit { it[CHAT_FONT_SIZE] = size }

    fun getChatEmoteSize(): Flow<Int> = dataStore.data.map { it[CHAT_EMOTE_SIZE] ?: 28 }
    suspend fun setChatEmoteSize(size: Int) = dataStore.edit { it[CHAT_EMOTE_SIZE] = size }

    fun getChatBadgeSize(): Flow<Int> = dataStore.data.map { it[CHAT_BADGE_SIZE] ?: 18 }
    suspend fun setChatBadgeSize(size: Int) = dataStore.edit { it[CHAT_BADGE_SIZE] = size }

    fun isImmersiveBackgroundEnabled(): Flow<Boolean> = dataStore.data.map { it[IMMERSIVE_BACKGROUND_ENABLED] ?: true }
    suspend fun setImmersiveBackgroundEnabled(enabled: Boolean) = dataStore.edit { it[IMMERSIVE_BACKGROUND_ENABLED] = enabled }

    fun getFullscreenChatRatio(): Flow<Int> = dataStore.data.map { it[FULLSCREEN_CHAT_RATIO] ?: 0 }
    suspend fun setFullscreenChatRatio(ratio: Int) = dataStore.edit { it[FULLSCREEN_CHAT_RATIO] = ratio }

    fun getEmoteQuality(): Flow<EmoteQuality> = dataStore.data.map { 
        EmoteQuality.entries[it[EMOTE_QUALITY] ?: EmoteQuality.MEDIUM.ordinal] 
    }
    suspend fun setEmoteQuality(quality: EmoteQuality) = dataStore.edit { 
        it[EMOTE_QUALITY] = quality.ordinal 
    }

    private fun getChannelThirdPartyEmotesKey(channel: String) = booleanPreferencesKey("third_party_emotes_${channel.lowercase()}")

    fun isThirdPartyEmotesEnabled(): Flow<Boolean> = dataStore.data.map { it[THIRD_PARTY_EMOTES_ENABLED] ?: true }
    suspend fun setThirdPartyEmotesEnabled(enabled: Boolean) = dataStore.edit { it[THIRD_PARTY_EMOTES_ENABLED] = enabled }

    fun isThirdPartyEmotesEnabledForChannel(channel: String): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[getChannelThirdPartyEmotesKey(channel)] ?: preferences[THIRD_PARTY_EMOTES_ENABLED] ?: true
    }
    suspend fun setThirdPartyEmotesEnabledForChannel(channel: String, enabled: Boolean) = dataStore.edit { preferences ->
        preferences[getChannelThirdPartyEmotesKey(channel)] = enabled
    }

    // Auth methods
    fun getAuthToken(): Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }
    fun getAuthClientId(): Flow<String?> = dataStore.data.map { it[AUTH_CLIENT_ID] }
    fun getAuthUserName(): Flow<String?> = dataStore.data.map { it[AUTH_USER_NAME] }
    fun getAuthUserId(): Flow<String?> = dataStore.data.map { it[AUTH_USER_ID] }
    fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { it[AUTH_IS_LOGGED_IN] ?: false }

    suspend fun setAuthData(token: String?, clientId: String?, userName: String?, userId: String?, isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            if (token != null) preferences[AUTH_TOKEN] = token else preferences.remove(AUTH_TOKEN)
            if (clientId != null) preferences[AUTH_CLIENT_ID] = clientId else preferences.remove(AUTH_CLIENT_ID)
            if (userName != null) preferences[AUTH_USER_NAME] = userName else preferences.remove(AUTH_USER_NAME)
            if (userId != null) preferences[AUTH_USER_ID] = userId else preferences.remove(AUTH_USER_ID)
            preferences[AUTH_IS_LOGGED_IN] = isLoggedIn
        }
    }
}
