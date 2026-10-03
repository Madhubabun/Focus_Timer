package com.lostsheep.focus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lostsheep.focus.ui.AppRoot
import com.lostsheep.focus.ui.ExternalRequests
import com.lostsheep.focus.ui.theme.LostSheepTheme

class MainActivity : ComponentActivity() {

    private val requests = ExternalRequests()
    private val manager get() = (application as LostSheepApp).container.sessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A fresh launch with a session still going: offer to resume, never discard it.
        val fromNotification = intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false) ||
            intent.getBooleanExtra(EXTRA_CONFIRM_END, false)
        if (savedInstanceState == null && manager.hasActiveSession && !fromNotification) {
            requests.showRecovery.value = true
        }
        handle(intent)
        manager.ensureServiceRunning()

        setContent {
            LostSheepTheme {
                AppRoot(requests)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        // If the time ran out while we were away, show completion rather than a stale timer.
        manager.completeIfDue()
    }

    private fun handle(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_CONFIRM_END, false)) {
            requests.showRecovery.value = false
            requests.confirmEnd.value = true
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)) requests.openSettings.value = true
        if (intent.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)) {
            requests.showRecovery.value = false
            manager.acknowledgeRecovery()
        }
        intent.removeExtra(EXTRA_CONFIRM_END)
        intent.removeExtra(EXTRA_OPEN_SETTINGS)
        intent.removeExtra(EXTRA_FROM_NOTIFICATION)
    }

    companion object {
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
        const val EXTRA_CONFIRM_END = "confirm_end"
        const val EXTRA_OPEN_SETTINGS = "open_settings"
    }
}
