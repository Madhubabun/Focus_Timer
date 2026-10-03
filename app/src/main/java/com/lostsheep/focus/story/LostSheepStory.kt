package com.lostsheep.focus.story

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Matthew 18:12–13, told as a focus session. */
object LostSheepStory : FocusStory {
    override val id = "lost-sheep"
    override val title = "The Lost Sheep"
    override val reference = "Matthew 18:12–13"
    override val completionTitle = "The lost sheep is found."

    override val phases = listOf(
        StoryPhase(
            "notice", 0f, 0.10f, "One Is Missing", "One is missing.", false,
            "A flock grazes in a green valley below the mountains. The shepherd counts them and notices one is missing.",
        ),
        StoryPhase(
            "search", 0.10f, 0.35f, "The Search", null, false,
            "The shepherd takes up his staff, leaves the flock and walks out across the hills, searching.",
        ),
        StoryPhase(
            "journey", 0.35f, 0.60f, "The Journey", "Keep going.", false,
            "He crosses the valley and the stream. Far away, the lost sheep can sometimes be seen.",
        ),
        StoryPhase(
            "approach", 0.60f, 0.80f, "Drawing Near", null, false,
            "From the hill he sees the sheep. It notices him, and slowly they come toward each other.",
        ),
        StoryPhase(
            "found", 0.80f, 0.95f, "Found", "You are not forgotten.", false,
            "The shepherd kneels and gathers the sheep into his arms in the warm light. It is safe.",
        ),
        StoryPhase(
            "return", 0.95f, 1f, "Homeward", null, false,
            "Carrying the sheep, he walks back through the valley to the waiting flock.",
        ),
    )

    override fun stateNameAt(progress: Float): String {
        val beat = LostSheepChoreography.beatAt(progress)
        return "scene_%04d_%s".format(java.util.Locale.ROOT, beat.index, beat.name)
    }

    @Composable
    override fun Scene(
        progress: () -> Float,
        time: () -> Float,
        celebration: () -> Float,
        modifier: Modifier,
    ) {
        val props = remember { SceneProps.create() }
        Canvas(modifier) {
            val base = LostSheepChoreography.at(progress(), celebration())
            val t = time()
            // A very slow breathing camera keeps the scene alive without drawing attention.
            val st = base.copy(
                cameraX = base.cameraX + 0.006f * sin(t * 0.05f),
                zoom = base.zoom * (1f + 0.008f * sin(t * 0.037f)),
            )
            drawLostSheepScene(st, t, props)
        }
    }
}

/** Fixed random layout, generated once. */
internal class SceneProps(
    val flockX: FloatArray,
    val flockDepth: FloatArray,
    val flockPhase: FloatArray,
    val flockFacing: FloatArray,
    val flockResting: BooleanArray,
    val bladeX: FloatArray,
    val bladeLen: FloatArray,
    val moteX: FloatArray,
    val moteSpeed: FloatArray,
    val motePhase: FloatArray,
    val moteSize: FloatArray,
    val flowerX: FloatArray,
    val flowerDepth: FloatArray,
) {
    companion object {
        fun create(): SceneProps {
            val r = Random(18)
            val n = 99 // the ninety-nine
            val order = (0 until n).map { r.nextFloat() }.sortedDescending() // far ones first
            val blades = 72
            val motes = 36
            val flowers = 160
            return SceneProps(
                flockX = FloatArray(n) { LostSheepChoreography.FLOCK_START + r.nextFloat() * (LostSheepChoreography.FLOCK_END - LostSheepChoreography.FLOCK_START) },
                flockDepth = FloatArray(n) { order[it] },
                flockPhase = FloatArray(n) { r.nextFloat() * 2f * PI.toFloat() },
                flockFacing = FloatArray(n) { if (r.nextBoolean()) 1f else -1f },
                flockResting = BooleanArray(n) { r.nextFloat() < 0.2f },
                bladeX = FloatArray(blades) { r.nextFloat() * 1.3f },
                bladeLen = FloatArray(blades) { 0.45f + r.nextFloat() * 0.55f },
                moteX = FloatArray(motes) { r.nextFloat() },
                moteSpeed = FloatArray(motes) { 0.010f + r.nextFloat() * 0.016f },
                motePhase = FloatArray(motes) { r.nextFloat() },
                moteSize = FloatArray(motes) { 1.0f + r.nextFloat() * 1.8f },
                flowerX = FloatArray(flowers) { -0.4f + r.nextFloat() * 3.6f },
                flowerDepth = FloatArray(flowers) { r.nextFloat() },
            )
        }
    }
}

private const val ANCHOR = 0.66f
private const val FEET = 0.03f

/** Clear morning → soft golden afternoon → warm evening light, following the session. */
private fun tri(a: Long, b: Long, c: Long, w: Float): Color =
    if (w < 0.5f) lerp(Color(a), Color(b), w * 2f) else lerp(Color(b), Color(c), (w - 0.5f) * 2f)

private class Palette(w: Float) {
    val skyTop = tri(0xFF4F9EE0, 0xFF5AA2E0, 0xFF6EA6D8, w)
    val skyHorizon = tri(0xFFD3ECF8, 0xFFF2EBC4, 0xFFFFD99A, w)
    val sun = tri(0xFFFFFCEB, 0xFFFFF2C0, 0xFFFFE29A, w)
    val sunGlow = tri(0xFFFFF6D0, 0xFFFFE08A, 0xFFFFC46E, w)
    val peak = tri(0xFF7F9CC0, 0xFF8A9EC0, 0xFF93A3B8, w)
    val snow = tri(0xFFF6FAFF, 0xFFFFF8EA, 0xFFFFEBD0, w)
    val farMountain = tri(0xFF6F92B0, 0xFF7895A8, 0xFF87979C, w)
    val nearMountain = tri(0xFF5E8A86, 0xFF6A8C78, 0xFF77896A, w)
    val hills = tri(0xFF84BE62, 0xFF94BE58, 0xFF9EB355, w)
    val lake = tri(0xFF5FA8D8, 0xFF6AAAD2, 0xFF86AECB, w)
    val lakeLight = tri(0xFFCFEAF8, 0xFFF4EBC8, 0xFFFFE0A8, w)
    val ground = tri(0xFF62AC48, 0xFF74AE44, 0xFF82A744, w)
    val groundDeep = tri(0xFF3F8636, 0xFF4F8A32, 0xFF5C8534, w)
    val mound = tri(0xFF357430, 0xFF41742C, 0xFF4B6E2E, w)
    val path = tri(0xFFD8C79C, 0xFFDCC594, 0xFFE0BE88, w)
    val water = tri(0xFF4E9BD0, 0xFF5BA0CC, 0xFF74A6C4, w)
    val flower = Color(0xFFFFFFFF)
    val flowerGold = Color(0xFFFFC93A)
    val flowerRed = Color(0xFFE2503C)
    val flowerBlue = Color(0xFF7FA6E8)
    val tuft = tri(0xFF357231, 0xFF41722D, 0xFF4D6C2C, w)
    val fgGrass = tri(0xFF2A6428, 0xFF346226, 0xFF3E5C28, w)
    val olive = tri(0xFF6E9A5E, 0xFF7A9A58, 0xFF829456, w)
    val oliveLight = tri(0xFFA9C4A0, 0xFFB4C496, 0xFFC0C08E, w)
    val cypress = tri(0xFF2F5E38, 0xFF365E34, 0xFF3E5A32, w)
    val trunk = Color(0xFF6A4E3A)
    val rock = tri(0xFF9C968E, 0xFFA59A88, 0xFFA8927E, w)
    val rockLight = tri(0xFFD6D0C4, 0xFFDCCFB6, 0xFFE0C6AA, w)
    val wool = Color(0xFFFFFDF7)
    val woolShade = tri(0xFFE0DCD3, 0xFFE8DDC6, 0xFFEED3B6, w)
    val sheepDark = Color(0xFF3B302A)
    val bell = Color(0xFFE2B23C)
    val robe = Color(0xFFFBF7EC)
    val robeShade = tri(0xFFDAD4C8, 0xFFE2D5BC, 0xFFE8CDAA, w)
    val mantle = Color(0xFF8E2430)
    val mantleShade = Color(0xFF6E1A25)
    val skin = Color(0xFFC6916A)
    val hair = Color(0xFF3A2719)
    val sandal = Color(0xFF7A5232)
    val staff = Color(0xFF6E4A2E)
    val light = Color(0xFFFFE6A6)
    val cloud = Color(0xFFFFFFFF)
    val bird = Color(0xFF3E4A55)
    val vista = tri(0xFF7FAE8A, 0xFF8CAE80, 0xFF9AA878, w)
    val groveGround = tri(0xFF86B04C, 0xFF94B048, 0xFF9AA846, w)
    val rockyGround = tri(0xFFAFA77A, 0xFFB8A774, 0xFFBDA070, w)
    val woodsGround = tri(0xFF468A3A, 0xFF4E8636, 0xFF587E36, w)
    val ridgeGrass = tri(0xFFA6B85A, 0xFFB4B654, 0xFFBEAE52, w)
    val moundRock = tri(0xFF8C8058, 0xFF948052, 0xFF967A50, w)
    val moundWood = tri(0xFF2A5A28, 0xFF325A26, 0xFF3A5626, w)
    val oak = tri(0xFF3E7A3A, 0xFF487A36, 0xFF527236, w)
    val shade = Color(0xFF1E3A22)
}

/**
 * Maps world coordinates to screen. Far layers move and zoom less (parallax).
 * Horizontal scale never drops below 0.75 × height, so a tall phone shows a narrower slice of
 * the same world instead of squashing everyone together.
 */
private class Camera(val w: Float, val h: Float, val st: SceneState) {
    val xs = max(w, h * 0.75f)
    /** On tall screens the land sits lower, leaving open sky above for the timer. */
    val lift = if (h > w * 1.4f) 0.09f * h else 0f
    fun zoom(par: Float) = 1f + (st.zoom - 1f) * par
    fun sx(x: Float, par: Float = 1f) = w / 2f + (x - st.cameraX * par) * xs * zoom(par)
    fun sy(y: Float, par: Float = 1f) = ANCHOR * h + (y - ANCHOR - st.cameraY * par) * h * zoom(par) + lift
    fun worldX(screenX: Float, par: Float = 1f) = (screenX - w / 2f) / (xs * zoom(par)) + st.cameraX * par
}

private fun ridge(x: Float, base: Float, amp: Float, seed: Float): Float {
    val n = 0.55f * sin(x * 2.2f + seed) + 0.30f * sin(x * 5.3f + seed * 1.7f) + 0.15f * sin(x * 11.7f + seed * 0.3f)
    return base - amp * (0.5f + 0.5f * n)
}

/** Sharper, alpine profile for the distant snowy range. */
private fun peaks(x: Float): Float {
    fun tri(v: Float) = 1f - 2f * abs(frac(v) - 0.5f)
    val a = tri(x * 0.9f + 0.3f)
    val b = tri(x * 2.3f + 0.7f)
    val c = tri(x * 5.1f + 0.1f)
    return 0.53f - 0.22f * (0.6f * a * a + 0.28f * b + 0.12f * c)
}

private fun frac(v: Float) = v - floor(v)

private fun ground(x: Float) = LostSheepChoreography.groundY(x)

/** Centre of the stream at depth [y] (scene fraction), winding toward us. */
private fun streamX(y: Float) = LostSheepChoreography.STREAM_X + 0.05f * sin((y - 0.72f) * 16f) + (y - 0.74f) * 0.35f

internal fun DrawScope.drawLostSheepScene(st: SceneState, t: Float, props: SceneProps) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val c = Camera(w, h, st)
    val pal = Palette(st.warmth)
    fun at(terrain: Terrain, x: Float = st.cameraX) = LostSheepChoreography.terrainWeight(terrain, x)

    // Sky.
    drawRect(Brush.verticalGradient(listOf(pal.skyTop, pal.skyHorizon), startY = 0f, endY = c.sy(0.56f, 0.1f)))
    drawRect(pal.skyHorizon, topLeft = Offset(0f, c.sy(0.56f, 0.1f)), size = Size(w, h))

    // Sun, lowering into the afternoon as the session goes on.
    val sunC = Offset(w * (0.72f - 0.04f * sin(st.cameraX * 0.21f)), c.sy(0.20f + 0.16f * st.warmth, 0.05f))
    val glowR = max(w, h) * (0.30f + 0.015f * sin(t * 0.2f))
    drawCircle(
        Brush.radialGradient(listOf(pal.sunGlow.copy(alpha = 0.50f), pal.sunGlow.copy(alpha = 0f)), center = sunC, radius = glowR),
        radius = glowR,
        center = sunC,
    )
    drawCircle(pal.sun, radius = min(w, h) * 0.045f, center = sunC)

    // Clouds drifting slowly.
    for (i in 0 until 6) {
        val cx = (frac((i * 0.23f + t * (0.0025f + i * 0.0005f) - st.cameraX * 0.05f) / 1.5f) * 1.5f - 0.25f) * w
        val cy = c.sy(0.06f + 0.05f * i + 0.02f * (i % 2), 0.05f)
        val r = min(w, h) * (0.040f + 0.012f * (i % 3))
        drawCloud(cx, cy, r, pal.cloud.copy(alpha = 0.88f - 0.08f * i))
    }

    // Birds, now and then, crossing high up.
    for (i in 0 until 4) {
        val f = frac(t * (0.010f + 0.003f * i) + i * 0.27f)
        if (f > 0.55f) continue // they pass, then the sky is quiet for a while
        val bx = (f / 0.55f * 1.4f - 0.2f) * w
        val by = c.sy(0.14f + 0.045f * i + 0.01f * sin(t * 0.4f + i), 0.05f)
        val bs = min(w, h) * (0.014f - 0.002f * i)
        val flap = sin(t * 5.5f + i * 1.7f) * bs * 0.55f
        val col = pal.bird.copy(alpha = 0.65f)
        drawLine(col, Offset(bx - bs, by - flap), Offset(bx, by), strokeWidth = bs * 0.22f, cap = StrokeCap.Round)
        drawLine(col, Offset(bx + bs, by - flap), Offset(bx, by), strokeWidth = bs * 0.22f, cap = StrokeCap.Round)
    }

    // Distant snow-capped range, then nearer mountains.
    drawLayer(c, 0.08f, pal.peak) { peaks(it) }
    drawSnow(c, 0.08f, pal.snow) { peaks(it) }
    drawLayer(c, 0.18f, pal.farMountain) { ridge(it, 0.53f, 0.11f, 0.7f) }
    drawLayer(c, 0.32f, lerp(pal.nearMountain, pal.skyHorizon, 0.10f)) { ridge(it, 0.585f, 0.07f, 2.1f) }

    // From the viewpoint and the ridge, more valleys open up below the mountains.
    val vista = max(at(Terrain.WIDE_VALLEY), at(Terrain.HIGH_RIDGE)) * 0.9f + 0.1f * at(Terrain.SHEEP_HILL)
    if (vista > 0.02f) {
        drawLayer(c, 0.38f, pal.vista.copy(alpha = vista)) { ridge(it, 0.615f, 0.05f, 5.3f) }
        drawLayer(c, 0.46f, lerp(pal.vista, pal.hills, 0.5f).copy(alpha = vista)) { ridge(it, 0.640f, 0.04f, 7.9f) }
    }

    // Calm lakes in the valleys (placed in the hills' own parallax space).
    for ((lx, lw) in lakes) {
        val lakeY = c.sy(0.603f, 0.45f)
        val lakeL = c.sx(lx - lw / 2f, 0.45f)
        val lakeR = c.sx(lx + lw / 2f, 0.45f)
        if (lakeR < 0f || lakeL > w) continue
        val lh = h * 0.022f * c.zoom(0.45f) * (0.8f + 0.4f * lw)
        drawOval(pal.lake, Offset(lakeL, lakeY - lh), Size(lakeR - lakeL, lh * 2f))
        for (k in 0 until 7) {
            val sx = lakeL + (lakeR - lakeL) * (0.15f + 0.11f * k)
            val a = 0.35f + 0.35f * sin(t * 0.8f + k * 1.3f)
            val sy = lakeY - lh * 0.2f + (k % 3) * lh * 0.3f
            drawLine(
                pal.lakeLight.copy(alpha = a.coerceIn(0f, 1f)),
                Offset(sx, sy),
                Offset(sx + (lakeR - lakeL) * 0.05f, sy),
                strokeWidth = (lh * 0.12f).coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }
    }
    drawLayer(c, 0.55f, lerp(pal.hills, pal.ridgeGrass, 0.4f * at(Terrain.ROCKY_HILLSIDE))) { ridge(it, 0.655f, 0.045f, 4.2f) }

    // Distant cypresses on the hills.
    for (k in LostSheepChoreography.slot(c.worldX(-40f, 0.55f), 0.29f)..LostSheepChoreography.slot(c.worldX(w + 40f, 0.55f), 0.29f)) {
        val x = k * 0.29f + 0.05f * sin(k * 2.7f)
        val sx = c.sx(x, 0.55f)
        val by = c.sy(ridge(x, 0.655f, 0.045f, 4.2f) + 0.012f, 0.55f)
        val th = h * 0.035f * c.zoom(0.55f) * (0.8f + 0.5f * LostSheepChoreography.hash(k, 9))
        drawOval(pal.cypress, Offset(sx - th * 0.13f, by - th), Size(th * 0.26f, th))
    }

    val xL = c.worldX(-0.15f * w)
    val xR = c.worldX(1.15f * w)

    // The ground everyone walks on, colored by the terrain beneath it.
    val field = Path()
    val steps = 56
    field.moveTo(-10f, h + 10f)
    for (i in 0..steps) {
        val sx = -10f + (w + 20f) * i / steps
        field.lineTo(sx, c.sy(ground(c.worldX(sx))))
    }
    field.lineTo(w + 10f, h + 10f)
    field.close()
    val stops = Array(9) { i -> (i / 8f) to groundColor(pal, c.worldX(w * i / 8f)) }
    drawPath(field, Brush.horizontalGradient(*stops, startX = 0f, endX = w))
    drawPath(field, Brush.verticalGradient(listOf(Color.Transparent, pal.groundDeep.copy(alpha = 0.75f)), startY = c.sy(0.68f), endY = h))

    val unit = h * c.zoom(1f) // one scene-height at the current zoom

    // The mountain stream, running down toward us.
    drawStream(c, unit, t, pal)

    // A worn path, broken where it fords the stream.
    val trail = Path()
    var started = false
    var x = max(xL, 0.70f)
    while (x <= xR) {
        val ford = abs(x - LostSheepChoreography.STREAM_X) < 0.12f
        val px = c.sx(x)
        val py = c.sy(ground(x) + FEET + 0.004f + 0.008f * sin(x * 3.1f))
        if (ford) started = false else if (!started) trail.moveTo(px, py).also { started = true } else trail.lineTo(px, py)
        x += 0.02f
    }
    val pathColor = lerp(pal.path, pal.rockLight, 0.5f * at(Terrain.ROCKY_HILLSIDE))
    drawPath(trail, pathColor.copy(alpha = 0.55f), style = Stroke(width = unit * 0.016f, cap = StrokeCap.Round))

    // Trees behind the path: a few near the flock, olive groves, then the woods.
    for (tx in LostSheepChoreography.trees) {
        val sx = c.sx(tx)
        if (sx < -unit * 0.3f || sx > w + unit * 0.3f) continue
        drawOlive(sx, c.sy(ground(tx) + 0.004f), unit, t + tx * 3f, pal)
    }
    for (k in LostSheepChoreography.slot(xL - 0.2f, 0.08f)..LostSheepChoreography.slot(xR + 0.2f, 0.08f)) {
        val tx = k * 0.08f + 0.06f * LostSheepChoreography.hash(k, 1)
        if (tx < 1.4f) continue
        val olive = 0.85f * at(Terrain.OLIVE_GROVE, tx) + 0.10f * at(Terrain.MEADOW, tx) + 0.12f * at(Terrain.WIDE_VALLEY, tx) +
            0.10f * at(Terrain.SHEEP_HILL, tx)
        val wood = 0.80f * at(Terrain.WOODED_HILLSIDE, tx) + 0.06f * at(Terrain.HIGH_RIDGE, tx)
        val r = LostSheepChoreography.hash(k, 2)
        // Spread through the depth of the land: small ones far up the slope, bigger ones nearer.
        val depth = LostSheepChoreography.hash(k, 3)
        val back = -0.045f * (1f - depth) + 0.002f
        val scale = (0.55f + 0.5f * depth) * (0.85f + 0.3f * LostSheepChoreography.hash(k, 4))
        val sx = c.sx(tx)
        val by = c.sy(ground(tx) + back)
        when {
            r < olive -> drawOlive(sx, by, unit * scale, t + tx * 3f, pal)
            r < olive + wood -> if (LostSheepChoreography.hash(k, 5) < 0.35f) drawCypress(sx, by, unit * scale, t + tx, pal) else drawOak(sx, by, unit * scale, t + tx * 2f, pal)
        }
    }

    // Rocks, many on the hillside, a few everywhere else.
    for (k in LostSheepChoreography.slot(xL, 0.09f)..LostSheepChoreography.slot(xR, 0.09f)) {
        val rx = k * 0.09f + 0.05f * LostSheepChoreography.hash(k, 11)
        val density = 0.60f * at(Terrain.ROCKY_HILLSIDE, rx) + 0.22f * at(Terrain.HIGH_RIDGE, rx) +
            0.10f * at(Terrain.STREAM, rx) + 0.04f
        if (LostSheepChoreography.hash(k, 12) > density || rx < 1.0f) continue
        val depth = LostSheepChoreography.hash(k, 13)
        val near = depth > 0.55f
        val ry = ground(rx) + if (near) 0.04f + 0.12f * (depth - 0.55f) / 0.45f else -0.002f
        drawRock(c.sx(rx), c.sy(ry), unit * (0.45f + 0.7f * depth), pal)
    }

    // Grass tufts, thinning out on the rocky ground.
    for (k in LostSheepChoreography.slot(xL, 0.022f)..LostSheepChoreography.slot(xR, 0.022f)) {
        val gx = k * 0.022f
        val sparse = 0.75f * at(Terrain.ROCKY_HILLSIDE, gx) + 0.3f * at(Terrain.HIGH_RIDGE, gx)
        if (LostSheepChoreography.hash(k, 21) < sparse) continue
        val sx = c.sx(gx)
        val base = c.sy(ground(gx) + 0.012f + 0.30f * LostSheepChoreography.hash(k, 22) * LostSheepChoreography.hash(k, 23))
        val len = unit * 0.020f
        val col = lerp(pal.tuft, pal.ridgeGrass, 0.6f * at(Terrain.HIGH_RIDGE, gx))
        for (b in -1..1) {
            val sway = sin(t * 1.3f + k * 0.7f + b) * len * 0.35f
            drawLine(
                col,
                Offset(sx + b * len * 0.25f, base),
                Offset(sx + b * len * 0.45f + sway, base - len * (1f - 0.2f * abs(b))),
                strokeWidth = (unit * 0.003f).coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }
    }

    // Wildflowers: thick in the meadow and the valley, sparse among the rocks.
    for (k in LostSheepChoreography.slot(xL, 0.014f)..LostSheepChoreography.slot(xR, 0.014f)) {
        val fx = k * 0.014f + 0.013f * LostSheepChoreography.hash(k, 31)
        val density = 0.35f + 0.6f * at(Terrain.MEADOW, fx) + 0.4f * at(Terrain.WIDE_VALLEY, fx) + 0.15f * at(Terrain.SHEEP_HILL, fx) -
            0.3f * at(Terrain.ROCKY_HILLSIDE, fx) - 0.2f * at(Terrain.WOODED_HILLSIDE, fx)
        if (LostSheepChoreography.hash(k, 32) > density) continue
        val d = LostSheepChoreography.hash(k, 33)
        val sway = 0.002f * sin(t * 1.1f + k)
        val sx = c.sx(fx + sway)
        val fy = c.sy(ground(fx) + 0.03f + 0.45f * d)
        val col = when (k and 3) { 0 -> pal.flowerGold; 1 -> pal.flowerRed; 2 -> pal.flowerBlue; else -> pal.flower }
        val r = unit * 0.0045f * (0.6f + d)
        drawCircle(col.copy(alpha = 0.92f), r, Offset(sx, fy))
        if (r > 2.5f) drawCircle(Color(0xFFFFE9A0), r * 0.35f, Offset(sx, fy))
    }

    // The ninety-nine: grazing in the home pasture. The path circles round, so the same pasture
    // waits at the end of the journey, where they gather toward the returning shepherd.
    val sheepUnit = unit * 0.04f
    for (home in 0..1) {
        val offset = home * LostSheepChoreography.LOOP
        if (offset + LostSheepChoreography.FLOCK_END + 0.3f < xL || offset - 0.3f > xR) continue
        for (i in props.flockX.indices) {
            val d = props.flockDepth[i]
            val ph = props.flockPhase[i]
            val gathering = home == 1 && st.flockGather > 0.05f
            val drift = 0.006f * sin(t * 0.03f + ph) - (if (home == 1) st.flockGather * 0.30f * (1f - 0.5f * d) else 0f)
            val fx = props.flockX[i] + offset + drift
            val sx = c.sx(fx)
            val s = sheepUnit * (0.62f + 0.38f * (1f - d))
            if (sx < -s * 2 || sx > w + s * 2) continue
            val feet = c.sy(ground(fx) + 0.012f + 0.10f * (1f - d))
            val facing = if (gathering) -1f else props.flockFacing[i]
            if (props.flockResting[i] && !gathering) {
                drawRestingSheep(sx, feet, s, facing, pal)
            } else {
                val graze = if (gathering) 0f else ((sin(t * 0.35f + ph * 3f) + 0.3f) / 1.3f).coerceIn(0f, 1f)
                drawSheep(sx, feet, s, facing, graze, if (gathering && st.flockGather < 1f) sin(t * 6f + ph) else 0f, pal)
            }
        }
    }

    // Butterflies over the flowers.
    for (k in LostSheepChoreography.slot(xL, 0.45f)..LostSheepChoreography.slot(xR, 0.45f)) {
        val base = k * 0.45f
        val meadowy = 0.25f + 0.7f * (at(Terrain.MEADOW, base) + at(Terrain.WIDE_VALLEY, base) + at(Terrain.PASTURE, base))
        if (LostSheepChoreography.hash(k, 41) > meadowy) continue
        val bxw = base + 0.2f + 0.05f * sin(t * 0.31f + k * 2f)
        val sx = c.sx(bxw)
        val by = c.sy(ground(bxw) + 0.06f - 0.03f * abs(sin(t * 0.9f + k)))
        val bs = unit * 0.006f
        val open = 0.25f + 0.75f * abs(sin(t * 9f + k * 1.3f))
        val col = when ((k % 3 + 3) % 3) { 0 -> Color(0xFFFFF4D6); 1 -> Color(0xFFFFCF4A); else -> Color(0xFFF2A65A) }
        drawOval(col, Offset(sx - bs * 1.4f * open, by - bs), Size(bs * 1.4f * open, bs * 1.3f))
        drawOval(col, Offset(sx, by - bs), Size(bs * 1.4f * open, bs * 1.3f))
    }

    // The one that wandered away, until it is carried home. A small bell on a red cord marks it.
    val sheepS = sheepUnit * 1.05f
    if (!st.carrying) {
        val lx = st.lostSheepX
        val swing = if (st.lostSheepWalking) sin(t * 6f) else 0f
        val headDown = (1f - st.lostSheepHeadUp) * ((sin(t * 0.5f) + 0.6f) / 1.6f).coerceIn(0f, 1f)
        drawSheep(c.sx(lx), c.sy(ground(lx) + FEET), sheepS, st.lostSheepFacing, headDown, swing, pal, bell = true)
    }

    // The rock it sheltered behind.
    for (rx in LostSheepChoreography.rocks) {
        val sx = c.sx(rx)
        if (sx < -unit * 0.2f || sx > w + unit * 0.2f) continue
        drawRock(sx, c.sy(ground(rx) + FEET + 0.006f), unit, pal)
    }

    // The Good Shepherd.
    drawShepherd(c.sx(st.shepherdX), c.sy(ground(st.shepherdX) + FEET), unit * 0.2f, st, t, pal, sheepS)

    // A soft rise of ground right in front of us.
    val mound = Path()
    mound.moveTo(-10f, h + 10f)
    for (i in 0..32) {
        val sx = -10f + (w + 20f) * i / 32
        val u = sx / w + st.cameraX * 0.35f
        mound.lineTo(sx, h * (0.95f + 0.018f * sin(u * 4.1f) + 0.008f * sin(u * 9.3f)))
    }
    mound.lineTo(w + 10f, h + 10f)
    mound.close()
    val moundColor = lerp(lerp(pal.mound, pal.moundRock, 0.45f * at(Terrain.ROCKY_HILLSIDE)), pal.moundWood, at(Terrain.WOODED_HILLSIDE))
    drawPath(mound, moundColor)

    // Close bushes and branches we pass in the grove and the woods.
    val leafy = max(at(Terrain.OLIVE_GROVE), at(Terrain.WOODED_HILLSIDE))
    if (leafy > 0.02f) {
        val par = 1.35f
        for (k in LostSheepChoreography.slot(c.worldX(-0.3f * w, par), 0.42f)..LostSheepChoreography.slot(c.worldX(1.3f * w, par), 0.42f)) {
            if (LostSheepChoreography.hash(k, 51) > 0.55f) continue
            val bx = c.sx(k * 0.42f + 0.2f * LostSheepChoreography.hash(k, 52), par)
            val br = w * (0.10f + 0.06f * LostSheepChoreography.hash(k, 53))
            val col = lerp(pal.olive, pal.oak, at(Terrain.WOODED_HILLSIDE)).copy(alpha = 0.92f * leafy)
            val sway = sin(t * 0.5f + k) * br * 0.03f
            drawCircle(col, br, Offset(bx + sway, h + br * 0.35f))
            drawCircle(col, br * 0.75f, Offset(bx - br * 0.8f + sway, h + br * 0.15f))
            drawCircle(col, br * 0.7f, Offset(bx + br * 0.85f + sway, h + br * 0.2f))
        }
    }

    // Foreground grass, closest to us, moving most.
    val fgCol = lerp(pal.fgGrass, pal.moundRock, 0.5f * at(Terrain.ROCKY_HILLSIDE))
    for (i in props.bladeX.indices) {
        val bx = (frac((props.bladeX[i] - st.cameraX * 0.25f) / 1.3f) * 1.3f - 0.15f) * w
        val len = props.bladeLen[i] * h * 0.06f
        val gust = 0.6f + 0.4f * sin(t * 0.21f)
        val sway = sin(t * 1.1f + i * 0.6f) * len * 0.18f * gust
        val blade = Path().apply {
            moveTo(bx, h + 2f)
            quadraticTo(bx + sway * 0.3f, h - len * 0.5f, bx + sway, h - len)
        }
        drawPath(blade, fgCol.copy(alpha = 0.85f), style = Stroke(width = (w * 0.008f).coerceAtLeast(1.5f), cap = StrokeCap.Round))
    }

    // Sunlight slanting through the olive branches; cooler shade under the trees.
    val grove = at(Terrain.OLIVE_GROVE)
    if (grove > 0.02f) {
        for (k in 0 until 4) {
            val x0 = w * (0.1f + 0.27f * k) + w * 0.05f * sin(t * 0.05f + k)
            val ray = Path().apply {
                moveTo(x0, 0f)
                lineTo(x0 + w * 0.08f, 0f)
                lineTo(x0 - w * 0.12f, h)
                lineTo(x0 - w * 0.24f, h)
                close()
            }
            drawPath(ray, Brush.verticalGradient(listOf(pal.light.copy(alpha = 0.16f * grove), Color.Transparent), startY = 0f, endY = h))
        }
    }
    val shade = at(Terrain.WOODED_HILLSIDE)
    if (shade > 0.02f) drawRect(pal.shade.copy(alpha = 0.12f * shade))

    // Sunlight: a gentle warmth through the session that fills the valley at the end.
    drawRect(
        Brush.radialGradient(
            listOf(pal.light.copy(alpha = 0.05f * st.warmth + 0.16f * st.celebrate), Color.Transparent),
            center = sunC,
            radius = max(w, h) * 1.1f,
        ),
    )
    if (st.celebrate > 0f) {
        for (i in props.moteX.indices) {
            val f = frac(t * props.moteSpeed[i] + props.motePhase[i])
            val mx = w * (props.moteX[i] + 0.02f * sin(t * 0.5f + props.motePhase[i] * 6f))
            val my = h * (1.0f - f * 0.6f)
            val a = st.celebrate * 0.5f * sin(PI.toFloat() * f)
            drawCircle(pal.light.copy(alpha = a.coerceIn(0f, 1f)), props.moteSize[i] * density, Offset(mx, my))
        }
    }
}

/** Lakes in the hills' parallax space: (centre, width). Seen early, from the viewpoint, and from the ridge. */
private val lakes = listOf(1.65f to 1.2f, 3.75f to 1.4f, 5.75f to 0.9f)

private fun groundColor(pal: Palette, x: Float): Color {
    fun at(terrain: Terrain) = LostSheepChoreography.terrainWeight(terrain, x)
    var col = pal.ground
    col = lerp(col, pal.groveGround, 0.6f * at(Terrain.OLIVE_GROVE))
    col = lerp(col, pal.rockyGround, 0.75f * at(Terrain.ROCKY_HILLSIDE))
    col = lerp(col, pal.woodsGround, 0.7f * at(Terrain.WOODED_HILLSIDE))
    col = lerp(col, pal.ridgeGrass, 0.55f * at(Terrain.HIGH_RIDGE))
    return col
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, r: Float, col: Color) {
    drawOval(col, Offset(cx - r * 2.4f, cy - r * 0.5f), Size(r * 4.8f, r * 1.1f))
    drawCircle(col, r * 0.95f, Offset(cx - r * 0.9f, cy - r * 0.45f))
    drawCircle(col, r * 1.2f, Offset(cx + r * 0.1f, cy - r * 0.75f))
    drawCircle(col, r * 0.8f, Offset(cx + r * 1.1f, cy - r * 0.35f))
    drawOval(Color(0x14506080), Offset(cx - r * 2.0f, cy + r * 0.25f), Size(r * 4.0f, r * 0.35f))
}

private inline fun DrawScope.drawLayer(c: Camera, par: Float, color: Color, heightAt: (Float) -> Float) {
    val path = Path()
    val steps = 60
    val w = size.width
    val h = size.height
    path.moveTo(-10f, h + 10f)
    for (i in 0..steps) {
        val sx = -10f + (w + 20f) * i / steps
        path.lineTo(sx, c.sy(heightAt(c.worldX(sx, par)), par))
    }
    path.lineTo(w + 10f, h + 10f)
    path.close()
    drawPath(path, color)
}

/** Snow on whatever part of the range rises above the snowline. */
private inline fun DrawScope.drawSnow(c: Camera, par: Float, color: Color, heightAt: (Float) -> Float) {
    val steps = 90
    val w = size.width
    val snowline = 0.385f
    val top = FloatArray(steps + 1)
    val xs = FloatArray(steps + 1)
    for (i in 0..steps) {
        xs[i] = -10f + (w + 20f) * i / steps
        top[i] = heightAt(c.worldX(xs[i], par))
    }
    val path = Path()
    path.moveTo(xs[0], c.sy(top[0], par))
    for (i in 0..steps) path.lineTo(xs[i], c.sy(top[i], par))
    for (i in steps downTo 0) {
        val y = top[i]
        val wobble = 0.006f * sin(xs[i] * 0.09f)
        val lower = if (y < snowline) min(snowline + wobble, y + 0.03f) else y
        path.lineTo(xs[i], c.sy(max(y, lower), par))
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawStream(c: Camera, unit: Float, t: Float, pal: Palette) {
    val w = size.width
    val top = ground(LostSheepChoreography.STREAM_X) - 0.022f
    val n = 40
    val left = ArrayList<Offset>(n + 1)
    val right = ArrayList<Offset>(n + 1)
    for (i in 0..n) {
        val y = top + (1.05f - top) * i / n
        val half = 0.004f + 0.05f * (y - top)
        val cx = streamX(y)
        left += Offset(c.sx(cx - half), c.sy(y))
        right += Offset(c.sx(cx + half), c.sy(y))
    }
    if (right.maxOf { it.x } < 0f || left.minOf { it.x } > w) return
    val path = Path()
    path.moveTo(left[0].x, left[0].y)
    for (p in left) path.lineTo(p.x, p.y)
    for (p in right.asReversed()) path.lineTo(p.x, p.y)
    path.close()
    drawPath(path, pal.water)
    // Light on moving water.
    for (k in 0 until 18) {
        val f = frac(k / 18f + t * 0.03f)
        val y = top + (1.0f - top) * f
        val cx = streamX(y) + 0.02f * (y - top) * sin(k * 2.1f)
        val len = 0.006f + 0.02f * (y - top)
        drawLine(
            pal.lakeLight.copy(alpha = 0.7f * sin(PI.toFloat() * f)),
            Offset(c.sx(cx - len), c.sy(y)),
            Offset(c.sx(cx + len), c.sy(y)),
            strokeWidth = (unit * 0.003f).coerceAtLeast(1f),
            cap = StrokeCap.Round,
        )
    }
    // Stepping stones where the path fords it.
    val sy = ground(LostSheepChoreography.STREAM_X) + FEET + 0.006f
    val cx = streamX(sy)
    for (k in -2..2) {
        val sx = c.sx(cx + k * 0.022f)
        val r = unit * (0.011f + 0.002f * (k and 1))
        drawOval(pal.rock, Offset(sx - r * 1.3f, c.sy(sy) - r * 0.55f), Size(r * 2.6f, r * 1.1f))
        drawOval(pal.rockLight, Offset(sx - r * 0.8f, c.sy(sy) - r * 0.55f), Size(r * 1.4f, r * 0.45f))
    }
}

/** A tall, dark Mediterranean cypress. */
private fun DrawScope.drawCypress(x: Float, baseY: Float, unit: Float, t: Float, pal: Palette) {
    val th = unit * 0.20f
    val sway = sin(t * 0.5f) * th * 0.01f
    drawOval(Color.Black.copy(alpha = 0.10f), Offset(x - th * 0.12f, baseY - th * 0.02f), Size(th * 0.24f, th * 0.05f))
    drawLine(pal.trunk, Offset(x, baseY), Offset(x, baseY - th * 0.12f), th * 0.04f, StrokeCap.Round)
    val body = Path().apply {
        moveTo(x - th * 0.10f, baseY - th * 0.08f)
        quadraticTo(x - th * 0.13f, baseY - th * 0.55f, x + sway, baseY - th)
        quadraticTo(x + th * 0.13f, baseY - th * 0.55f, x + th * 0.10f, baseY - th * 0.08f)
        close()
    }
    drawPath(body, pal.cypress)
    drawLine(pal.oliveLight.copy(alpha = 0.25f), Offset(x - th * 0.03f, baseY - th * 0.2f), Offset(x - th * 0.02f + sway, baseY - th * 0.8f), th * 0.02f, StrokeCap.Round)
}

/** A rounded broadleaf tree for the wooded hillside. */
private fun DrawScope.drawOak(x: Float, baseY: Float, unit: Float, t: Float, pal: Palette) {
    val th = unit * 0.16f
    val sway = sin(t * 0.55f) * th * 0.015f
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(x - th * 0.45f, baseY - th * 0.03f), Size(th * 0.9f, th * 0.08f))
    drawLine(pal.trunk, Offset(x, baseY), Offset(x + sway, baseY - th * 0.55f), th * 0.07f, StrokeCap.Round)
    val top = Offset(x + sway, baseY - th * 0.75f)
    val lobes = floatArrayOf(-0.30f, 0.0f, 0.30f, -0.15f, 0.16f)
    val lobesY = floatArrayOf(0.08f, -0.10f, 0.08f, -0.22f, -0.20f)
    for (i in lobes.indices) drawCircle(pal.oak, th * 0.26f, Offset(top.x + lobes[i] * th, top.y + lobesY[i] * th))
    drawCircle(pal.oliveLight.copy(alpha = 0.25f), th * 0.12f, Offset(top.x - th * 0.1f, top.y - th * 0.2f))
}

/** A silvery olive tree with a gnarled trunk. */
private fun DrawScope.drawOlive(x: Float, baseY: Float, unit: Float, t: Float, pal: Palette) {
    val th = unit * 0.14f
    val sway = sin(t * 0.6f) * th * 0.015f
    drawOval(Color.Black.copy(alpha = 0.10f), Offset(x - th * 0.4f, baseY - th * 0.03f), Size(th * 0.8f, th * 0.08f))
    val trunk = Path().apply {
        moveTo(x - th * 0.05f, baseY)
        quadraticTo(x - th * 0.10f, baseY - th * 0.3f, x + th * 0.02f + sway, baseY - th * 0.62f)
        lineTo(x + th * 0.07f + sway, baseY - th * 0.60f)
        quadraticTo(x, baseY - th * 0.3f, x + th * 0.06f, baseY)
        close()
    }
    drawPath(trunk, pal.trunk)
    drawLine(pal.trunk, Offset(x, baseY - th * 0.38f), Offset(x - th * 0.18f + sway, baseY - th * 0.58f), th * 0.035f, StrokeCap.Round)
    val top = Offset(x + sway, baseY - th * 0.78f)
    val lobes = floatArrayOf(-0.32f, 0.06f, -0.12f, 0.26f, 0.38f, -0.40f)
    val lobesY = floatArrayOf(0.06f, -0.12f, 0.12f, 0.08f, -0.02f, -0.06f)
    for (i in lobes.indices) {
        drawCircle(pal.olive, th * (0.20f + 0.03f * (i % 2)), Offset(top.x + lobes[i] * th, top.y + lobesY[i] * th))
    }
    for (i in 0 until 4) {
        drawCircle(pal.oliveLight.copy(alpha = 0.55f), th * 0.08f, Offset(top.x + lobes[i] * th - th * 0.04f, top.y + lobesY[i] * th - th * 0.08f))
    }
}

private fun DrawScope.drawRock(x: Float, baseY: Float, unit: Float, pal: Palette) {
    val rw = unit * 0.085f
    val rh = unit * 0.055f
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(x - rw * 0.6f, baseY - rh * 0.12f), Size(rw * 1.2f, rh * 0.28f))
    drawOval(pal.rock, Offset(x - rw * 0.5f, baseY - rh), Size(rw, rh * 1.1f))
    drawOval(pal.rock, Offset(x + rw * 0.2f, baseY - rh * 0.6f), Size(rw * 0.55f, rh * 0.7f))
    drawOval(pal.rockLight, Offset(x - rw * 0.32f, baseY - rh * 0.95f), Size(rw * 0.5f, rh * 0.35f))
}

/** A softly drawn sheep standing with its feet at ([cx], [feetY]); [s] is its body height. */
private fun DrawScope.drawSheep(
    cx: Float,
    feetY: Float,
    s: Float,
    facing: Float,
    headDown: Float,
    legSwing: Float,
    pal: Palette,
    shadow: Boolean = true,
    bell: Boolean = false,
) {
    if (shadow) drawOval(Color.Black.copy(alpha = 0.12f), Offset(cx - 0.8f * s, feetY - 0.1f * s), Size(1.6f * s, 0.22f * s))
    if (s > 5f) {
        val legs = floatArrayOf(-0.42f, -0.2f, 0.2f, 0.42f)
        for ((i, lx) in legs.withIndex()) {
            val sw = (if (i % 2 == 0) legSwing else -legSwing) * 0.12f * s
            drawLine(pal.sheepDark, Offset(cx + lx * s, feetY - 0.45f * s), Offset(cx + lx * s + sw, feetY), strokeWidth = 0.11f * s, cap = StrokeCap.Round)
        }
    }
    drawOval(pal.woolShade, Offset(cx - 0.75f * s, feetY - 0.98f * s), Size(1.5f * s, 0.68f * s))
    drawOval(pal.wool, Offset(cx - 0.72f * s, feetY - 1.06f * s), Size(1.44f * s, 0.62f * s))
    if (s > 4f) {
        // Fluffy fleece.
        drawCircle(pal.wool, 0.26f * s, Offset(cx - 0.40f * s, feetY - 0.96f * s))
        drawCircle(pal.wool, 0.28f * s, Offset(cx - 0.08f * s, feetY - 1.04f * s))
        drawCircle(pal.wool, 0.26f * s, Offset(cx + 0.26f * s, feetY - 1.00f * s))
        drawCircle(pal.woolShade.copy(alpha = 0.6f), 0.10f * s, Offset(cx - facing * 0.5f * s, feetY - 0.62f * s))
    }
    val hx = cx + facing * 0.8f * s
    val hy = feetY - (0.95f - 0.5f * headDown) * s
    drawOval(pal.sheepDark, Offset(hx - 0.24f * s, hy - 0.18f * s), Size(0.48f * s, 0.34f * s))
    drawOval(pal.sheepDark, Offset(hx - facing * 0.22f * s - 0.13f * s, hy - 0.2f * s), Size(0.26f * s, 0.1f * s))
    if (s > 4f) drawCircle(pal.wool, 0.12f * s, Offset(hx - facing * 0.08f * s, hy - 0.2f * s)) // woolly topknot
    if (bell) {
        val bx = cx + facing * 0.58f * s
        val by = feetY - (0.70f - 0.2f * headDown) * s
        drawLine(pal.mantle, Offset(bx - facing * 0.06f * s, by - 0.12f * s), Offset(bx + facing * 0.06f * s, by - 0.04f * s), 0.05f * s, StrokeCap.Round)
        drawCircle(pal.bell, 0.085f * s, Offset(bx, by + 0.03f * s))
    }
}

/** A sheep lying down to rest. */
private fun DrawScope.drawRestingSheep(cx: Float, feetY: Float, s: Float, facing: Float, pal: Palette) {
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(cx - 0.85f * s, feetY - 0.1f * s), Size(1.7f * s, 0.22f * s))
    drawOval(pal.woolShade, Offset(cx - 0.78f * s, feetY - 0.62f * s), Size(1.56f * s, 0.62f * s))
    drawOval(pal.wool, Offset(cx - 0.74f * s, feetY - 0.70f * s), Size(1.48f * s, 0.56f * s))
    val hx = cx + facing * 0.72f * s
    val hy = feetY - 0.52f * s
    drawOval(pal.sheepDark, Offset(hx - 0.22f * s, hy - 0.16f * s), Size(0.44f * s, 0.30f * s))
}

/**
 * The Good Shepherd, drawn simply and respectfully: white robe, burgundy mantle, long dark hair
 * and beard, sandals and a wooden staff, lit only by the sun. [h] is standing height in pixels.
 */
private fun DrawScope.drawShepherd(
    cx: Float,
    feetY: Float,
    h: Float,
    st: SceneState,
    t: Float,
    pal: Palette,
    sheepSize: Float,
) {
    val f = st.shepherdFacing
    val k = st.kneel
    val walkPhase = if (st.shepherdWalking) t * 3.2f else 0f
    val bob = if (st.shepherdWalking) abs(sin(walkPhase)) * 0.012f * h else 0f
    val hh = h * (1f - 0.30f * k)
    val top = feetY - hh - bob
    val shoulderY = top + 0.20f * h
    val hemY = feetY - 0.03f * h
    val headC = Offset(cx + f * 0.01f * h, top + 0.10f * h)

    drawOval(Color.Black.copy(alpha = 0.14f), Offset(cx - 0.24f * h, feetY - 0.025f * h), Size(0.48f * h, 0.05f * h))

    // Staff: resting against him while he counts, then in hand; laid aside while he kneels and carries.
    val staffAlpha = if (st.carrying) 0f else (1f - k)
    val held = st.staffHeld
    val staffX = cx + f * (0.19f + 0.06f * (1f - held)) * h + sin(walkPhase) * 0.012f * h
    val staffTopX = staffX - f * 0.10f * h * (1f - held)
    val staffTop = feetY - 1.06f * h
    if (staffAlpha > 0.01f) {
        val sw = 0.022f * h
        val col = pal.staff.copy(alpha = staffAlpha)
        drawLine(col, Offset(staffX, feetY), Offset(staffTopX, staffTop + 0.05f * h), strokeWidth = sw, cap = StrokeCap.Round)
        val r = 0.045f * h
        val center = Offset(staffTopX + f * r, staffTop + 0.05f * h)
        drawArc(
            color = col,
            startAngle = if (f > 0) 180f else 0f,
            sweepAngle = if (f > 0) 200f else -200f,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(2 * r, 2 * r),
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }

    // Sandalled feet under the hem.
    if (k < 0.3f) {
        val step = if (st.shepherdWalking) sin(walkPhase) * 0.05f * h else 0.025f * h
        for (dir in floatArrayOf(1f, -1f)) {
            val fx = cx + dir * step + f * 0.02f * h
            drawOval(pal.skin, Offset(fx - 0.035f * h + f * 0.01f * h, feetY - 0.03f * h), Size(0.07f * h, 0.03f * h))
            drawLine(pal.sandal, Offset(fx - 0.035f * h, feetY - 0.004f * h), Offset(fx + 0.045f * h, feetY - 0.004f * h), 0.012f * h, StrokeCap.Round)
            drawLine(pal.sandal, Offset(fx, feetY - 0.028f * h), Offset(fx + f * 0.012f * h, feetY - 0.004f * h), 0.008f * h, StrokeCap.Round)
        }
    }

    // Robe.
    val sway = sin(walkPhase) * 0.022f * h
    val hemHalf = 0.16f * h + 0.08f * h * k
    val robe = Path().apply {
        moveTo(cx - 0.095f * h, shoulderY)
        lineTo(cx + 0.095f * h, shoulderY)
        quadraticTo(cx + 0.14f * h, (shoulderY + hemY) / 2f, cx + hemHalf + sway, hemY)
        lineTo(cx - hemHalf + sway, hemY)
        quadraticTo(cx - 0.14f * h, (shoulderY + hemY) / 2f, cx - 0.095f * h, shoulderY)
        close()
    }
    drawPath(robe, pal.robe)
    // Shade on the side away from the sun, and a soft fold.
    val fold = Path().apply {
        moveTo(cx - f * 0.02f * h, shoulderY + 0.05f * h)
        lineTo(cx - f * 0.095f * h, shoulderY)
        quadraticTo(cx - f * 0.14f * h, (shoulderY + hemY) / 2f, cx - f * hemHalf + sway, hemY)
        lineTo(cx - f * 0.05f * h + sway, hemY)
        close()
    }
    drawPath(fold, pal.robeShade)
    drawLine(pal.robeShade, Offset(cx + f * 0.03f * h, shoulderY + 0.3f * h), Offset(cx + f * 0.05f * h + sway, hemY - 0.02f * h), 0.008f * h, StrokeCap.Round)

    // Burgundy mantle across the body and over one shoulder.
    val drop = hemY - shoulderY
    val mantle = Path().apply {
        moveTo(cx - f * 0.10f * h, shoulderY - 0.005f * h)
        lineTo(cx + f * 0.02f * h, shoulderY - 0.005f * h)
        quadraticTo(cx + f * 0.15f * h, shoulderY + 0.35f * drop, cx + f * 0.14f * h + sway * 0.5f, shoulderY + 0.62f * drop)
        lineTo(cx + f * 0.02f * h + sway * 0.5f, shoulderY + 0.70f * drop)
        quadraticTo(cx - f * 0.08f * h, shoulderY + 0.45f * drop, cx - f * 0.13f * h + sway * 0.4f, shoulderY + 0.80f * drop)
        lineTo(cx - f * 0.15f * h + sway * 0.4f, shoulderY + 0.55f * drop)
        close()
    }
    drawPath(mantle, pal.mantle)
    drawLine(pal.mantleShade, Offset(cx - f * 0.02f * h, shoulderY + 0.02f * h), Offset(cx + f * 0.09f * h + sway * 0.4f, shoulderY + 0.55f * drop), 0.012f * h, StrokeCap.Round)
    // Belt.
    drawLine(pal.sandal, Offset(cx - 0.10f * h, shoulderY + 0.30f * h), Offset(cx + 0.10f * h, shoulderY + 0.30f * h), 0.012f * h, StrokeCap.Round)

    // Long dark hair falling behind the shoulders.
    val r = 0.062f * h
    val hairBack = Path().apply {
        moveTo(headC.x - f * 0.02f * h, headC.y - r * 0.9f)
        quadraticTo(headC.x - f * 0.11f * h, headC.y, headC.x - f * 0.085f * h, shoulderY + 0.05f * h)
        quadraticTo(headC.x - f * 0.06f * h, shoulderY + 0.08f * h, headC.x - f * 0.03f * h, shoulderY + 0.06f * h)
        quadraticTo(headC.x - f * 0.01f * h, shoulderY + 0.02f * h, headC.x + f * 0.03f * h, shoulderY + 0.02f * h)
        lineTo(headC.x + f * 0.04f * h, headC.y)
        close()
    }
    drawPath(hairBack, pal.hair)

    // Carried home across his shoulders.
    if (st.carrying) {
        drawSheep(cx - f * 0.02f * h, shoulderY + 0.17f * h, sheepSize * 0.95f, f, 0.35f, 0f, pal, shadow = false, bell = true)
    }

    // Arms.
    val armW = 0.045f * h
    val shoulderFront = Offset(cx + f * 0.07f * h, shoulderY + 0.02f * h)
    val shoulderBack = Offset(cx - f * 0.04f * h, shoulderY + 0.03f * h)
    val hipHand = Offset(cx - f * 0.05f * h, shoulderY + 0.30f * h)
    val browHand = Offset(cx + f * 0.075f * h, headC.y - 0.005f * h)
    val sheepHand = Offset(cx + f * 0.17f * h, feetY - 0.2f * h)
    val staffHand = lerpOffset(Offset(cx + f * 0.06f * h, shoulderY + 0.30f * h), Offset(staffX - f * 0.004f * h, feetY - 0.55f * hh), held)
    when {
        st.carrying -> {
            drawArm(pal, shoulderFront, Offset(cx + f * 0.10f * h, shoulderY - 0.01f * h), armW)
            drawArm(pal, shoulderBack, Offset(cx - f * 0.11f * h, shoulderY), armW)
        }
        st.embrace > 0f -> {
            drawArm(pal, shoulderFront, lerpOffset(staffHand, sheepHand, st.embrace), armW)
            drawArm(pal, shoulderBack, lerpOffset(hipHand, sheepHand + Offset(-f * 0.03f * h, -0.04f * h), st.embrace), armW)
        }
        else -> {
            drawArm(pal, shoulderFront, staffHand, armW)
            drawArm(pal, shoulderBack, lerpOffset(hipHand, browHand, st.look), armW)
        }
    }

    // Head: face in profile toward where he walks, beard, and hair framing it.
    val face = headC + Offset(f * 0.012f * h, 0.004f * h)
    drawCircle(pal.hair, r * 1.06f, headC + Offset(-f * 0.016f * h, 0f))
    drawCircle(pal.skin, r * 0.84f, face)
    drawOval(pal.hair, Offset(face.x - 0.04f * h + f * 0.004f * h, face.y + 0.012f * h), Size(0.08f * h, 0.072f * h)) // beard
    drawArc(
        color = pal.hair,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headC.x - r * 1.02f, headC.y - r * 1.04f),
        size = Size(2.04f * r, 1.3f * r),
    )
    // A lock of hair falling in front of the shoulder.
    drawOval(pal.hair, Offset(headC.x - f * 0.05f * h - 0.022f * h, headC.y - 0.01f * h), Size(0.044f * h, 0.12f * h))
    if (h > 120f) {
        // At close range, a calm eye and a gentle brow line.
        val eye = Offset(face.x + f * r * 0.42f, face.y - r * 0.12f)
        drawLine(pal.hair.copy(alpha = 0.8f), eye + Offset(-r * 0.12f, 0f), eye + Offset(r * 0.12f, r * 0.02f), r * 0.07f, StrokeCap.Round)
        drawLine(pal.hair.copy(alpha = 0.6f), eye + Offset(-r * 0.15f, -r * 0.2f), eye + Offset(r * 0.15f, -r * 0.24f), r * 0.06f, StrokeCap.Round)
    }
}

private fun DrawScope.drawArm(pal: Palette, from: Offset, to: Offset, width: Float) {
    drawLine(pal.robeShade, from, to, width, StrokeCap.Round)
    drawCircle(pal.skin, width * 0.42f, to)
}

private fun lerpOffset(a: Offset, b: Offset, f: Float) = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)
