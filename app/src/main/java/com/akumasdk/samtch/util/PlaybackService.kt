package com.akumasdk.samtch.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.akumasdk.samtch.MainActivity
import com.akumasdk.samtch.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Service to keep the app alive during background playback.
 * The actual audio is played by the WebView in MainActivity.
 */
class PlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastChannelName = "Twitch"
    private var lastStreamTitle = ""
    private var isAppInForeground = true
    private var lastLargeIcon: Bitmap? = null

    companion object {
        private const val TAG = "PlaybackService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "playback_channel"
        
        const val ACTION_START = "com.akumasdk.samtch.ACTION_START"
        const val ACTION_STOP = "com.akumasdk.samtch.ACTION_STOP"
        const val ACTION_STOP_PLAYER = "com.akumasdk.samtch.STOP_PLAYER"
        const val ACTION_UPDATE_METADATA = "com.akumasdk.samtch.ACTION_UPDATE_METADATA"
        const val ACTION_UPDATE_VISIBILITY = "com.akumasdk.samtch.ACTION_UPDATE_VISIBILITY"
        
        const val EXTRA_CHANNEL_NAME = "channel_name"
        const val EXTRA_STREAM_TITLE = "stream_title"
        const val EXTRA_AVATAR_URL = "avatar_url"
        const val EXTRA_IS_FOREGROUND = "is_foreground"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand: action=$action")
        
        if (action == ACTION_START) {
            // Immediate foreground start to comply with Android 14+
            val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Twitch"
            lastChannelName = channelName
            startForegroundService(channelName)
            
            // Then validate settings asynchronously
            validateSettingsAndStopIfNeeded()
        } else if (action == ACTION_UPDATE_METADATA || action == ACTION_UPDATE_VISIBILITY) {
            serviceScope.launch {
                if (isSettingEnabled()) {
                    handleAction(intent)
                } else {
                    Log.d(TAG, "Setting disabled, stopping service")
                    stopForeground(true)
                    stopSelf()
                }
            }
        } else if (action == ACTION_STOP) {
            handleAction(intent)
        }
        
        return START_STICKY
    }

    private suspend fun isSettingEnabled(): Boolean = withContext(Dispatchers.IO) {
        SettingsManager.isBackgroundPlayEnabled(applicationContext).first()
    }

    private fun validateSettingsAndStopIfNeeded() {
        serviceScope.launch {
            if (!isSettingEnabled()) {
                Log.d(TAG, "Background play disabled by settings, stopping service after start")
                stopForeground(true)
                stopSelf()
            }
        }
    }

    private fun handleAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_START -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Twitch"
                lastChannelName = channelName
                startForegroundService(channelName)
            }
            ACTION_UPDATE_METADATA -> {
                val avatarUrl = intent.getStringExtra(EXTRA_AVATAR_URL)
                val streamTitle = intent.getStringExtra(EXTRA_STREAM_TITLE)
                
                if (streamTitle != null) {
                    lastStreamTitle = streamTitle
                }
                
                if (avatarUrl != null) {
                    updateNotificationMetadata(avatarUrl)
                } else {
                    startForegroundService(lastChannelName, lastLargeIcon)
                }
            }
            ACTION_UPDATE_VISIBILITY -> {
                isAppInForeground = intent.getBooleanAsDefault(EXTRA_IS_FOREGROUND, true)
                startForegroundService(lastChannelName, lastLargeIcon)
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping service and player")
                sendBroadcast(Intent(ACTION_STOP_PLAYER).setPackage(packageName))
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startForegroundService(channelName: String, largeIcon: Bitmap? = null) {
        lastLargeIcon = largeIcon
        Log.d(TAG, "startForegroundService for $channelName")
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_CHANNEL_NAME, channelName)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, PlaybackService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val statusSuffix = if (!isAppInForeground) " • ${getString(R.string.bg_play_status_background)}" else ""
        val infoText = if (isAppInForeground) {
            getString(R.string.bg_play_now_playing)
        } else {
            "${getString(R.string.bg_play_notification_text)}$statusSuffix"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(channelName)
            .setContentText(lastStreamTitle.ifEmpty { getString(R.string.bg_play_now_playing) })
            .setSubText(infoText)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(stopPendingIntent) // Stop playback when dismissed
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true) // Prevent dismissal while playing
            .addAction(R.drawable.ic_refresh, getString(R.string.bg_play_stop_action), stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Calling startForeground with mediaPlayback type")
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                Log.d(TAG, "Calling startForeground (Legacy)")
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun Intent.getBooleanAsDefault(key: String, defaultValue: Boolean): Boolean {
        return if (hasExtra(key)) getBooleanExtra(key, defaultValue) else defaultValue
    }

    private fun updateNotificationMetadata(avatarUrl: String) {
        serviceScope.launch {
            val bitmap = downloadBitmap(avatarUrl)
            if (bitmap != null) {
                Log.d(TAG, "Updating notification with avatar bitmap")
                startForegroundService(lastChannelName, bitmap)
            }
        }
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading avatar", e)
            null
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Delete old channel if it was low priority
        if (notificationManager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_LOW) {
            Log.d(TAG, "Deleting low priority notification channel")
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        }

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            Log.d(TAG, "Creating notification channel")
            val name = getString(R.string.bg_play_channel_name)
            val descriptionText = getString(R.string.bg_play_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Increased from LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
