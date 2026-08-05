package com.akumasdk.samtch.util

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.akumasdk.samtch.R

object PipManager {
    @RequiresApi(Build.VERSION_CODES.S)
    fun getPipParams(
        context: Context,
        isPipEnabled: Boolean,
        currentChannel: String?,
        isAudioOnly: Boolean,
        isInPipMode: Boolean
    ): PictureInPictureParams {
        val actions = if (currentChannel != null && isInPipMode) {
            listOf(
                RemoteAction(
                    Icon.createWithResource(context, R.drawable.ic_refresh),
                    context.getString(R.string.pip_action_refresh),
                    context.getString(R.string.pip_action_refresh_description),
                    PendingIntent.getBroadcast(
                        context, 0, Intent(Constants.Actions.REFRESH).setPackage(context.packageName), PendingIntent.FLAG_IMMUTABLE
                    )
                )
            )
        } else {
            emptyList()
        }

        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions)
            .setAutoEnterEnabled(currentChannel != null && isPipEnabled && !isAudioOnly)
            .build()
    }
}
