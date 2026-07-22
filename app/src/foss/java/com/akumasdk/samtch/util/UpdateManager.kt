package com.akumasdk.samtch.util

import android.content.Context
import com.akumasdk.samtch.data.model.GitHubRelease
import com.akumasdk.samtch.data.model.GitHubAsset

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
