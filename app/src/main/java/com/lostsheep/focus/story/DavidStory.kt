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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** 1 Samuel 17: a shepherd boy, five smooth stones, and one that is enough. */
object DavidStory : FocusStory {
    override val id = "david-goliath"
    override val title = "David & Goliath"
    override val reference = "1 Samuel 17"
    override val completionTitle = "The giant has fallen."

    override val phases = listOf(
        StoryPhase(
            "fields", 0f, 0.12f, "Keeping the Sheep", "Faithful in small things.", false,
            "Young David keeps his father's sheep on a quiet hillside.",
        ),
        StoryPhase(
            "valley", 0.12f, 0.35f, "The Valley of Elah", null, false,
            "He walks past Israel's camp and down into the valley between two armies. Across it stands the giant.",
        ),
        StoryPhase(
            "brook", 0.35f, 0.55f, "Five Smooth Stones", "Choose your stones.", false,
            "At the brook he kneels and chooses five smooth stones for his shepherd's bag.",
        ),
        StoryPhase(
            "approach", 0.55f, 0.80f, "Facing the Giant", "The battle is the Lord’s.", false,
            "With his staff and sling he walks toward Goliath, who comes forward in bronze armor.",
        ),
        StoryPhase(
            "sling", 0.80f, 0.93f, "One Stone", null, false,
            "David swings his sling. One stone flies, and the giant falls.",
        ),
        StoryPhase(
            "victory", 0.93f, 1f, "Victory", null, false,
            "Israel's army cheers from the hill as the sun breaks over the valley.",
        ),
    )

    override val endingDescription = "David stands in the valley in warm light while Israel's army cheers from the hill."

    override fun stateNameAt(progress: Float): String = DavidChoreography.beatName(progress)

    @Composable
    override fun Scene(progress: () -> Float, time: () -> Float, celebration: () -> Float, modifier: Modifier) {
        Canvas(modifier) { drawDavidScene(DavidChoreography.at(progress(), celebration()), time()) }
    }
}

internal data class DavidState(
    val davidX: Float,
    val davidWalking: Boolean,
    val davidFacing: Float,
    val kneel: Float,
    val stones: Int,
    /** 0 idle, then the sling whirls (fraction of the wind-up). */
    val windUp: Float,
    /** -1 before the throw, then 0..1 along the stone's flight. */
    val stoneFlight: Float,
    val goliathX: Float,
    val goliathWalking: Boolean,
    val goliathFall: Float,
    val cheer: Float,
    val philistinesFlee: Float,
    val cameraX: Float,
    val zoom: Float,
    val warmth: Float,
)

internal object DavidChoreography {
    const val BROOK_X = 1.0f

    private fun k(vararg p: Pair<Float, Float>) = p.toList()

    private val davidKeys = k(
        0f to -2.0f, 0.10f to -2.0f, 0.16f to -1.6f, 0.24f to -0.5f, 0.30f to 0.40f, 0.345f to 0.86f,
        0.355f to BROOK_X - 0.06f, 0.55f to BROOK_X - 0.06f, 0.78f to 1.75f, 1f to 1.75f,
    )
    private val goliathKeys = k(0f to 2.78f, 0.52f to 2.78f, 0.79f to 2.36f, 1f to 2.36f)
    private val camKeys = k(
        0f to -1.95f, 0.10f to -1.95f, 0.20f to -0.75f, 0.28f to 0.15f, 0.35f to BROOK_X - 0.04f, 0.55f to BROOK_X - 0.04f, 1f to BROOK_X,
    )
    private val zoomKeys = k(
        0f to 0.62f, 0.10f to 0.80f, 0.20f to 0.55f, 0.28f to 0.50f, 0.35f to 0.95f, 0.52f to 1.08f, 1f to 1.0f,
    )

    fun at(p0: Float, celebration: Float = -1f): DavidState {
        val p = p0.coerceIn(0f, 1f)
        val x = keyed(davidKeys, p)
        val dx = keyed(davidKeys, p + 0.002f) - keyed(davidKeys, p - 0.002f)
        val kneel = ramp(p, 0.352f, 0.37f) * (1f - ramp(p, 0.53f, 0.55f))
        val stones = if (p < 0.37f) 0 else min(5, 1 + ((p - 0.37f) / 0.032f).toInt())
        val windUp = if (p in 0.80f..0.865f) (p - 0.80f) / 0.065f else 0f
        val flight = if (p < 0.865f) -1f else ((p - 0.865f) / 0.025f).coerceAtMost(1f)
        val gx = keyed(goliathKeys, p)
        val gWalk = p in 0.52f..0.79f
        // From the brook on, the camera keeps both David and Goliath in view, closing in as they meet,
        // then pulls back over the valley for the victory.
        val span = gx - x
        val frame = ramp(p, 0.55f, 0.64f)
        val wide = ramp(p, 0.93f, 1f)
        var cam = keyed(camKeys, p) * (1f - frame) + ((x + gx) / 2f) * frame
        var zoom = keyed(zoomKeys, p) * (1f - frame) + (0.62f / (span + 0.30f)).coerceIn(0.42f, 1.05f) * frame
        cam = cam * (1f - wide) + 1.55f * wide
        zoom = zoom * (1f - wide) + 0.50f * wide
        val cel = if (celebration >= 0f) min(celebration / 12f, 1f) else 0f
        cam += 0.25f * cel
        zoom *= 1f - 0.12f * cel
        return DavidState(
            davidX = x,
            davidWalking = dx > 0.0004f,
            davidFacing = 1f,
            kneel = kneel,
            stones = stones,
            windUp = windUp,
            stoneFlight = flight,
            goliathX = gx,
            goliathWalking = gWalk,
            goliathFall = ramp(p, 0.892f, 0.93f),
            cheer = max(ramp(p, 0.935f, 0.97f), cel),
            philistinesFlee = max(ramp(p, 0.94f, 1f), cel),
            cameraX = cam,
            zoom = zoom,
            warmth = p * 0.85f + 0.15f * cel,
        )
    }

    /** Ground height under world x: David's pasture, Israel's hill, the valley, the Philistine hill. */
    fun groundY(x: Float): Float = keyed(
        k(-4f to 0.70f, -1.4f to 0.70f, -0.8f to 0.655f, 0.0f to 0.648f, 0.55f to 0.725f, 2.85f to 0.728f, 3.3f to 0.655f, 4.4f to 0.645f, 7f to 0.66f),
        x,
    ) + 0.008f * sin(x * 3.1f)

    private val names = listOf(
        0f to "with_the_sheep", 0.10f to "leaves_the_sheep", 0.16f to "past_the_camp", 0.24f to "down_the_hill",
        0.30f to "into_the_valley", 0.345f to "reaches_the_brook", 0.37f to "first_stone", 0.40f to "second_stone",
        0.43f to "third_stone", 0.46f to "fourth_stone", 0.49f to "fifth_stone", 0.55f to "rises",
        0.62f to "walks_toward_goliath", 0.70f to "goliath_comes_forward", 0.80f to "whirls_the_sling",
        0.865f to "the_stone_flies", 0.892f to "goliath_falls", 0.935f to "israel_cheers", 0.97f to "sunlight_in_the_valley",
    )

    fun beatName(p: Float): String {
        val i = names.indexOfLast { p >= it.first }.coerceAtLeast(0)
        return "scene_%02d_%s".format(java.util.Locale.ROOT, i + 1, names[i].second)
    }
}

private val davidOutfit = Outfit(
    robe = Color(0xFFD9B98A),
    robeShade = Color(0xFFBF9E6E),
    sash = Color(0xFF3D6FA8),
    hair = Color(0xFF7A3E1E),
    tunic = true,
)
private val soldierIsrael = Outfit(Color(0xFFE9E1CF), Color(0xFFC9BFA8), Color(0xFF3D6FA8), headCloth = Color(0xFFDCD3BE))
private val soldierPhilistine = Outfit(Color(0xFF8C6A4A), Color(0xFF6E523A), Color(0xFF9C3A2A), headCloth = Color(0xFFB08A4A))
private val goliathOutfit = Outfit(
    robe = Color(0xFF8A6A3C),
    robeShade = Color(0xFF6E5430),
    sash = Color(0xFF5A3A22),
    skin = Color(0xFFB58460),
    hair = Color(0xFF2A1E16),
    beard = Color(0xFF2A1E16),
    tunic = true,
)
private val bronze = Color(0xFFB8893E)
private val bronzeLight = Color(0xFFE2BC6E)

internal fun DrawScope.drawDavidScene(st: DavidState, t: Float) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return
    val c = KitCamera(w, h, st.cameraX, st.zoom)
    val wm = st.warmth

    // Sky, sun and drifting clouds.
    kitSky(warmTri(0xFF4F9EE0, 0xFF5AA2E0, 0xFF6EA6D8, wm), warmTri(0xFFD3ECF8, 0xFFF2EBC4, 0xFFFFD99A, wm), c.sy(0.58f, 0.1f))
    kitSun(Offset(w * 0.70f, c.sy(0.18f + 0.15f * wm, 0.05f)), warmTri(0xFFFFFCEB, 0xFFFFF2C0, 0xFFFFE29A, wm), warmTri(0xFFFFF6D0, 0xFFFFE08A, 0xFFFFC46E, wm), t)
    kitClouds(c, t, Color.White)
    kitBirds(c, t, Color(0xFF3E4A55).copy(alpha = 0.6f))

    // Dry, sunlit hills of Judah.
    kitLayer(c, 0.10f, warmTri(0xFFA7B4C8, 0xFFB4B4C0, 0xFFBDAFB0, wm)) { kitRidge(it, 0.55f, 0.10f, 1.3f) }
    kitLayer(c, 0.22f, warmTri(0xFF9FAE8C, 0xFFAAAE84, 0xFFB2A67E, wm)) { kitRidge(it, 0.585f, 0.07f, 3.7f) }
    kitLayer(c, 0.40f, warmTri(0xFFB9B27A, 0xFFC2B072, 0xFFC6A66C, wm)) { kitRidge(it, 0.625f, 0.045f, 6.1f) }

    // Ground.
    val field = Path()
    field.moveTo(-10f, h + 10f)
    for (i in 0..56) {
        val sx = -10f + (w + 20f) * i / 56
        field.lineTo(sx, c.sy(DavidChoreography.groundY(c.worldX(sx))))
    }
    field.lineTo(w + 10f, h + 10f)
    field.close()
    val grass = warmTri(0xFF7DAA4C, 0xFF8EAA48, 0xFF98A246, wm)
    val dry = warmTri(0xFFB9AE72, 0xFFC2AC6C, 0xFFC4A468, wm)
    val stops = Array(9) { i ->
        val x = c.worldX(w * i / 8f)
        // Green pasture, then the dry valley floor.
        val v = ramp(x, -1.1f, 0.4f) * (1f - 0.4f * ramp(x, 3.0f, 3.8f))
        (i / 8f) to lerp(grass, dry, v)
    }
    drawPath(field, Brush.horizontalGradient(*stops, startX = 0f, endX = w))
    drawPath(field, Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF4A5A2A).copy(alpha = 0.55f)), startY = c.sy(0.68f), endY = h))

    val unit = c.unit

    // The brook, winding toward us.
    run {
        val water = warmTri(0xFF5BA6D6, 0xFF66A8D0, 0xFF7EAEC8, wm)
        val path = Path()
        val yTop = DavidChoreography.groundY(DavidChoreography.BROOK_X) - 0.004f
        val n = 16
        fun bx(y: Float) = DavidChoreography.BROOK_X + 0.04f * sin((y - 0.72f) * 18f) + (y - 0.73f) * 0.5f
        for (i in 0..n) {
            val y = yTop + (1.02f - yTop) * i / n
            val half = 0.018f + (y - yTop) * 0.10f
            val sx = c.sx(bx(y) - half)
            if (i == 0) path.moveTo(sx, c.sy(y)) else path.lineTo(sx, c.sy(y))
        }
        for (i in n downTo 0) {
            val y = yTop + (1.02f - yTop) * i / n
            val half = 0.018f + (y - yTop) * 0.10f
            path.lineTo(c.sx(bx(y) + half), c.sy(y))
        }
        path.close()
        drawPath(path, water)
        for (j in 0 until 10) {
            val y = yTop + 0.01f + 0.03f * j
            val a = 0.35f + 0.3f * sin(t * 1.4f + j)
            drawLine(Color.White.copy(alpha = a.coerceIn(0f, 1f)), Offset(c.sx(bx(y) - 0.01f), c.sy(y)), Offset(c.sx(bx(y) + 0.012f), c.sy(y)), max(1f, unit * 0.002f), StrokeCap.Round)
        }
        // Smooth stones along the bank.
        for (j in 0 until 9) {
            val y = yTop + 0.012f + 0.02f * j
            val side = if (j % 2 == 0) -1f else 1f
            val sx = c.sx(bx(y) + side * (0.03f + (y - yTop) * 0.1f))
            val r = unit * (0.004f + 0.0015f * (j % 3)) * (1f + (y - yTop) * 3f)
            drawOval(Color(0xFFB9B2A6), Offset(sx - r * 1.3f, c.sy(y) - r * 0.7f), Size(r * 2.6f, r * 1.4f))
        }
    }

    // Scrub and stones across the valley floor, nearer ones larger.
    for (k in LostSheepChoreography.slot(c.worldX(-60f), 0.07f)..LostSheepChoreography.slot(c.worldX(w + 60f), 0.07f)) {
        val hsh = LostSheepChoreography.hash(k, 41)
        if (hsh < 0.45f) continue
        val x = k * 0.07f + 0.05f * LostSheepChoreography.hash(k, 42)
        val depth = LostSheepChoreography.hash(k, 43) * 0.22f
        val sx = c.sx(x)
        val by = c.sy(DavidChoreography.groundY(x) + depth)
        val s = unit * (0.008f + 0.02f * depth / 0.22f) * (0.7f + 0.6f * hsh)
        if (hsh > 0.8f) {
            drawOval(Color(0xFFA59A88), Offset(sx - s * 1.4f, by - s * 0.9f), Size(s * 2.8f, s * 1.1f))
            drawOval(Color(0xFFD2C8B4), Offset(sx - s * 1.0f, by - s * 0.9f), Size(s * 1.4f, s * 0.45f))
        } else {
            val bush = lerp(Color(0xFF6E8A3A), Color(0xFF8A8A44), wm * 0.6f)
            drawCircle(bush, s, Offset(sx - s * 0.6f, by - s * 0.6f))
            drawCircle(bush, s * 1.2f, Offset(sx + s * 0.3f, by - s * 0.9f))
            drawCircle(lerp(bush, Color.Black, 0.15f), s * 0.8f, Offset(sx + s * 1.0f, by - s * 0.5f))
        }
    }

    // Tents on both hills.
    fun tent(x: Float, col: Color, size: Float) {
        val sx = c.sx(x)
        val by = c.sy(DavidChoreography.groundY(x) - 0.01f)
        val s = unit * size
        val p = Path().apply { moveTo(sx - s, by); lineTo(sx, by - s * 0.9f); lineTo(sx + s, by); close() }
        drawPath(p, col)
        drawLine(Color.Black.copy(alpha = 0.15f), Offset(sx, by - s * 0.9f), Offset(sx, by), s * 0.08f)
    }
    for (i in 0 until 6) tent(-0.85f + i * 0.21f + 0.04f * sin(i * 2f), if (i % 2 == 0) Color(0xFFEDE4CE) else Color(0xFFD9CCAE), 0.035f)
    for (i in 0 until 6) tent(3.2f + i * 0.2f + 0.04f * sin(i * 3f), if (i % 2 == 0) Color(0xFF9A7A54) else Color(0xFF7E6244), 0.035f)

    // The two armies along the crests.
    fun army(from: Float, to: Float, outfit: Outfit, facing: Float, cheer: Float, flee: Float, spear: Color, seed: Int) {
        var i = 0
        var x = from
        while (x < to) {
            val jitter = 0.025f * LostSheepChoreography.hash(i, seed)
            // Israel runs down into the valley after the victory; the Philistines flee over their hill.
            val wx = x + jitter + flee * (0.6f + 0.4f * LostSheepChoreography.hash(i, seed + 1)) + cheer * (1.0f + 0.3f * LostSheepChoreography.hash(i, seed + 2))
            val gy = DavidChoreography.groundY(wx) + 0.006f * (i % 3)
            val sh = unit * 0.075f
            val arms = if (cheer > 0f) {
                val wave = sin(t * 4f + i) * 0.05f * cheer
                Offset(0.05f, 0.30f - 0.55f * cheer + wave)
            } else {
                Offset(0.07f, 0.12f)
            }
            val alpha = 1f - flee
            if (alpha > 0.02f) {
                val sx = c.sx(wx)
                val fy = c.sy(gy)
                if (cheer == 0f) drawLine(spear.copy(alpha = alpha), Offset(sx + facing * 0.08f * sh, fy), Offset(sx + facing * 0.08f * sh, fy - sh * 1.25f), sh * 0.025f)
                kitPerson(sx, fy, sh, if (flee > 0.05f) -facing else facing, outfit, walking = flee > 0.05f || cheer in 0.05f..0.95f, walkPhase = t * 4f + i, frontHand = arms, backHand = if (cheer > 0f) Offset(-0.03f, 0.25f - 0.5f * cheer) else Offset(-0.05f, 0.30f))
            }
            x += 0.075f
            i++
        }
    }
    army(-0.62f, 0.30f, soldierIsrael, 1f, st.cheer, 0f, Color(0xFF6E4A2E), 3)
    army(3.05f, 3.95f, soldierPhilistine, -1f, 0f, st.philistinesFlee, Color(0xFF5A4630), 5)

    // David's flock in the pasture.
    for (i in 0 until 9) {
        val fx = -2.6f + i * 0.09f + 0.03f * sin(i * 2.3f)
        val sx = c.sx(fx)
        if (sx < -50f || sx > w + 50f) continue
        val s = unit * 0.026f * (0.85f + 0.3f * LostSheepChoreography.hash(i, 11))
        kitSheep(sx, c.sy(DavidChoreography.groundY(fx) + 0.01f * (i % 3)), s, if (i % 2 == 0) 1f else -1f, 0.5f + 0.5f * sin(t * 0.3f + i), 0f)
    }

    // Goliath, in bronze.
    val gh = unit * 0.25f
    val gsx = c.sx(st.goliathX)
    val gfy = c.sy(DavidChoreography.groundY(st.goliathX))
    rotate(degrees = -88f * st.goliathFall, pivot = Offset(gsx, gfy)) {
        drawGoliath(gsx, gfy, gh, t, st.goliathWalking)
    }

    // David.
    val dh = unit * 0.115f
    val dsx = c.sx(st.davidX)
    val dfy = c.sy(DavidChoreography.groundY(st.davidX))
    val walk = if (st.davidWalking) t * 3.4f else 0f
    // The sling whirls above his head; the stone leaves on the last turn.
    val whirl = st.windUp * 6f * 2f * PI.toFloat()
    val frontHand = when {
        st.windUp > 0f -> Offset(0.04f + 0.10f * cos(whirl), -0.10f + 0.10f * sin(whirl))
        st.stoneFlight in 0f..0.35f -> Offset(0.20f, -0.02f)
        st.cheer > 0f -> Offset(0.05f, -0.30f * st.cheer)
        st.kneel > 0.5f -> Offset(0.18f, 0.55f)
        else -> Offset(0.06f, 0.30f)
    }
    val backHand = if (st.cheer > 0f) Offset(-0.03f, -0.28f * st.cheer) else Offset(-0.06f, 0.28f)
    // Staff in the back hand while he walks.
    if (st.cheer < 0.5f && st.windUp == 0f && st.stoneFlight < 0f && st.kneel < 0.5f) {
        val tx = dsx - 0.10f * dh
        drawLine(Color(0xFF6E4A2E), Offset(tx + 0.02f * dh, dfy), Offset(tx - 0.03f * dh, dfy - 1.05f * dh), 0.03f * dh, StrokeCap.Round)
    }
    kitPerson(dsx, dfy, dh, st.davidFacing, davidOutfit, walk, st.davidWalking, st.kneel, frontHand, backHand)
    // Shepherd's bag, with the stones he has chosen.
    val bagX = dsx - 0.07f * dh
    val bagY = dfy - (0.52f - 0.15f * st.kneel) * dh
    drawOval(Color(0xFF8A6440), Offset(bagX - 0.06f * dh, bagY - 0.05f * dh), Size(0.12f * dh, 0.11f * dh))
    for (s in 0 until st.stones) {
        drawCircle(Color(0xFFD8D2C6), 0.012f * dh, Offset(bagX - 0.035f * dh + s * 0.017f * dh, bagY - 0.045f * dh))
    }
    if (st.windUp > 0f) {
        val hand = Offset(dsx + (0.06f * dh) + 0.10f * cos(whirl) * dh, dfy - dh * (1f - 0.20f) - 0.10f * dh + 0.10f * sin(whirl) * dh)
        val end = hand + Offset(cos(whirl * 1f) * 0.16f * dh, sin(whirl * 1f) * 0.16f * dh)
        drawLine(Color(0xFF5A3E26), hand, end, 0.012f * dh, StrokeCap.Round)
        drawCircle(Color(0xFFD8D2C6), 0.02f * dh, end)
    }
    // The stone in flight, from his hand to the giant's forehead.
    if (st.stoneFlight in 0f..0.999f) {
        val from = Offset(dsx + 0.2f * dh, dfy - 0.95f * dh)
        val to = Offset(gsx - 0.05f * gh, gfy - 0.90f * gh)
        val f = st.stoneFlight
        val pos = Offset(from.x + (to.x - from.x) * f, from.y + (to.y - from.y) * f - sin(f * PI.toFloat()) * 0.06f * unit)
        drawCircle(Color(0xFFE8E2D6), 0.018f * dh, pos)
        drawLine(Color.White.copy(alpha = 0.35f), pos, pos - Offset((to.x - from.x) * 0.06f, 0f), 0.012f * dh, StrokeCap.Round)
    }

    // Foreground grass and warm light toward the end.
    for (i in 0 until 40) {
        val bx = kitFrac(i * 0.137f - c.camX * 0.09f) * w * 1.2f - 0.1f * w
        val len = h * (0.03f + 0.03f * LostSheepChoreography.hash(i, 21))
        val sway = sin(t * 0.9f + i) * len * 0.15f
        drawLine(Color(0xFF4E6A2C), Offset(bx, h + 2f), Offset(bx + sway, h - len), max(1.5f, w * 0.004f), StrokeCap.Round)
    }
    val light = 0.10f * wm + 0.18f * st.cheer
    if (light > 0f) {
        drawRect(Brush.radialGradient(listOf(Color(0xFFFFE6A6).copy(alpha = light), Color.Transparent), center = Offset(w * 0.6f, h * 0.35f), radius = max(w, h)))
    }
}

private fun DrawScope.drawGoliath(cx: Float, feetY: Float, h: Float, t: Float, walking: Boolean) {
    val walk = if (walking) t * 2.2f else 0f
    // Spear, held upright behind him.
    drawLine(Color(0xFF5A4630), Offset(cx + 0.14f * h, feetY), Offset(cx + 0.10f * h, feetY - 1.25f * h), 0.03f * h, StrokeCap.Round)
    val tip = Path().apply {
        moveTo(cx + 0.10f * h, feetY - 1.38f * h)
        lineTo(cx + 0.125f * h, feetY - 1.25f * h)
        lineTo(cx + 0.075f * h, feetY - 1.25f * h)
        close()
    }
    drawPath(tip, Color(0xFF9AA0A6))
    kitPerson(cx, feetY, h, -1f, goliathOutfit, walk, walking, 0f, frontHand = Offset(0.10f, 0.32f), backHand = Offset(-0.14f, 0.05f))
    // Scale armor over the chest.
    val shoulderY = feetY - h + 0.20f * h
    drawRoundRectArmor(cx, shoulderY, h)
    // Bronze helmet with a crest.
    val headC = Offset(cx - 0.01f * h, feetY - h + 0.10f * h)
    drawArc(bronze, 180f, 180f, true, Offset(headC.x - 0.07f * h, headC.y - 0.075f * h), Size(0.14f * h, 0.11f * h))
    drawLine(bronzeLight, Offset(headC.x - 0.06f * h, headC.y - 0.02f * h), Offset(headC.x + 0.06f * h, headC.y - 0.02f * h), 0.012f * h, StrokeCap.Round)
    drawLine(Color(0xFF9C3A2A), Offset(headC.x - 0.03f * h, headC.y - 0.08f * h), Offset(headC.x + 0.05f * h, headC.y - 0.085f * h), 0.025f * h, StrokeCap.Round)
    // Round shield on his arm.
    val shield = Offset(cx - 0.10f * h, feetY - 0.62f * h)
    drawCircle(bronze, 0.12f * h, shield)
    drawCircle(bronzeLight.copy(alpha = 0.6f), 0.12f * h, shield, style = androidx.compose.ui.graphics.drawscope.Stroke(0.012f * h))
    drawCircle(bronzeLight, 0.025f * h, shield)
    // Greaves on his shins.
    drawLine(bronze, Offset(cx + 0.03f * h, feetY - 0.22f * h), Offset(cx + 0.03f * h, feetY - 0.05f * h), 0.05f * h, StrokeCap.Round)
}

private fun DrawScope.drawRoundRectArmor(cx: Float, shoulderY: Float, h: Float) {
    val top = shoulderY + 0.01f * h
    for (row in 0 until 5) {
        for (col in 0 until 4) {
            val x = cx - 0.075f * h + col * 0.045f * h + (row % 2) * 0.02f * h
            val y = top + row * 0.045f * h
            drawArc(if ((row + col) % 2 == 0) bronze else bronzeLight, 0f, 180f, true, Offset(x - 0.022f * h, y - 0.012f * h), Size(0.044f * h, 0.04f * h))
        }
    }
}
