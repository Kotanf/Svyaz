package ru.maximnosko.svyaz.data

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import ru.maximnosko.svyaz.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.maximnosko.svyaz.MmsService
import kotlin.time.Duration.Companion.seconds

/**
 * MessagingRepository handles reliable communication.
 * Enhanced: Outgoing SMS are now manually inserted into the system's 'Sent' folder to ensure visibility.
 * Added error tracking for SMS dispatch.
 */
class MessagingRepository(private val context: Context) {

    private val smsManager: SmsManager by lazy {
        context.getSystemService(SmsManager::class.java)
    }

    private val complianceManager = ComplianceManager(context)

    suspend fun sendMessage(
        address: String, 
        content: String, 
        isMms: Boolean = false,
        mediaUri: String? = null,
        mediaType: MediaType? = null,
        subId: Int? = null,
        requestDeliveryReport: Boolean = false,
        messageId: String = java.util.UUID.randomUUID().toString(),
        threadId: String? = null
    ): TimelineEvent.Message = withContext(Dispatchers.IO) {
        
        if (address == "Неизвестный" || address.isBlank()) {
            throw Exception("Некорректный номер получателя")
        }

        if (content.isBlank() && !isMms && mediaUri == null) {
            throw Exception("Сообщение не может быть пустым")
        }

        if (!complianceManager.auditContent(content)) {
            Log.w("MessagingRepository", "Blocked suspicious content")
            throw Exception("Сообщение заблокировано фильтром безопасности")
        }

        var status: MessageStatus = MessageStatus.SENT
        val timestamp = System.currentTimeMillis()
        
        val activeSmsManager = if (subId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager.createForSubscriptionId(subId)
        } else if (subId != null) {
            @Suppress("DEPRECATION")
            SmsManager.getSmsManagerForSubscriptionId(subId)
        } else {
            smsManager
        }

        if (isMms) {
            try {
                if (BuildConfig.DEBUG) Log.d("MessagingRepository", "Sending MMS via MmsService")
                val mmsIntent = Intent(context, MmsService::class.java).apply {
                    putExtra("phoneNumber", address)
                    putExtra("message", content)
                    putExtra("imageUri", mediaUri)
                }
                context.startService(mmsIntent)
                status = MessageStatus.SENT
            } catch (e: Exception) {
                Log.e("MessagingRepository", "MMS failed", e)
                status = MessageStatus.ERROR
                throw Exception("Ошибка при отправке MMS: ${e.message}")
            }
        } else {
            try {
                val sentIntent = PendingIntent.getBroadcast(
                    context, 
                    messageId.hashCode(), 
                    Intent("ru.maximnosko.svyaz.SMS_SENT").apply { 
                        setPackage(context.packageName)
                        putExtra("messageId", messageId)
                    }, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val deliveryIntent = if (requestDeliveryReport) {
                    val intent = Intent(context, ru.maximnosko.svyaz.SmsDeliveryReceiver::class.java).apply {
                        action = "ru.maximnosko.svyaz.SMS_DELIVERED"
                        putExtra("messageId", messageId)
                        putExtra("threadId", threadId)
                        putExtra("address", address)
                    }
                    PendingIntent.getBroadcast(
                        context,
                        (messageId + "_delivery").hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                } else null
                
                val parts = activeSmsManager.divideMessage(content)
                if (parts != null && parts.size > 1) {
                    val sentIntents = ArrayList<PendingIntent>()
                    val deliveryIntents = ArrayList<PendingIntent>()
                    for (i in parts.indices) {
                        sentIntents.add(sentIntent)
                        if (deliveryIntent != null) {
                            deliveryIntents.add(deliveryIntent)
                        }
                    }
                    activeSmsManager.sendMultipartTextMessage(
                        address,
                        null,
                        parts,
                        sentIntents,
                        if (deliveryIntent != null) deliveryIntents else null
                    )
                } else {
                    activeSmsManager.sendTextMessage(address, null, content, sentIntent, deliveryIntent)
                }
                status = MessageStatus.SENT
                
                // CRITICAL: Manually insert into System 'Sent' folder
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, content)
                    put(Telephony.Sms.DATE, timestamp)
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            } catch (e: Exception) {
                status = MessageStatus.ERROR
                Log.e("MessagingRepository", "SMS sending failed", e)
                throw Exception("Ошибка при отправке SMS: ${e.message}")
            }
        }

        // Save into local Room DB
        try {
            val db = ru.maximnosko.svyaz.data.local.SvyazDatabase.getDatabase(context)
            val resolvedThreadId = threadId ?: address
            db.threadDao().insertMessage(
                ru.maximnosko.svyaz.data.local.MessageEntity(
                    id = messageId,
                    threadId = resolvedThreadId,
                    timestamp = timestamp,
                    content = content,
                    isFromMe = true,
                    isEncrypted = false,
                    isEdited = false,
                    status = status.name,
                    mediaUri = mediaUri,
                    mediaType = mediaType?.name
                )
            )
        } catch (e: Exception) {
            Log.e("MessagingRepository", "Failed to cache message locally", e)
        }

        complianceManager.logEvent(
            type = if (isMms) "MMS" else "SMS",
            direction = "OUTGOING",
            sender = "ME",
            recipient = address,
            content = content,
            status = status.name,
            metadata = if (mediaUri != null) "Media: $mediaUri" else "Cellular"
        )

        TimelineEvent.Message(
            id = messageId,
            timestamp = timestamp,
            content = content, 
            isFromMe = true,
            isPending = false,
            status = status,
            isMms = isMms,
            isEncrypted = false, 
            mediaUri = mediaUri,
            mediaType = mediaType
        )
    }
}
