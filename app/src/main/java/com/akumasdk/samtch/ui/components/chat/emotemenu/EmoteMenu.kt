package com.akumasdk.samtch.ui.components.chat.emotemenu

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.data.emote.EmoteType
import com.akumasdk.samtch.ui.components.playerComponents.PlayerBackground
import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenu(
    tabs: Map<Int, List<Emote>>,
    onEmoteClick: (Emote) -> Unit,
    onEmoteLongClick: (Emote) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    channel: String = "",
    previewImageUrl: String? = null,
    isImmersiveEnabled: Boolean = true,
    isLoading: Boolean = false
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabResIds = tabs.keys.toList()

    Log.d("EmoteMenu", "Rendering menu. Tabs found: ${tabResIds.size}. Channel: $channel")
    
    val isLightMode = SamtchTheme.colors.dialogBackground.luminance() > 0.5f
    val surfaceAlpha = if (isImmersiveEnabled) {
        if (isLightMode) 0.94f else 0.82f
    } else 1.0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (!isImmersiveEnabled) {
                    Modifier.background(SamtchTheme.colors.dialogBackground.copy(alpha = surfaceAlpha))
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (tabResIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = SamtchTheme.colors.accentColor,
                        edgePadding = 0.dp,
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        tabResIds.forEachIndexed { index, resId ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = stringResource(resId),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selectedTabIndex == index) SamtchTheme.colors.accentColor else SamtchTheme.colors.secondaryText
                                    )
                                }
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.padding(horizontal = 2.dp).size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SamtchTheme.colors.secondaryText.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.padding(end = 4.dp).size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = SamtchTheme.colors.secondaryText.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    val currentEmotes = tabs[tabResIds[selectedTabIndex]] ?: emptyList()
                    
                    // Group emotes by type for sections
                    val groupedEmotes = remember(currentEmotes) {
                        currentEmotes.groupBy { it.type }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 48.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp, start = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedEmotes.forEach { (type, emotes) ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text(
                                        text = when(type) {
                                            EmoteType.TWITCH -> stringResource(R.string.emote_source_twitch)
                                            EmoteType.SEVENTV -> stringResource(R.string.emote_source_seventv)
                                            EmoteType.BTTV -> stringResource(R.string.emote_source_bttv)
                                            EmoteType.FFZ -> stringResource(R.string.emote_source_ffz)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SamtchTheme.colors.secondaryText.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                    )
                                    HorizontalDivider(
                                        thickness = 0.3.dp,
                                        color = SamtchTheme.colors.divider.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            
                            items(emotes, key = { it.id }) { emote ->
                                EmoteItem(
                                    emote = emote,
                                    onClick = { onEmoteClick(emote) },
                                    onLongClick = { onEmoteLongClick(emote) }
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = SamtchTheme.colors.accentColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = SamtchTheme.colors.accentColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.emote_menu_empty),
                            color = SamtchTheme.colors.secondaryText
                        )
                    }
                }
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
