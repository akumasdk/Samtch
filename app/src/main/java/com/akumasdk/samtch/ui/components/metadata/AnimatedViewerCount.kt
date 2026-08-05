package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.ui.components.metadata.util.formatViewerCount

@Composable
fun AnimatedViewerCount(
    count: Int,
    color: Color = SamtchTheme.colors.accentColor,
    fontSize: TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(fontSize.value.dp * 1.2f)
        )
        
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                } else {
                    (slideInVertically { height -> -height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> height } + fadeOut())
                }.using(
                    SizeTransform(clip = false)
                )
            },
            label = "ViewerCountAnimation"
        ) { targetCount ->
            Text(
                text = formatViewerCount(targetCount),
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1
            )
        }
    }
}
