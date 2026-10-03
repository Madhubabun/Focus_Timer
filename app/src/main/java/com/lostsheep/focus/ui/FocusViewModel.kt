package com.lostsheep.focus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lostsheep.focus.LostSheepApp
import com.lostsheep.focus.blocking.AppCatalog
import com.lostsheep.focus.blocking.InstalledApp
import com.lostsheep.focus.data.FocusStats
import com.lostsheep.focus.data.FocusStatsCalculator
import com.lostsheep.focus.data.AppSettings
import com.lostsheep.focus.data.FocusSchedule
import com.lostsheep.focus.reminders.Reminders
import com.lostsheep.focus.story.ClosingWords
import com.lostsheep.focus.widget.FocusWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as LostSheepApp).container
    private val manager = container.sessionManager

    val clock = container.clock
    val session = manager.session
    val outcome = manager.outcome
    val settings = container.settings.settings
    val blockedApps = container.blockedApps
    val history = container.history

    val stats: StateFlow<FocusStats> = container.history
        .map { FocusStatsCalculator.compute(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusStatsCalculator.compute(container.history.value))

    private val _installedApps = MutableStateFlow<List<InstalledApp>?>(null)
    val installedApps: StateFlow<List<InstalledApp>?> = _installedApps.asStateFlow()

    /** What the next session is for, typed on the home screen. Cleared once the session starts. */
    private val _intentionDraft = MutableStateFlow("")
    val intentionDraft: StateFlow<String> = _intentionDraft.asStateFlow()
    fun setIntentionDraft(text: String) { _intentionDraft.value = text.take(MAX_INTENTION) }

    fun begin(minutes: Int) {
        container.settings.update { it.copy(breakEndsAtWall = 0L) }
        val started = manager.start(
            durationMs = minutes.coerceIn(1, 180) * 60_000L,
            storyId = settings.value.storyId,
            blockedApps = blockedApps.value.map { it.packageName }.toSet(),
            intention = _intentionDraft.value,
        )
        if (started) _intentionDraft.value = ""
    }

    fun saveReflection(sessionId: String, intentionDone: Boolean?, reflection: String?) =
        manager.saveReflection(sessionId, intentionDone, reflection)

    fun pause() = manager.pause()
    fun resume() = manager.resume()
    fun acknowledgeRecovery() = manager.acknowledgeRecovery()
    fun end() = manager.end()
    fun completeIfDue() = manager.completeIfDue()
    fun dismissOutcome() = manager.dismissOutcome()

    fun startBreak() {
        manager.dismissOutcome()
        val minutes = settings.value.breakMinutes
        container.settings.update { it.copy(breakEndsAtWall = System.currentTimeMillis() + minutes * 60_000L) }
    }

    fun endBreak() = container.settings.update { it.copy(breakEndsAtWall = 0L) }

    fun setDefaultDuration(minutes: Int) = container.settings.update { it.copy(defaultDurationMin = minutes) }
    fun setSound(on: Boolean) = container.settings.update { it.copy(soundOn = on) }
    fun setShowTimerNotification(on: Boolean) = container.settings.update { it.copy(showTimerNotification = on) }
    fun setVerse(id: String) = container.settings.update { it.copy(verseId = id) }
    fun setDailyGoal(sessions: Int) {
        container.settings.update { it.copy(dailyGoalSessions = sessions.coerceIn(1, 12)) }
        FocusWidget.refresh(getApplication())
    }
    fun setVersesWhileFocusing(on: Boolean) = container.settings.update { it.copy(versesWhileFocusing = on) }
    fun setClosingWords(choice: ClosingWords) = container.settings.update { it.copy(closingWords = choice) }

    fun setReminder(on: Boolean, minuteOfDay: Int = settings.value.reminderMinute) = updateAlarms {
        it.copy(reminderOn = on, reminderMinute = minuteOfDay.coerceIn(0, 24 * 60 - 1))
    }

    fun saveSchedule(schedule: FocusSchedule) = updateAlarms { s ->
        val others = s.schedules.filter { it.id != schedule.id }
        s.copy(schedules = (others + schedule).sortedBy { it.minuteOfDay })
    }

    fun deleteSchedule(id: String) = updateAlarms { s -> s.copy(schedules = s.schedules.filter { it.id != id }) }

    private fun updateAlarms(transform: (AppSettings) -> AppSettings) {
        container.settings.update(transform)
        Reminders.sync(getApplication(), container.settings.settings.value)
    }

    fun setBlocked(app: InstalledApp, blocked: Boolean) = container.setBlocked(app.packageName, app.label, blocked)

    private companion object {
        const val MAX_INTENTION = 80
    }

    fun loadInstalledApps() {
        if (_installedApps.value != null) return
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) { AppCatalog.load(getApplication<Application>()) }
        }
    }
}
