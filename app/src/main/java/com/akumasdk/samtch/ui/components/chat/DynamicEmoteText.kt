package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.akumasdk.samtch.data.emote.EmoteRepository

@Composable
fun DynamicEmoteText(
    text: AnnotatedString,
    emotes: List<EmoteInfo>,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    style: TextStyle = TextStyle.Default,
    additionalInlineContent: Map<String, InlineTextContent> = emptyMap()
) {
    val context = LocalContext.current
    val baseHeight = if (isCompact) 24f else 32f
    
    val measuredWidths = remember { mutableStateMapOf<String, Float>() }

    val emoteInlineContent = remember(emotes, measuredWidths.toMap(), baseHeight) {
        emotes.associate { emote ->
            val urls = emote.url.split("|")
            val baseEmoteUrl = urls.first()
            val cachedRatio = EmoteRepository.getAspectRatio(baseEmoteUrl)
            
            val width = measuredWidths[emote.id] ?: (if (cachedRatio != null) baseHeight * cachedRatio else baseHeight)
            
            emote.id to InlineTextContent(
                Placeholder(width.sp, baseHeight.sp, PlaceholderVerticalAlign.Center)
            ) {
                Box {
                    urls.forEach { url ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(url)
                                .size(Size.ORIGINAL)
                                .build(),
                            contentDescription = emote.code,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            onSuccess = { state ->
                                if (url == baseEmoteUrl) {
                                    val drawable = state.result.drawable
                                    val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
                                    val calculatedWidth = baseHeight * ratio
                                    if (measuredWidths[emote.id] != calculatedWidth) {
                                        measuredWidths[emote.id] = calculatedWidth
                                        EmoteRepository.putAspectRatio(url, ratio)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val finalInlineContent = remember(emoteInlineContent, additionalInlineContent) {
        emoteInlineContent + additionalInlineContent
    }

    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        inlineContent = finalInlineContent
    )
}
