package com.akumasdk.samtch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R

@Composable
fun TwitchChannelSelector(
    onChannelSelected: (String) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0E10))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBF94FF) // Twitch Purple
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = channelName,
            onValueChange = { channelName = it.trim() },
            label = { Text(stringResource(R.string.twitch_username_label)) },
            modifier = Modifier
                .fillMaxWidth(0.8f)
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
                onGo = {
                    if (channelName.isNotEmpty()) {
                        onChannelSelected(channelName)
                    }
                }
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (channelName.isNotEmpty()) {
                    onChannelSelected(channelName)
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = channelName.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF9146FF), // Twitch Primary Purple
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF9146FF).copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            )
        ) {
            Text(stringResource(R.string.watch_button_text))
        }
    }
}
