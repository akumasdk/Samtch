package com.akumasdk.samtch.ui.components.chat.emotemenu

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.data.emote.EmoteTab
import com.akumasdk.samtch.ui.theme.SamtchTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteMenu(
    tabs: List<EmoteTab>,
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
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val safeTabIndex = remember(tabs, selectedTabIndex) {
        if (tabs.isEmpty()) 0 else selectedTabIndex.coerceIn(0, tabs.size - 1)
    }

    LaunchedEffect(tabs) {
        if (selectedTabIndex >= tabs.size && tabs.isNotEmpty()) {
            selectedTabIndex = 0
        }
    }

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
            if (tabs.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryScrollableTabRow(
                        selectedTabIndex = safeTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = SamtchTheme.colors.accentColor,
                        edgePadding = 0.dp,
                        divider = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = safeTabIndex == index,
                                onClick = {
                                    if (selectedTabIndex != index) {
                                        selectedTabIndex = index
                                        coroutineScope.launch {
                                            gridState.scrollToItem(0)
                                        }
                                    }
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (tab.iconResId != null) {
                                            Icon(
                                                painter = painterResource(id = tab.iconResId),
                                                contentDescription = null,
                                                tint = if (safeTabIndex == index) SamtchTheme.colors.accentColor else SamtchTheme.colors.secondaryText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = stringResource(tab.titleResId),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (safeTabIndex == index) SamtchTheme.colors.accentColor else SamtchTheme.colors.secondaryText
                                        )
                                    }
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
                    val currentTab = tabs.getOrNull(safeTabIndex)
                    val currentEmotes = currentTab?.emotes ?: emptyList()

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 48.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 8.dp, start = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = currentEmotes,
                            key = { "${it.type.name}_${it.id}_${it.code}" },
                            contentType = { "emote" }
                        ) { emote ->
                            EmoteItem(
                                emote = emote,
                                onClick = { onEmoteClick(emote) },
                                onLongClick = { onEmoteLongClick(emote) }
                            )
                        }
                    }

                    if (isLoading && currentEmotes.isEmpty()) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmoteItem(
    emote: Emote,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subRequiredMsg = stringResource(R.string.emote_requires_subscription)

    val imageRequest = remember(emote.url) {
        ImageRequest.Builder(context)
            .data(emote.url)
            .size(128)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCacheKey(emote.url)
            .crossfade(true)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)

    Box(
        modifier = modifier
            .size(48.dp)
            .combinedClickable(
                onClick = {
                    if (emote.isUnlocked) {
                        onClick()
                    } else {
                        Toast.makeText(context, "$subRequiredMsg: ${emote.code}", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = emote.code,
            modifier = Modifier
                .fillMaxSize()
                .then(if (!emote.isUnlocked) Modifier.alpha(0.38f) else Modifier),
            contentScale = ContentScale.Fit
        )

        if (!emote.isUnlocked) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
