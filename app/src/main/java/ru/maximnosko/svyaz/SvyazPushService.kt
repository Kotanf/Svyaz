package ru.maximnosko.svyaz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import ru.rustore.sdk.pushclient.messaging.exception.RuStorePushClientException
import ru.rustore.sdk.pushclient.messaging.model.RemoteMessage
import ru.rustore.sdk.pushclient.messaging.service.RuStoreMessagingService

/**
 * SvyazPushService integrates with RuStore Push SDK to handle remote notifications.
 */
class SvyazPushService : RuStoreMessagingService() {

    override fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) Log.d("SvyazPush", "Received new push token")
        try {
            val prefs = getSharedPreferences("svyaz_app_prefs", MODE_PRIVATE)
            prefs.edit { putString("rustore_push_token", token) }
        } catch (e: Exception) {
            Log.e("SvyazPush", "Failed to save push token", e)
        }
    }

    override fun onError(errors: List<RuStorePushClientException>) {
        for (error in errors) {
            Log.w("SvyazPush", "RuStore Push Service error handled: ${error.message}", error)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("SvyazPush", "Push Message ID: ${message.messageId}")
        
        val title = message.notification?.title ?: "Связь"
        val body = message.notification?.body ?: ""
        
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "push_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Уведомления сервиса", NotificationManager.IMPORTANCE_DEFAULT).apply {
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
