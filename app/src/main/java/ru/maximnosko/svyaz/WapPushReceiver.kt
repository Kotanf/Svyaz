package ru.maximnosko.svyaz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class WapPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            try {
                val mimeType = intent.type ?: "application/vnd.wap.mms-message"
                Log.d("WapPushReceiver", "Received WAP PUSH deliver intent with mimeType: $mimeType")
            } catch (e: Exception) {
                Log.e("WapPushReceiver", "Error processing WAP PUSH deliver intent", e)
            }
        }
    }
}
