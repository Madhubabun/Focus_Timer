package com.lostsheep.focus.story

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Where everyone is in the Lost Sheep scene for a given session progress.
 * Pure and continuous in [progress], so pausing at 63% and resuming shows exactly the 63% frame.
 *
 * World units: 1.0 = one screen width at zoom 1. The search is a long walk to the right through
 * connected terrains (see [Terrain]); the path circles the mountain, so carrying the sheep home
 * he crests its hill and comes down to the flock grazing in the home pasture at [LOOP] + x.
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
    /** Vertical camera offset in scene heights; follows the climbs and looks down from high ground. */
    val cameraY: Float = 0f,
)

/** One named moment of the story, from [start] progress until the next beat begins. */
data class StoryBeat(val index: Int, val start: Float, val name: String)

/** The terrains of the search, in walking order. Each blends softly into the next. */
enum class Terrain(val from: Float, val to: Float) {
    PASTURE(-9f, 1.7f),
    MEADOW(1.7f, 3.4f),
    OLIVE_GROVE(3.4f, 5.3f),
    ROCKY_HILLSIDE(5.3f, 7.1f),
    WIDE_VALLEY(7.1f, 9.3f),
    STREAM(9.3f, 10.3f),
    WOODED_HILLSIDE(10.3f, 12.0f),
    HIGH_RIDGE(12.0f, 13.3f),
    SHEEP_HILL(13.3f, 15.3f),
    HOME_PASTURE(15.3f, 40f),
}

object LostSheepChoreography {

    const val FLOCK_START = 0.08f
    const val FLOCK_END = 0.76f
    /** The path circles the mountain: the home pasture comes round again this far along. */
    const val LOOP = 16.0f
    const val STREAM_X = 9.82f
    /** Where the lost sheep waits, half hidden by [rocks]' last entry. */
    const val LOST_SHEEP_X = 14.70f
    /** Rocks placed by hand where the story needs them; the terrain adds its own. */
    val rocks = floatArrayOf(LOST_SHEEP_X + 0.075f)
    val trees = floatArrayOf(0.95f, 1.12f)

    /** The searching part of the story, progress 0 until the shepherd reaches the sheep. */
    const val SEARCH_END = 0.80f
    const val SEARCH_FRAMES = 720

    private val searchChapters = listOf(
        60 to "leaving_the_flock",
        70 to "green_pasture",
        70 to "olive_grove",
        70 to "rocky_hillside",
        80 to "wide_valley",
        60 to "stream_crossing",
        60 to "wooded_hillside",
        60 to "high_ridge",
        70 to "first_sight",
        120 to "drawing_near",
    )

    private val foundAndHome = listOf(
        0.800f to "reaches_the_sheep", 0.820f to "kneels", 0.840f to "gathers_the_sheep",
        0.860f to "embraces_the_sheep", 0.880f to "the_sheep_is_safe", 0.900f to "lifts_the_sheep",
        0.920f to "rises_with_the_sheep", 0.940f to "stands_together", 0.963f to "first_steps_home",
        0.970f to "over_the_hill", 0.977f to "flock_comes_into_view", 0.987f to "approaches_flock",
        0.993f to "flock_gathers", 0.997f to "wide_valley_sunlight",
    )

    /**
     * Named states: 720 for the search (in ten chapters, e.g. `olive_grove_034`), then the found
     * and homeward moments. The scene is drawn continuously between them; each name marks a
     * distinct moment and is what screen readers report.
     */
    val beats: List<StoryBeat> = buildList {
        var frame = 0
        for ((count, chapter) in searchChapters) {
            for (k in 1..count) {
                add(StoryBeat(size + 1, SEARCH_END * frame / SEARCH_FRAMES, String.format(Locale.ROOT, "%s_%03d", chapter, k)))
                frame++
            }
        }
        for ((start, name) in foundAndHome) add(StoryBeat(size + 1, start, name))
    }

    fun beatAt(progress: Float): StoryBeat {
        val p = progress.coerceIn(0f, 1f)
        // Beats are sorted by start; binary search for the last one at or before p.
        var lo = 0
        var hi = beats.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (beats[mid].start <= p) lo = mid else hi = mid - 1
        }
        return beats[lo]
    }

    // Where he stands. Repeated values are pauses: he stops, looks around, then walks on.
    private val shepherdKeys = keys(
        0f to 0.80f, 0.045f to 0.80f, // counts the flock, notices, takes up his staff
        0.066f to 1.25f, // walks away; the flock grows small behind him
        0.095f to 2.20f, 0.108f to 2.20f, // stops in the meadow and looks around
        0.144f to 3.30f,
        0.180f to 4.35f, 0.192f to 4.35f, // among the olive trees, looking
        0.222f to 5.20f,
        0.255f to 6.10f, 0.266f to 6.10f, // a breath on the rocky climb
        0.300f to 7.05f, 0.326f to 7.05f, // the viewpoint: the whole valley below
        0.389f to 9.35f,
        0.400f to 9.62f, 0.414f to 9.62f, // at the stream, finding a safe crossing
        0.440f to 10.02f, // across the stones
        0.456f to 10.30f,
        0.490f to 11.15f, 0.501f to 11.15f, // in the woods, looking left and right
        0.522f to 11.75f,
        0.560f to 12.68f, 0.589f to 12.68f, // on the high ridge, looking across the land
        0.600f to 12.80f, 0.622f to 12.80f, // he sees it, far away
        0.667f to 13.55f,
        0.700f to 14.02f, 0.716f to 14.02f, // the sheep lifts its head; he waits
        0.750f to 14.30f,
        0.800f to 14.55f, 0.963f to 14.55f, // reaches it, kneels, gathers it up, stands
        1.0f to 15.55f, // the final minute: over the hill, home to the flock
    )

    private val lostSheepKeys = keys(
        0f to LOST_SHEEP_X, 0.735f to LOST_SHEEP_X, // grazes; notices him; hesitates
        0.770f to 14.63f, // then comes toward him
        0.790f to 14.60f,
    )

    /** How far ahead of the shepherd the camera looks. Large values are the wide reveals. */
    private val leadKeys = keys(
        0f to -0.15f, 0.030f to -0.10f, 0.045f to -0.05f,
        0.066f to -0.12f, // looking back over the flock as he leaves it
        0.090f to 0.12f, 0.108f to 0.05f, 0.144f to 0.16f,
        0.180f to 0.06f, 0.222f to 0.14f,
        0.266f to 0.10f, 0.300f to 0.30f, 0.326f to 0.50f, // the valley opens up
        0.360f to 0.22f, 0.389f to 0.14f,
        0.414f to 0.10f, 0.440f to 0.02f, 0.456f to 0.14f,
        0.501f to 0.10f, 0.522f to 0.16f,
        0.560f to 0.20f, 0.589f to 0.40f,
        0.605f to 0.95f, 0.622f to 0.95f, // the tiny white shape far off
        0.667f to 0.48f, 0.700f to 0.34f, 0.750f to 0.16f,
        0.800f to 0.04f, 0.900f to 0.02f, 0.955f to 0.0f,
        1.0f to 0.38f, // the flock ahead in the home pasture
    )

    private val zoomKeys = keys(
        0f to 0.50f, 0.030f to 0.56f, 0.045f to 0.62f, // wide establishing shot
        0.066f to 0.54f,
        0.100f to 0.74f, 0.144f to 0.62f, // meadow: medium, then wider
        0.180f to 0.84f, 0.222f to 0.70f, // olive grove: close among the trees
        0.255f to 0.80f, 0.300f to 0.62f, // rocky climb
        0.326f to 0.38f, 0.360f to 0.42f, 0.389f to 0.58f, // the wide valley; he is small in it
        0.414f to 0.90f, 0.440f to 0.98f, 0.456f to 0.74f, // careful steps across the stream
        0.490f to 0.82f, 0.522f to 0.70f, // the woods
        0.560f to 0.58f, 0.589f to 0.36f, // the ridge and the enormous land beyond
        0.605f to 0.30f, 0.622f to 0.30f,
        0.667f to 0.55f, 0.700f to 0.72f, 0.750f to 0.86f, // drawing near
        0.800f to 1.10f, 0.900f to 1.18f, // close and intimate
        0.955f to 1.0f, 1.0f to 0.50f,
    )

    /** Extra look-down from high ground, so the land below fills the frame. */
    private val tiltKeys = keys(
        0f to 0f, 0.300f to 0f, 0.326f to 0.035f, 0.380f to 0f,
        0.560f to 0f, 0.589f to 0.04f, 0.622f to 0.03f, 0.667f to 0f, 1f to 0f,
    )

    private val warmthKeys = keys(
        0f to 0f, 0.10f to 0.08f, 0.35f to 0.35f, 0.60f to 0.62f, 0.80f to 0.82f, 1f to 1f,
    )

    private val lookWindows = listOf(
        0.018f to 0.030f, // across the flock
        0.030f to 0.045f, // toward the distant hills
        0.095f to 0.108f, // around the meadow
        0.180f to 0.192f, // between the olive trees
        0.255f to 0.266f, // over the rocks
        0.302f to 0.326f, // across the valley
        0.400f to 0.414f, // along the stream for a crossing
        0.490f to 0.501f, // through the woods
        0.562f to 0.589f, // from the ridge
        0.600f to 0.622f, // he sees the sheep
    )

    /**
     * @param celebrationSec seconds since completion, negative when the session is not complete.
     */
    fun at(progress: Float, celebrationSec: Float = -1f): SceneState {
        val p = progress.coerceIn(0f, 1f)
        val celebrating = celebrationSec >= 0f

        var shepherdX = glide(shepherdKeys, p)
        val dx = glide(shepherdKeys, min(1f, p + 0.0015f)) - glide(shepherdKeys, max(0f, p - 0.0015f))
        var walking = abs(dx) > 2e-4f
        val facing = when {
            p < 0.030f -> -1f // counting the flock
            p in 0.072f..0.080f -> -1f // a last look back at them
            p in 0.494f..0.498f -> -1f // looking the other way through the trees
            else -> 1f
        }

        val kneel = window(p, 0.80f, 0.94f, 0.025f)
        val embrace = window(p, 0.82f, 0.90f, 0.02f)
        val carrying = p >= 0.90f
        val look = lookWindows.maxOf { (a, b) -> window(p, a, b, 0.004f) }

        var sheepX = glide(lostSheepKeys, p)
        val sdx = glide(lostSheepKeys, min(1f, p + 0.0015f)) - glide(lostSheepKeys, max(0f, p - 0.0015f))
        var sheepWalking = abs(sdx) > 2e-4f
        val sheepFacing = if (p >= 0.705f) -1f else 1f
        val headUp = if (p < 0.700f) 0f else smooth(((p - 0.700f) / 0.012f).coerceIn(0f, 1f))
        if (p >= 0.79f) {
            sheepX = shepherdX + 0.06f * facing
            sheepWalking = false
        }

        var cam = shepherdX + glide(leadKeys, p)
        var zoom = glide(zoomKeys, p)
        var gather = smooth(((p - 0.975f) / 0.025f).coerceIn(0f, 1f))
        // The last seconds fill with warm light.
        var celebrate = smooth(((p - 0.985f) / 0.015f).coerceIn(0f, 1f)) * 0.6f
        if (celebrating) {
            // After 00:00 the two keep walking down into the home pasture to the waiting flock.
            val k = smooth((celebrationSec / 40f).coerceIn(0f, 1f))
            shepherdX += 0.80f * k
            walking = k < 1f
            cam += 0.62f * k
            zoom = 0.50f
            gather = 1f
            celebrate = 0.6f + 0.4f * smooth((celebrationSec / 2.5f).coerceIn(0f, 1f))
        }
        val camY = 0.6f * (groundY(cam) - 0.72f) + glide(tiltKeys, p)

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
            staffHeld = smooth(((p - 0.038f) / 0.008f).coerceIn(0f, 1f)),
            cameraY = camY,
        )
    }

    /** 0..1 presence of [terrain] at world x, blending over [edge] units at each border. */
    fun terrainWeight(terrain: Terrain, x: Float, edge: Float = 0.45f): Float {
        val a = smooth(((x - terrain.from) / edge + 0.5f).coerceIn(0f, 1f))
        val b = smooth(((terrain.to - x) / edge + 0.5f).coerceIn(0f, 1f))
        return min(a, b)
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

    /**
     * Smooth monotone interpolation (Fritsch–Carlson). Unlike [track] it flows through keys
     * without stopping; it only comes to rest where two keys hold the same value.
     */
    fun glide(keys: List<Pair<Float, Float>>, p: Float): Float {
        if (p <= keys.first().first) return keys.first().second
        if (p >= keys.last().first) return keys.last().second
        var i = 1
        while (p > keys[i].first) i++
        val (x0, y0) = keys[i - 1]
        val (x1, y1) = keys[i]
        val h = x1 - x0
        if (h <= 0f) return y1
        val m0 = tangent(keys, i - 1)
        val m1 = tangent(keys, i)
        val t = (p - x0) / h
        val t2 = t * t
        val t3 = t2 * t
        return (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + t) * h * m0 + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * h * m1
    }

    private fun tangent(keys: List<Pair<Float, Float>>, i: Int): Float {
        fun slope(a: Int) = (keys[a + 1].second - keys[a].second) / (keys[a + 1].first - keys[a].first)
        if (i == 0 || i == keys.size - 1) return 0f
        val s0 = slope(i - 1)
        val s1 = slope(i)
        if (s0 * s1 <= 0f) return 0f // a hold or a turn: come to rest
        // Harmonic mean keeps the curve from overshooting between keys.
        return 2f * s0 * s1 / (s0 + s1)
    }

    /** 0 outside [a, b], 1 inside, with soft edges of width [edge]. */
    private fun window(p: Float, a: Float, b: Float, edge: Float): Float = when {
        p <= a || p >= b -> 0f
        p < a + edge -> smooth((p - a) / edge)
        p > b - edge -> smooth((b - p) / edge)
        else -> 1f
    }

    fun smooth(x: Float): Float = x * x * (3 - 2 * x)

    // The lie of the land along the path: climbs to a viewpoint, down into the valley, up to the ridge.
    private val groundKeys = keys(
        -9f to 0.72f, 1.6f to 0.72f, 3.4f to 0.715f, 5.2f to 0.71f, 6.2f to 0.685f,
        7.1f to 0.66f, 7.9f to 0.735f, 9.2f to 0.742f, 10.3f to 0.742f, 11.2f to 0.715f,
        12.0f to 0.685f, 12.7f to 0.64f, 13.4f to 0.668f, 14.2f to 0.676f, 14.8f to 0.666f,
        15.6f to 0.70f, 16.3f to 0.72f, 40f to 0.72f,
    )

    /** Ground height (fraction of scene height) under world x. */
    fun groundY(x: Float): Float {
        // The home pasture repeats around the loop, with the same gentle swell.
        val u = if (x > LOOP - 0.5f) x - LOOP else x
        val swell = 0.020f * sin(u * 2.3f + 0.4f) + 0.010f * sin(u * 6.1f)
        val steep = 1f - 0.5f * terrainWeight(Terrain.HIGH_RIDGE, x)
        return glide(groundKeys, x) + swell * steep
    }

    /** Deterministic 0..1 noise for placing things along the path. */
    fun hash(i: Int, salt: Int): Float {
        var v = i * 374761393 + salt * 668265263
        v = (v xor (v ushr 13)) * 1274126177
        v = v xor (v ushr 16)
        return (v and 0xFFFFFF) / 16777216f
    }

    fun slot(x: Float, spacing: Float): Int = floor(x / spacing).toInt()
}
