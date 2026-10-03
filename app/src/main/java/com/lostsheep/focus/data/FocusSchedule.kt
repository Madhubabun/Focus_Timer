package com.lostsheep.focus.data

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** A focus session that starts by itself at a set time on chosen days. */
data class FocusSchedule(
    val id: String,
    /** Minutes after midnight, local time. */
    val minuteOfDay: Int,
    val days: Set<DayOfWeek>,
    val durationMin: Int,
    val enabled: Boolean = true,
) {
    val time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    /** The next start strictly after [now], or null when no day is chosen. */
    fun nextAfter(now: ZonedDateTime): ZonedDateTime? {
        if (days.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek !in days) continue
            val at = date.atTime(time).atZone(now.zone)
            if (at.isAfter(now)) return at
        }
        return null
    }

    /** "Mon–Fri · 09:00 · 50 min". */
    fun describe(): String = "${describeDays(days)} · ${formatMinuteOfDay(minuteOfDay)} · $durationMin min"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("minuteOfDay", minuteOfDay)
        put("days", JSONArray(days.map { it.value }))
        put("durationMin", durationMin)
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(o: JSONObject): FocusSchedule? = runCatching {
            val d = o.getJSONArray("days")
            FocusSchedule(
                id = o.getString("id"),
                minuteOfDay = o.getInt("minuteOfDay").coerceIn(0, 24 * 60 - 1),
                days = (0 until d.length()).map { DayOfWeek.of(d.getInt(it)) }.toSet(),
                durationMin = o.getInt("durationMin").coerceIn(1, 180),
                enabled = o.optBoolean("enabled", true),
            )
        }.getOrNull()

        fun listToJson(list: List<FocusSchedule>): String = JSONArray(list.map { it.toJson() }).toString()

        fun listFromJson(json: String?): List<FocusSchedule> {
            if (json.isNullOrBlank()) return emptyList()
            val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { fromJson(arr.getJSONObject(it)) }
        }

        /** The earliest upcoming start across [schedules], with the schedule it belongs to. */
        fun next(schedules: List<FocusSchedule>, now: ZonedDateTime): Pair<FocusSchedule, ZonedDateTime>? =
            schedules.filter { it.enabled }
                .mapNotNull { s -> s.nextAfter(now)?.let { s to it } }
                .minByOrNull { it.second }
    }
}

private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

fun describeDays(days: Set<DayOfWeek>): String = when {
    days.size == 7 -> "Every day"
    days == weekdays -> "Mon–Fri"
    days == setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) -> "Weekends"
    days.isEmpty() -> "No days"
    else -> days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

/** "09:00", "18:30". */
fun formatMinuteOfDay(m: Int): String = String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60)
