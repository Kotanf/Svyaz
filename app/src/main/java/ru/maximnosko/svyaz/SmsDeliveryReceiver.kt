package ru.maximnosko.svyaz

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.maximnosko.svyaz.data.MessageStatus
import ru.maximnosko.svyaz.data.local.SvyazDatabase

data class DeliveryUpdate(val messageId: String, val threadId: String?, val status: MessageStatus)

class SmsDeliveryReceiver : BroadcastReceiver() {

    companion object {
        private val _deliveryUpdates = MutableSharedFlow<DeliveryUpdate>(extraBufferCapacity = 64)
        val deliveryUpdates = _deliveryUpdates.asSharedFlow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (BuildConfig.DEBUG) Log.d("SmsDeliveryReceiver", "Delivery broadcast received: $action, resultCode=$resultCode")
        if (action == "ru.maximnosko.svyaz.SMS_DELIVERED" || action == "android.provider.Telephony.SMS_DELIVERED") {
            val messageId = intent.getStringExtra("messageId") ?: return
            val threadId = intent.getStringExtra("threadId")
            val isDelivered = (resultCode == Activity.RESULT_OK)
            val status = if (isDelivered) MessageStatus.DELIVERED else MessageStatus.ERROR
            
            _deliveryUpdates.tryEmit(DeliveryUpdate(messageId, threadId, status))

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = SvyazDatabase.getDatabase(context.applicationContext)
                    db.threadDao().updateMessageStatus(messageId, status.name)
                    if (BuildConfig.DEBUG) Log.d("SmsDeliveryReceiver", "Updated delivery status in database")
                } catch (e: Exception) {
                    Log.e("SmsDeliveryReceiver", "Failed to update delivery status", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
