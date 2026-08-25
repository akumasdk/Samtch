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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SamtchTheme.colors.dialogBackground,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Stream Quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = SamtchTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableQualities.sortedByDescending { it.bandwidth ?: 0L }) { quality ->
                        val isSelected = quality == selectedQuality
                        
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

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = SamtchTheme.colors.twitchPurpleLight)
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SamtchTheme.colors.twitchPurple.copy(alpha = 0.15f) 
                else Color.Transparent,
        contentColor = if (isSelected) SamtchTheme.colors.twitchPurpleLight 
                      else SamtchTheme.colors.primaryText,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = quality.name ?: quality.resolution ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                if (quality.resolution != null && quality.name != quality.resolution) {
                    Text(
                        text = quality.resolution!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) SamtchTheme.colors.twitchPurpleLight.copy(alpha = 0.7f)
                                else SamtchTheme.colors.secondaryText
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SamtchTheme.colors.twitchPurpleLight
                )
            }
        }
    }
}
