package com.syncmyshit.app.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.syncmyshit.app.MainActivity
import com.syncmyshit.app.R

object NotificationHelper {

    const val CHANNEL_SYNC = "sync_channel"
    const val CHANNEL_SERVICE = "service_channel"

    const val NOTIFICATION_ID_SERVICE = 1001
    const val NOTIFICATION_ID_SYNC = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.notif_channel_sync),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_sync_desc)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                context.getString(R.string.notif_channel_service),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = context.getString(R.string.notif_channel_service_desc)
            }

            notificationManager.createNotificationChannel(syncChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildForegroundServiceNotification(context: Context, statusText: String): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle("syncMyShit Active")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    fun showSyncNotification(
        context: Context,
        title: String,
        message: String,
        progress: Int? = null,
        maxProgress: Int? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        if (progress != null && maxProgress != null) {
            builder.setProgress(maxProgress, progress, false)
        }

        notificationManager.notify(NOTIFICATION_ID_SYNC, builder.build())
    }
}
