package com.lostsheep.focus.story

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Shared drawing pieces for the newer stories: a parallax camera, sky, hills and a simple,
 * respectful human figure. The Lost Sheep keeps its own hand-tuned drawing.
 */

internal fun kitFrac(v: Float) = v - floor(v)

internal fun kitSmooth(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return c * c * (3 - 2 * c)
}

/** 0 before [a], 1 after [b], eased in between. */
internal fun ramp(p: Float, a: Float, b: Float): Float = if (b <= a) (if (p >= b) 1f else 0f) else kitSmooth((p - a) / (b - a))

/** Keyframes with soft starts and stops, and flowing through the keys in between. */
internal fun keyed(keys: List<Pair<Float, Float>>, p: Float): Float = LostSheepChoreography.glide(keys, p)

/** World-to-screen mapping with parallax; world y is a fraction of scene height. */
internal class KitCamera(val w: Float, val h: Float, val camX: Float, val zoom: Float, val camY: Float = 0f) {
    private val anchor = 0.66f
    val xs = max(w, h * 0.75f)
    val lift = if (h > w * 1.4f) 0.09f * h else 0f
    fun zoom(par: Float) = 1f + (zoom - 1f) * par
    fun sx(x: Float, par: Float = 1f) = w / 2f + (x - camX * par) * xs * zoom(par)
    fun sy(y: Float, par: Float = 1f) = anchor * h + (y - anchor - camY * par) * h * zoom(par) + lift
    fun worldX(screenX: Float, par: Float = 1f) = (screenX - w / 2f) / (xs * zoom(par)) + camX * par
    /** One scene-height in pixels at the ground's zoom. */
    val unit get() = h * zoom(1f)
}

internal fun kitRidge(x: Float, base: Float, amp: Float, seed: Float): Float {
    val n = 0.55f * sin(x * 2.2f + seed) + 0.30f * sin(x * 5.3f + seed * 1.7f) + 0.15f * sin(x * 11.7f + seed * 0.3f)
    return base - amp * (0.5f + 0.5f * n)
}

internal inline fun DrawScope.kitLayer(c: KitCamera, par: Float, color: Color, heightAt: (Float) -> Float) {
    val path = Path()
    val steps = 60
    path.moveTo(-10f, size.height + 10f)
    for (i in 0..steps) {
        val sx = -10f + (size.width + 20f) * i / steps
        path.lineTo(sx, c.sy(heightAt(c.worldX(sx, par)), par))
    }
    path.lineTo(size.width + 10f, size.height + 10f)
    path.close()
    drawPath(path, color)
}

internal fun DrawScope.kitSky(top: Color, horizon: Color, horizonY: Float) {
    drawRect(Brush.verticalGradient(listOf(top, horizon), startY = 0f, endY = horizonY))
    drawRect(horizon, topLeft = Offset(0f, horizonY), size = Size(size.width, size.height))
}

internal fun DrawScope.kitSun(center: Offset, core: Color, glow: Color, t: Float) {
    val glowR = max(size.width, size.height) * (0.30f + 0.015f * sin(t * 0.2f))
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.5f * glow.alpha), glow.copy(alpha = 0f)), center = center, radius = glowR), glowR, center)
    drawCircle(core, min(size.width, size.height) * 0.045f, center)
}

internal fun DrawScope.kitCloud(cx: Float, cy: Float, r: Float, col: Color) {
    drawOval(col, Offset(cx - r * 2.4f, cy - r * 0.5f), Size(r * 4.8f, r * 1.1f))
    drawCircle(col, r * 0.95f, Offset(cx - r * 0.9f, cy - r * 0.45f))
    drawCircle(col, r * 1.2f, Offset(cx + r * 0.1f, cy - r * 0.75f))
    drawCircle(col, r * 0.8f, Offset(cx + r * 1.1f, cy - r * 0.35f))
}

/** Drifting clouds; [cover] 0..1 adds more and larger ones (for gathering rain). */
internal fun DrawScope.kitClouds(c: KitCamera, t: Float, col: Color, cover: Float = 0f) {
    val n = 6 + (cover * 8).toInt()
    for (i in 0 until n) {
        val cx = (kitFrac((i * 0.23f + t * (0.0025f + (i % 6) * 0.0005f) - c.camX * 0.05f) / 1.5f) * 1.5f - 0.25f) * c.w
        val cy = c.sy(0.06f + 0.05f * (i % 6) + 0.02f * (i % 2) - 0.03f * cover, 0.05f)
        val r = min(c.w, c.h) * (0.040f + 0.012f * (i % 3)) * (1f + 0.8f * cover)
        kitCloud(cx, cy, r, col.copy(alpha = (0.88f - 0.06f * (i % 6)) * col.alpha))
    }
}

internal fun DrawScope.kitBirds(c: KitCamera, t: Float, col: Color, count: Int = 4) {
    for (i in 0 until count) {
        val f = kitFrac(t * (0.010f + 0.003f * i) + i * 0.27f)
        if (f > 0.55f) continue
        val bx = (f / 0.55f * 1.4f - 0.2f) * c.w
        val by = c.sy(0.14f + 0.045f * i + 0.01f * sin(t * 0.4f + i), 0.05f)
        val bs = min(c.w, c.h) * (0.014f - 0.002f * i)
        val flap = sin(t * 5.5f + i * 1.7f) * bs * 0.55f
        drawLine(col, Offset(bx - bs, by - flap), Offset(bx, by), bs * 0.22f, StrokeCap.Round)
        drawLine(col, Offset(bx + bs, by - flap), Offset(bx, by), bs * 0.22f, StrokeCap.Round)
    }
}

/** A plain, fluffy sheep, the same family as the Lost Sheep's flock. */
internal fun DrawScope.kitSheep(cx: Float, feetY: Float, s: Float, facing: Float, headDown: Float, legSwing: Float, wool: Color = Color(0xFFFFFDF7), shade: Color = Color(0xFFE6DCC8)) {
    val dark = Color(0xFF3B302A)
    drawOval(Color.Black.copy(alpha = 0.12f), Offset(cx - 0.8f * s, feetY - 0.1f * s), Size(1.6f * s, 0.22f * s))
    if (s > 5f) {
        val legs = floatArrayOf(-0.42f, -0.2f, 0.2f, 0.42f)
        for ((i, lx) in legs.withIndex()) {
            val sw = (if (i % 2 == 0) legSwing else -legSwing) * 0.12f * s
            drawLine(dark, Offset(cx + lx * s, feetY - 0.45f * s), Offset(cx + lx * s + sw, feetY), 0.11f * s, StrokeCap.Round)
        }
    }
    drawOval(shade, Offset(cx - 0.75f * s, feetY - 0.98f * s), Size(1.5f * s, 0.68f * s))
    drawOval(wool, Offset(cx - 0.72f * s, feetY - 1.06f * s), Size(1.44f * s, 0.62f * s))
    if (s > 4f) {
        drawCircle(wool, 0.26f * s, Offset(cx - 0.40f * s, feetY - 0.96f * s))
        drawCircle(wool, 0.28f * s, Offset(cx - 0.08f * s, feetY - 1.04f * s))
        drawCircle(wool, 0.26f * s, Offset(cx + 0.26f * s, feetY - 1.00f * s))
    }
    val hx = cx + facing * 0.8f * s
    val hy = feetY - (0.95f - 0.5f * headDown) * s
    drawOval(dark, Offset(hx - 0.24f * s, hy - 0.18f * s), Size(0.48f * s, 0.34f * s))
    drawOval(dark, Offset(hx - facing * 0.22f * s - 0.13f * s, hy - 0.2f * s), Size(0.26f * s, 0.1f * s))
}

/** How a figure is dressed. */
internal data class Outfit(
    val robe: Color,
    val robeShade: Color,
    val sash: Color,
    val skin: Color = Color(0xFFC6916A),
    val hair: Color = Color(0xFF3A2719),
    val beard: Color? = null,
    val sandal: Color = Color(0xFF7A5232),
    /** Short tunic (a young man) instead of a long robe. */
    val tunic: Boolean = false,
    val headCloth: Color? = null,
)

/**
 * A simple standing or kneeling person in profile. Hands go where the caller puts them, as
 * offsets from the shoulders in units of height, so each story can pose its people.
 */
internal fun DrawScope.kitPerson(
    cx: Float,
    feetY: Float,
    h: Float,
    facing: Float,
    outfit: Outfit,
    walkPhase: Float = 0f,
    walking: Boolean = false,
    kneel: Float = 0f,
    frontHand: Offset = Offset(0.06f, 0.30f),
    backHand: Offset = Offset(-0.05f, 0.30f),
) {
    val f = facing
    val bob = if (walking) abs(sin(walkPhase)) * 0.012f * h else 0f
    val hh = h * (1f - 0.30f * kneel)
    val top = feetY - hh - bob
    val shoulderY = top + 0.20f * h
    val hemY = feetY - (if (outfit.tunic) 0.26f * (1f - kneel) + 0.03f else 0.03f) * h
    val headC = Offset(cx + f * 0.01f * h, top + 0.10f * h)
    drawOval(Color.Black.copy(alpha = 0.14f), Offset(cx - 0.22f * h, feetY - 0.025f * h), Size(0.44f * h, 0.05f * h))

    // Legs for a short tunic, feet for a long robe.
    val step = if (walking) sin(walkPhase) * 0.06f * h else 0.025f * h
    if (outfit.tunic && kneel < 0.5f) {
        for (dir in floatArrayOf(1f, -1f)) {
            val fx = cx + dir * step
            drawLine(outfit.skin, Offset(cx + dir * 0.03f * h, hemY - 0.01f * h), Offset(fx, feetY - 0.02f * h), 0.04f * h, StrokeCap.Round)
        }
    }
    if (kneel < 0.3f) {
        for (dir in floatArrayOf(1f, -1f)) {
            val fx = cx + dir * step + f * 0.02f * h
            drawOval(outfit.skin, Offset(fx - 0.035f * h + f * 0.01f * h, feetY - 0.03f * h), Size(0.07f * h, 0.03f * h))
            drawLine(outfit.sandal, Offset(fx - 0.035f * h, feetY - 0.004f * h), Offset(fx + 0.045f * h, feetY - 0.004f * h), 0.012f * h, StrokeCap.Round)
        }
    }

    val sway = if (walking) sin(walkPhase) * 0.02f * h else 0f
    val hemHalf = (if (outfit.tunic) 0.12f else 0.16f) * h + 0.08f * h * kneel
    val robe = Path().apply {
        moveTo(cx - 0.09f * h, shoulderY)
        lineTo(cx + 0.09f * h, shoulderY)
        quadraticTo(cx + 0.13f * h, (shoulderY + hemY) / 2f, cx + hemHalf + sway, hemY)
        lineTo(cx - hemHalf + sway, hemY)
        quadraticTo(cx - 0.13f * h, (shoulderY + hemY) / 2f, cx - 0.09f * h, shoulderY)
        close()
    }
    drawPath(robe, outfit.robe)
    val fold = Path().apply {
        moveTo(cx - f * 0.02f * h, shoulderY + 0.05f * h)
        lineTo(cx - f * 0.09f * h, shoulderY)
        quadraticTo(cx - f * 0.13f * h, (shoulderY + hemY) / 2f, cx - f * hemHalf + sway, hemY)
        lineTo(cx - f * 0.05f * h + sway, hemY)
        close()
    }
    drawPath(fold, outfit.robeShade)
    // Sash across the body.
    drawLine(outfit.sash, Offset(cx - f * 0.08f * h, shoulderY + 0.01f * h), Offset(cx + f * 0.09f * h, shoulderY + 0.30f * h), 0.035f * h, StrokeCap.Round)
    drawLine(outfit.sash, Offset(cx - 0.10f * h, shoulderY + 0.30f * h), Offset(cx + 0.10f * h, shoulderY + 0.30f * h), 0.018f * h, StrokeCap.Round)

    // Hair behind the head.
    val r = 0.062f * h
    drawCircle(outfit.hair, r * 1.06f, headC + Offset(-f * 0.016f * h, 0f))

    // Arms.
    val armW = 0.045f * h
    val shoulderFront = Offset(cx + f * 0.06f * h, shoulderY + 0.02f * h)
    val shoulderBack = Offset(cx - f * 0.04f * h, shoulderY + 0.03f * h)
    fun hand(o: Offset) = Offset(cx + f * o.x * h, shoulderY + o.y * hh)
    val back = hand(backHand)
    drawLine(outfit.robeShade, shoulderBack, back, armW, StrokeCap.Round)
    drawCircle(outfit.skin, armW * 0.42f, back)

    // Head.
    val face = headC + Offset(f * 0.012f * h, 0.004f * h)
    drawCircle(outfit.skin, r * 0.84f, face)
    if (outfit.beard != null) {
        drawOval(outfit.beard, Offset(face.x - 0.04f * h + f * 0.004f * h, face.y + 0.010f * h), Size(0.08f * h, 0.075f * h))
    }
    val crown = outfit.headCloth ?: outfit.hair
    drawArc(crown, 180f, 180f, true, Offset(headC.x - r * 1.02f, headC.y - r * 1.04f), Size(2.04f * r, 1.3f * r))
    if (outfit.headCloth != null) {
        drawRect(outfit.headCloth, Offset(headC.x - f * r * 1.0f - (if (f > 0) 0f else r * 0.5f), headC.y - r * 0.4f), Size(r * 0.5f, r * 1.8f))
    }
    if (h > 120f) {
        val eye = Offset(face.x + f * r * 0.42f, face.y - r * 0.12f)
        drawLine(outfit.hair.copy(alpha = 0.8f), eye + Offset(-r * 0.12f, 0f), eye + Offset(r * 0.12f, r * 0.02f), r * 0.07f, StrokeCap.Round)
    }

    val front = hand(frontHand)
    drawLine(lerp(outfit.robe, outfit.robeShade, 0.3f), shoulderFront, front, armW, StrokeCap.Round)
    drawCircle(outfit.skin, armW * 0.42f, front)
}

/** Light that follows the session: clear morning to warm late afternoon. */
internal fun warmTri(a: Long, b: Long, c: Long, w: Float): Color =
    if (w < 0.5f) lerp(Color(a), Color(b), w * 2f) else lerp(Color(b), Color(c), (w - 0.5f) * 2f)
