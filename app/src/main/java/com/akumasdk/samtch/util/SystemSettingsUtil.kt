package com.akumasdk.samtch.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object SystemSettingsUtil {
    fun observeAutoRotate(context: Context): Flow<Boolean> = callbackFlow {
        val contentResolver = context.contentResolver
        val settingUri = Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION)

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val isEnabled = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
                trySend(isEnabled)
            }
        }

        contentResolver.registerContentObserver(settingUri, false, observer)

        // Send initial value
        val initialValue = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        trySend(initialValue)

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }
}
