package com.akumasdk.samtch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsManager {
    private val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
    private val AUDIO_ONLY_BACKGROUND_ENABLED = booleanPreferencesKey("audio_only_background_enabled")
    private val AD_BLOCK_MODE = booleanPreferencesKey("ad_block_mode_is_vaft") // true = VAFT, false = VideoSwap

    enum class AdBlockMode {
        VAFT, VIDEO_SWAP
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
            if (preferences[AD_BLOCK_MODE] ?: false) AdBlockMode.VAFT else AdBlockMode.VIDEO_SWAP
        }
    }

    suspend fun setAdBlockMode(context: Context, mode: AdBlockMode) {
        context.dataStore.edit { preferences ->
            preferences[AD_BLOCK_MODE] = mode == AdBlockMode.VAFT
        }
    }
}
