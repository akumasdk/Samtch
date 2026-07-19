package com.akumasdk.samtch.util

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String
)

/**
 * FOSS stub for UpdateManager. In-app updates are not allowed in F-Droid.
 */
object UpdateManager {
    suspend fun checkForUpdate(): GitHubRelease? {
        // No-op for FOSS builds
        return null
    }

    fun downloadAndInstall(context: Context, release: GitHubRelease) {
        // No-op for FOSS builds
    }
}
