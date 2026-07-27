package com.akumasdk.samtch.ui.components.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun DynamicEmoteText(
    text: AnnotatedString,
    emotes: List<EmoteInfo>,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    val context = LocalContext.current
    
    val measuredWidths = remember { mutableStateMapOf<String, Float>() }

    val inlineContent = remember(emotes, measuredWidths.toMap()) {
        emotes.associate { emote ->
            val width = measuredWidths[emote.id] ?: 20f
            val urls = emote.url.split("|")
            
            emote.id to InlineTextContent(
                Placeholder(width.sp, 20.sp, PlaceholderVerticalAlign.Center)
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
                            onSuccess = { state ->
                                // We only measure based on the base emote (first in cluster)
                                if (url == urls.first()) {
                                    val drawable = state.result.drawable
                                    val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
                                    val calculatedWidth = 20f * ratio
                                    if (measuredWidths[emote.id] != calculatedWidth) {
                                        measuredWidths[emote.id] = calculatedWidth
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        inlineContent = inlineContent
    )
}
