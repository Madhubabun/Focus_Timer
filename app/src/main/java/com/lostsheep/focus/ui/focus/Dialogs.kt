package com.lostsheep.focus.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.lostsheep.focus.data.FocusSchedule
import com.lostsheep.focus.story.Stories
import androidx.compose.foundation.layout.width
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lostsheep.focus.session.ActiveSession
import com.lostsheep.focus.session.TimeSource
import com.lostsheep.focus.session.formatClock
import com.lostsheep.focus.story.FocusStory
import com.lostsheep.focus.ui.components.PrimaryPill
import com.lostsheep.focus.ui.theme.LocalSheepColors
import kotlinx.coroutines.delay

@Composable
private fun SheepDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Composable
fun CustomDurationDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var value by remember { mutableIntStateOf(initial.coerceIn(1, 180)) }
    SheepDialog(onDismiss) {
        Text("Custom focus", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { value = (value - 1).coerceAtLeast(1) }) { Text("–", style = MaterialTheme.typography.headlineSmall) }
            Text(
                if (value == 1) "1 minute" else "$value minutes",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            TextButton(onClick = { value = (value + 1).coerceAtMost(180) }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { value = snapMinutes(it) },
            valueRange = 1f..180f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Use – and + for an exact minute.", style = MaterialTheme.typography.labelMedium, color = LocalSheepColors.current.muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(onClick = { onConfirm(value) }) { Text("Set") }
        }
    }
}

/** Choose which Bible story plays during focus. Each shows a small still from its journey. */
@Composable
fun StoryPickerDialog(selectedId: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    SheepDialog(onDismiss) {
        Text("Choose a story", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Stories.all.forEach { story ->
            val selected = story.id == selectedId
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .toggleable(value = selected, role = Role.RadioButton) { onPick(story.id) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                story.Scene(
                    progress = { 0.3f },
                    time = { 0f },
                    celebration = { -1f },
                    modifier = Modifier
                        .size(width = 84.dp, height = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clearAndSetSemantics { },
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(story.title, style = MaterialTheme.typography.titleMedium)
                    Text(story.reference, style = MaterialTheme.typography.labelMedium, color = LocalSheepColors.current.muted)
                }
                if (selected) Text("✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
        }
        Text(
            "More stories coming: " + Stories.comingSoon.joinToString { it.first },
            style = MaterialTheme.typography.labelMedium,
            color = LocalSheepColors.current.muted,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onDismiss) { Text("Done") }
    }
}

/** Picks a time of day, as minutes after midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeOfDayDialog(title: String, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinute / 60, initialMinute = initialMinute % 60)
    SheepDialog(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        TimePicker(state = state)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("Set") }
        }
    }
}

/** Adds or edits a scheduled focus session: time, days and length. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(initial: FocusSchedule, onDismiss: () -> Unit, onSave: (FocusSchedule) -> Unit) {
    val time = rememberTimePickerState(initialHour = initial.minuteOfDay / 60, initialMinute = initial.minuteOfDay % 60)
    var days by remember { mutableStateOf(initial.days) }
    var minutes by remember { mutableIntStateOf(initial.durationMin) }
    SheepDialog(onDismiss) {
        Text("Scheduled focus", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        TimeInput(state = time)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DayOfWeek.entries.forEach { d ->
                val on = d in days
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .toggleable(value = on, role = Role.Checkbox) { days = if (it) days + d else days - d }
                        .semantics { contentDescription = d.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
                        .wrapContentHeight(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { minutes = (minutes - 5).coerceAtLeast(1) }) { Text("–", style = MaterialTheme.typography.titleLarge) }
            Text("$minutes min", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { minutes = (if (minutes == 1) 5 else minutes + 5).coerceAtMost(180) }) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        Text(
            "The session starts by itself and your chosen apps wait until it ends.",
            style = MaterialTheme.typography.labelMedium,
            color = LocalSheepColors.current.muted,
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(
                enabled = days.isNotEmpty(),
                onClick = { onSave(initial.copy(minuteOfDay = time.hour * 60 + time.minute, days = days, durationMin = minutes)) },
            ) { Text("Save") }
        }
    }
}

/** Single minutes up to 10, then steps of 5, so the slider is easy to land on a round number. */
fun snapMinutes(raw: Float): Int {
    val m = raw.toInt().coerceIn(1, 180)
    return if (m <= 10) m else ((m + 2) / 5 * 5).coerceIn(10, 180)
}

/** Shown when the app opens and a session is still going. Never discards it on its own. */
@Composable
fun RecoveryDialog(
    session: ActiveSession,
    clock: TimeSource,
    story: FocusStory,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(session) {
        while (true) {
            now = System.nanoTime()
            delay(1_000)
        }
    }
    val remaining = run { now; session.remainingMs(clock) }
    val progress = session.progress(clock)

    SheepDialog(onDismiss = onResume) {
        story.Scene(
            progress = { progress },
            time = { 0f },
            celebration = { -1f },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .clearAndSetSemantics { contentDescription = story.phaseAt(progress).description },
        )
        Spacer(Modifier.height(20.dp))
        Text("Your focus session is still active.", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "${formatClock(remaining)} remaining" + if (session.paused) " · paused" else "",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalSheepColors.current.muted,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryPill("Resume Focus", onClick = onResume, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onEnd) { Text("End Session", color = LocalSheepColors.current.muted) }
    }
}

@Composable
fun EndConfirmDialog(session: ActiveSession, clock: TimeSource, onKeep: () -> Unit, onEnd: () -> Unit) {
    val remaining = session.remainingMs(clock)
    SheepDialog(onDismiss = onKeep) {
        Text("End this focus session?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "${formatClock(remaining)} remaining",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalSheepColors.current.muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        PrimaryPill("Keep Focusing", onClick = onKeep, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onEnd) { Text("End Session", color = LocalSheepColors.current.muted) }
    }
}

/** Plain-language reason before sending someone to a system permission screen. */
@Composable
fun PermissionExplainDialog(title: String, body: String, onDismiss: () -> Unit, onContinue: () -> Unit) {
    SheepDialog(onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = LocalSheepColors.current.muted)
        Spacer(Modifier.height(22.dp))
        PrimaryPill("Continue", onClick = onContinue, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onDismiss) { Text("Not now", color = LocalSheepColors.current.muted) }
    }
}

object PermissionCopy {
    const val ACCESSIBILITY_TITLE = "Let the flock wait"
    const val ACCESSIBILITY_BODY =
        "To set apps aside during a focus session, The Lost Sheep uses Android's Accessibility permission. " +
            "It only notices which app comes to the front, and only during a session you started. " +
            "It never reads what is on your screen, never types or taps for you, and nothing leaves your phone.\n\n" +
            "On the next screen, find The Lost Sheep under Installed apps (or Downloaded apps) and turn it on."
    const val USAGE_TITLE = "Usage Access"
    const val USAGE_BODY =
        "Usage Access is a lighter fallback: it lets The Lost Sheep see which app is open so it can gently remind you " +
            "to return to focus. It can't always bring you back by itself, so Accessibility is recommended. " +
            "The information stays on your phone."
}
