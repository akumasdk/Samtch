package com.akumasdk.samtch.ui.components.chat.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.api.helix.dto.UserDto
import com.akumasdk.samtch.ui.components.chat.emote.InfoRow
import com.akumasdk.samtch.ui.theme.SamtchTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoDialog(
    user: UserDto,
    onDismiss: () -> Unit,
) {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
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
                    .data(user.avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = user.displayName,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SamtchTheme.colors.primaryText
                )
                if (user.name.lowercase() != user.displayName.lowercase()) {
                    Text(
                        text = user.name,
                        fontSize = 14.sp,
                        color = SamtchTheme.colors.secondaryText
                    )
                }
            }

            if (!user.description.isNullOrEmpty()) {
                Text(
                    text = user.description,
                    fontSize = 14.sp,
                    color = SamtchTheme.colors.primaryText,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 3
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoRow(
                    label = "Created At", 
                    value = user.createdAt.substringBefore("T")
                )
                InfoRow(
                    label = "Broadcaster Type", 
                    value = user.broadcasterType.ifEmpty { "User" }.replaceFirstChar { it.uppercase() }
                )
                InfoRow(
                    label = "View Count", 
                    value = String.format(locale, "%,d", user.viewCount)
                )
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
