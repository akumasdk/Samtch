package com.akumasdk.samtch.ui.components.chat.emote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.emote.Emote
import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoteInfoDialog(
    emote: Emote,
    onDismiss: () -> Unit,
    onUseEmote: (Emote) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SamtchTheme.colors.dialogBackground,
        contentColor = SamtchTheme.colors.primaryText,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SamtchTheme.colors.secondaryText) }
    ) {
        com.akumasdk.samtch.util.MaintainFullscreenEffect()
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
                InfoRow(label = stringResource(R.string.emote_info_source), value = emote.type.name)
                InfoRow(label = stringResource(R.string.emote_info_id), value = emote.id)
                if (emote.isZeroWidth) {
                    InfoRow(label = stringResource(R.string.emote_info_type), value = stringResource(R.string.emote_info_zero_width))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        onUseEmote(emote)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamtchTheme.colors.twitchPurple,
                        contentColor = SamtchTheme.colors.primaryText
                    ),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.emote_info_use))
                }

                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(emote.code))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SamtchTheme.colors.primaryText
                    ),
                    border = BorderStroke(1.dp, SamtchTheme.colors.divider),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.emote_info_copy))
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.close_button), color = SamtchTheme.colors.secondaryText)
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
