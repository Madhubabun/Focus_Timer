package com.lostsheep.focus.ui.components

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lostsheep.focus.ui.theme.LocalSheepColors
import kotlinx.coroutines.delay

/** Thin circular track with an arc for [fraction] (0..1), drawn clockwise from 12 o'clock. */
@Composable
fun TimerRing(fraction: Float, modifier: Modifier = Modifier, stroke: Dp = 3.dp, arcColor: Color = MaterialTheme.colorScheme.primary) {
    val track = LocalSheepColors.current.track
    Canvas(modifier) {
        val sw = stroke.toPx()
        val d = size.minDimension - sw
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        drawArc(track, 0f, 360f, false, topLeft, Size(d, d), style = Stroke(sw))
        if (fraction > 0f) {
            drawArc(arcColor, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
        }
    }
}

@Composable
fun PrimaryPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(horizontal = 36.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 52.dp),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground) }
}

/** Respect the system "remove animations" setting. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * A clock for animated scenes: frame-driven at about 30 fps while [running], once a second with
 * reduced motion, and completely still otherwise. Gentle scenes don't need more, and it saves battery.
 */
@Composable
fun rememberSceneClock(running: Boolean, reducedMotion: Boolean): State<Long> {
    val now = remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(running, reducedMotion) {
        if (!running) return@LaunchedEffect
        if (reducedMotion) {
            while (true) {
                now.longValue = System.nanoTime()
                delay(1_000)
            }
        } else {
            var last = 0L
            while (true) {
                withFrameNanos { frame ->
                    if (frame - last >= 32_000_000L) {
                        last = frame
                        now.longValue = System.nanoTime()
                    }
                }
            }
        }
    }
    return now
}

@Composable
fun FullWidthDivider(modifier: Modifier = Modifier) {
    val c = LocalSheepColors.current.hairline
    Canvas(modifier.fillMaxWidth().height(1.dp)) { drawRect(c) }
}

@Composable
fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) { content() }
}

/** A handful of quiet line icons, drawn here so the app needs no icon library. */
object SheepIcons {
    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private val ink = SolidColor(Color.Black)

    val Focus: ImageVector = icon("Focus") {
        path(stroke = ink, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 3f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 21f)
            arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12f, y1 = 3f)
            moveTo(12f, 7.5f)
            lineTo(12f, 12f)
            lineTo(15f, 14f)
        }
    }

    val Journey: ImageVector = icon("Journey") {
        path(stroke = ink, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(2.5f, 19f)
            quadTo(8f, 8f, 13f, 15f)
            quadTo(17f, 10.5f, 21.5f, 19f)
            moveTo(18f, 4.5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18f, y1 = 8.5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18f, y1 = 4.5f)
        }
    }

    val Stats: ImageVector = icon("Stats") {
        path(stroke = ink, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(6f, 20f); lineTo(6f, 13f)
            moveTo(12f, 20f); lineTo(12f, 6f)
            moveTo(18f, 20f); lineTo(18f, 10f)
        }
    }

    val Settings: ImageVector = icon("Settings") {
        path(stroke = ink, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(4f, 7f); lineTo(7f, 7f)
            moveTo(11f, 7f); lineTo(20f, 7f)
            moveTo(4f, 17f); lineTo(13f, 17f)
            moveTo(17f, 17f); lineTo(20f, 17f)
            moveTo(9f, 5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9f, y1 = 9f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9f, y1 = 5f)
            moveTo(15f, 15f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 19f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 15f)
        }
    }

    val Pause: ImageVector = icon("Pause") {
        path(fill = ink) {
            moveTo(7f, 5f); lineTo(10f, 5f); lineTo(10f, 19f); lineTo(7f, 19f); close()
            moveTo(14f, 5f); lineTo(17f, 5f); lineTo(17f, 19f); lineTo(14f, 19f); close()
        }
    }

    val Back: ImageVector = icon("Back") {
        path(stroke = ink, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f)
        }
    }
}
