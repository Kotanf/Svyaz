package ru.maximnosko.svyaz

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.maximnosko.svyaz.data.SystemDataProvider

class CallService : InCallService() {

    companion object {
        // The currently active telecom Call. Held so the call UI can act on it
        // (e.g. disconnect) instead of just finishing a detached Activity.
        @Volatile
        var activeCall: Call? = null
            private set

        fun hangUp() {
            activeCall?.disconnect()
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call

        val number = call.details?.handle?.schemeSpecificPart ?: "Неизвестный"

        // Contact resolution touches ContentResolver; keep it off the main thread.
        CoroutineScope(Dispatchers.IO).launch {
            val name = if (number != "Неизвестный") {
                SystemDataProvider(applicationContext).getContactNameFromNumber(number) ?: "Собеседник"
            } else {
                "Неизвестный номер"
            }

            val intent = Intent(this@CallService, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("number", number)
                putExtra("name", name)
            }
            startActivity(intent)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (activeCall == call) {
            activeCall = null
        }
    }
}
