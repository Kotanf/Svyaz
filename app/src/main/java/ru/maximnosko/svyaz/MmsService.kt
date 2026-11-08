package ru.maximnosko.svyaz

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri

class MmsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "android.intent.action.RESPOND_VIA_MESSAGE" ||
            action == "android.telephony.action.RESPOND_VIA_MESSAGE"
        ) {
            handleIncomingMms(intent)
        } else {
            // Outgoing MMS started from MessagingRepository via explicit extras.
            val phoneNumber = intent?.getStringExtra("phoneNumber")
            if (!phoneNumber.isNullOrBlank()) {
                sendMms(
                    phoneNumber = phoneNumber,
                    message = intent.getStringExtra("message") ?: "",
                    imageUri = intent.getStringExtra("imageUri")
                )
            }
        }
        return START_NOT_STICKY
    }

    private fun handleIncomingMms(intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action
            Log.d("MmsService", "Handling MMS action: $action")
            if (action == "android.intent.action.RESPOND_VIA_MESSAGE" || action == "android.telephony.action.RESPOND_VIA_MESSAGE") {
                val uri = intent.data
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                val recipient = uri?.schemeSpecificPart ?: ""
                if (recipient.isNotBlank() && text.isNotBlank()) {
                    val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        getSystemService(android.telephony.SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        android.telephony.SmsManager.getDefault()
                    }
                    if (text.length > 70) {
                        val parts = smsManager.divideMessage(text)
                        smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(recipient, null, text, null, null)
                    }
                    if (BuildConfig.DEBUG) Log.d("MmsService", "Quick response SMS sent")
                }
            }
        } catch (e: Exception) {
            Log.e("MmsService", "Error handling incoming MMS", e)
        }
    }

    private fun sendMms(phoneNumber: String, message: String, imageUri: String?) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpg"
                putExtra("address", phoneNumber)
                putExtra("sms_body", message)
                if (imageUri != null) {
                    // Only content:// URIs can be shared safely across apps; a raw
                    // file:// path would throw FileUriExposedException on API 24+.
                    val parsed = imageUri.toUri()
                    if (parsed.scheme == "content") {
                        putExtra(Intent.EXTRA_STREAM, parsed)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        Log.w("MmsService", "Skipping non-content media uri for MMS")
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(sendIntent, "Send MMS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(chooser)
        } catch (e: Exception) {
            Log.e("MmsService", "Error sending MMS", e)
        }
    }
}
