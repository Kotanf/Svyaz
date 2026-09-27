package ru.maximnosko.svyaz.data

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
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
        subId: Int? = null
    ): TimelineEvent.Message = withContext(Dispatchers.IO) {
        
        if (address == "Неизвестный" || address.isBlank()) {
            throw Exception("Некорректный номер получателя")
        }

        if (!complianceManager.auditContent(content)) {
            Log.w("MessagingRepository", "Blocked suspicious content directed to $address")
            throw Exception("Сообщение заблокировано фильтром безопасности")
        }

        var status: MessageStatus
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
                Log.d("MessagingRepository", "Sending MMS to $address via MmsService")
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
                    context, 0, Intent("SMS_SENT").apply { setPackage(context.packageName) }, PendingIntent.FLAG_IMMUTABLE
                )
                
                activeSmsManager.sendTextMessage(address, null, content, sentIntent, null)
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
                Log.e("MessagingRepository", "SMS sending failed for $address", e)
                throw Exception("Ошибка при отправке SMS: ${e.message}")
            }
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
