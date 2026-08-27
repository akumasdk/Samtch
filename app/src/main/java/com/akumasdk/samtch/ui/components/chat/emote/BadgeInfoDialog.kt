package com.akumasdk.samtch.ui.components.chat.emote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.badge.TwitchBadgeDto
import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeInfoDialog(
    badge: TwitchBadgeDto,
    onDismiss: () -> Unit,
) {
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
                    .data(badge.bestUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = badge.title,
                modifier = Modifier
                    .size(64.dp)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = badge.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SamtchTheme.colors.primaryText
            )

            if (badge.description.isNotEmpty()) {
                Text(
                    text = badge.description,
                    fontSize = 14.sp,
                    color = SamtchTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoRow(label = stringResource(R.string.emote_info_id), value = badge.setID)
                InfoRow(label = "Version", value = badge.version)
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
