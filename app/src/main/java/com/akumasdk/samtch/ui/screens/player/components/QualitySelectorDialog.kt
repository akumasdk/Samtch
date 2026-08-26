package com.akumasdk.samtch.ui.screens.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.akumasdk.samtch.ui.theme.SamtchTheme
import com.akumasdk.samtch.util.ExtMediaEntry

@Composable
fun QualitySelectorDialog(
    availableQualities: List<ExtMediaEntry>,
    selectedQuality: ExtMediaEntry?,
    onQualitySelected: (ExtMediaEntry) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(availableQualities) {
        availableQualities.forEachIndexed { index, quality ->
            android.util.Log.d("QualitySelector", "Quality [$index]: name=${quality.name}, res=${quality.resolution}, bw=${quality.bandwidth}, fps=${quality.frameRate}, url=${quality.playlistUrl}")
        }
    }

    // Filter out invalid entries and sort them properly
    val filteredQualities = remember(availableQualities) {
        val result = availableQualities
            .filter { 
                val hasIdentifier = !it.name.isNullOrBlank() || !it.resolution.isNullOrBlank()
                val isNotUnknown = it.name?.contains("Unknown", ignoreCase = true) != true
                val hasUrl = !it.playlistUrl.isNullOrBlank()
                hasIdentifier && isNotUnknown && hasUrl
            }
            .sortedByDescending { it.bandwidth ?: 0L }
            .distinctBy { it.playlistUrl }

        // Fallback: If filtering is too aggressive, show all entries with URLs
        if (result.isEmpty() && availableQualities.isNotEmpty()) {
            availableQualities.filter { !it.playlistUrl.isNullOrBlank() }
        } else {
            result
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SamtchTheme.colors.dialogBackground,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Stream Quality",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SamtchTheme.colors.primaryText,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(filteredQualities) { quality ->
                        val isSelected = quality.playlistUrl == selectedQuality?.playlistUrl
                        
                        QualityItem(
                            quality = quality,
                            isSelected = isSelected,
                            onClick = {
                                onQualitySelected(quality)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SamtchTheme.colors.twitchPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QualityItem(
    quality: ExtMediaEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayName = remember(quality) {
        val name = quality.name ?: ""
        val resolution = quality.resolution ?: ""
        
        when {
            name.contains("source", ignoreCase = true) -> name
            resolution.isNotBlank() -> {
                val height = resolution.split('x').lastOrNull()
                if (height != null) "${height}p" else resolution
            }
            name.isNotBlank() -> name
            else -> "Unknown"
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) SamtchTheme.colors.twitchPurple.copy(alpha = 0.2f) 
                else SamtchTheme.colors.cardBackground.copy(alpha = 0.05f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, SamtchTheme.colors.twitchPurple) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) SamtchTheme.colors.twitchPurpleLight else SamtchTheme.colors.primaryText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                
                val detailText = mutableListOf<String>()
                if (quality.resolution != null && !displayName.contains(quality.resolution!!)) {
                    detailText.add(quality.resolution!!)
                }
                if (quality.frameRate != null && quality.frameRate!! > 0) {
                    detailText.add("${quality.frameRate!!.toInt()} fps")
                }
                
                if (detailText.isNotEmpty()) {
                    Text(
                        text = detailText.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) SamtchTheme.colors.twitchPurpleLight.copy(alpha = 0.7f)
                                else SamtchTheme.colors.secondaryText
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(24.dp),
                    tint = SamtchTheme.colors.twitchPurpleLight
                )
            }
        }
    }
}
