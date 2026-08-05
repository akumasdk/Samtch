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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun ChatInputBox(
    isLoggedIn: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SamtchTheme.colors.dialogBackground, // Theme-aware background
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .imePadding()
                .minHeight(56.dp), // Consistent height
            contentAlignment = Alignment.Center
        ) {
            if (isLoggedIn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Send a message…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SamtchTheme.colors.primaryText,
                            unfocusedTextColor = SamtchTheme.colors.primaryText,
                            focusedContainerColor = SamtchTheme.colors.textFieldBackground,
                            unfocusedContainerColor = SamtchTheme.colors.textFieldBackground,
                            cursorColor = SamtchTheme.colors.twitchPurpleLight,
                            focusedPlaceholderColor = SamtchTheme.colors.secondaryText,
                            unfocusedPlaceholderColor = SamtchTheme.colors.secondaryText
                        ),
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        })
                    )
                    
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) SamtchTheme.colors.twitchPurpleLight else SamtchTheme.colors.secondaryText
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.chat_login_prompt),
                    color = SamtchTheme.colors.primaryText.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private fun Modifier.minHeight(height: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.heightIn(min = height)
)
