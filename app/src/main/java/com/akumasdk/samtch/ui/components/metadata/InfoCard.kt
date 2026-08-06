package com.akumasdk.samtch.ui.components.metadata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akumasdk.samtch.ui.theme.SamtchTheme

@Composable
fun InfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    maxLines: Int = 2,
    isScrollable: Boolean = false
) {
    Surface(
        color = SamtchTheme.colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SamtchTheme.colors.primaryText.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = SamtchTheme.colors.accentColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SamtchTheme.colors.primaryText.copy(alpha = 0.5f),
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }
            
            val textModifier = if (isScrollable) {
                Modifier
                    .heightIn(max = 120.dp)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 22.sp
                ),
                fontWeight = FontWeight.Bold,
                color = SamtchTheme.colors.primaryText,
                maxLines = if (isScrollable) Int.MAX_VALUE else maxLines,
                overflow = if (isScrollable) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = textModifier
            )
        }
    }
}
