package com.lostsheep.focus

import com.lostsheep.focus.data.FocusSessionEntity
import com.lostsheep.focus.data.FocusStatsCalculator
import com.lostsheep.focus.story.LostSheepChoreography
import com.lostsheep.focus.story.LostSheepStory
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryAndStatsTest {

    @Test
    fun phasesMatchTheTwentyFiveMinuteScript() {
        val story = LostSheepStory
        fun at(minLeft: Double) = story.phaseAt((1 - minLeft / 25.0).toFloat()).key
        assertEquals("notice", at(24.0))
        assertEquals("search", at(20.0))
        assertEquals("journey", at(15.0))
        assertEquals("approach", at(10.0))
        assertEquals("found", at(4.0))
        assertEquals("return", at(0.5))
    }

    @Test
    fun sixtyNamedStatesInOrder() {
        val beats = LostSheepChoreography.beats
        assertEquals(60, beats.size)
        assertEquals(0f, beats.first().start)
        assertTrue(beats.zipWithNext().all { (a, b) -> b.start > a.start })
        assertEquals(beats.size, beats.map { it.name }.toSet().size)
        assertEquals("one_is_missing", LostSheepChoreography.beatAt(0.035f).name)
        assertEquals("sees_the_sheep", LostSheepChoreography.beatAt(0.63f).name)
        assertEquals("wide_valley_sunlight", LostSheepChoreography.beatAt(1f).name)
    }

    @Test
    fun sceneMovesContinuouslySoResumingNeverJumps() {
        var prev = LostSheepChoreography.at(0f)
        var p = 0.0005f
        while (p <= 1f) {
            val s = LostSheepChoreography.at(p)
            assertTrue("shepherd jumped at $p", abs(s.shepherdX - prev.shepherdX) < 0.02f)
            assertTrue("camera jumped at $p", abs(s.cameraX - prev.cameraX) < 0.02f)
            assertTrue("zoom jumped at $p", abs(s.zoom - prev.zoom) < 0.02f)
            prev = s
            p += 0.0005f
        }
    }

    @Test
    fun storyBeatsHappen() {
        val start = LostSheepChoreography.at(0f)
        val found = LostSheepChoreography.at(0.86f)
        val home = LostSheepChoreography.at(0.99f)
        assertTrue(start.lostSheepX - start.shepherdX > 1.5f) // far from the flock
        assertTrue(found.kneel > 0.9f && found.embrace > 0.9f)
        assertTrue(home.carrying && home.shepherdFacing < 0)
        assertTrue(home.warmth > start.warmth)
    }

    @Test
    fun streaksAndTotals() {
        val zone = ZoneOffset.UTC
        val today = LocalDate.of(2026, 10, 3)
        fun s(daysAgo: Long, completed: Boolean, min: Long = 25, distractions: Int = 0): FocusSessionEntity {
            val end = today.minusDays(daysAgo).atTime(12, 0).toInstant(zone).toEpochMilli()
            return FocusSessionEntity("$daysAgo-$completed-$min", "lost-sheep", end - min * 60_000, end, 25 * 60_000, min * 60_000, if (completed) "COMPLETED" else "ABANDONED", distractions)
        }
        val list = listOf(s(0, true, distractions = 2), s(0, false, 10), s(1, true), s(2, true), s(4, true, distractions = 1))
        val stats = FocusStatsCalculator.compute(list, today, zone)
        assertEquals(35 * 60_000L, stats.todayFocusMs)
        assertEquals(1, stats.todayCompleted)
        assertEquals(3, stats.currentStreakDays)
        assertEquals(3, stats.distractionsBlocked)
        assertEquals(4, stats.totalCompleted)
        assertEquals(7, stats.lastSevenDays.size)

        // Nothing yet today: the streak from yesterday still stands.
        val noToday = FocusStatsCalculator.compute(list.drop(2), today, zone)
        assertEquals(2, noToday.currentStreakDays)
    }
}
