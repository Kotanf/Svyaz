package ru.maximnosko.svyaz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.maximnosko.svyaz.data.ComplianceManager
import ru.maximnosko.svyaz.data.SystemDataProvider
import ru.maximnosko.svyaz.data.local.MessageEntity
import ru.maximnosko.svyaz.data.local.SvyazDatabase
import ru.maximnosko.svyaz.data.local.ThreadEntity

/**
 * SmsReceiver handles incoming SMS messages.
 * It ensures messages are saved to both the system database and local SvyazDatabase,
 * and logged for ORI compliance.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            try {
                handleIncomingSms(context, intent)
            } catch (e: Exception) {
                Log.e("SmsReceiver", "CRITICAL ERROR: Failed to process incoming SMS", e)
            }
        }
    }

    private fun handleIncomingSms(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w("SmsReceiver", "Received SMS_DELIVER intent but no messages found")
            return
        }

        val complianceManager = ComplianceManager(context)
        val appContext = context.applicationContext
        
        for (sms in messages) {
            val address = sms.displayOriginatingAddress ?: "Unknown"
            val body = sms.displayMessageBody ?: ""
            val timestamp = System.currentTimeMillis()
            
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
            }
            
            // 1. Guaranteed System DB insertion
            val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            if (uri == null) {
                Log.e("SmsReceiver", "FAILED to save SMS to system database")
            }

            // 2. User Notification
            showNotification(appContext, address, "Новое сообщение")

            // 3. Save to local SvyazDatabase and log ORI compliance
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = SvyazDatabase.getDatabase(appContext)
                    val dao = database.threadDao()
                    val systemDataProvider = SystemDataProvider(appContext)

                    val threadId = systemDataProvider.resolveAddress(address)
                        ?: try {
                            Telephony.Threads.getOrCreateThreadId(appContext, address).toString()
                        } catch (_: Exception) {
                            "0"
                        }

                    val smsId = uri?.lastPathSegment ?: System.currentTimeMillis().toString()

                    val localMessage = MessageEntity(
                        id = smsId,
                        threadId = threadId,
                        timestamp = timestamp,
                        content = body,
                        isFromMe = false,
                        isEncrypted = false,
                        isEdited = false,
                        status = "RECEIVED",
                        mediaUri = null,
                        mediaType = null
                    )
                    dao.insertMessage(localMessage)

                    // Sync threads list into DB so UI updates immediately
                    val domainThreads = systemDataProvider.fetchThreads()
                    if (domainThreads.isNotEmpty()) {
                        val threadEntities = domainThreads.map { thread ->
                            ThreadEntity(
                                id = thread.id,
                                contactName = thread.contactName,
                                phoneNumber = thread.phoneNumber,
                                lastMessage = thread.lastMessage,
                                lastMessageTime = thread.lastMessageTime,
                                unreadCount = thread.unreadCount,
                                isPinned = thread.isPinned,
                                isMachine = thread.isMachine,
                                category = thread.category.name,
                                folder = thread.folder
                            )
                        }
                        dao.insertThreads(threadEntities)
                    }

                    complianceManager.logEvent(
                        type = "SMS",
                        direction = "INCOMING",
                        sender = address,
                        recipient = "ME",
                        content = body
                    )
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Compliance or local DB logging failed for $address", e)
                }
            }
        }
    }

    private fun showNotification(context: Context, sender: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "incoming_sms"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Входящие сообщения", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(sender)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
