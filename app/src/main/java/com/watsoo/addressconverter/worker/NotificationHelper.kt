package com.watsoo.addressconverter.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.watsoo.addressconverter.R
import com.watsoo.addressconverter.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "address_conversion_channel"
    private const val CHANNEL_NAME = "Address Conversion"
    const val NOTIFICATION_ID = 42

    fun createNotification(
        context: Context,
        title: String,
        message: String,
        processedCount: Int = 0,
        totalCount: Int = 0
    ): Notification {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Cancel action
        val cancelIntent = Intent(context, CancelWorkReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 1, cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(0, "Cancel", cancelPendingIntent)

        if (totalCount > 0) {
            builder.setProgress(totalCount, processedCount, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                android.util.Log.d("NotificationHelper", "Creating notification channel: $CHANNEL_ID")
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
                android.util.Log.d("NotificationHelper", "Notification channel created")
            }
        }
    }
}
