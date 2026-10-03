package com.lostsheep.focus.story

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Where everyone is in the Lost Sheep scene for a given session progress.
 * Pure and continuous in [progress], so pausing at 63% and resuming shows exactly the 63% frame.
 *
 * World units: 1.0 = one screen width at zoom 1. The flock grazes around x 0.08..0.76;
 * the lost sheep waits near x 2.62, half hidden by a rock.
 */
data class SceneState(
    val shepherdX: Float,
    val shepherdFacing: Float,
    val shepherdWalking: Boolean,
    val kneel: Float,
    val look: Float,
    val carrying: Boolean,
    val embrace: Float,
    val lostSheepX: Float,
    val lostSheepFacing: Float,
    val lostSheepWalking: Boolean,
    val lostSheepHeadUp: Float,
    val cameraX: Float,
    val zoom: Float,
    /** 0 = clear morning light, 1 = warm golden evening. */
    val warmth: Float,
    /** 0..1 strength of the homecoming glow and drifting light. */
    val celebrate: Float,
    /** 0..1 how far the flock has gathered toward the returning shepherd. */
    val flockGather: Float,
    /** 0 = staff resting against him, 1 = staff in hand. */
    val staffHeld: Float,
)

/** One named moment of the story, from [start] progress until the next beat begins. */
data class StoryBeat(val index: Int, val start: Float, val name: String)

object LostSheepChoreography {

    const val FLOCK_START = 0.08f
    const val FLOCK_END = 0.76f
    const val STREAM_X = 1.90f
    val rocks = floatArrayOf(1.30f, 2.02f, 2.69f)
    val trees = floatArrayOf(0.95f, 1.12f, 1.62f, 1.72f, 2.30f, 2.86f)

    /**
     * Sixty named states. The scene is drawn continuously between them, but each one is a
     * distinct, recognisable moment, and the app reports the current one to screen readers.
     */
    val beats: List<StoryBeat> = listOf(
        0.000f to "flock_grazing", 0.010f to "shepherd_counting", 0.020f to "counting_the_ninety_nine",
        0.030f to "one_is_missing", 0.040f to "shepherd_turns", 0.050f to "looks_across_valley",
        0.065f to "shields_eyes", 0.080f to "picks_up_staff", 0.090f to "turns_from_flock",
        0.100f to "first_step", 0.110f to "second_step", 0.120f to "walking_01",
        0.135f to "walking_02", 0.150f to "walking_03", 0.160f to "stops_on_rise",
        0.170f to "looks_back", 0.180f to "looks_ahead", 0.190f to "walking_04",
        0.210f to "walking_05", 0.230f to "rocky_path", 0.250f to "reaches_rock",
        0.260f to "looks_behind_rock", 0.280f to "distant_glimpse", 0.300f to "glimpse_fades",
        0.320f to "walking_06", 0.340f to "climbing_hill", 0.350f to "journey_continues",
        0.370f to "walking_07", 0.390f to "green_valley", 0.410f to "olive_grove",
        0.430f to "reaches_stream", 0.440f to "looks_across_valley_again", 0.460f to "distant_glimpse_again",
        0.480f to "crosses_stream", 0.500f to "walking_08", 0.520f to "stops_to_listen",
        0.550f to "walking_09", 0.580f to "second_hill", 0.600f to "stops_on_hill",
        0.620f to "sees_the_sheep", 0.640f to "sheep_notices", 0.660f to "walks_closer",
        0.690f to "sheep_takes_a_step", 0.710f to "closer_still", 0.740f to "sheep_approaches",
        0.770f to "almost_together", 0.800f to "reaches_the_sheep", 0.820f to "kneels",
        0.840f to "gathers_the_sheep", 0.860f to "embraces_the_sheep", 0.880f to "the_sheep_is_safe",
        0.900f to "lifts_the_sheep", 0.920f to "rises_with_the_sheep", 0.940f to "stands_together",
        0.963f to "first_steps_home", 0.970f to "walking_through_valley", 0.977f to "flock_comes_into_view",
        0.987f to "approaches_flock", 0.993f to "flock_gathers", 0.997f to "wide_valley_sunlight",
    ).mapIndexed { i, (start, name) -> StoryBeat(i + 1, start, name) }

    fun beatAt(progress: Float): StoryBeat = beats.lastOrNull { progress >= it.start } ?: beats.first()

    private val shepherdKeys = keys(
        0f to 0.80f, 0.10f to 0.80f, // counts the flock, notices, takes up his staff
        0.16f to 0.98f, 0.19f to 0.98f, // stops on a rise, looks back and ahead
        0.25f to 1.20f, 0.30f to 1.20f, // looks behind the rock; a distant glimpse
        0.35f to 1.48f,
        0.43f to 1.80f, 0.47f to 1.80f, // at the stream, looking across the valley
        0.52f to 2.02f, 0.55f to 2.02f, // stops to listen
        0.60f to 2.22f, 0.66f to 2.22f, // sees the sheep; it sees him
        0.70f to 2.34f, 0.74f to 2.34f,
        0.80f to 2.46f, 0.963f to 2.46f, // reaches it, kneels, gathers it up, stands
        1.0f to 1.80f, // the final minute: walking home
    )

    private val lostSheepKeys = keys(
        0f to 2.62f, 0.69f to 2.62f,
        0.71f to 2.58f, 0.74f to 2.58f, // takes a step
        0.79f to 2.52f, // and comes to him
    )

    private val cameraKeys = keys(
        0f to 0.70f, 0.04f to 0.74f, // wide establishing shot of the flock
        0.08f to 0.92f, 0.12f to 0.98f,
        0.20f to 1.16f, 0.26f to 1.32f,
        0.29f to 1.92f, // the distant glimpse
        0.32f to 1.44f, 0.38f to 1.64f,
        0.44f to 1.88f, 0.46f to 2.20f, // and again
        0.50f to 1.98f, 0.58f to 2.18f,
        0.62f to 2.42f, 0.70f to 2.46f,
        0.80f to 2.49f, 0.90f to 2.48f,
        0.955f to 2.40f, 0.978f to 2.07f, 1.0f to 1.55f, // pulls back into the wide valley
    )

    private val zoomKeys = keys(
        0f to 0.50f, 0.04f to 0.54f,
        0.08f to 0.60f, 0.12f to 0.62f,
        0.20f to 0.66f, 0.26f to 0.62f,
        0.29f to 0.38f,
        0.32f to 0.64f, 0.38f to 0.70f,
        0.44f to 0.70f, 0.46f to 0.55f,
        0.50f to 0.72f, 0.58f to 0.76f,
        0.62f to 0.80f, 0.70f to 0.92f,
        0.80f to 1.10f, 0.90f to 1.18f, // close and intimate
        0.955f to 1.0f, 1.0f to 0.50f,
    )

    private val warmthKeys = keys(
        0f to 0f, 0.10f to 0.08f, 0.35f to 0.35f, 0.60f to 0.62f, 0.80f to 0.82f, 1f to 1f,
    )

    private val lookWindows = listOf(
        0.050f to 0.080f, // across the valley, shading his eyes
        0.255f to 0.300f, // behind the rock, then the far glimpse
        0.440f to 0.470f, // across the valley again
        0.600f to 0.640f, // he sees the sheep
    )

    /**
     * @param celebrationSec seconds since completion, negative when the session is not complete.
     */
    fun at(progress: Float, celebrationSec: Float = -1f): SceneState {
        val p = progress.coerceIn(0f, 1f)
        val celebrating = celebrationSec >= 0f

        var shepherdX = track(shepherdKeys, p)
        val dx = track(shepherdKeys, min(1f, p + 0.002f)) - track(shepherdKeys, max(0f, p - 0.002f))
        var walking = abs(dx) > 1e-4f
        // Facing the flock while counting, a glance back from the rise, home at the end.
        val facing = when {
            p < 0.04f -> -1f
            p in 0.168f..0.180f -> -1f
            p >= 0.963f || dx < -1e-4f -> -1f
            else -> 1f
        }

        val kneel = window(p, 0.80f, 0.94f, 0.025f)
        val embrace = window(p, 0.82f, 0.90f, 0.02f)
        val carrying = p >= 0.90f
        val look = lookWindows.maxOf { (a, b) -> window(p, a, b, 0.008f) }

        var sheepX = track(lostSheepKeys, p)
        val sdx = track(lostSheepKeys, min(1f, p + 0.002f)) - track(lostSheepKeys, max(0f, p - 0.002f))
        var sheepWalking = abs(sdx) > 1e-4f
        val sheepFacing = if (p >= 0.64f) -1f else 1f
        val headUp = if (p < 0.64f) 0f else smooth(((p - 0.64f) / 0.015f).coerceIn(0f, 1f))
        if (p >= 0.79f) {
            sheepX = shepherdX + 0.06f * facing
            sheepWalking = false
        }

        var cam = track(cameraKeys, p)
        var zoom = track(zoomKeys, p)
        var gather = smooth(((p - 0.975f) / 0.025f).coerceIn(0f, 1f))
        // The last seconds fill with warm light.
        var celebrate = smooth(((p - 0.985f) / 0.015f).coerceIn(0f, 1f)) * 0.6f
        if (celebrating) {
            // After 00:00 the two keep walking home toward the waiting flock.
            val k = smooth((celebrationSec / 40f).coerceIn(0f, 1f))
            shepherdX -= 0.75f * k
            walking = k < 1f
            cam -= 0.2f * k
            zoom = 0.50f
            gather = 1f
            celebrate = 0.6f + 0.4f * smooth((celebrationSec / 2.5f).coerceIn(0f, 1f))
        }

        return SceneState(
            shepherdX = shepherdX,
            shepherdFacing = facing,
            shepherdWalking = walking,
            kneel = kneel,
            look = if (carrying) 0f else look,
            carrying = carrying,
            embrace = embrace,
            lostSheepX = sheepX,
            lostSheepFacing = sheepFacing,
            lostSheepWalking = sheepWalking,
            lostSheepHeadUp = headUp,
            cameraX = cam,
            zoom = zoom,
            warmth = track(warmthKeys, p),
            celebrate = celebrate,
            flockGather = gather,
            staffHeld = smooth(((p - 0.075f) / 0.012f).coerceIn(0f, 1f)),
        )
    }

    private fun keys(vararg pairs: Pair<Float, Float>): List<Pair<Float, Float>> = pairs.toList()

    /** Eased interpolation between keyframes, so movement starts and stops softly. */
    fun track(keys: List<Pair<Float, Float>>, p: Float): Float {
        if (p <= keys.first().first) return keys.first().second
        for (i in 1 until keys.size) {
            val (p1, v1) = keys[i]
            if (p <= p1) {
                val (p0, v0) = keys[i - 1]
                val f = if (p1 == p0) 1f else (p - p0) / (p1 - p0)
                return v0 + (v1 - v0) * smooth(f)
            }
        }
        return keys.last().second
    }

    /** 0 outside [a, b], 1 inside, with soft edges of width [edge]. */
    private fun window(p: Float, a: Float, b: Float, edge: Float): Float = when {
        p <= a || p >= b -> 0f
        p < a + edge -> smooth((p - a) / edge)
        p > b - edge -> smooth((b - p) / edge)
        else -> 1f
    }

    fun smooth(x: Float): Float = x * x * (3 - 2 * x)

    /** Ground height (fraction of scene height) under world x. */
    fun groundY(x: Float): Float =
        0.72f + 0.020f * kotlin.math.sin(x * 2.3f + 0.4f) + 0.010f * kotlin.math.sin(x * 6.1f)
}
