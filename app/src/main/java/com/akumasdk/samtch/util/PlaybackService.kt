package com.akumasdk.samtch.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.akumasdk.samtch.MainActivity
import com.akumasdk.samtch.R

/**
 * Service to keep the app alive during background playback.
 * The actual audio is played by the WebView in MainActivity.
 */
class PlaybackService : Service() {

    companion object {
        private const val TAG = "PlaybackService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "playback_channel"
        
        const val ACTION_START = "com.akumasdk.samtch.ACTION_START"
        const val ACTION_STOP = "com.akumasdk.samtch.ACTION_STOP"
        const val EXTRA_CHANNEL_NAME = "channel_name"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Twitch"
                startForegroundService(channelName)
            }
            ACTION_STOP -> {
                Log.d(TAG, "Stopping service")
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService(channelName: String) {
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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_refresh)
            .setContentTitle(channelName)
            .setContentText("Reproduciendo en segundo plano")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Increased from LOW
            .setOngoing(true)
            .addAction(R.drawable.ic_refresh, "Detener", stopPendingIntent) // Added icon
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Make visible on lockscreen
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

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Delete old channel if it was low priority
        if (notificationManager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_LOW) {
            Log.d(TAG, "Deleting low priority notification channel")
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
        }

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            Log.d(TAG, "Creating notification channel")
            val name = "Reproducción"
            val descriptionText = "Notificaciones de reproducción en segundo plano"
            val importance = NotificationManager.IMPORTANCE_DEFAULT // Increased from LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
