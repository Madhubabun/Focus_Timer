package com.lostsheep.focus.story

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Genesis 6–9: the ark is built plank by plank, the rain comes, and the promise is kept. */
object NoahStory : FocusStory {
    override val id = "noah"
    override val title = "Noah’s Ark"
    override val reference = "Genesis 6–9"
    override val completionTitle = "A promise in the sky."

    override val phases = listOf(
        StoryPhase(
            "build", 0f, 0.25f, "Building the Ark", "Keep building.", false,
            "Noah builds a great wooden ark on a green plain, one plank at a time.",
        ),
        StoryPhase(
            "animals", 0.25f, 0.45f, "Two by Two", "Two by two.", false,
            "The animals come in pairs and walk up the ramp into the ark.",
        ),
        StoryPhase(
            "rain", 0.45f, 0.65f, "The Rain", null, false,
            "The door is shut. Clouds gather, rain falls and the water rises until the ark floats.",
        ),
        StoryPhase(
            "flood", 0.65f, 0.82f, "The Flood", "Hold steady.", false,
            "The ark rides calmly on wide grey water as the rain slowly eases.",
        ),
        StoryPhase(
            "dove", 0.82f, 0.93f, "The Dove", null, false,
            "The sky clears. A dove flies out and returns with an olive leaf as a mountaintop appears.",
        ),
        StoryPhase(
            "promise", 0.93f, 1f, "The Promise", "Every promise kept.", false,
            "The ark rests on the mountain and a rainbow fills the sky.",
        ),
    )

    override val endingDescription = "The ark rests on the mountain under a bright rainbow while the animals come out."

    override fun stateNameAt(progress: Float): String = NoahChoreography.beatName(progress)

    @Composable
    override fun Scene(progress: () -> Float, time: () -> Float, celebration: () -> Float, modifier: Modifier) {
        Canvas(modifier) { drawNoahScene(NoahChoreography.at(progress(), celebration()), time()) }
    }
}

internal data class NoahState(
    val build: Float,
    val noahX: Float,
    val noahWalking: Boolean,
    val noahFacing: Float,
    val hammer: Boolean,
    val noahVisible: Float,
    /** 0..1 through the animals' arrival. */
    val animals: Float,
    val doorClosed: Float,
    val rain: Float,
    val storm: Float,
    val waterY: Float,
    /** The flood fades in over the plain as the rain starts, and dries away at the end. */
    val waterAlpha: Float,
    val arkX: Float,
    val dove: Float,
    val rainbow: Float,
    val disembark: Float,
    val cameraX: Float,
    val zoom: Float,
)

internal object NoahChoreography {
    const val ARK_LEN = 1.0f
    const val ARARAT_X = 1.7f
    const val PLAIN_Y = 0.72f
    const val ARARAT_TOP = 0.625f
    const val HULL = 0.075f

    private fun k(vararg p: Pair<Float, Float>) = p.toList()

    private val noahKeys = k(0f to 0.62f, 0.07f to 0.62f, 0.11f to -0.20f, 0.17f to -0.20f, 0.21f to 0.62f, 0.25f to 0.62f, 0.29f to -0.62f, 0.44f to -0.62f, 0.455f to -0.30f)
    private val waterKeys = k(0f to 0.725f, 0.47f to 0.725f, 0.64f to 0.585f, 0.82f to 0.585f, 0.95f to 0.71f, 1f to 0.725f)
    private val arkKeys = k(0f to 0f, 0.58f to 0f, 0.82f to ARARAT_X, 1f to ARARAT_X)
    private val camKeys = k(0f to 0.15f, 0.25f to 0.15f, 0.30f to -0.55f, 0.44f to -0.55f, 0.52f to 0.10f, 0.70f to 0.95f, 0.85f to ARARAT_X + 0.25f, 1f to ARARAT_X)
    private val zoomKeys = k(0f to 0.62f, 0.08f to 0.85f, 0.18f to 0.70f, 0.25f to 0.62f, 0.32f to 0.78f, 0.44f to 0.70f, 0.52f to 0.52f, 0.70f to 0.44f, 0.84f to 0.56f, 0.93f to 0.50f, 1f to 0.45f)

    fun at(p0: Float, celebration: Float = -1f): NoahState {
        val p = p0.coerceIn(0f, 1f)
        val nx = keyed(noahKeys, p)
        val dn = keyed(noahKeys, p + 0.002f) - keyed(noahKeys, p - 0.002f)
        val cel = if (celebration >= 0f) min(celebration / 14f, 1f) else 0f
        val water = keyed(waterKeys, p)
        val storm = ramp(p, 0.44f, 0.52f) * (1f - ramp(p, 0.76f, 0.88f))
        return NoahState(
            build = ramp(p, 0.01f, 0.245f),
            noahX = nx,
            noahWalking = kotlin.math.abs(dn) > 0.0004f,
            noahFacing = if (dn < -0.0004f || (p in 0.25f..0.44f)) -1f else 1f,
            hammer = (p in 0.0f..0.07f) || (p in 0.11f..0.17f) || (p in 0.21f..0.245f),
            noahVisible = 1f - ramp(p, 0.455f, 0.465f),
            animals = ((p - 0.27f) / 0.17f).coerceIn(0f, 1f),
            doorClosed = ramp(p, 0.455f, 0.475f) * (1f - ramp(p, 0.955f, 0.975f)),
            rain = ramp(p, 0.47f, 0.52f) * (1f - ramp(p, 0.72f, 0.82f)),
            storm = storm,
            waterY = water,
            waterAlpha = ramp(p, 0.47f, 0.50f) * (1f - max(ramp(p, 0.955f, 1f), cel)),
            arkX = keyed(arkKeys, p),
            dove = ((p - 0.835f) / 0.08f).coerceIn(0f, 1f),
            rainbow = max(ramp(p, 0.935f, 0.985f), cel),
            disembark = max(ramp(p, 0.97f, 1f) * 0.25f, cel),
            cameraX = keyed(camKeys, p) - 0.4f * cel,
            zoom = keyed(zoomKeys, p) * (1f - 0.1f * cel),
        )
    }

    /** The plain, rising to the mountain where the ark will rest. */
    fun groundY(x: Float): Float {
        val d = (x - ARARAT_X) / 0.45f
        val mountain = (PLAIN_Y - ARARAT_TOP) * kotlin.math.exp(-d * d)
        return PLAIN_Y + 0.006f * sin(x * 2.7f) - mountain
    }

    /** The ark's keel: on the plain, then lifted by the water, then resting on the mountain. */
    fun keelY(st: NoahState): Float {
        val land = groundY(st.arkX)
        val floatY = st.waterY + 0.025f
        return min(land, floatY)
    }

    private val names = listOf(
        0f to "lays_the_keel", 0.06f to "raises_the_ribs", 0.11f to "first_planks", 0.17f to "hull_rises", 0.21f to "roof_and_door",
        0.27f to "sheep_arrive", 0.30f to "elephants_arrive", 0.33f to "giraffes_arrive", 0.37f to "lions_arrive", 0.40f to "birds_fly_in",
        0.455f to "the_door_is_shut", 0.48f to "rain_begins", 0.56f to "the_ark_floats", 0.65f to "wide_waters", 0.74f to "the_rain_eases",
        0.82f to "the_sky_clears", 0.835f to "the_dove_flies_out", 0.88f to "an_olive_leaf", 0.93f to "resting_on_the_mountain", 0.95f to "the_rainbow",
    )

    fun beatName(p: Float): String {
        val i = names.indexOfLast { p >= it.first }.coerceAtLeast(0)
        return "scene_%02d_%s".format(java.util.Locale.ROOT, i + 1, names[i].second)
    }
}

private val noahOutfit = Outfit(
    robe = Color(0xFF9C7A54),
    robeShade = Color(0xFF7E6040),
    sash = Color(0xFF5A7A3A),
    hair = Color(0xFFD6D0C4),
    beard = Color(0xFFEDEAE2),
)
private val wood = Color(0xFF8A5E3A)
private val woodLight = Color(0xFFB27E4E)
private val woodDark = Color(0xFF5E3E26)

/** The pairs, in the order they walk in. */
private enum class Kind { Sheep, Elephant, Giraffe, Lion, Bird }

internal fun DrawScope.drawNoahScene(st: NoahState, t: Float) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val c = KitCamera(w, h, st.cameraX, st.zoom)
    val unit = c.unit
    val grey = st.storm

    // Sky: clear, then heavy grey, then washed clean.
    val top = lerp(Color(0xFF5AA2E0), Color(0xFF5E6672), grey)
    val horizon = lerp(Color(0xFFE8F0EC), Color(0xFF9AA0A6), grey)
    kitSky(top, horizon, c.sy(0.58f, 0.1f))
    if (grey < 0.9f) kitSun(Offset(w * 0.72f, c.sy(0.22f, 0.05f)), Color(0xFFFFF6D8).copy(alpha = 1f - grey), Color(0xFFFFE9A8).copy(alpha = 1f - grey), t)
    kitClouds(c, t, lerp(Color.White, Color(0xFF6E747C), grey), cover = grey)
    if (grey < 0.3f) kitBirds(c, t, Color(0xFF3E4A55).copy(alpha = 0.6f * (1f - grey / 0.3f)))

    // The promise.
    if (st.rainbow > 0f) {
        val center = Offset(w * 0.55f, c.sy(0.70f, 0.3f))
        val r0 = max(w, h) * 0.42f
        val bands = listOf(0xFFE5534B, 0xFFF29A3A, 0xFFF4D44E, 0xFF6CC06A, 0xFF4E95D9, 0xFF7A5BC4)
        bands.forEachIndexed { i, col ->
            val r = r0 - i * r0 * 0.035f
            drawArc(Color(col).copy(alpha = 0.55f * st.rainbow), 180f, 180f, false, Offset(center.x - r, center.y - r), Size(2 * r, 2 * r), style = Stroke(r0 * 0.036f))
        }
    }

    // Distant hills, which the flood covers and gives back.
    kitLayer(c, 0.12f, lerp(Color(0xFF9DB4C8), Color(0xFF8A9098), grey)) { kitRidge(it, 0.57f, 0.07f, 2.4f) }
    kitLayer(c, 0.30f, lerp(Color(0xFF8DB880), Color(0xFF7A8A78), grey)) { kitRidge(it, 0.62f, 0.04f, 4.6f) }

    // Ground and the mountain.
    val field = Path()
    field.moveTo(-10f, h + 10f)
    for (i in 0..60) {
        val sx = -10f + (w + 20f) * i / 60
        field.lineTo(sx, c.sy(NoahChoreography.groundY(c.worldX(sx))))
    }
    field.lineTo(w + 10f, h + 10f)
    field.close()
    val wet = 0.25f * st.waterAlpha
    drawPath(field, lerp(lerp(Color(0xFF6CB04C), Color(0xFF7E8A6A), grey), Color(0xFF8A7A5A), wet))
    drawPath(field, Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF2E4A26).copy(alpha = 0.5f)), startY = c.sy(0.70f), endY = h))

    // A few trees on the plain (before the water).
    for (i in 0 until 8) {
        val x = -2.6f + i * 0.55f + 0.1f * sin(i * 1.7f)
        if (kotlin.math.abs(x) < 0.7f) continue
        val sx = c.sx(x)
        val by = c.sy(NoahChoreography.groundY(x))
        val th = unit * 0.07f
        drawLine(woodDark, Offset(sx, by), Offset(sx, by - th * 0.5f), th * 0.12f)
        drawCircle(lerp(Color(0xFF5E9A48), Color(0xFF5A6A52), grey), th * 0.35f, Offset(sx, by - th * 0.7f))
    }

    // Animals coming two by two.
    if (st.animals > 0f && st.doorClosed < 1f) drawAnimals(c, st, t)

    // Noah, outside while he builds and welcomes the animals.
    if (st.noahVisible > 0.01f) {
        val nh = unit * 0.10f
        val sx = c.sx(st.noahX)
        val fy = c.sy(NoahChoreography.groundY(st.noahX))
        val swing = if (st.hammer) sin(t * 6f) else 0f
        val front = when {
            st.hammer -> Offset(0.12f, -0.02f + 0.10f * swing)
            st.animals > 0f && st.animals < 1f -> Offset(0.16f, 0.12f) // welcoming
            else -> Offset(0.06f, 0.30f)
        }
        kitPerson(sx, fy, nh, st.noahFacing, noahOutfit, t * 3f, st.noahWalking, 0f, front)
        if (st.hammer) {
            val hand = Offset(sx + st.noahFacing * 0.12f * nh, fy - nh + 0.2f * nh - 0.02f * nh + 0.10f * swing * nh * 0.8f)
            drawLine(woodDark, hand, hand + Offset(st.noahFacing * 0.06f * nh, -0.04f * nh), 0.02f * nh, StrokeCap.Round)
            drawRect(Color(0xFF6A6E72), hand + Offset(st.noahFacing * 0.05f * nh - 0.02f * nh, -0.07f * nh), Size(0.04f * nh, 0.04f * nh))
        }
    }

    // The ark.
    drawArk(c, st, t)

    // The water, with the ark riding on it.
    if (st.waterAlpha > 0.01f) {
        val waterTop = c.sy(st.waterY)
        val sea = lerp(Color(0xFF5E9EC8), Color(0xFF6A7E8C), grey)
        val path = Path()
        path.moveTo(-10f, h + 10f)
        for (i in 0..60) {
            val sx = -10f + (w + 20f) * i / 60
            val wave = sin(sx / w * 18f + t * 0.9f) * unit * 0.004f * (1f + st.rain)
            path.lineTo(sx, waterTop + wave)
        }
        path.lineTo(w + 10f, h + 10f)
        path.close()
        drawPath(path, sea.copy(alpha = 0.92f * st.waterAlpha))
        for (j in 0 until 14) {
            val sx = kitFrac(j * 0.173f + t * 0.004f) * w
            val sy = waterTop + unit * (0.01f + 0.02f * (j % 5))
            drawLine(Color.White.copy(alpha = 0.25f * st.waterAlpha), Offset(sx, sy), Offset(sx + w * 0.05f, sy), max(1f, unit * 0.002f), StrokeCap.Round)
        }
    }

    // Rain.
    if (st.rain > 0.01f) {
        val col = Color(0xFFD6DEE6).copy(alpha = 0.55f * st.rain)
        val n = (160 * st.rain).toInt()
        for (i in 0 until n) {
            val x0 = LostSheepChoreography.hash(i, 31) * w
            val speed = 0.6f + 0.4f * LostSheepChoreography.hash(i, 32)
            val y0 = kitFrac(LostSheepChoreography.hash(i, 33) + t * speed) * (h * 1.1f) - h * 0.05f
            val len = h * 0.03f
            drawLine(col, Offset(x0, y0), Offset(x0 - len * 0.15f, y0 + len), max(1f, w * 0.002f), StrokeCap.Round)
        }
    }

    // The dove: out over the water, then back with a leaf.
    if (st.dove > 0f && st.dove < 1f) {
        val keel = NoahChoreography.keelY(st)
        val home = Offset(c.sx(st.arkX + 0.1f), c.sy(keel - NoahChoreography.HULL - 0.06f))
        val out = if (st.dove < 0.5f) st.dove * 2f else (1f - st.dove) * 2f
        val pos = Offset(home.x + out * w * 0.45f, home.y - out * h * 0.18f - sin(out * PI.toFloat()) * h * 0.05f)
        val s = unit * 0.018f
        val flap = sin(t * 9f) * s * 0.8f
        val dir = if (st.dove < 0.5f) 1f else -1f
        drawOval(Color.White, Offset(pos.x - s, pos.y - s * 0.35f), Size(2 * s, s * 0.7f))
        drawLine(Color.White, pos, pos + Offset(-dir * s * 0.4f, -s - flap), s * 0.45f, StrokeCap.Round)
        drawCircle(Color.White, s * 0.32f, pos + Offset(dir * s * 0.95f, -s * 0.15f))
        if (st.dove >= 0.5f) drawOval(Color(0xFF6E9A3E), Offset(pos.x + dir * s * 1.2f - s * 0.25f, pos.y - s * 0.05f), Size(s * 0.5f, s * 0.25f))
    }

    // Soft light as the sky clears.
    val glow = 0.18f * st.rainbow
    if (glow > 0f) drawRect(Brush.radialGradient(listOf(Color(0xFFFFE6A6).copy(alpha = glow), Color.Transparent), center = Offset(w * 0.6f, h * 0.35f), radius = max(w, h)))
}

private fun DrawScope.drawArk(c: KitCamera, st: NoahState, t: Float) {
    val keel = NoahChoreography.keelY(st)
    val floating = keel < NoahChoreography.groundY(st.arkX) - 0.002f
    val bob = if (floating) sin(t * 0.8f) * 0.003f else 0f
    val half = NoahChoreography.ARK_LEN / 2f
    val l = c.sx(st.arkX - half)
    val r = c.sx(st.arkX + half)
    val bottom = c.sy(keel + bob)
    val unit = c.unit
    val hullH = unit * NoahChoreography.HULL
    val deck = bottom - hullH
    val tilt = if (floating) sin(t * 0.6f) * unit * 0.004f else 0f

    val hull = Path().apply {
        moveTo(l - (r - l) * 0.04f, deck - tilt)
        lineTo(r + (r - l) * 0.04f, deck + tilt)
        quadraticTo(r, bottom, r - (r - l) * 0.12f, bottom)
        lineTo(l + (r - l) * 0.12f, bottom)
        quadraticTo(l, bottom, l - (r - l) * 0.04f, deck - tilt)
        close()
    }
    val b = st.build
    // Keel and ribs first.
    drawLine(woodDark, Offset(l + (r - l) * 0.12f, bottom), Offset(r - (r - l) * 0.12f, bottom), unit * 0.008f, StrokeCap.Round)
    val ribs = 14
    val ribsShown = (ramp(b, 0f, 0.3f) * ribs).toInt()
    for (i in 0 until ribsShown) {
        val x = l + (r - l) * (0.06f + 0.88f * i / (ribs - 1))
        drawLine(woodDark, Offset(x, bottom), Offset(x, deck), unit * 0.005f, StrokeCap.Round)
    }
    // Planks, rising from the keel.
    val planks = ramp(b, 0.3f, 0.75f)
    if (planks > 0f) {
        clipRect(left = 0f, top = bottom - hullH * planks - 1f, right = size.width, bottom = bottom + 2f) {
            drawPath(hull, wood)
            for (row in 1 until 6) {
                val y = bottom - hullH * row / 6f
                drawLine(woodDark.copy(alpha = 0.5f), Offset(l, y), Offset(r, y), unit * 0.0025f)
            }
        }
    }
    // The house on deck, roof and door.
    val house = ramp(b, 0.75f, 1f)
    if (house > 0f) {
        val hl = l + (r - l) * 0.18f
        val hr = r - (r - l) * 0.22f
        val hh = unit * 0.045f * house
        drawRect(woodLight, Offset(hl, deck - hh), Size(hr - hl, hh))
        for (k in 0 until 6) {
            val wx = hl + (hr - hl) * (0.1f + 0.16f * k)
            drawRect(woodDark.copy(alpha = 0.6f), Offset(wx, deck - hh * 0.7f), Size((hr - hl) * 0.05f, hh * 0.25f))
        }
        val roof = Path().apply {
            moveTo(hl - (hr - hl) * 0.04f, deck - hh)
            lineTo(hr + (hr - hl) * 0.04f, deck - hh)
            lineTo(hr - (hr - hl) * 0.05f, deck - hh - unit * 0.022f * house)
            lineTo(hl + (hr - hl) * 0.05f, deck - hh - unit * 0.022f * house)
            close()
        }
        drawPath(roof, woodDark)
        // Noah on deck once the rain has passed.
        if (st.rainbow > 0f) {
            val nh = unit * 0.08f
            kitPerson(hl + (hr - hl) * 0.75f, deck, nh, -1f, noahOutfit, frontHand = Offset(0.06f, -0.25f * st.rainbow), backHand = Offset(-0.03f, -0.22f * st.rainbow))
        }
    }
    // The door and ramp on the side.
    if (b > 0.85f) {
        val dx = l + (r - l) * 0.30f
        val dw = (r - l) * 0.08f
        val dh = hullH * 0.6f
        drawRect(Color(0xFF3A2616), Offset(dx, bottom - hullH * 0.85f), Size(dw, dh))
        val open = 1f - st.doorClosed
        if (open > 0.01f && !floating) {
            val foot = Offset(dx - (r - l) * 0.28f * open, c.sy(NoahChoreography.groundY(st.arkX - half * 0.9f)))
            drawLine(woodLight, Offset(dx + dw * 0.5f, bottom - hullH * 0.25f), foot, unit * 0.012f, StrokeCap.Round)
        }
        if (st.doorClosed > 0f) drawRect(wood, Offset(dx, bottom - hullH * 0.85f), Size(dw, dh * st.doorClosed))
    }
}

private fun DrawScope.drawAnimals(c: KitCamera, st: NoahState, t: Float) {
    val kinds = Kind.entries
    val unit = c.unit
    val door = -NoahChoreography.ARK_LEN / 2f + NoahChoreography.ARK_LEN * 0.33f
    val rampFoot = door - 0.30f
    kinds.forEachIndexed { i, kind ->
        // Each pair has its own slice of the arrival.
        val a = ((st.animals - i * 0.17f) / 0.32f).coerceIn(0f, 1f)
        if (a <= 0f || a >= 1f) return@forEachIndexed
        for (j in 0 until 2) {
            val lag = j * 0.10f
            val f = ((a - lag) / (1f - lag)).coerceIn(0f, 1f)
            if (f <= 0f || f >= 0.98f) continue
            val x = -2.4f + (rampFoot + 0.25f + 2.4f) * f
            val climb = ramp(f, 0.82f, 0.98f)
            val gy = NoahChoreography.groundY(x) - climb * 0.05f
            val sx = c.sx(x)
            val fy = c.sy(gy)
            val step = sin(t * 5f + i + j * 1.3f)
            when (kind) {
                Kind.Sheep -> kitSheep(sx, fy, unit * 0.028f, 1f, 0.2f, step)
                Kind.Elephant -> drawElephant(sx, fy, unit * 0.075f, step)
                Kind.Giraffe -> drawGiraffe(sx, fy, unit * 0.13f, step)
                Kind.Lion -> drawLion(sx, fy, unit * 0.045f, step, male = j == 0)
                Kind.Bird -> {
                    val by = fy - unit * (0.12f - 0.08f * climb) + sin(t * 2f + j) * unit * 0.01f
                    val s = unit * 0.014f
                    val flap = sin(t * 9f + j) * s * 0.7f
                    drawLine(Color(0xFF3E5A8A), Offset(sx - s, by - flap), Offset(sx, by), s * 0.3f, StrokeCap.Round)
                    drawLine(Color(0xFF3E5A8A), Offset(sx + s, by - flap), Offset(sx, by), s * 0.3f, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.drawElephant(cx: Float, feetY: Float, s: Float, step: Float) {
    val body = Color(0xFF8E9298)
    val shade = Color(0xFF6E737A)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(cx - 0.6f * s, feetY - 0.06f * s), Size(1.2f * s, 0.12f * s))
    for ((i, lx) in floatArrayOf(-0.35f, -0.15f, 0.2f, 0.38f).withIndex()) {
        val sw = (if (i % 2 == 0) step else -step) * 0.05f * s
        drawLine(if (i % 2 == 0) shade else body, Offset(cx + lx * s, feetY - 0.4f * s), Offset(cx + lx * s + sw, feetY), 0.16f * s, StrokeCap.Round)
    }
    drawOval(body, Offset(cx - 0.55f * s, feetY - 0.85f * s), Size(1.1f * s, 0.6f * s))
    drawCircle(body, 0.28f * s, Offset(cx + 0.55f * s, feetY - 0.72f * s))
    drawOval(shade, Offset(cx + 0.30f * s, feetY - 0.85f * s), Size(0.26f * s, 0.36f * s)) // ear
    val trunk = Path().apply {
        moveTo(cx + 0.75f * s, feetY - 0.70f * s)
        quadraticTo(cx + 0.92f * s, feetY - 0.45f * s, cx + 0.85f * s + step * 0.04f * s, feetY - 0.18f * s)
    }
    drawPath(trunk, body, style = Stroke(0.12f * s, cap = StrokeCap.Round))
    drawLine(Color(0xFFF2EEE4), Offset(cx + 0.70f * s, feetY - 0.58f * s), Offset(cx + 0.82f * s, feetY - 0.52f * s), 0.04f * s, StrokeCap.Round)
}

private fun DrawScope.drawGiraffe(cx: Float, feetY: Float, s: Float, step: Float) {
    val coat = Color(0xFFE2B46A)
    val spot = Color(0xFFA0683A)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(cx - 0.3f * s, feetY - 0.03f * s), Size(0.6f * s, 0.06f * s))
    for ((i, lx) in floatArrayOf(-0.18f, -0.08f, 0.12f, 0.2f).withIndex()) {
        val sw = (if (i % 2 == 0) step else -step) * 0.03f * s
        drawLine(coat, Offset(cx + lx * s, feetY - 0.45f * s), Offset(cx + lx * s + sw, feetY), 0.05f * s, StrokeCap.Round)
    }
    drawOval(coat, Offset(cx - 0.25f * s, feetY - 0.62f * s), Size(0.5f * s, 0.22f * s))
    drawLine(coat, Offset(cx + 0.18f * s, feetY - 0.55f * s), Offset(cx + 0.32f * s, feetY - 1.0f * s), 0.08f * s, StrokeCap.Round)
    drawOval(coat, Offset(cx + 0.28f * s, feetY - 1.06f * s), Size(0.16f * s, 0.08f * s))
    for (k in 0 until 5) drawCircle(spot, 0.025f * s, Offset(cx - 0.15f * s + k * 0.08f * s, feetY - 0.52f * s + (k % 2) * 0.04f * s))
    for (k in 0 until 3) drawCircle(spot, 0.018f * s, Offset(cx + 0.21f * s + k * 0.035f * s, feetY - 0.65f * s - k * 0.11f * s))
}

private fun DrawScope.drawLion(cx: Float, feetY: Float, s: Float, step: Float, male: Boolean) {
    val coat = Color(0xFFD9A55A)
    val mane = Color(0xFF9A5E2A)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(cx - 0.6f * s, feetY - 0.06f * s), Size(1.2f * s, 0.12f * s))
    for ((i, lx) in floatArrayOf(-0.4f, -0.22f, 0.22f, 0.4f).withIndex()) {
        val sw = (if (i % 2 == 0) step else -step) * 0.08f * s
        drawLine(coat, Offset(cx + lx * s, feetY - 0.35f * s), Offset(cx + lx * s + sw, feetY), 0.12f * s, StrokeCap.Round)
    }
    drawOval(coat, Offset(cx - 0.55f * s, feetY - 0.7f * s), Size(1.1f * s, 0.42f * s))
    drawLine(coat, Offset(cx - 0.55f * s, feetY - 0.55f * s), Offset(cx - 0.85f * s, feetY - 0.75f * s), 0.06f * s, StrokeCap.Round)
    if (male) drawCircle(mane, 0.34f * s, Offset(cx + 0.55f * s, feetY - 0.72f * s))
    drawCircle(coat, 0.22f * s, Offset(cx + 0.62f * s, feetY - 0.70f * s))
    drawCircle(Color(0xFF3A2A1E), 0.035f * s, Offset(cx + 0.78f * s, feetY - 0.68f * s))
}
