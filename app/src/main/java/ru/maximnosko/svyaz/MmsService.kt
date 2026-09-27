package ru.maximnosko.svyaz

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.net.toUri

class MmsService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIncomingMms(intent)
        return START_NOT_STICKY
    }

    private fun handleIncomingMms(intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action
            Log.d("MmsService", "Handling MMS action: $action")
        } catch (e: Exception) {
            Log.e("MmsService", "Error handling incoming MMS", e)
        }
    }

    fun sendMms(phoneNumber: String, message: String, imageUri: String?) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpg"
            putExtra("address", phoneNumber)
            putExtra("sms_body", message)
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri.toUri())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        val chooser = Intent.createChooser(sendIntent, "Send MMS").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(chooser)
    }
}
