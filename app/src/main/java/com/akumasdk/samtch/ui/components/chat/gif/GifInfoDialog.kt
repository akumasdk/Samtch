package com.akumasdk.samtch.ui.components.chat.gif

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.akumasdk.samtch.R
import com.akumasdk.samtch.ui.components.chat.emote.InfoRow
import com.akumasdk.samtch.ui.theme.SamtchTheme
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifInfoDialog(
    url: String,
    isFullscreen: Boolean = false,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
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
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.gif_info_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isFullscreen) 220.dp else 320.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                text = stringResource(R.string.gif_info_title),
                fontSize = if (isFullscreen) 18.sp else 20.sp,
                fontWeight = FontWeight.Bold,
                color = SamtchTheme.colors.primaryText
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                InfoRow(label = stringResource(R.string.gif_info_source), value = "Twitch")
                Text(
                    text = url,
                    color = SamtchTheme.colors.secondaryText,
                    fontSize = 12.sp
                )
            }

            OutlinedButton(
                onClick = { clipboardManager.setText(AnnotatedString(url)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SamtchTheme.colors.primaryText),
                border = BorderStroke(1.dp, SamtchTheme.colors.divider)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.gif_info_copy_url))
            }

            if (!isFullscreen) {
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.close_button), color = SamtchTheme.colors.secondaryText)
                }
            }
        }
    }
}
