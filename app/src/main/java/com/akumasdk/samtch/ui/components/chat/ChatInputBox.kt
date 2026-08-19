package com.akumasdk.samtch.ui.components.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    onToggleMode: (() -> Unit)? = null,
    onLoginRequested: () -> Unit = {}
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val isImeVisible = WindowInsets.isImeVisible
    
    LaunchedEffect(isEmoteMenuVisible, isImeVisible) {
        if (!isEmoteMenuVisible && !isImeVisible) {
            focusManager.clearFocus()
        }
    }

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
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Far Left: Mode Toggle Button (Independent Circle)
                    AnimatedVisibility(
                        visible = onToggleMode != null && portraitMode != null && portraitMode != PortraitMode.AUDIO_AND_CHAT,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        val currentMode = portraitMode ?: return@AnimatedVisibility
                        val currentToggle = onToggleMode ?: return@AnimatedVisibility

                        Surface(
                            onClick = currentToggle,
                            color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.05f),
                            shape = CircleShape,
                            border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = currentMode,
                                    transitionSpec = {
                                        (fadeIn() + scaleIn(initialScale = 0.8f))
                                            .togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
                                    },
                                    label = "ModeIconMorph"
                                ) { mode ->
                                    Icon(
                                        imageVector = when (mode) {
                                            PortraitMode.VIDEO_AND_CHAT, PortraitMode.AUDIO_AND_CHAT -> Icons.AutoMirrored.Filled.Chat
                                            PortraitMode.CHAT_ONLY -> Icons.Default.SmartDisplay
                                        },
                                        contentDescription = stringResource(R.string.content_desc_toggle_mode),
                                        tint = SamtchTheme.colors.secondaryText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Center Pill: Emote Toggle + Text Field
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, end = 16.dp)
                        ) {
                            // Integrated Emote Toggle
                            IconButton(
                                onClick = {
                                    if (isEmoteMenuVisible) {
                                        if (isImeVisible) {
                                            keyboardController?.hide()
                                        } else {
                                            if (!isFocused) {
                                                focusRequester.requestFocus()
                                            }
                                            keyboardController?.show()
                                        }
                                    } else {
                                        onEmoteToggle()
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                val isEmoteIcon = !(isEmoteMenuVisible && !isImeVisible)

                                AnimatedContent(
                                    targetState = isEmoteIcon,
                                    transitionSpec = {
                                        (fadeIn() + scaleIn(initialScale = 0.7f))
                                            .togetherWith(fadeOut() + scaleOut(targetScale = 0.7f))
                                    },
                                    label = "EmoteIconMorph"
                                ) { showEmote ->
                                    Icon(
                                        imageVector = if (showEmote) Icons.Default.EmojiEmotions else Icons.Default.Keyboard,
                                        contentDescription = if (showEmote) stringResource(R.string.content_desc_emotes) else stringResource(R.string.content_desc_keyboard),
                                        tint = SamtchTheme.colors.secondaryText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.chat_input_placeholder),
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
                                    cursorBrush = SolidColor(SamtchTheme.colors.accentColor),
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
                                        .onFocusChanged { isFocused = it.isFocused }
                                )
                            }
                        }
                    }

                    // Right Pill: Send Button
                    val sendEnabled = textFieldValue.text.isNotBlank()
                    val sendIconColor by animateColorAsState(
                        if (sendEnabled) Color.White else SamtchTheme.colors.secondaryText.copy(alpha = 0.4f),
                        label = "SendButtonColor"
                    )
                    val sendBgColor by animateColorAsState(
                        if (sendEnabled) SamtchTheme.colors.twitchPurple else SamtchTheme.colors.textFieldBackground.copy(alpha = 0.05f),
                        label = "SendBgColor"
                    )

                    Surface(
                        onClick = {
                            if (sendEnabled) {
                                onSendMessage(textFieldValue.text)
                                textFieldValue = TextFieldValue("")
                                onTextChange("", 0)
                            }
                        },
                        enabled = sendEnabled,
                        color = sendBgColor,
                        shape = CircleShape,
                        border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.content_desc_send),
                                tint = sendIconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = onToggleMode != null && portraitMode != null && portraitMode != PortraitMode.AUDIO_AND_CHAT,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        val currentMode = portraitMode ?: return@AnimatedVisibility
                        val currentToggle = onToggleMode ?: return@AnimatedVisibility

                        Surface(
                            onClick = currentToggle,
                            color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.05f),
                            shape = CircleShape,
                            border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = currentMode,
                                    transitionSpec = {
                                        (fadeIn() + scaleIn(initialScale = 0.8f))
                                            .togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
                                    },
                                    label = "ModeIconMorphNotLoggedIn"
                                ) { mode ->
                                    Icon(
                                        imageVector = when (mode) {
                                            PortraitMode.VIDEO_AND_CHAT, PortraitMode.AUDIO_AND_CHAT -> Icons.AutoMirrored.Filled.Chat
                                            PortraitMode.CHAT_ONLY -> Icons.Default.SmartDisplay
                                        },
                                        contentDescription = stringResource(R.string.content_desc_toggle_mode),
                                        tint = SamtchTheme.colors.secondaryText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLoginRequested() }
                    ) {
                        Text(
                            text = stringResource(R.string.chat_login_prompt),
                            color = SamtchTheme.colors.primaryText.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
