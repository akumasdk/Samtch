package com.akumasdk.samtch.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.akumasdk.samtch.tv.ui.LandingScreen
import com.akumasdk.samtch.tv.ui.PlayerScreen

class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentChannel by remember { mutableStateOf<String?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (currentChannel == null) {
                    LandingScreen(
                        onChannelSelected = { channel ->
                            currentChannel = channel
                        }
                    )
                } else {
                    PlayerScreen(
                        channel = currentChannel!!,
                        onBack = {
                            currentChannel = null
                        }
                    )
                }
            }
        }
    }
}
