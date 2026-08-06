package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.ui.components.chat.suggestion.EmoteSuggestions
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ChatInputBox(
    isLoggedIn: Boolean,
    onSendMessage: (String) -> Unit,
    onEmoteToggle: () -> Unit,
    suggestions: List<Emote>,
    onEmoteSelected: (Emote) -> Unit,
    onEmoteLongClick: (Emote) -> Unit,
    onTextChange: (String, Int) -> Unit,
    emoteInsertFlow: SharedFlow<Emote>,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    val handleEmoteSelected: (Emote) -> Unit = { emote ->
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val cursorPos = selection.start
        
        var start = cursorPos
        while (start > 0 && text[start - 1] != ' ') start--
        
        val newText = text.substring(0, start) + emote.code + " " + text.substring(cursorPos)
        val newCursorPos = start + emote.code.length + 1
        textFieldValue = TextFieldValue(newText, TextRange(newCursorPos))
        onTextChange(newText, newCursorPos)
        onEmoteSelected(emote)
    }

    LaunchedEffect(emoteInsertFlow) {
        emoteInsertFlow.collectLatest { emote ->
            val text = textFieldValue.text
            val selection = textFieldValue.selection
            val cursorPos = selection.start
            
            val newText = text.substring(0, cursorPos) + emote.code + " " + text.substring(cursorPos)
            val newCursorPos = cursorPos + emote.code.length + 1
            textFieldValue = TextFieldValue(newText, TextRange(newCursorPos))
            onTextChange(newText, newCursorPos)
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SamtchTheme.colors.dialogBackground,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            if (suggestions.isNotEmpty()) {
                EmoteSuggestions(
                    suggestions = suggestions,
                    onEmoteClick = handleEmoteSelected,
                    onEmoteLongClick = onEmoteLongClick
                )
            }

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .minHeight(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onEmoteToggle) {
                            Icon(
                                imageVector = Icons.Default.EmojiEmotions,
                                contentDescription = "Emotes",
                                tint = SamtchTheme.colors.secondaryText
                            )
                        }

                        OutlinedTextField(
                            value = textFieldValue,
                            onValueChange = { 
                                textFieldValue = it
                                onTextChange(it.text, it.selection.start)
                            },
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
                                if (textFieldValue.text.isNotBlank()) {
                                    onSendMessage(textFieldValue.text)
                                    textFieldValue = TextFieldValue("")
                                    onTextChange("", 0)
                                }
                            })
                        )
                        
                        IconButton(
                            onClick = {
                                if (textFieldValue.text.isNotBlank()) {
                                    onSendMessage(textFieldValue.text)
                                    textFieldValue = TextFieldValue("")
                                    onTextChange("", 0)
                                }
                            },
                            enabled = textFieldValue.text.isNotBlank(),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (textFieldValue.text.isNotBlank()) SamtchTheme.colors.twitchPurpleLight else SamtchTheme.colors.secondaryText
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
}

private fun Modifier.minHeight(height: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.heightIn(min = height)
)
