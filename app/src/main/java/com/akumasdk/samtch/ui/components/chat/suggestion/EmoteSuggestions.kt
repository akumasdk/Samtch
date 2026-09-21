package com.akumasdk.samtch.ui.components.chat.suggestion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SamtchTheme.colors.dialogBackground.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.15f))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(suggestions, key = { "${it.id}_${it.code}" }) { emote ->
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
    Surface(
        color = SamtchTheme.colors.textFieldBackground.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.3.dp, SamtchTheme.colors.glassBorder.copy(alpha = 0.15f)),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(emote) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(emote.url)
                    .crossfade(true)
                    .build(),
                contentDescription = emote.code,
                modifier = Modifier.size(22.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = emote.code,
                color = SamtchTheme.colors.primaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
