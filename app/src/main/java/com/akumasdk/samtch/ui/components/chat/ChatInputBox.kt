package com.akumasdk.samtch.ui.components.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.ui.components.chat.suggestion.EmoteSuggestions
import com.akumasdk.samtch.ui.screens.player.models.PortraitMode
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatInputBox(
    isLoggedIn: Boolean,
    onSendMessage: (String) -> Unit,
    onEmoteToggle: () -> Unit,
    isEmoteMenuVisible: Boolean,
    suggestions: List<Emote>,
    onEmoteSelected: (Emote) -> Unit,
    onEmoteLongClick: (Emote) -> Unit,
    onTextChange: (String, Int) -> Unit,
    emoteInsertFlow: SharedFlow<Emote>,
    modifier: Modifier = Modifier,
    portraitMode: PortraitMode? = null,
    onToggleMode: (() -> Unit)? = null
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible
    
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
        color = Color.Transparent, // Managed by parent background or inner Column
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (suggestions.isNotEmpty()) {
                EmoteSuggestions(
                    suggestions = suggestions,
                    onEmoteClick = handleEmoteSelected,
                    onEmoteLongClick = onEmoteLongClick,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .background(
                            color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Buttons: Emote & Toggle Mode
                    IconButton(
                        onClick = {
                            if (isEmoteMenuVisible) {
                                if (isImeVisible) {
                                    // Keyboard is open over emotes, hide it to reveal menu
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                } else {
                                    // Menu is open, show keyboard to cover it
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            } else {
                                // Transition to emotes
                                onEmoteToggle()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isEmoteMenuVisible && !isImeVisible) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                            contentDescription = if (isEmoteMenuVisible && !isImeVisible) "Keyboard" else "Emotes",
                            tint = SamtchTheme.colors.secondaryText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (onToggleMode != null && portraitMode != null) {
                        IconButton(
                            onClick = onToggleMode,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = when (portraitMode) {
                                    PortraitMode.VIDEO_AND_CHAT, PortraitMode.AUDIO_AND_CHAT -> Icons.AutoMirrored.Filled.Chat
                                    PortraitMode.CHAT_ONLY -> Icons.Default.SmartDisplay
                                },
                                contentDescription = "Toggle Mode",
                                tint = SamtchTheme.colors.secondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Text Field
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textFieldValue.text.isEmpty()) {
                            Text(
                                text = "Send a message…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SamtchTheme.colors.secondaryText.copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                textFieldValue = it
                                onTextChange(it.text, it.selection.start)
                            },
                            textStyle = TextStyle(
                                color = SamtchTheme.colors.primaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(SamtchTheme.colors.twitchPurpleLight),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (textFieldValue.text.isNotBlank()) {
                                    onSendMessage(textFieldValue.text)
                                    textFieldValue = TextFieldValue("")
                                    onTextChange("", 0)
                                }
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    // Send Button
                    val sendEnabled = textFieldValue.text.isNotBlank()
                    val sendIconColor by animateColorAsState(
                        if (sendEnabled) SamtchTheme.colors.twitchPurpleLight else SamtchTheme.colors.secondaryText.copy(alpha = 0.4f),
                        label = "SendButtonColor"
                    )

                    IconButton(
                        onClick = {
                            if (sendEnabled) {
                                onSendMessage(textFieldValue.text)
                                textFieldValue = TextFieldValue("")
                                onTextChange("", 0)
                            }
                        },
                        enabled = sendEnabled,
                        modifier = Modifier
                            .size(40.dp)
                            .padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = sendIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                Surface(
                    color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.chat_login_prompt),
                        color = SamtchTheme.colors.primaryText.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
