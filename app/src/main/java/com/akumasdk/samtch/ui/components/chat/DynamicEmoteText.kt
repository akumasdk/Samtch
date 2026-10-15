package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.akumasdk.samtch.data.emote.EmoteRepository

@Composable
fun DynamicEmoteText(
    text: AnnotatedString,
    emotes: List<EmoteInfo>,
    emoteRepository: EmoteRepository,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    style: TextStyle = TextStyle.Default,
    onEmoteClick: ((EmoteInfo) -> Unit)? = null,
    onEmoteLongClick: ((EmoteInfo) -> Unit)? = null,
    onClick: ((Int) -> Unit)? = null,
    emoteSize: Int = 28,
    badgeSize: Int = 18
) {
    val context = LocalContext.current
    val baseEmoteHeight = if (isCompact) (emoteSize * 0.8f) else emoteSize.toFloat()
    val baseBadgeHeight = if (isCompact) (badgeSize * 0.8f) else badgeSize.toFloat()
    
    val measuredWidths = remember { mutableStateMapOf<String, Float>() }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val inlineContent = remember(emotes, measuredWidths.toMap(), baseEmoteHeight, baseBadgeHeight, onEmoteClick, onEmoteLongClick) {
        emotes.associate { emote ->
            val isBadge = emote.source == "Badge"
            val baseHeight = if (isBadge) baseBadgeHeight else baseEmoteHeight

            val urls = emote.url.split("|")
            val baseEmoteUrl = urls.first()
            val cachedRatio = emoteRepository.getAspectRatio(baseEmoteUrl)
            
            val width = measuredWidths[emote.id] ?: (if (cachedRatio != null) baseHeight * cachedRatio else baseHeight)
            
            emote.id to InlineTextContent(
                Placeholder(width.sp, baseHeight.sp, PlaceholderVerticalAlign.Center)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(emote, onEmoteClick, onEmoteLongClick) {
                            detectTapGestures(
                                onTap = { onEmoteClick?.invoke(emote) },
                                onLongPress = { onEmoteLongClick?.invoke(emote) }
                            )
                        }
                ) {
                    urls.forEach { url ->
                        val imageRequest = remember(url) {
                            ImageRequest.Builder(context)
                                .data(url)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCacheKey(url)
                                .build()
                        }
                        val painter = rememberAsyncImagePainter(model = imageRequest)
                        val painterState = painter.state

                        if (url == baseEmoteUrl && painterState is AsyncImagePainter.State.Success) {
                            val drawable = painterState.result.drawable
                            if (drawable.intrinsicHeight > 0) {
                                val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
                                if (cachedRatio != ratio) {
                                    emoteRepository.putAspectRatio(url, ratio)
                                    val calculatedWidth = baseHeight * ratio
                                    if (measuredWidths[emote.id] != calculatedWidth) {
                                        measuredWidths[emote.id] = calculatedWidth
                                    }
                                }
                            }
                        }

                        Image(
                            painter = painter,
                            contentDescription = emote.code,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }
    }

    BasicText(
        text = text,
        modifier = modifier.pointerInput(onClick) {
            detectTapGestures { offset ->
                layoutResult?.let { result ->
                    val position = result.getOffsetForPosition(offset)
                    onClick?.invoke(position)
                }
            }
        },
        style = style,
        onTextLayout = { layoutResult = it },
        inlineContent = inlineContent
    )
}
