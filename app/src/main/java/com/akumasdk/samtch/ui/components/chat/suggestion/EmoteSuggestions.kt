package com.akumasdk.samtch.ui.components.chat.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun EmoteSuggestions(
    suggestions: List<Emote>,
    onEmoteClick: (Emote) -> Unit,
    onEmoteLongClick: (Emote) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SamtchTheme.colors.dialogBackground.copy(alpha = 0.95f))
            .height(56.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(suggestions, key = { it.id }) { emote ->
                SuggestionItem(
                    emote = emote,
                    onClick = { onEmoteClick(emote) },
                    onLongClick = { onEmoteLongClick(emote) }
                )
            }
        }
    }
}

@Composable
fun SuggestionItem(
    emote: Emote,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .background(
                color = SamtchTheme.colors.textFieldBackground,
                shape = MaterialTheme.shapes.small
            )
            .pointerInput(emote) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(emote.url)
                .crossfade(true)
                .build(),
            contentDescription = emote.code,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = emote.code,
            color = SamtchTheme.colors.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
