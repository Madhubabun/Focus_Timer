package com.lostsheep.focus.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lostsheep.focus.LostSheepApp

/** Fires when the session should be over, even if the app was closed or the phone asleep. */
class SessionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = (context.applicationContext as LostSheepApp).container.sessionManager
        if (!manager.completeIfDue()) manager.rescheduleIfRunning()
    }
}

/** Restores an active session after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        // Creating the container restores the saved session, re-anchors its clock and completes it if due.
        val manager = (context.applicationContext as LostSheepApp).container.sessionManager
        if (manager.hasActiveSession) manager.ensureServiceRunning()
        // Starting the app already re-armed the daily reminder and scheduled sessions.
    }
}
