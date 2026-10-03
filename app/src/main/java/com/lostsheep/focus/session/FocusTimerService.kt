package com.lostsheep.focus.session

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.lostsheep.focus.LostSheepApp
import com.lostsheep.focus.blocking.BlockedActivity
import com.lostsheep.focus.blocking.BlockingPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps an active session alive while the app is in the background or the screen is locked:
 * the ongoing notification, completion, ambient sound and blocking health checks.
 * The timer itself is timestamp-based in [SessionManager]; this service never counts down.
 */
class FocusTimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null
    private var inForeground = false
    private var warnedBlocking = false
    private var lastForegroundPackage: String? = null
    private var tick = 0

    private val container get() = (application as LostSheepApp).container
    private val manager get() = container.sessionManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        SessionNotifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always promote to foreground first; Android requires it promptly after startForegroundService.
        goForeground()
        when (intent?.action) {
            ACTION_PAUSE -> manager.pause()
            ACTION_RESUME -> manager.resume()
        }
        if (!manager.hasActiveSession) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        if (watchJob == null) watchJob = scope.launch { watch() }
        return START_STICKY // if Android kills us, we come back and recompute from the saved session
    }

    private fun goForeground() {
        val n = SessionNotifications.buildSession(
            this,
            manager.session.value,
            manager.clock,
            container.settings.settings.value.showTimerNotification,
        )
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, SessionNotifications.ID_SESSION, n, type)
        inForeground = true
    }

    private suspend fun watch() {
        scope.launch {
            manager.session.combine(container.settings.settings) { s, settings -> s to settings }
                .collect { (s, settings) ->
                    if (s == null || !s.isActive) {
                        AmbientSound.stopAmbient()
                        stopSelfCleanly()
                        return@collect
                    }
                    runCatching {
                        NotificationManagerCompat.from(this@FocusTimerService).notify(
                            SessionNotifications.ID_SESSION,
                            SessionNotifications.buildSession(this@FocusTimerService, s, manager.clock, settings.showTimerNotification),
                        )
                    }
                    if (settings.soundOn && !s.paused) AmbientSound.startAmbient() else AmbientSound.stopAmbient()
                }
        }
        while (scope.isActive) {
            manager.completeIfDue()
            if (tick % 10 == 0) checkBlockingHealth()
            pollUsageFallback()
            tick++
            delay(1000)
        }
    }

    /** If the user turned blocking off mid-session, keep the timer and tell them once. */
    private fun checkBlockingHealth() {
        val s = manager.session.value ?: return
        if (s.selectedBlockedApps.isEmpty()) return
        val ok = BlockingPermissions.isAccessibilityEnabled(this) || BlockingPermissions.hasUsageAccess(this)
        if (!ok && !warnedBlocking) {
            SessionNotifications.showBlockingNeedsRestore(this)
            warnedBlocking = true
        } else if (ok) {
            warnedBlocking = false
        }
    }

    /**
     * Basic blocking when only Usage Access is granted. Android may not allow us to open a screen
     * from the background, so a quiet notification is shown as well.
     */
    private fun pollUsageFallback() {
        val s = manager.session.value ?: return
        if (s.selectedBlockedApps.isEmpty()) return
        if (BlockingPermissions.isAccessibilityEnabled(this)) return
        if (!BlockingPermissions.hasUsageAccess(this)) return
        val power = getSystemService(PowerManager::class.java)
        if (power != null && !power.isInteractive) return

        val usm = getSystemService(UsageStatsManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 5_000, now)
        val e = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            @Suppress("DEPRECATION")
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) latest = e.packageName
        }
        if (latest == null || latest == lastForegroundPackage) return
        lastForegroundPackage = latest
        if (manager.shouldBlock(latest)) {
            manager.recordDistraction()
            runCatching {
                startActivity(Intent(this, BlockedActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            SessionNotifications.showFlockCanWait(this)
        }
    }

    private fun stopSelfCleanly() {
        SessionNotifications.cancelSessionAlerts(this)
        if (inForeground) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            inForeground = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        watchJob = null
        if (!manager.hasActiveSession) AmbientSound.stopAmbient()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.lostsheep.focus.action.PAUSE"
        const val ACTION_RESUME = "com.lostsheep.focus.action.RESUME"
    }
}
