package com.lostsheep.focus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lostsheep.focus.LostSheepApp
import com.lostsheep.focus.blocking.AppCatalog
import com.lostsheep.focus.blocking.InstalledApp
import com.lostsheep.focus.data.FocusStats
import com.lostsheep.focus.data.FocusStatsCalculator
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

    fun begin(minutes: Int) {
        container.settings.update { it.copy(breakEndsAtWall = 0L) }
        manager.start(
            durationMs = minutes * 60_000L,
            storyId = settings.value.storyId,
            blockedApps = blockedApps.value.map { it.packageName }.toSet(),
        )
    }

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
    fun setDailyGoal(sessions: Int) = container.settings.update { it.copy(dailyGoalSessions = sessions.coerceIn(1, 12)) }

    fun setBlocked(app: InstalledApp, blocked: Boolean) = container.setBlocked(app.packageName, app.label, blocked)

    fun loadInstalledApps() {
        if (_installedApps.value != null) return
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) { AppCatalog.load(getApplication<Application>()) }
        }
    }
}
