package com.lostsheep.focus.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayTotal(val date: LocalDate, val focusedMs: Long)

data class FocusStats(
    val todayFocusMs: Long = 0,
    val todayCompleted: Int = 0,
    val currentStreakDays: Int = 0,
    val distractionsBlocked: Int = 0,
    val totalCompleted: Int = 0,
    val totalFocusMs: Long = 0,
    val lastSevenDays: List<DayTotal> = emptyList(),
)

object FocusStatsCalculator {

    /**
     * Focus time counts for both completed and ended sessions (time spent focusing is real either way);
     * only completed sessions count as "sessions", for streaks, and for the Journey.
     */
    fun compute(
        sessions: List<FocusSessionEntity>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): FocusStats {
        fun dayOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

        val todays = sessions.filter { dayOf(it.endedAt) == today }
        val completedDays = sessions.filter { it.completed }.map { dayOf(it.endedAt) }.toSet()

        // A streak survives until the end of today: if nothing is done yet today, count from yesterday.
        var cursor = if (today in completedDays) today else today.minusDays(1)
        var streak = 0
        while (cursor in completedDays) {
            streak++
            cursor = cursor.minusDays(1)
        }

        val byDay = sessions.groupBy { dayOf(it.endedAt) }
        val week = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            DayTotal(d, byDay[d].orEmpty().sumOf { it.focusedMs })
        }

        return FocusStats(
            todayFocusMs = todays.sumOf { it.focusedMs },
            todayCompleted = todays.count { it.completed },
            currentStreakDays = streak,
            distractionsBlocked = sessions.sumOf { it.distractionsBlocked },
            totalCompleted = sessions.count { it.completed },
            totalFocusMs = sessions.sumOf { it.focusedMs },
            lastSevenDays = week,
        )
    }
}

/** "2h 15m", "45m", "0m". */
fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}
