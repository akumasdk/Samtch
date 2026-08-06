package com.akumasdk.samtch.ui.components.chat.emotemenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenu(
    tabs: Map<String, List<Emote>>,
    onEmoteClick: (Emote) -> Unit,
    onEmoteLongClick: (Emote) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = tabs.keys.toList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(SamtchTheme.colors.dialogBackground)
    ) {
        if (tabTitles.isNotEmpty()) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SamtchTheme.colors.dialogBackground,
                contentColor = SamtchTheme.colors.twitchPurpleLight,
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTabIndex == index) SamtchTheme.colors.twitchPurpleLight else SamtchTheme.colors.secondaryText
                            )
                        }
                    )
                }
            }

            val currentEmotes = tabs[tabTitles[selectedTabIndex]] ?: emptyList()
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 48.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentEmotes, key = { it.id }) { emote ->
                    EmoteItem(
                        emote = emote,
                        onClick = { onEmoteClick(emote) },
                        onLongClick = { onEmoteLongClick(emote) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No emotes available",
                    color = SamtchTheme.colors.secondaryText
                )
            }
        }
    }
}

@Composable
fun EmoteItem(
    emote: Emote,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(emote) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(emote.url)
                .crossfade(true)
                .build(),
            contentDescription = emote.code,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
