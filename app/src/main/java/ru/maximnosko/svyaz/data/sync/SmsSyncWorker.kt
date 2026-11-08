package ru.maximnosko.svyaz.data.sync

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.maximnosko.svyaz.data.SystemDataProvider
import ru.maximnosko.svyaz.data.local.MessageEntity
import ru.maximnosko.svyaz.data.local.SvyazDatabase
import ru.maximnosko.svyaz.data.local.ThreadDao
import ru.maximnosko.svyaz.data.local.ThreadEntity

class SmsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val appContext = applicationContext
        val database = SvyazDatabase.getDatabase(appContext)
        val dao: ThreadDao = database.threadDao()
        val systemDataProvider = SystemDataProvider(appContext)

        try {
            // 1. Sync system SMS threads into local database
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

            // 2. Query system SMS provider and sync messages into local database
            val smsUri: Uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
            )

            val cursor = contentResolver.query(
                smsUri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1000"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(Telephony.Sms._ID)
                val threadIdIdx = c.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)

                val messagesList = mutableListOf<MessageEntity>()

                while (c.moveToNext()) {
                    val smsId = if (idIdx != -1) c.getString(idIdx) ?: System.currentTimeMillis().toString() else System.currentTimeMillis().toString()
                    val rawThreadId = if (threadIdIdx != -1) c.getString(threadIdIdx) else null
                    val address = if (addressIdx != -1) c.getString(addressIdx) ?: "Неизвестный" else "Неизвестный"
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx != -1) c.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX

                    val finalThreadId = if (!rawThreadId.isNullOrBlank() && rawThreadId != "0") {
                        rawThreadId
                    } else if (address != "Неизвестный" && address.isNotBlank()) {
                        address
                    } else {
                        "0"
                    }

                    messagesList.add(
                        MessageEntity(
                            id = smsId,
                            threadId = finalThreadId,
                            timestamp = timestamp,
                            content = body,
                            isFromMe = (type == Telephony.Sms.MESSAGE_TYPE_SENT),
                            isEncrypted = false,
                            isEdited = false,
                            status = "SENT",
                            mediaUri = null,
                            mediaType = null
                        )
                    )
                }

                if (messagesList.isNotEmpty()) {
                    dao.insertMessages(messagesList)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SmsSyncWorker", "Failed to sync system SMS", e)
            Result.retry()
        }
    }
}
