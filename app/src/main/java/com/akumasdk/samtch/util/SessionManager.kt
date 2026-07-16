package com.akumasdk.samtch.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SessionManager {
    private const val PREF_NAME = "samtch_session_prefs"
    private const val KEY_LAST_ACTIVE_TIME = "last_active_time"
    private const val REFRESH_THRESHOLD_MS = 30 * 60 * 1000 // 30 minutes

    private var isFirstRunInProcess = true
    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Determines if the player should be refreshed.
     * Returns true if this is the first run in this process OR if a significant time has passed since last activity.
     */
    fun shouldTriggerRefresh(): Boolean {
        if (isFirstRunInProcess) {
            Log.d("SessionManager", "Refresh triggered: First run in process")
            isFirstRunInProcess = false
            return true
        }

        val lastActive = prefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - lastActive

        if (lastActive != 0L && timeElapsed > REFRESH_THRESHOLD_MS) {
            Log.d("SessionManager", "Refresh triggered: Long inactivity (${timeElapsed / 1000 / 60} minutes)")
            return true
        }

        return false
    }

    /**
     * Updates the last active timestamp to the current time.
     */
    fun updateLastActiveTime() {
        if (!::prefs.isInitialized) return
        val currentTime = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_ACTIVE_TIME, currentTime).apply()
        Log.d("SessionManager", "Updated last active time: $currentTime")
    }
}
