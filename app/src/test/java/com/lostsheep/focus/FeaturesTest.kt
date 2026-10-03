package com.lostsheep.focus

import com.lostsheep.focus.data.FocusSchedule
import com.lostsheep.focus.data.FocusSessionEntity
import com.lostsheep.focus.data.FocusStats
import com.lostsheep.focus.data.FocusStatsCalculator
import com.lostsheep.focus.data.describeDays
import com.lostsheep.focus.reminders.Reminders
import com.lostsheep.focus.session.ActiveSession
import com.lostsheep.focus.session.SessionOutcome
import com.lostsheep.focus.session.TimeSource
import com.lostsheep.focus.story.ClosingWords
import com.lostsheep.focus.story.FocusVerses
import com.lostsheep.focus.story.Prayers
import com.lostsheep.focus.story.Verses
import com.lostsheep.focus.story.closingFor
import com.lostsheep.focus.ui.focus.snapMinutes
import com.lostsheep.focus.widget.WidgetText
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val MIN = 60_000L

class FeaturesTest {

    private object Clock : TimeSource {
        override fun elapsedRealtime() = 5_000_000L
        override fun wallTime() = 1_700_000_000_000L
        override fun bootCount() = 1
    }

    // ---- Custom timer from one minute -------------------------------------------------------

    @Test
    fun customTimerGoesDownToOneMinute() {
        assertEquals(1, snapMinutes(1f))
        assertEquals(1, snapMinutes(0.2f))
        assertEquals(7, snapMinutes(7.4f))
        assertEquals(10, snapMinutes(11f))
        assertEquals(15, snapMinutes(13f))
        assertEquals(180, snapMinutes(400f))
    }

    @Test
    fun aOneMinuteSessionRunsTheWholeStory() {
        val s = ActiveSession.start("one", "lost-sheep", 1 * MIN, emptySet(), Clock)
        assertEquals(0f, s.progress(Clock), 0f)
        val done = s.copy(bankedMs = MIN, paused = true)
        assertEquals(1f, done.progress(Clock), 0f)
    }

    // ---- Verses while focusing --------------------------------------------------------------

    @Test
    fun focusVerseDependsOnlyOnFocusedTime() {
        val a = FocusVerses.at("session-1", 7 * MIN + 20_000, 25 * MIN)
        val b = FocusVerses.at("session-1", 7 * MIN + 20_000, 25 * MIN)
        assertEquals(a, b) // paused at the same focused time: same verse, same fade
    }

    @Test
    fun focusVerseFadesInRestsThenLeavesTheSceneClear() {
        val planned = 25 * MIN // a new verse every 5 minutes
        assertEquals(5 * MIN, FocusVerses.intervalMs(planned))
        assertEquals(0f, FocusVerses.at("s", 1_000, planned).alpha, 0f)
        assertEquals(1f, FocusVerses.at("s", 40_000, planned).alpha, 0f)
        assertEquals(0f, FocusVerses.at("s", 4 * MIN, planned).alpha, 0f)
        // Short sessions still get a verse, every minute.
        assertEquals(MIN, FocusVerses.intervalMs(MIN))
        assertTrue(FocusVerses.at("s", 20_000, MIN).alpha > 0f)
    }

    @Test
    fun aSessionDoesNotRepeatAVerseUntilAllHaveShown() {
        val planned = 180 * MIN
        val interval = FocusVerses.intervalMs(planned)
        val seen = (0 until Verses.focus.size).map { FocusVerses.at("long", it * interval + 30_000, planned).verse.id }
        assertEquals(Verses.focus.size, seen.toSet().size)
    }

    // ---- Closing prayer or verse ------------------------------------------------------------

    @Test
    fun closingWordsFollowTheSetting() {
        assertNull(closingFor(ClosingWords.Off, "abc"))
        val prayer = closingFor(ClosingWords.Prayer, "abc")!!
        assertNull(prayer.reference)
        assertTrue(prayer.text in Prayers.all)
        val verse = closingFor(ClosingWords.Verse, "abc")!!
        assertNotNull(verse.reference)
        assertEquals(closingFor(ClosingWords.Alternate, "abc"), closingFor(ClosingWords.Alternate, "abc"))
        val kinds = (1..20).map { closingFor(ClosingWords.Alternate, "session-$it")!!.reference == null }.toSet()
        assertEquals(setOf(true, false), kinds) // both prayers and verses come up
    }

    // ---- Intention and journal --------------------------------------------------------------

    @Test
    fun intentionSurvivesSavingAndOldSessionsStillLoad() {
        val s = ActiveSession.start("i", "lost-sheep", 25 * MIN, emptySet(), Clock, intention = "  Finish chapter 3 ")
        val back = ActiveSession.fromJson(s.toJson(Clock))!!
        assertEquals("Finish chapter 3", back.intention)
        val old = s.toJson(Clock).replace("\"intention\":\"Finish chapter 3\",", "")
        assertEquals("", ActiveSession.fromJson(old)!!.intention)
        val outcome = SessionOutcome("i", true, 1, 1, 0, 2, "Read")
        assertEquals("Read", SessionOutcome.fromJson(outcome.toJson())!!.intention)
    }

    @Test
    fun statsCountFinishedIntentions() {
        fun row(id: String, intention: String?, done: Boolean?) =
            FocusSessionEntity(id, "lost-sheep", 0, 1, MIN, MIN, "COMPLETED", 0, intention, done, null)
        val stats = FocusStatsCalculator.compute(
            listOf(row("a", "Read", true), row("b", "Write", false), row("c", "Pray", null), row("d", null, null)),
        )
        assertEquals(2, stats.intentionsAnswered)
        assertEquals(1, stats.intentionsDone)
    }

    // ---- Daily reminder and scheduled focus -------------------------------------------------

    private val zone = ZoneId.of("Europe/London")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone)

    @Test
    fun reminderIsLaterTodayOrTomorrow() {
        assertEquals(at(2026, 10, 5, 9, 0), Reminders.nextReminder(9 * 60, at(2026, 10, 5, 8, 0)))
        assertEquals(at(2026, 10, 6, 9, 0), Reminders.nextReminder(9 * 60, at(2026, 10, 5, 9, 0)))
    }

    @Test
    fun weekdayScheduleSkipsTheWeekend() {
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val s = FocusSchedule("w", 9 * 60, weekdays, 50)
        // Friday 2 Oct 2026 after 9:00 -> Monday 5 Oct.
        assertEquals(at(2026, 10, 5, 9, 0), s.nextAfter(at(2026, 10, 2, 10, 0)))
        // Monday before 9:00 -> the same morning.
        assertEquals(at(2026, 10, 5, 9, 0), s.nextAfter(at(2026, 10, 5, 7, 30)))
        assertEquals("Mon–Fri", describeDays(weekdays))
        assertNull(s.copy(days = emptySet()).nextAfter(at(2026, 10, 5, 7, 30)))
    }

    @Test
    fun theEarliestEnabledScheduleWins() {
        val now = at(2026, 10, 5, 8, 0)
        val early = FocusSchedule("e", 8 * 60 + 30, DayOfWeek.entries.toSet(), 25, enabled = false)
        val mid = FocusSchedule("m", 10 * 60, DayOfWeek.entries.toSet(), 25)
        val late = FocusSchedule("l", 14 * 60, DayOfWeek.entries.toSet(), 25)
        val next = FocusSchedule.next(listOf(late, early, mid), now)!!
        assertEquals("m", next.first.id)
        val restored = FocusSchedule.listFromJson(FocusSchedule.listToJson(listOf(early, mid)))
        assertEquals(listOf(early, mid), restored)
        assertEquals(emptyList<FocusSchedule>(), FocusSchedule.listFromJson("not json"))
    }

    // ---- Widget -----------------------------------------------------------------------------

    @Test
    fun widgetShowsGoalStreakOrTheRunningSession() {
        val idle = WidgetText.of(FocusStats(todayCompleted = 2, currentStreakDays = 3, todayFocusMs = 50 * MIN), goal = 4, remainingMs = null, paused = false)
        assertEquals("2 of 4 sessions today", idle.progress)
        assertTrue(idle.detail.contains("3 day streak"))
        assertEquals("Begin Focus", idle.button)
        val reached = WidgetText.of(FocusStats(todayCompleted = 4), goal = 4, remainingMs = null, paused = false)
        assertTrue(reached.progress.startsWith("Goal reached"))
        val running = WidgetText.of(FocusStats(), goal = 4, remainingMs = 10 * MIN, paused = false)
        assertEquals("10:00 left", running.detail)
        assertNotEquals(running.button, idle.button)
    }
}
