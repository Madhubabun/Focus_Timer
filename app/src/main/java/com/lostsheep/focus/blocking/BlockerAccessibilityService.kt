package com.lostsheep.focus.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.lostsheep.focus.LostSheepApp

/**
 * Only listens for "a different app came to the front". It never reads screen content.
 * When that app is one the user chose to block during an active session, it shows
 * "The flock can wait." on top.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val manager = (application as LostSheepApp).container.sessionManager
        if (!manager.shouldBlock(pkg)) return

        // One app launch can fire several window events; count it once.
        val now = SystemClock.elapsedRealtime()
        if (pkg != lastBlockedPackage || now - lastBlockedAt > 2_000) {
            manager.recordDistraction()
        }
        lastBlockedPackage = pkg
        lastBlockedAt = now

        startActivity(
            Intent(this, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION),
        )
    }

    override fun onInterrupt() = Unit
}
