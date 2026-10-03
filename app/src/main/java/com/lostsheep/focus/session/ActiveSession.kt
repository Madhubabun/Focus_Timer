package com.lostsheep.focus.session

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

enum class SessionState { IDLE, RUNNING, PAUSED, COMPLETED, ABANDONED, RECOVERABLE }

/** Clocks the session math depends on, so it can be tested and so it never trusts a single source. */
interface TimeSource {
    /** Monotonic time since boot, including deep sleep. Immune to the user changing the clock. */
    fun elapsedRealtime(): Long

    /** Wall-clock time; only used to bridge a reboot, when the monotonic clock restarts. */
    fun wallTime(): Long

    /** Changes on every reboot; tells us whether [elapsedRealtime] values are comparable. */
    fun bootCount(): Int
}

/**
 * The one active focus session. Time is never counted down in memory: elapsed time is
 * banked time plus the length of the current running segment, measured on the monotonic clock.
 */
data class ActiveSession(
    val sessionId: String,
    val storyId: String,
    val startTime: Long,
    val plannedDurationMs: Long,
    /** Focus time banked before the current running segment. */
    val bankedMs: Long,
    /** [TimeSource.elapsedRealtime] when the current running segment began. */
    val segmentStartElapsed: Long,
    /** [TimeSource.wallTime] when the current running segment began (reboot fallback). */
    val segmentStartWall: Long,
    val bootCount: Int,
    val paused: Boolean,
    val pauseTimestamp: Long?,
    val selectedBlockedApps: Set<String>,
    val sessionState: SessionState,
    val distractionsBlocked: Int,
) {
    val isActive: Boolean
        get() = sessionState == SessionState.RUNNING ||
            sessionState == SessionState.PAUSED ||
            sessionState == SessionState.RECOVERABLE

    fun elapsedMs(clock: TimeSource): Long {
        if (paused) return bankedMs.coerceIn(0, plannedDurationMs)
        val segment = if (clock.bootCount() == bootCount && clock.elapsedRealtime() >= segmentStartElapsed) {
            clock.elapsedRealtime() - segmentStartElapsed
        } else {
            // Rebooted since this segment started: fall back to the wall clock, never negative.
            (clock.wallTime() - segmentStartWall).coerceAtLeast(0)
        }
        return (bankedMs + segment).coerceIn(0, plannedDurationMs)
    }

    fun remainingMs(clock: TimeSource): Long = plannedDurationMs - elapsedMs(clock)

    /** Story progress 0..1. The animation is driven by this, never by frame counts. */
    fun progress(clock: TimeSource): Float =
        if (plannedDurationMs <= 0) 1f else elapsedMs(clock).toFloat() / plannedDurationMs

    fun isDue(clock: TimeSource): Boolean = !paused && remainingMs(clock) <= 0

    fun paused(clock: TimeSource): ActiveSession = if (paused) this else copy(
        bankedMs = elapsedMs(clock),
        paused = true,
        pauseTimestamp = clock.wallTime(),
        sessionState = SessionState.PAUSED,
    )

    fun resumed(clock: TimeSource): ActiveSession = if (!paused) rebased(clock).copy(sessionState = SessionState.RUNNING) else copy(
        paused = false,
        pauseTimestamp = null,
        segmentStartElapsed = clock.elapsedRealtime(),
        segmentStartWall = clock.wallTime(),
        bootCount = clock.bootCount(),
        sessionState = SessionState.RUNNING,
    )

    /** Banks the running segment and restarts it on the current clocks (used after a reboot). */
    fun rebased(clock: TimeSource): ActiveSession = if (paused) this else copy(
        bankedMs = elapsedMs(clock),
        segmentStartElapsed = clock.elapsedRealtime(),
        segmentStartWall = clock.wallTime(),
        bootCount = clock.bootCount(),
    )

    fun toJson(clock: TimeSource): String = JSONObject().apply {
        put("sessionId", sessionId)
        put("storyId", storyId)
        put("startTime", startTime)
        put("plannedDuration", plannedDurationMs)
        put("bankedMs", bankedMs)
        put("segmentStartElapsed", segmentStartElapsed)
        put("segmentStartWall", segmentStartWall)
        put("bootCount", bootCount)
        put("paused", paused)
        if (pauseTimestamp != null) put("pauseTimestamp", pauseTimestamp)
        put("selectedBlockedApps", JSONArray(selectedBlockedApps.toList()))
        put("sessionState", sessionState.name)
        put("distractionsBlocked", distractionsBlocked)
        // Snapshots for inspection and recovery; the authoritative values are recomputed from the clocks.
        put("elapsedTime", elapsedMs(clock))
        put("remainingTime", remainingMs(clock))
        put("storyProgress", progress(clock).toDouble())
    }.toString()

    companion object {
        fun start(
            sessionId: String,
            storyId: String,
            durationMs: Long,
            blockedApps: Set<String>,
            clock: TimeSource,
        ) = ActiveSession(
            sessionId = sessionId,
            storyId = storyId,
            startTime = clock.wallTime(),
            plannedDurationMs = durationMs,
            bankedMs = 0,
            segmentStartElapsed = clock.elapsedRealtime(),
            segmentStartWall = clock.wallTime(),
            bootCount = clock.bootCount(),
            paused = false,
            pauseTimestamp = null,
            selectedBlockedApps = blockedApps,
            sessionState = SessionState.RUNNING,
            distractionsBlocked = 0,
        )

        fun fromJson(json: String): ActiveSession? = runCatching {
            val o = JSONObject(json)
            val apps = o.optJSONArray("selectedBlockedApps") ?: JSONArray()
            ActiveSession(
                sessionId = o.getString("sessionId"),
                storyId = o.optString("storyId", "lost-sheep"),
                startTime = o.getLong("startTime"),
                plannedDurationMs = o.getLong("plannedDuration"),
                bankedMs = o.getLong("bankedMs"),
                segmentStartElapsed = o.getLong("segmentStartElapsed"),
                segmentStartWall = o.getLong("segmentStartWall"),
                bootCount = o.getInt("bootCount"),
                paused = o.getBoolean("paused"),
                pauseTimestamp = if (o.has("pauseTimestamp")) o.getLong("pauseTimestamp") else null,
                selectedBlockedApps = (0 until apps.length()).map { apps.getString(it) }.toSet(),
                sessionState = SessionState.valueOf(o.getString("sessionState")),
                distractionsBlocked = o.optInt("distractionsBlocked", 0),
            )
        }.getOrNull()
    }
}

/** What the user sees after a session closes; persisted so it survives the app being killed. */
data class SessionOutcome(
    val sessionId: String,
    val completed: Boolean,
    val plannedMs: Long,
    val focusedMs: Long,
    val distractionsBlocked: Int,
    val endedAt: Long,
) {
    fun toJson(): String = JSONObject().apply {
        put("sessionId", sessionId)
        put("completed", completed)
        put("plannedMs", plannedMs)
        put("focusedMs", focusedMs)
        put("distractionsBlocked", distractionsBlocked)
        put("endedAt", endedAt)
    }.toString()

    companion object {
        fun fromJson(json: String): SessionOutcome? = runCatching {
            val o = JSONObject(json)
            SessionOutcome(
                sessionId = o.getString("sessionId"),
                completed = o.getBoolean("completed"),
                plannedMs = o.getLong("plannedMs"),
                focusedMs = o.getLong("focusedMs"),
                distractionsBlocked = o.getInt("distractionsBlocked"),
                endedAt = o.getLong("endedAt"),
            )
        }.getOrNull()
    }
}

/** "17:42". Rounds up so the display never shows 00:00 while time remains. */
fun formatClock(ms: Long): String {
    val totalSec = ((ms.coerceAtLeast(0) + 999) / 1000)
    return String.format(Locale.ROOT, "%02d:%02d", totalSec / 60, totalSec % 60)
}
