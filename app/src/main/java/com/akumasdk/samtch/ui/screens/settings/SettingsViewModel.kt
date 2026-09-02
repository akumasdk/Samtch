package com.akumasdk.samtch.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.akumasdk.samtch.data.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsManager: SettingsManager
) : ViewModel()
