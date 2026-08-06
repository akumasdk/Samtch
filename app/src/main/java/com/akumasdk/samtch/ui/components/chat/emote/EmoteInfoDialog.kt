package com.akumasdk.samtch.ui.components.chat.emote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteInfoDialog(
    emote: Emote,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SamtchTheme.colors.dialogBackground,
        contentColor = SamtchTheme.colors.primaryText,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SamtchTheme.colors.secondaryText) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(emote.url)
                    .crossfade(true)
                    .build(),
                contentDescription = emote.code,
                modifier = Modifier
                    .size(128.dp)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = emote.code,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SamtchTheme.colors.primaryText
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoRow(label = "Source", value = emote.type.name)
                InfoRow(label = "ID", value = emote.id)
                if (emote.isZeroWidth) {
                    InfoRow(label = "Type", value = "Zero-width overlay")
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SamtchTheme.colors.twitchPurpleLight,
                    contentColor = SamtchTheme.colors.primaryText
                )
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SamtchTheme.colors.secondaryText,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = SamtchTheme.colors.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
