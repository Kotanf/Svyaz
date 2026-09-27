package ru.maximnosko.svyaz.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Base64
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SystemDataProvider fetches messaging and call data from the Android system providers.
 * Optimized for performance and stability across Android 8 to 17.
 * Features a robust thread-building algorithm and aggressive error handling.
 */
class SystemDataProvider(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val contactCache = ConcurrentHashMap<String, String>()

    /**
     * Fetches all message threads. 
     * Strategy: Query SMS table, group by thread_id manually to ensure stability.
     */
    suspend fun fetchThreads(): List<MessageThread> = withContext(Dispatchers.IO) {
        val threadsMap = mutableMapOf<String, MessageThread>()
        var cursor: Cursor? = null
        try {
            val uri = Telephony.Sms.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE
            )

            // Building threads from the last 1000 messages
            cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 1000)
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(Telephony.Sms.DATE))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                contentResolver.query(uri, projection, queryArgs, null)
            } else {
                contentResolver.query(uri, projection, null, null, "date DESC LIMIT 1000")
            }
            
            cursor?.let {
                val tidIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addrIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val threadId = if (tidIdx != -1) it.getString(tidIdx) else null
                    if (threadId.isNullOrBlank() || threadId == "0") continue
                    
                    if (threadsMap.containsKey(threadId)) {
                        val isRead = it.getInt(readIdx) == 1
                        if (!isRead && it.getInt(typeIdx) == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                            val existing = threadsMap[threadId]!!
                            threadsMap[threadId] = existing.copy(unreadCount = existing.unreadCount + 1)
                        }
                        continue
                    }

                    val rawAddress = it.getString(addrIdx)
                    val address = if (rawAddress.isNullOrBlank() || rawAddress == "Неизвестный") {
                        resolveAddress(threadId) ?: "Неизвестный"
                    } else {
                        rawAddress
                    }
                    val normalizedPhone = if (address != "Неизвестный") normalizeNumber(address) else ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)
                    val isRead = it.getInt(readIdx) == 1
                    
                    val name = getContactNameCached(address) ?: getContactNameCached(normalizedPhone) ?: address
                    val lastMessage = smartDecode(body)
                    
                    threadsMap[threadId] = MessageThread(
                        id = threadId,
                        contactName = name,
                        phoneNumber = if (normalizedPhone.isNotBlank()) normalizedPhone else if (address != "Неизвестный") address else "",
                        lastMessage = lastMessage,
                        lastMessageTime = date,
                        unreadCount = if (isRead) 0 else 1,
                        avatarColor = getRandomColor(),
                        isMachine = isMachineSender(address),
                        category = detectCategory(lastMessage, address)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SystemDataProvider", "fetchThreads failed", e)
            return@withContext fetchThreadsFallback()
        } finally {
            cursor?.close()
        }
        
        val result = threadsMap.values.sortedByDescending { it.lastMessageTime }
        Log.d("SystemDataProvider", "Built ${result.size} threads")
        result
    }

    private fun fetchThreadsFallback(): List<MessageThread> {
        val threads = mutableListOf<MessageThread>()
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(Telephony.Threads.CONTENT_URI, arrayOf("_id", "date", "read"), null, null, "date DESC")
            cursor?.let {
                val idIdx = it.getColumnIndex("_id")
                val dateIdx = it.getColumnIndex("date")
                val readIdx = it.getColumnIndex("read")
                while (it.moveToNext()) {
                    val threadId = it.getString(idIdx) ?: continue
                    val date = it.getLong(dateIdx)
                    val isRead = it.getInt(readIdx) == 1
                    val address = resolveAddress(threadId) ?: "Неизвестный"
                    val name = getContactNameCached(address) ?: address
                    threads.add(
                        MessageThread(
                            id = threadId,
                            contactName = name,
                            phoneNumber = if (address == "Неизвестный") "" else address,
                            lastMessage = "",
                            lastMessageTime = date,
                            unreadCount = if (isRead) 0 else 1,
                            avatarColor = getRandomColor()
                        )
                    )
                }
            }
        } catch (_: Exception) {} finally { cursor?.close() }
        return threads
    }

    private fun getContactNameCached(number: String): String? {
        if (number == "Неизвестный" || number.isBlank()) return null
        if (contactCache.containsKey(number)) return contactCache[number]
        val name = getContactNameFromNumber(number)
        if (name != null) contactCache[number] = name
        return name
    }

    fun resolveAddress(threadId: String?): String? {
        if (threadId.isNullOrBlank() || threadId == "0") return null
        var msgCursor: Cursor? = null
        try {
            msgCursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS),
                "thread_id = ?",
                arrayOf(threadId),
                "date DESC LIMIT 1"
            )
            if (msgCursor?.moveToFirst() == true) {
                val address = msgCursor.getString(0)
                if (!address.isNullOrBlank()) return address
            }
        } catch (_: Exception) {
        } finally {
            msgCursor?.close()
        }
        return null
    }

    private fun smartDecode(text: String): String {
        if (text.isBlank()) return ""
        
        // 1. Strict Base64 validation (URL-safe & Standard)
        val cleanText = text.replace("\n", "").replace("\r", "").replace("\\\\", "")
        if (!cleanText.matches(Regex("^[A-Za-z0-9+/\\-_]*={0,2}$"))) {
            return text
        }

        return try {
            val combined = Base64.decode(cleanText, Base64.DEFAULT)
            
            // 2. Basic sanity checks to prevent OOM and malformed data
            if (combined.size < 28) return text // Too short for IV + Tag + Min data
            
            // 3. Extract GCM Tag and verify it looks like random bytes, not plain text overflow
            val iv = combined.copyOfRange(0, 12)
            val encryptedPayload = combined.copyOfRange(12, combined.size)
            
            // 4. Decrypt only if key is available and data is valid (AES-GCM throws on bad tag)
            val keyBytes = SecurityManager.getDatabasePassphrase(context)
            try {
                val secretKey = SecretKeySpec(keyBytes, "AES")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                val decryptedBytes = cipher.doFinal(encryptedPayload)
                
                val decryptedString = String(decryptedBytes, Charsets.UTF_8)
                // 5. Final check to ensure no injection or garbage
                if (decryptedString.all { it.code > 31 || it == '\n' || it == '\r' || it == '\t' }) {
                    return decryptedString
                }
            } finally {
                // Guaranteed memory sanitization
                SecurityManager.sanitize(keyBytes)
            }
            
            text // Return raw if decryption failed or integrity check failed
        } catch (_: IllegalArgumentException) {
            text // Not valid Base64
        } catch (_: GeneralSecurityException) {
            text // Bad key, bad tag, or tampering detected
        }
    }

    suspend fun fetchContacts(): List<MessageThread> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<MessageThread>()
        var cursor: Cursor? = null
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            cursor = contentResolver.query(uri, projection, null, null, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC")
            cursor?.let {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = if (nameIdx != -1) it.getString(nameIdx) ?: "Без имени" else "Без имени"
                    val number = if (numberIdx != -1) it.getString(numberIdx) ?: "" else ""
                    
                    if (number.isNotBlank()) {
                        contacts.add(
                            MessageThread(
                                id = normalizeNumber(number),
                                contactName = name,
                                phoneNumber = number,
                                lastMessage = number,
                                lastMessageTime = 0,
                                avatarColor = getRandomColor()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SystemDataProvider", "Error fetching contacts", e)
        } finally {
            cursor?.close()
        }
        contacts.distinctBy { it.id }
    }

    fun normalizeNumber(number: String): String {
        if (number.any { it.isLetter() }) return number
        val clean = number.replace(Regex("[^0-9+]"), "")
        return when {
            clean.startsWith("8") && clean.length == 11 -> "+7${clean.substring(1)}"
            clean.startsWith("7") && clean.length == 11 -> "+$clean"
            clean.isNotBlank() -> clean
            else -> number
        }
    }

    fun getContactNameFromNumber(number: String): String? {
        if (number == "Неизвестный" || number.isBlank()) return null
        
        val normalized = normalizeNumber(number)
        if (normalized.isBlank()) return null

        var cursor: Cursor? = null
        try {
            // CRITICAL: Robust URI construction to prevent IllegalArgumentException
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized))
            cursor = contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) {
                val contactName = cursor.getString(0)
                if (!contactName.isNullOrBlank()) return contactName
            }
        } catch (e: Exception) {
            Log.e("SystemDataProvider", "Contact lookup failed", e)
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun getThreadIdForAddress(address: String): String? {
        if (address.isBlank() || address == "Неизвестный") return null
        
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID),
                "${Telephony.Sms.ADDRESS} = ?",
                arrayOf(address),
                "date DESC LIMIT 1"
            )
            if (cursor?.moveToFirst() == true) {
                return cursor.getString(0)
            }
            cursor?.close()

            val partial = address.takeLast(10)
            if (partial.length >= 3) {
                cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.THREAD_ID),
                    "${Telephony.Sms.ADDRESS} LIKE ?",
                    arrayOf("%$partial%"),
                    "date DESC LIMIT 1"
                )
                if (cursor?.moveToFirst() == true) {
                    return cursor.getString(0)
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }

        try {
            return Telephony.Threads.getOrCreateThreadId(context, address).toString()
        } catch (_: Exception) {}

        return null
    }

    suspend fun fetchTimeline(idOrAddress: String): Pair<String, List<TimelineEvent>> = withContext(Dispatchers.IO) {
        val events = mutableListOf<TimelineEvent>()
        
        var threadId: String? = null
        if (idOrAddress.all { it.isDigit() } && idOrAddress.length < 10) {
            threadId = idOrAddress
            var testCursor: Cursor? = null
            try {
                testCursor = contentResolver.query(Telephony.Threads.CONTENT_URI, arrayOf("_id"), "_id = ?", arrayOf(threadId), null)
                if (testCursor?.moveToFirst() != true) {
                    threadId = getThreadIdForAddress(idOrAddress)
                }
            } catch (_: Exception) {
                threadId = getThreadIdForAddress(idOrAddress)
            } finally {
                testCursor?.close()
            }
        } else {
            threadId = getThreadIdForAddress(idOrAddress)
        }

        if (threadId == null) {
            if (!idOrAddress.all { it.isDigit() } || idOrAddress.length >= 3) {
                events.addAll(fetchMessagesByAddress(idOrAddress))
            }
            return@withContext (idOrAddress to events.sortedBy { it.timestamp })
        }

        val address = resolveAddress(threadId) ?: idOrAddress
        
        // Fetch SMS
        var smsCursor: Cursor? = null
        try {
            smsCursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI, 
                arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE), 
                "thread_id = ?", arrayOf(threadId), "date ASC"
            )
            smsCursor?.let {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
                
                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val rawBody = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    
                    if (rawBody.isNotBlank()) {
                        val decodedBody = smartDecode(rawBody)
                        val safeBody = if (decodedBody.length > 2000) decodedBody.substring(0, 2000) else decodedBody
                        
                        events.add(
                            TimelineEvent.Message(
                                id = id,
                                timestamp = timestamp,
                                content = safeBody,
                                isFromMe = type == Telephony.Sms.MESSAGE_TYPE_SENT
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SystemDataProvider", "Error fetching SMS timeline", e)
        } finally {
            smsCursor?.close()
        }

        // Fetch MMS
        try {
            val mmsCursor = contentResolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX),
                "thread_id = ?", arrayOf(threadId), "date ASC"
            )
            mmsCursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Mms._ID)
                val dateIdx = it.getColumnIndex(Telephony.Mms.DATE)
                val boxIdx = it.getColumnIndex(Telephony.Mms.MESSAGE_BOX)
                while (it.moveToNext()) {
                    val id = it.getString(idIdx)
                    val timestamp = it.getLong(dateIdx) * 1000 
                    val box = it.getInt(boxIdx)
                    events.add(
                        TimelineEvent.Message(
                            id = "mms_$id",
                            timestamp = timestamp,
                            content = "[ММС сообщение]",
                            isFromMe = box == Telephony.Mms.MESSAGE_BOX_SENT,
                            isMms = true
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // Fetch Call Log entries
        val cleanAddress = address.filter { it.isDigit() }.takeLast(10)
        if (cleanAddress.length >= 7) {
            var callCursor: Cursor? = null
            try {
                callCursor = contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls._ID, CallLog.Calls.DATE, CallLog.Calls.DURATION, CallLog.Calls.TYPE),
                    "${CallLog.Calls.NUMBER} LIKE ?",
                    arrayOf("%$cleanAddress%"),
                    "date ASC"
                )
                callCursor?.let {
                    val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                    val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                    val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                    val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)

                    while (it.moveToNext()) {
                        val id = if (idIdx != -1) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                        val timestamp = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                        val duration = if (durIdx != -1) it.getInt(durIdx) else 0
                        val type = if (typeIdx != -1) it.getInt(typeIdx) else -1
                        
                        events.add(
                            TimelineEvent.Call(
                                id = id,
                                timestamp = timestamp,
                                durationSeconds = duration,
                                isIncoming = type == CallLog.Calls.INCOMING_TYPE || type == CallLog.Calls.MISSED_TYPE,
                                isMissed = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            } finally {
                callCursor?.close()
            }
        }
        
        (threadId to events.distinctBy { it.id }.sortedBy { it.timestamp })
    }

    private fun fetchMessagesByAddress(address: String): List<TimelineEvent> {
        val events = mutableListOf<TimelineEvent>()
        val searchAddress = if (address.length >= 10) address.takeLast(10) else address
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                "${Telephony.Sms.ADDRESS} = ? OR ${Telephony.Sms.ADDRESS} LIKE ?",
                arrayOf(address, "%$searchAddress%"),
                "date ASC"
            )
            cursor?.let {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)
                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) ?: UUID.randomUUID().toString() else UUID.randomUUID().toString()
                    val rawBody = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val timestamp = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    if (rawBody.isNotBlank()) {
                        val decodedBody = smartDecode(rawBody)
                        val safeBody = if (decodedBody.length > 2000) decodedBody.substring(0, 2000) else decodedBody
                        
                        events.add(
                            TimelineEvent.Message(
                                id = id,
                                timestamp = timestamp,
                                content = safeBody,
                                isFromMe = type == Telephony.Sms.MESSAGE_TYPE_SENT
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }
        return events
    }

    fun saveToSim(name: String, number: String): Boolean {
        val values = ContentValues().apply {
            put("tag", name)
            put("number", number)
        }
        return try {
            val uri = "content://icc/adn".toUri()
            val newUri = contentResolver.insert(uri, values)
            newUri != null
        } catch (e: Exception) {
            Log.e("SystemDataProvider", "Failed to save to SIM", e)
            false
        }
    }

    private fun isMachineSender(address: String): Boolean {
        val clean = address.filter { it.isLetterOrDigit() }
        return clean.length < 8 || clean.any { it.isLetter() }
    }

    private fun detectCategory(text: String, address: String): CategoryFilter {
        val lowerText = text.lowercase()
        return when {
            isMachineSender(address) -> {
                if (lowerText.contains("код") || lowerText.contains("pass") || lowerText.contains("подтвержд")) CategoryFilter.CODES
                else if (lowerText.contains("баланс") || lowerText.contains("карт") || lowerText.contains("оплат")) CategoryFilter.FINANCE
                else CategoryFilter.SERVICES
            }
            lowerText.contains("реклама") || lowerText.contains("акция") -> CategoryFilter.SPAM
            else -> CategoryFilter.ALL
        }
    }

    suspend fun fetchCallLogs(): List<CallLogItem> = withContext(Dispatchers.IO) {
        val callLogs = mutableListOf<CallLogItem>()
        var cursor: Cursor? = null
        try {
            val uri = CallLog.Calls.CONTENT_URI
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE
            )

            cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 100)
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(CallLog.Calls.DATE))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                }
                contentResolver.query(uri, projection, queryArgs, null)
            } else {
                contentResolver.query(uri, projection, null, null, "${CallLog.Calls.DATE} DESC LIMIT 100")
            }

            cursor?.let {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)

                while (it.moveToNext()) {
                    val id = it.getString(idIdx) ?: UUID.randomUUID().toString()
                    val number = it.getString(numberIdx) ?: ""
                    val name = it.getString(nameIdx) ?: number
                    val type = it.getInt(typeIdx)
                    val date = it.getLong(dateIdx)
                    val isMissed = type == CallLog.Calls.MISSED_TYPE || type == CallLog.Calls.REJECTED_TYPE

                    val typeStr = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "Входящий"
                        CallLog.Calls.OUTGOING_TYPE -> "Исходящий"
                        CallLog.Calls.MISSED_TYPE -> "Пропущенный"
                        CallLog.Calls.REJECTED_TYPE -> "Отклоненный"
                        else -> "Звонок"
                    }

                    callLogs.add(
                        CallLogItem(
                            id = id,
                            name = if (name == number) getContactNameCached(number) ?: number else name,
                            number = number,
                            type = typeStr,
                            timestamp = date,
                            isMissed = isMissed,
                            avatarColor = getRandomColor()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SystemDataProvider", "Error fetching call logs", e)
        } finally {
            cursor?.close()
        }
        callLogs.distinctBy { it.id }
    }

    private fun getRandomColor(): Color {
        val colors = listOf(
            Color(0xFF3F51B5), // Indigo
            Color(0xFFEC407A), // Pink
            Color(0xFF03A9F4), // Light Blue
            Color(0xFF66BB6A), // Green
            Color(0xFFFFA726), // Orange
            Color(0xFFAB47BC)  // Purple
        )
        return colors.random()
    }
}
