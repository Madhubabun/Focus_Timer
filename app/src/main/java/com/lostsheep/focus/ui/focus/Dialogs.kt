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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
    var value by remember { mutableFloatStateOf(initial.toFloat()) }
    SheepDialog(onDismiss) {
        Text("Custom focus", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("${value.toInt()} minutes", style = MaterialTheme.typography.headlineMedium)
        Slider(
            value = value,
            onValueChange = { value = (it / 5f).toInt() * 5f },
            valueRange = 5f..180f,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            TextButton(onClick = { onConfirm(value.toInt().coerceAtLeast(5)) }) { Text("Set") }
        }
    }
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
