package com.akumasdk.samtch.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.akumasdk.samtch.BuildConfig
import com.akumasdk.samtch.R
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

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

object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val REPO_URL = "https://api.github.com/repos/akumasdk/Samtch/releases/latest"

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    suspend fun checkForUpdate(): GitHubRelease? {
        return try {
            val release: GitHubRelease = client.get(REPO_URL).body()
            val latestVersion = release.tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME

            Log.d(TAG, "Latest version: $latestVersion, Current version: $currentVersion")

            if (isNewerVersion(currentVersion, latestVersion)) {
                release
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            null
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(currentParts.size, latestParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }

    fun downloadAndInstall(context: Context, release: GitHubRelease) {
        val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: return
        val downloadUrl = apkAsset.downloadUrl
        val fileName = apkAsset.name

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(context.getString(R.string.update_download_title))
            .setDescription(context.getString(R.string.update_download_description))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    val file = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )
                    installApk(context, file)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Already unregistered
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(context: Context, file: File) {
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
