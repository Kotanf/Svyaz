package ru.maximnosko.svyaz

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import ru.maximnosko.svyaz.data.SystemDataProvider

class CallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        
        val number = call.details.handle.schemeSpecificPart
        val systemDataProvider = SystemDataProvider(this)
        val name = systemDataProvider.getContactNameFromNumber(number) ?: "Собеседник"

        // Launch Call UI
        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("number", number)
            putExtra("name", name)
        }
        startActivity(intent)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
    }
}
