package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.akumasdk.samtch.R

@Composable
fun ChatInputBox(
    isLoggedIn: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1F1F23), // Twitch standard input background
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                enabled = isLoggedIn,
                placeholder = {
                    Text(
                        text = if (isLoggedIn) stringResource(R.string.chat_welcome).substringBefore(" ") + "…" 
                               else stringResource(R.string.chat_login_prompt),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.Gray,
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    disabledContainerColor = Color.Black.copy(alpha = 0.1f),
                    cursorColor = Color(0xFFBF94FF),
                    focusedPlaceholderColor = Color.Gray,
                    unfocusedPlaceholderColor = Color.Gray,
                    disabledPlaceholderColor = Color.White.copy(alpha = 0.5f)
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank() && isLoggedIn) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                })
            )
            
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && isLoggedIn) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = isLoggedIn && inputText.isNotBlank(),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (isLoggedIn && inputText.isNotBlank()) Color(0xFFBF94FF) else Color.Gray
                )
            }
        }
    }
}
