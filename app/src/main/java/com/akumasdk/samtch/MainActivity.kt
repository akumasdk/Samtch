package com.akumasdk.samtch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import com.akumasdk.samtch.ui.screens.tv.TVLandingScreen
import com.akumasdk.samtch.ui.screens.tv.TVPlayerScreen
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.ScriptLoader

class MainActivity : ComponentActivity() {

    fun setSoftInputDisabled(disabled: Boolean) {
        if (disabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        ScriptLoader.initialize(this)
        
        super.onCreate(savedInstanceState)

        // Prevent keyboard from resizing the UI and keep it hidden on TV
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or 
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        setContent {
            SamtchTheme {
                var selectedChannel by rememberSaveable { mutableStateOf<String?>(null) }

                Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
                    if (selectedChannel == null) {
                        TVLandingScreen(
                            onStreamerSelected = { channel ->
                                selectedChannel = channel
                            }
                        )
                    } else {
                        TVPlayerScreen(
                            channel = selectedChannel!!,
                            onBack = { selectedChannel = null }
                        )
                    }
                }
            }
        }
    }
}
