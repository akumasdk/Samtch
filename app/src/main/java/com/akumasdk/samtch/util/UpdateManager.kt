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
            val latestVersion = release.tagName.removePrefix("v").trim()
            val currentVersion = BuildConfig.VERSION_NAME.trim()

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
        return try {
            val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

            val size = maxOf(currentParts.size, latestParts.size)
            for (i in 0 until size) {
                val c = currentParts.getOrNull(i) ?: 0
                val l = latestParts.getOrNull(i) ?: 0
                if (l > c) return true
                if (l < c) return false
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Version comparison failed", e)
            false
        }
    }

    fun downloadAndInstall(context: Context, release: GitHubRelease) {
        val appContext = context.applicationContext
        val apkAsset = release.assets.find { it.name.endsWith(".apk") } ?: run {
            Log.e(TAG, "No APK asset found in release")
            return
        }
        
        val downloadUrl = apkAsset.downloadUrl
        val fileName = apkAsset.name

        Log.d(TAG, "Starting download for $fileName from $downloadUrl")

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(appContext.getString(R.string.update_download_title))
            .setDescription(appContext.getString(R.string.update_download_description))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue download", e)
            return
        }

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    Log.d(TAG, "Download complete (ID: $id). Querying for local file...")
                    
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val localUriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        cursor.close()
                        
                        if (localUriString != null) {
                            val localUri = Uri.parse(localUriString)
                            val path = localUri.path
                            if (path != null) {
                                val file = File(path)
                                if (file.exists()) {
                                    Log.d(TAG, "File found at $path. Triggering installation...")
                                    installApk(appContext, file)
                                } else {
                                    Log.e(TAG, "File does not exist at path: $path")
                                }
                            }
                        }
                    } else {
                        cursor.close()
                        Log.e(TAG, "Failed to query download status")
                    }

                    try {
                        appContext.unregisterReceiver(this)
                    } catch (e: Exception) {
                        Log.w(TAG, "Receiver already unregistered")
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Log.d(TAG, "Installation intent started for URI: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start installation intent", e)
        }
    }
}
