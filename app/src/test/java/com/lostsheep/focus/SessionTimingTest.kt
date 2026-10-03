package com.lostsheep.focus

import com.lostsheep.focus.session.ActiveSession
import com.lostsheep.focus.session.SessionState
import com.lostsheep.focus.session.TimeSource
import com.lostsheep.focus.session.formatClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeClock(var elapsed: Long = 1_000_000L, var wall: Long = 1_700_000_000_000L, var boot: Int = 7) : TimeSource {
    override fun elapsedRealtime() = elapsed
    override fun wallTime() = wall
    override fun bootCount() = boot

    /** Real time passing: both clocks move. */
    fun advance(ms: Long) {
        elapsed += ms
        wall += ms
    }
}

private const val MIN = 60_000L

class SessionTimingTest {

    private fun start(clock: FakeClock, minutes: Long = 25) =
        ActiveSession.start("id", "lost-sheep", minutes * MIN, setOf("com.instagram.android"), clock)

    @Test
    fun lockedPhoneKeepsCounting() {
        // Start 25:00, lock at 22:15, unlock 8 minutes later: 14:15 remains.
        val clock = FakeClock()
        val s = start(clock)
        clock.advance(2 * MIN + 45_000)
        assertEquals("22:15", formatClock(s.remainingMs(clock)))
        clock.advance(8 * MIN)
        assertEquals("14:15", formatClock(s.remainingMs(clock)))
    }

    @Test
    fun pausedTimeDoesNotCountEvenForHours() {
        // 25:00 → pause at 18:30 → locked 2 hours → still 18:30 → resume → 18:29.
        val clock = FakeClock()
        var s = start(clock)
        clock.advance(6 * MIN + 30_000)
        s = s.paused(clock)
        assertEquals(SessionState.PAUSED, s.sessionState)
        clock.advance(120 * MIN)
        assertEquals("18:30", formatClock(s.remainingMs(clock)))
        s = s.resumed(clock)
        clock.advance(1_000)
        assertEquals("18:29", formatClock(s.remainingMs(clock)))
    }

    @Test
    fun storyProgressSurvivesPauseResume() {
        val clock = FakeClock()
        var s = start(clock, minutes = 100)
        clock.advance(63 * MIN)
        s = s.paused(clock)
        clock.advance(500 * MIN)
        assertEquals(0.63f, s.progress(clock), 0.0001f)
        s = s.resumed(clock)
        assertEquals(0.63f, s.progress(clock), 0.0001f)
    }

    @Test
    fun changingTheSystemClockDoesNotCorruptTheSession() {
        val clock = FakeClock()
        val s = start(clock)
        clock.advance(5 * MIN)
        clock.wall += 3 * 60 * MIN // user moves the clock forward three hours
        assertEquals("20:00", formatClock(s.remainingMs(clock)))
        clock.wall -= 10 * 60 * MIN // and back
        assertEquals("20:00", formatClock(s.remainingMs(clock)))
    }

    @Test
    fun rebootFallsBackToWallClockAndReanchors() {
        val clock = FakeClock()
        var s = start(clock)
        clock.advance(10 * MIN)
        // Reboot: monotonic clock restarts, boot count changes; phone was off 2 minutes.
        clock.boot += 1
        clock.elapsed = 30_000
        clock.wall += 2 * MIN
        assertEquals("13:00", formatClock(s.remainingMs(clock)))
        s = s.rebased(clock)
        clock.advance(MIN)
        assertEquals("12:00", formatClock(s.remainingMs(clock)))
    }

    @Test
    fun expiresAndClampsAtZero() {
        val clock = FakeClock()
        val s = start(clock)
        assertFalse(s.isDue(clock))
        clock.advance(40 * MIN)
        assertTrue(s.isDue(clock))
        assertEquals(0L, s.remainingMs(clock))
        assertEquals(1f, s.progress(clock), 0f)
    }

    @Test
    fun pausedSessionIsNeverDue() {
        val clock = FakeClock()
        val s = start(clock).paused(clock)
        clock.advance(400 * MIN)
        assertFalse(s.isDue(clock))
    }

    @Test
    fun jsonRoundTripKeepsEverything() {
        val clock = FakeClock()
        var s = start(clock)
        clock.advance(3 * MIN)
        s = s.paused(clock).copy(distractionsBlocked = 2)
        val back = ActiveSession.fromJson(s.toJson(clock))
        assertNotNull(back)
        assertEquals(s, back)
    }

    @Test
    fun clockFormatRoundsUp() {
        assertEquals("00:01", formatClock(1))
        assertEquals("00:00", formatClock(0))
        assertEquals("25:00", formatClock(25 * MIN))
        assertEquals("17:42", formatClock(17 * MIN + 42_000))
    }
}
