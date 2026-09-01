package com.akumasdk.samtch.ui.components.chat.emote

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.data.badge.TwitchBadgeDto
import com.akumasdk.samtch.ui.theme.SamtchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeInfoDialog(
    badge: TwitchBadgeDto,
    isFullscreen: Boolean = false,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SamtchTheme.colors.dialogBackground,
        contentColor = SamtchTheme.colors.primaryText,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SamtchTheme.colors.secondaryText) },
        contentWindowInsets = { if (isFullscreen) WindowInsets(0) else BottomSheetDefaults.windowInsets }
    ) {
        if (isFullscreen) {
            val view = LocalView.current
            DisposableEffect(view) {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null) {
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                onDispose {}
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    bottom = if (isFullscreen) 16.dp else 32.dp, 
                    start = 16.dp, 
                    end = 16.dp
                )
                .then(if (isFullscreen) Modifier.widthIn(max = 450.dp) else Modifier)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isFullscreen) 12.dp else 16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(badge.bestUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = badge.title,
                modifier = Modifier
                    .size(if (isFullscreen) 48.dp else 64.dp)
                    .padding(if (isFullscreen) 4.dp else 8.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = badge.title,
                fontSize = if (isFullscreen) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = SamtchTheme.colors.primaryText
            )

            if (badge.description.isNotEmpty()) {
                Text(
                    text = badge.description,
                    fontSize = 14.sp,
                    color = SamtchTheme.colors.secondaryText,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = if (isFullscreen) 2 else Int.MAX_VALUE
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoRow(label = stringResource(R.string.emote_info_id), value = badge.setID)
                if (!isFullscreen) {
                    InfoRow(label = "Version", value = badge.version)
                }
            }

            if (!isFullscreen) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close_button), color = SamtchTheme.colors.secondaryText)
                }
            }
        }
    }
}
