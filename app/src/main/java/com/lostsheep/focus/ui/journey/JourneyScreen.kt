package com.lostsheep.focus.ui.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.lostsheep.focus.data.FocusSessionEntity
import com.lostsheep.focus.data.formatDuration
import com.lostsheep.focus.story.Verse
import com.lostsheep.focus.ui.components.FullWidthDivider
import com.lostsheep.focus.ui.theme.LocalSheepColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun JourneyScreen(completed: Int, history: List<FocusSessionEntity>, verse: Verse, dark: Boolean) {
    val muted = LocalSheepColors.current.muted
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 24.dp),
    ) {
        item {
            Text("My Journey", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(16.dp))
            JourneyLandscape(
                completed,
                dark,
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clearAndSetSemantics { contentDescription = landscapeDescription(completed) },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (completed == 1) "1 session completed" else "$completed sessions completed",
                style = MaterialTheme.typography.titleMedium,
            )
            nextMilestone(completed)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = muted)
            }
            Spacer(Modifier.height(36.dp))

            Text("TODAY'S REFLECTION", style = MaterialTheme.typography.labelSmall, color = muted)
            Spacer(Modifier.height(12.dp))
            Text(verse.reference, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                verse.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = MaterialTheme.typography.titleLarge.fontFamily, fontStyle = FontStyle.Italic),
            )
            if (verse.translation.isNotEmpty()) {
                Text(verse.translation, style = MaterialTheme.typography.labelSmall, color = muted)
            }
            Spacer(Modifier.height(16.dp))
            Text("Every focused moment is a step toward what matters.", style = MaterialTheme.typography.bodyMedium, color = muted)
            Spacer(Modifier.height(36.dp))

            Text("SESSION HISTORY", style = MaterialTheme.typography.labelSmall, color = muted)
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text("Your first session will appear here.", style = MaterialTheme.typography.bodyMedium, color = muted)
            }
        }
        items(history.take(60), key = { it.id }) { s -> HistoryRow(s) }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm")

@Composable
private fun HistoryRow(s: FocusSessionEntity) {
    val muted = LocalSheepColors.current.muted
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).format(dateFormat),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    if (s.completed) "The lost sheep was found" else "Ended early",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (s.completed) LocalSheepColors.current.gold else muted,
                )
            }
            Text(formatDuration(s.focusedMs), style = MaterialTheme.typography.titleMedium)
        }
        FullWidthDivider()
    }
}

private fun nextMilestone(n: Int): String? = when {
    n < 1 -> "Complete a session to begin your landscape."
    n < 5 -> "Trees appear after 5 sessions."
    n < 10 -> "The landscape widens after 10 sessions."
    n < 20 -> "A peaceful valley awaits at 20 sessions."
    else -> null
}

private fun landscapeDescription(n: Int): String = when {
    n < 1 -> "An empty field, waiting for your first session."
    n < 5 -> "A small hill with $n sheep."
    n < 10 -> "A hill with trees and $n sheep."
    n < 20 -> "A wider landscape with mountains, trees and ${min(n, 40)} sheep."
    else -> "A peaceful valley with a stream, trees and a flock of ${min(n, 40)} sheep."
}

/** A landscape that grows quietly with each completed session; one more sheep for each. */
@Composable
fun JourneyLandscape(completed: Int, dark: Boolean, modifier: Modifier) {
    Canvas(modifier) { drawJourney(completed, dark) }
}

private fun DrawScope.drawJourney(n: Int, dark: Boolean) {
    val w = size.width
    val h = size.height
    val skyTop = if (dark) Color(0xFF2A3530) else Color(0xFFE6EEE8)
    val skyBottom = if (dark) Color(0xFF3B4038) else Color(0xFFF6EEDC)
    drawRect(Brush.verticalGradient(listOf(skyTop, skyBottom)))

    val r = Random(7)
    if (n >= 10) {
        val m = Path().apply {
            moveTo(0f, h * 0.55f)
            lineTo(w * 0.18f, h * 0.30f); lineTo(w * 0.32f, h * 0.46f); lineTo(w * 0.5f, h * 0.24f)
            lineTo(w * 0.7f, h * 0.44f); lineTo(w * 0.85f, h * 0.32f); lineTo(w, h * 0.5f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(m, if (dark) Color(0xFF4A5650) else Color(0xFFBCC8C2))
    }
    // Sun.
    drawCircle(if (dark) Color(0xFFD9C08A) else Color(0xFFF3DDA6), h * 0.07f, Offset(w * 0.8f, h * 0.22f))

    val far = if (dark) Color(0xFF55664F) else Color(0xFFB4C6A2)
    val near = if (dark) Color(0xFF475A43) else Color(0xFF97B08A)

    if (n >= 10) {
        val hill2 = Path().apply {
            moveTo(0f, h * 0.7f)
            quadraticTo(w * 0.25f, h * 0.5f, w * 0.55f, h * 0.66f)
            quadraticTo(w * 0.8f, h * 0.58f, w, h * 0.68f)
            lineTo(w, h); lineTo(0f, h); close()
        }
        drawPath(hill2, far)
    }
    // The first hill appears with the first session; it grows a little until session 10.
    val hillH = if (n >= 1) h * (0.18f + 0.012f * min(n, 10)) else h * 0.08f
    val hill = Path().apply {
        moveTo(0f, h)
        lineTo(0f, h * 0.86f)
        quadraticTo(w * 0.45f, h - hillH * 2f, w, h * 0.84f)
        lineTo(w, h)
        close()
    }
    drawPath(hill, near)

    if (n >= 20) {
        // A stream winding through the valley.
        val stream = Path().apply {
            moveTo(w * 0.05f, h)
            cubicTo(w * 0.3f, h * 0.86f, w * 0.15f, h * 0.8f, w * 0.45f, h * 0.76f)
        }
        drawPath(stream, if (dark) Color(0xFF7D9AA3) else Color(0xFFBFD6DC), style = Stroke(h * 0.035f, cap = StrokeCap.Round))
    }
    if (n >= 3) {
        val path = Path().apply {
            moveTo(w * 0.62f, h)
            quadraticTo(w * 0.5f, h * 0.88f, w * 0.58f, h * 0.8f)
        }
        drawPath(path, if (dark) Color(0x55E8D9B5) else Color(0x99EBDDBB), style = Stroke(h * 0.02f, cap = StrokeCap.Round))
    }
    if (n >= 5) {
        val trees = min(2 + (n - 5) / 2, 8)
        repeat(trees) { i ->
            val tx = w * (0.08f + 0.85f * ((i * 0.37f + 0.13f) % 1f))
            val ty = h * (0.80f + 0.06f * sin(i * 1.7f))
            drawLine(Color(0xFF6A5442), Offset(tx, ty), Offset(tx, ty - h * 0.08f), strokeWidth = h * 0.012f)
            drawCircle(if (dark) Color(0xFF5E7552) else Color(0xFF7E9468), h * 0.05f, Offset(tx, ty - h * 0.1f))
        }
    }
    // One sheep for each completed session (a gentle flock, capped so it stays calm).
    val sheep = min(n, 40)
    repeat(sheep) {
        val sx = w * (0.1f + 0.8f * r.nextFloat())
        val sy = h * (0.84f + 0.12f * r.nextFloat())
        val s = h * 0.022f
        drawOval(Color(0xFFFAF6EC), Offset(sx - s, sy - s * 0.7f), Size(s * 2f, s * 1.3f))
        drawCircle(Color(0xFF43392F), s * 0.4f, Offset(sx + s * 1.05f, sy - s * 0.35f))
    }
}
