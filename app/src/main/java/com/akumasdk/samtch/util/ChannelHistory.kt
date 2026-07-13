package com.akumasdk.samtch.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "channel_history")

class ChannelHistory(private val context: Context) {
    private val HISTORY_KEY = stringPreferencesKey("history_list_ordered")

    val history: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val historyString = preferences[HISTORY_KEY] ?: ""
            if (historyString.isEmpty()) emptyList() 
            else historyString.split(",").filter { it.isNotEmpty() }
        }

    suspend fun addChannel(channel: String) {
        context.dataStore.edit { preferences ->
            val currentString = preferences[HISTORY_KEY] ?: ""
            val currentList = if (currentString.isEmpty()) emptyList() 
                            else currentString.split(",").filter { it.isNotEmpty() }
            
            // Remove if exists and add to front (most recent)
            val updatedList = mutableListOf(channel)
            updatedList.addAll(currentList.filter { it != channel })
            
            // Limit to top 10
            val limitedList = updatedList.take(10)
            preferences[HISTORY_KEY] = limitedList.joinToString(",")
        }
    }

    suspend fun removeChannel(channel: String) {
        context.dataStore.edit { preferences ->
            val currentString = preferences[HISTORY_KEY] ?: ""
            val currentList = if (currentString.isEmpty()) emptyList() 
                            else currentString.split(",").filter { it.isNotEmpty() }
            
            val updatedList = currentList.filter { it != channel }
            preferences[HISTORY_KEY] = updatedList.joinToString(",")
        }
    }
}
