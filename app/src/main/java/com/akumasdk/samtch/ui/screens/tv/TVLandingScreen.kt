package com.akumasdk.samtch.ui.screens.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.akumasdk.samtch.data.settings.SettingsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TVLandingScreen(
    onStreamerSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val history by SettingsManager.getSearchHistory(context).collectAsState(initial = emptyList())
    
    var streamerName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10)) // Twitch-like dark background
            .padding(48.dp)
    ) {
        // History List (Left Side)
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
        ) {
            Text(
                text = "History",
                style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { name ->
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                SettingsManager.addToSearchHistory(context, name)
                                onStreamerSelected(name)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = Color(0xFF9146FF)
                        ),
                        shape = ClickableSurfaceDefaults.shape(androidx.tv.material3.MaterialTheme.shapes.small)
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White,
                            style = androidx.tv.material3.MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(48.dp))

        // Input Area (Right Side / Center)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Enter Streamer Name",
                style = androidx.tv.material3.MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = streamerName,
                onValueChange = { streamerName = it },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .focusRequester(focusRequester),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    cursorColor = Color(0xFF9146FF),
                    focusedIndicatorColor = Color(0xFF9146FF)
                ),
                textStyle = androidx.tv.material3.LocalTextStyle.current.copy(fontSize = 24.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (streamerName.isNotBlank()) {
                            coroutineScope.launch {
                                SettingsManager.addToSearchHistory(context, streamerName.trim())
                                onStreamerSelected(streamerName.trim())
                            }
                        }
                    }
                ),
                placeholder = { Text("e.g. forsen", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            androidx.tv.material3.Button(
                onClick = {
                    if (streamerName.isNotBlank()) {
                        coroutineScope.launch {
                            SettingsManager.addToSearchHistory(context, streamerName.trim())
                            onStreamerSelected(streamerName.trim())
                        }
                    }
                },
                modifier = Modifier.width(200.dp),
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = Color(0xFF9146FF),
                    focusedContainerColor = Color(0xFF772CE8)
                )
            ) {
                Text("Watch Now")
            }
        }
    }
}
