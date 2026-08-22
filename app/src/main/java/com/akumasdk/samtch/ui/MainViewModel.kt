package com.akumasdk.samtch.ui

import android.app.Application
import android.content.Intent
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.akumasdk.samtch.util.Constants

class MainViewModel(application: Application) : AndroidViewModel(application) {
    var selectedChannel by mutableStateOf<String?>(null)
    var isInPipMode by mutableStateOf(false)
    var wasInPip by mutableStateOf(false)
    var pipRect by mutableStateOf<Rect?>(null)
    var refreshTrigger by mutableIntStateOf(0)
    var isAppLoaded by mutableStateOf(false)
    var isMinimized by mutableStateOf(false)
    var isAudioOnlyMode by mutableStateOf(false)
    var isSettingsOpen by mutableStateOf(false)
    var lastDarkTheme: Boolean? = null

    var lastAvatarUrl: String? = null
    var lastSubtitle: String? = null

    fun handleIntent(intent: Intent?): String? {
        val action = intent?.getStringExtra(Constants.Extras.ACTION)
        val newChannel = intent?.getStringExtra(Constants.Extras.CHANNEL)
        
        if (action == Constants.Actions.STOP) {
            selectedChannel = null
            isMinimized = false
            return null
        } else if (newChannel != null) {
            selectedChannel = newChannel
            isMinimized = false
            return newChannel
        }
        
        val intentUrl = intent?.data?.toString()
        val channelFromUrl = extractChannelFromUrl(intentUrl)
        if (channelFromUrl != null) {
            selectedChannel = channelFromUrl
            isMinimized = false
            return channelFromUrl
        }
        return null
    }

    private fun extractChannelFromUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val regex = """(?:www\.|m\.)?${Constants.Twitch.DOMAIN}/([^/?]+)""".toRegex()
        return regex.find(url)?.groupValues?.getOrNull(1)?.trim()
    }
    
    fun toggleSettings(open: Boolean) {
        isSettingsOpen = open
    }
    
    fun updateChannel(channel: String?) {
        selectedChannel = channel
        if (channel != null) isMinimized = false
    }

    fun incrementRefreshTrigger() {
        refreshTrigger++
    }
}
