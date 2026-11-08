package ru.maximnosko.svyaz

import android.app.Application
import android.content.ServiceConnection
import android.util.Log

/**
 * Application class for Svyaz app.
 * Overrides [unbindService] to safely catch and ignore [IllegalArgumentException] ("Service not registered")
 * that can be thrown by RuStore SDK or third-party service binders when unbinding services.
 */
class SvyazApplication : Application() {

    override fun unbindService(conn: ServiceConnection) {
        try {
            super.unbindService(conn)
        } catch (e: IllegalArgumentException) {
            Log.w("SvyazApplication", "Safely caught unbindService error (Service not registered): ${e.message}")
        } catch (e: Exception) {
            Log.e("SvyazApplication", "Unexpected exception during unbindService: ${e.message}", e)
        }
    }
}
