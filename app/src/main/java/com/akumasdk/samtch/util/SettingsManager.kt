package com.akumasdk.samtch.util

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
    private val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
    private val PIP_ENABLED = booleanPreferencesKey("pip_enabled")

    fun isBackgroundPlayEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BACKGROUND_PLAY_ENABLED] ?: false
        }
    }

    suspend fun setBackgroundPlayEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_PLAY_ENABLED] = enabled
        }
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
}
