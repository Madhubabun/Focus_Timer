package com.lostsheep.focus.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lostsheep.focus.data.FocusStats
import com.lostsheep.focus.data.formatDuration
import com.lostsheep.focus.ui.components.FullWidthDivider
import com.lostsheep.focus.ui.theme.LocalSheepColors
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(stats: FocusStats, dailyGoal: Int) {
    val muted = LocalSheepColors.current.muted
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Stats", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Today's Focus", formatDuration(stats.todayFocusMs), Modifier.weight(1f))
            Stat("Sessions · goal $dailyGoal", "${stats.todayCompleted}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("Current Streak", if (stats.currentStreakDays == 1) "1 day" else "${stats.currentStreakDays} days", Modifier.weight(1f))
            Stat("Distractions Blocked", "${stats.distractionsBlocked}", Modifier.weight(1f))
        }
        if (stats.intentionsAnswered > 0) {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat("Intentions finished", "${stats.intentionsDone} of ${stats.intentionsAnswered}", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(36.dp))
        FullWidthDivider()
        Spacer(Modifier.height(24.dp))
        Text("LAST 7 DAYS", style = MaterialTheme.typography.labelSmall, color = muted)
        Spacer(Modifier.height(16.dp))
        WeekChart(stats)
        Spacer(Modifier.height(36.dp))
        FullWidthDivider()
        Spacer(Modifier.height(24.dp))
        Text("ALL TIME", style = MaterialTheme.typography.labelSmall, color = muted)
        Spacer(Modifier.height(8.dp))
        Text(
            "${formatDuration(stats.totalFocusMs)} focused across ${stats.totalCompleted} completed sessions",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelMedium, color = LocalSheepColors.current.muted)
    }
}

@Composable
private fun WeekChart(stats: FocusStats) {
    val days = stats.lastSevenDays
    if (days.isEmpty()) return
    val bar = MaterialTheme.colorScheme.secondary
    val today = LocalSheepColors.current.gold
    val track = LocalSheepColors.current.track
    val maxMs = (days.maxOf { it.focusedMs }).coerceAtLeast(30 * 60_000L)
    val summary = days.joinToString { "${it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${formatDuration(it.focusedMs)}" }

    Column(Modifier.clearAndSetSemantics { contentDescription = "Focus time over the last seven days: $summary" }) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / days.size
            val bw = slot * 0.36f
            days.forEachIndexed { i, d ->
                val x = slot * i + (slot - bw) / 2f
                drawRoundRect(track, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 2f))
                val bh = size.height * (d.focusedMs.toFloat() / maxMs)
                if (bh > 0f) {
                    drawRoundRect(
                        if (i == days.lastIndex) today else bar,
                        Offset(x, size.height - bh),
                        Size(bw, bh),
                        CornerRadius(bw / 2f),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            days.forEach { d ->
                Text(
                    d.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalSheepColors.current.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

