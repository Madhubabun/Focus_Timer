package com.lostsheep.focus

import com.lostsheep.focus.data.FocusSessionEntity
import com.lostsheep.focus.data.FocusStatsCalculator
import com.lostsheep.focus.story.LostSheepChoreography
import com.lostsheep.focus.story.LostSheepStory
import com.lostsheep.focus.story.Terrain
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
    fun searchHas720NamedFramesThenFoundAndHome() {
        val beats = LostSheepChoreography.beats
        assertEquals(LostSheepChoreography.SEARCH_FRAMES + 14, beats.size)
        assertEquals(0f, beats.first().start)
        assertTrue(beats.zipWithNext().all { (a, b) -> b.start > a.start })
        assertEquals(beats.size, beats.map { it.name }.toSet().size)
        fun frame(n: Int) = LostSheepChoreography.SEARCH_END * (n - 1) / LostSheepChoreography.SEARCH_FRAMES + 1e-5f
        assertEquals("leaving_the_flock_001", LostSheepChoreography.beatAt(0f).name)
        assertEquals("green_pasture_001", LostSheepChoreography.beatAt(frame(61)).name)
        assertEquals("olive_grove_001", LostSheepChoreography.beatAt(frame(131)).name)
        assertEquals("wide_valley_001", LostSheepChoreography.beatAt(frame(271)).name)
        assertEquals("stream_crossing_001", LostSheepChoreography.beatAt(frame(351)).name)
        assertEquals("first_sight_001", LostSheepChoreography.beatAt(frame(531)).name)
        assertEquals("drawing_near_120", LostSheepChoreography.beatAt(frame(720)).name)
        assertEquals("reaches_the_sheep", LostSheepChoreography.beatAt(0.8f).name)
        assertEquals("wide_valley_sunlight", LostSheepChoreography.beatAt(1f).name)
        assertEquals("scene_0131_olive_grove_001", LostSheepStory.stateNameAt(frame(131)))
    }

    @Test
    fun sceneMovesContinuouslySoResumingNeverJumps() {
        // Measured on screen: how far things move per 0.05% of the session (under a second at 25 min).
        var prev = LostSheepChoreography.at(0f)
        var p = 0.0005f
        while (p <= 1f) {
            val s = LostSheepChoreography.at(p)
            assertTrue("shepherd jumped at $p", abs(s.shepherdX - prev.shepherdX) * s.zoom < 0.02f)
            assertTrue("camera jumped at $p", abs(s.cameraX - prev.cameraX) * s.zoom < 0.02f)
            assertTrue("camera height jumped at $p", abs(s.cameraY - prev.cameraY) < 0.01f)
            assertTrue("zoom jumped at $p", abs(s.zoom - prev.zoom) < 0.02f)
            prev = s
            p += 0.0005f
        }
    }

    @Test
    fun pausedProgressAlwaysShowsTheSameFrame() {
        for (p in listOf(0f, 0.137f, 0.42f, 0.63f, 0.81f, 0.97f, 1f)) {
            assertEquals(LostSheepChoreography.at(p), LostSheepChoreography.at(p))
            assertEquals(LostSheepChoreography.beatAt(p), LostSheepChoreography.beatAt(p))
        }
    }

    @Test
    fun theSearchWalksThroughEachTerrainInOrder() {
        // The shepherd only ever moves forward while searching, so the terrains follow each other.
        var last = LostSheepChoreography.at(0f).shepherdX
        var p = 0.001f
        while (p <= LostSheepChoreography.SEARCH_END) {
            val x = LostSheepChoreography.at(p).shepherdX
            assertTrue("walked back at $p", x >= last - 1e-4f)
            last = x
            p += 0.001f
        }
        fun terrainAtFrame(n: Int): Terrain {
            val x = LostSheepChoreography.at(LostSheepChoreography.SEARCH_END * (n - 1) / LostSheepChoreography.SEARCH_FRAMES).shepherdX
            return Terrain.entries.maxBy { LostSheepChoreography.terrainWeight(it, x) }
        }
        assertEquals(Terrain.PASTURE, terrainAtFrame(10))
        assertEquals(Terrain.MEADOW, terrainAtFrame(100))
        assertEquals(Terrain.OLIVE_GROVE, terrainAtFrame(170))
        assertEquals(Terrain.ROCKY_HILLSIDE, terrainAtFrame(240))
        assertEquals(Terrain.WIDE_VALLEY, terrainAtFrame(320))
        assertEquals(Terrain.STREAM, terrainAtFrame(390))
        assertEquals(Terrain.WOODED_HILLSIDE, terrainAtFrame(450))
        assertEquals(Terrain.HIGH_RIDGE, terrainAtFrame(510))
        assertEquals(Terrain.SHEEP_HILL, terrainAtFrame(650))
    }

    @Test
    fun lostSheepStaysOutOfSightUntilTheRidge() {
        // A tall phone (450 x 975): half the visible width in world units is 225 / (731 * zoom).
        var p = 0f
        while (p < 0.58f) {
            val s = LostSheepChoreography.at(p)
            val halfWidth = 225f / (731f * s.zoom)
            assertTrue("sheep visible at $p", s.lostSheepX - s.cameraX > halfWidth + 0.03f)
            p += 0.002f
        }
        val sight = LostSheepChoreography.at(0.61f)
        assertTrue(sight.lostSheepX - sight.cameraX < 225f / (731f * sight.zoom))
    }

    @Test
    fun storyBeatsHappen() {
        val start = LostSheepChoreography.at(0f)
        val found = LostSheepChoreography.at(0.86f)
        val home = LostSheepChoreography.at(0.99f)
        val end = LostSheepChoreography.at(1f)
        assertTrue(start.lostSheepX - start.shepherdX > 10f) // a long journey away
        assertTrue(found.kneel > 0.9f && found.embrace > 0.9f)
        assertTrue(home.carrying && home.shepherdWalking)
        // Over the hill, the home pasture and the flock are just ahead.
        assertTrue(LostSheepChoreography.LOOP + LostSheepChoreography.FLOCK_START - end.shepherdX < 1.0f)
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
