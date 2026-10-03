package com.lostsheep.focus.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val defaultDurationMin: Int = 25,
    val soundOn: Boolean = true,
    val showTimerNotification: Boolean = true,
    val verseId: String = "matthew-18-12",
    val storyId: String = "lost-sheep",
    val dailyGoalSessions: Int = 4,
    val breakEndsAtWall: Long = 0L,
    val breakMinutes: Int = 5,
)

/** Small user preferences, stored on the device only. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun read() = AppSettings(
        defaultDurationMin = prefs.getInt("defaultDurationMin", 25),
        soundOn = prefs.getBoolean("soundOn", true),
        showTimerNotification = prefs.getBoolean("showTimerNotification", true),
        verseId = prefs.getString("verseId", null) ?: "matthew-18-12",
        storyId = prefs.getString("storyId", null) ?: "lost-sheep",
        dailyGoalSessions = prefs.getInt("dailyGoalSessions", 4),
        breakEndsAtWall = prefs.getLong("breakEndsAtWall", 0L),
        breakMinutes = prefs.getInt("breakMinutes", 5),
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putInt("defaultDurationMin", next.defaultDurationMin)
            .putBoolean("soundOn", next.soundOn)
            .putBoolean("showTimerNotification", next.showTimerNotification)
            .putString("verseId", next.verseId)
            .putString("storyId", next.storyId)
            .putInt("dailyGoalSessions", next.dailyGoalSessions)
            .putLong("breakEndsAtWall", next.breakEndsAtWall)
            .putInt("breakMinutes", next.breakMinutes)
            .apply()
        _settings.value = next
    }
}
