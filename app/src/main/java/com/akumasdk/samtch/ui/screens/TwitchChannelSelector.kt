package com.akumasdk.samtch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.util.ChannelHistory
import kotlinx.coroutines.launch

@Composable
fun TwitchChannelSelector(
    onChannelSelected: (String) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val historyManager = remember { ChannelHistory(context) }
    val history by historyManager.history.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val handleSelection = { name: String ->
        if (name.isNotEmpty()) {
            scope.launch {
                historyManager.addChannel(name)
                onChannelSelected(name)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Left Side: Search and Input
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBF94FF)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = channelName,
                onValueChange = { channelName = it.trim() },
                label = { Text(stringResource(R.string.twitch_username_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFBF94FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color(0xFFBF94FF),
                    cursorColor = Color(0xFFBF94FF),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = { handleSelection(channelName) }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { handleSelection(channelName) },
                modifier = Modifier.fillMaxWidth(),
                enabled = channelName.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9146FF),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF9146FF).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text(stringResource(R.string.watch_button_text))
            }
        }

        // Right Side: History List
        if (history.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recents",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { channel ->
                        HistoryItem(
                            channel = channel,
                            onClick = { handleSelection(channel) },
                            onRemove = {
                                scope.launch {
                                    historyManager.removeChannel(channel)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    channel: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var isItemFocused by remember { mutableStateOf(false) }
        
        Surface(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { isItemFocused = it.isFocused },
            color = if (isItemFocused) Color(0xFF1F1F23) else Color.Transparent,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = channel,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        
        var isRemoveFocused by remember { mutableStateOf(false) }
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(36.dp)
                .onFocusChanged { isRemoveFocused = it.isFocused }
                .background(
                    color = if (isRemoveFocused) Color.Red.copy(alpha = 0.4f) else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = if (isRemoveFocused) Color.White else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
