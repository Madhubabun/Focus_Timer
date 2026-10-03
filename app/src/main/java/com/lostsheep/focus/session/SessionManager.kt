package com.lostsheep.focus.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.lostsheep.focus.data.FocusSessionEntity
import com.lostsheep.focus.data.LostSheepDatabase
import com.lostsheep.focus.data.SessionOutcomeKind
import com.lostsheep.focus.data.SettingsStore
import com.lostsheep.focus.story.Stories
import com.lostsheep.focus.widget.FocusWidget
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidTimeSource(private val context: Context) : TimeSource {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
    override fun wallTime(): Long = System.currentTimeMillis()
    override fun bootCount(): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}

/**
 * Single owner of the active focus session. Every change is written to disk synchronously
 * before it is published, so the session survives the process being killed at any moment.
 * Timer, app blocking and story all read from this one source.
 */
class SessionManager(
    private val context: Context,
    private val db: LostSheepDatabase,
    private val settings: SettingsStore,
    val clock: TimeSource,
    private val scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences("active_session", Context.MODE_PRIVATE)

    private val _session = MutableStateFlow<ActiveSession?>(null)
    val session: StateFlow<ActiveSession?> = _session.asStateFlow()

    private val _outcome = MutableStateFlow<SessionOutcome?>(null)
    val outcome: StateFlow<SessionOutcome?> = _outcome.asStateFlow()

    init {
        restore()
    }

    @Synchronized
    private fun restore() {
        _outcome.value = prefs.getString(KEY_OUTCOME, null)?.let(SessionOutcome::fromJson)
        val saved = prefs.getString(KEY_SESSION, null)?.let(ActiveSession::fromJson) ?: return
        if (!saved.isActive) {
            prefs.edit().remove(KEY_SESSION).commit()
            return
        }
        // After a reboot the monotonic clock restarted; re-anchor using the wall clock once.
        val anchored = if (saved.bootCount != clock.bootCount()) saved.rebased(clock) else saved
        val restored = if (anchored.paused) anchored else anchored.copy(sessionState = SessionState.RECOVERABLE)
        publish(restored)
        if (restored.isDue(clock)) {
            // The session ended while we were gone: show it as completed, never restart it.
            finish(completed = true)
        } else if (!restored.paused) {
            scheduleCompletionAlarm(restored)
        }
    }

    val hasActiveSession: Boolean get() = _session.value?.isActive == true

    @Synchronized
    fun start(durationMs: Long, storyId: String, blockedApps: Set<String>, intention: String = ""): Boolean {
        if (hasActiveSession) return false // only one focus session may exist at a time
        val session = ActiveSession.start(UUID.randomUUID().toString(), storyId, durationMs, blockedApps, clock, intention)
        _outcome.value = null
        prefs.edit().remove(KEY_OUTCOME).commit()
        publish(session)
        scheduleCompletionAlarm(session)
        startTimerService()
        FocusWidget.refresh(context)
        return true
    }

    /** True when the timer service could be started (it can't from the background on newer Android). */
    val serviceRunning: Boolean get() = lastServiceStart

    /** Saves the after-session answer and note to the history; queued behind the session's own save. */
    fun saveReflection(sessionId: String, intentionDone: Boolean?, reflection: String?) {
        scope.launch(dbWriter) {
            db.sessions().saveReflection(sessionId, intentionDone, reflection?.trim()?.takeIf { it.isNotEmpty() })
        }
    }

    @Synchronized
    fun pause() {
        val s = _session.value ?: return
        if (!s.isActive || s.paused) return
        cancelCompletionAlarm()
        publish(s.paused(clock))
    }

    @Synchronized
    fun resume() {
        val s = _session.value ?: return
        if (!s.isActive) return
        val resumed = s.resumed(clock)
        publish(resumed)
        scheduleCompletionAlarm(resumed)
        startTimerService()
    }

    /** The user saw the recovery dialog and chose to continue. A paused session stays paused. */
    @Synchronized
    fun acknowledgeRecovery() {
        val s = _session.value ?: return
        if (s.sessionState == SessionState.RECOVERABLE) publish(s.copy(sessionState = SessionState.RUNNING))
        startTimerService()
    }

    /** Ends early: saved as abandoned with the time actually focused, and no completion reward. */
    @Synchronized
    fun end() {
        if (hasActiveSession) finish(completed = false)
    }

    /** Completes the session if its time is up. Safe to call from anywhere, any number of times. */
    @Synchronized
    fun completeIfDue(): Boolean {
        val s = _session.value ?: return false
        if (!s.isActive || !s.isDue(clock)) return false
        finish(completed = true)
        return true
    }

    @Synchronized
    fun recordDistraction() {
        val s = _session.value ?: return
        if (s.isActive) publish(s.copy(distractionsBlocked = s.distractionsBlocked + 1))
    }

    fun shouldBlock(packageName: String): Boolean {
        val s = _session.value ?: return false
        // Apps stay blocked while paused too: pausing is a breath, not a break.
        return s.isActive && packageName in s.selectedBlockedApps
    }

    @Synchronized
    fun dismissOutcome() {
        _outcome.value = null
        prefs.edit().remove(KEY_OUTCOME).commit()
    }

    private fun finish(completed: Boolean) {
        val s = _session.value ?: return
        val now = clock.wallTime()
        val focused = if (completed) s.plannedDurationMs else s.elapsedMs(clock)
        val outcome = SessionOutcome(
            sessionId = s.sessionId,
            completed = completed,
            plannedMs = s.plannedDurationMs,
            focusedMs = focused,
            distractionsBlocked = s.distractionsBlocked,
            endedAt = now,
            intention = s.intention,
        )
        val record = FocusSessionEntity(
            id = s.sessionId,
            storyId = s.storyId,
            startedAt = s.startTime,
            endedAt = now,
            plannedMs = s.plannedDurationMs,
            focusedMs = focused,
            outcome = (if (completed) SessionOutcomeKind.COMPLETED else SessionOutcomeKind.ABANDONED).name,
            distractionsBlocked = s.distractionsBlocked,
            intention = s.intention.takeIf { it.isNotEmpty() },
        )
        prefs.edit()
            .remove(KEY_SESSION)
            .putString(KEY_OUTCOME, outcome.toJson())
            .commit()
        cancelCompletionAlarm()
        _outcome.value = outcome
        _session.value = null
        scope.launch(dbWriter) {
            db.sessions().insert(record)
            FocusWidget.refresh(context)
        }

        if (completed) {
            SessionNotifications.showCompleted(context, outcome, Stories.byId(s.storyId).completionTitle)
            if (settings.settings.value.soundOn) AmbientSound.playCompletionChime()
        }
    }

    private fun publish(s: ActiveSession) {
        prefs.edit().putString(KEY_SESSION, s.toJson(clock)).commit()
        _session.value = s
    }

    private var lastServiceStart = false

    private fun startTimerService() {
        lastServiceStart = runCatching {
            ContextCompat.startForegroundService(context, Intent(context, FocusTimerService::class.java))
        }.isSuccess
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, SessionAlarmReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Wakes the app when the session is due, even if it was closed or the phone is asleep. */
    private fun scheduleCompletionAlarm(s: ActiveSession) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val at = SystemClock.elapsedRealtime() + s.remainingMs(clock).coerceAtLeast(0)
        runCatching {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, alarmIntent())
        }
    }

    private fun cancelCompletionAlarm() {
        context.getSystemService(AlarmManager::class.java)?.cancel(alarmIntent())
    }

    /** Keeps the timer service (notification, blocking checks, sound) alive for an active session. */
    fun ensureServiceRunning() {
        if (hasActiveSession) startTimerService()
    }

    fun rescheduleIfRunning() {
        val s = _session.value ?: return
        if (s.isActive && !s.paused) scheduleCompletionAlarm(s)
    }

    private companion object {
        /** One writer, so a reflection is never saved before the session it belongs to. */
        val dbWriter = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        const val KEY_SESSION = "session"
        const val KEY_OUTCOME = "outcome"
    }
}
