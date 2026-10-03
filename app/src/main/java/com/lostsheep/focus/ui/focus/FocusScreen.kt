package com.lostsheep.focus.ui.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.core.content.ContextCompat
import com.lostsheep.focus.data.AppSettings
import com.lostsheep.focus.data.FocusStats
import com.lostsheep.focus.data.formatDuration
import com.lostsheep.focus.session.ActiveSession
import com.lostsheep.focus.session.SessionOutcome
import com.lostsheep.focus.session.TimeSource
import com.lostsheep.focus.session.formatClock
import com.lostsheep.focus.story.Closing
import com.lostsheep.focus.story.FocusStory
import com.lostsheep.focus.story.FocusVerses
import com.lostsheep.focus.story.Verse
import com.lostsheep.focus.story.StoryVideo
import com.lostsheep.focus.story.rememberStoryClips
import com.lostsheep.focus.story.stageFraction
import com.lostsheep.focus.ui.components.PrimaryPill
import com.lostsheep.focus.ui.components.SecondaryPill
import com.lostsheep.focus.ui.components.SheepIcons
import com.lostsheep.focus.ui.components.TimerRing
import com.lostsheep.focus.ui.components.rememberReducedMotion
import com.lostsheep.focus.ui.components.rememberSceneClock
import com.lostsheep.focus.ui.theme.LocalSheepColors
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Idle: the home screen
// ---------------------------------------------------------------------------------------------

private val presets = listOf(15, 25, 45, 60)

@Composable
fun FocusHome(
    settings: AppSettings,
    stats: FocusStats,
    story: FocusStory,
    blockedCount: Int,
    blockingReady: Boolean,
    intention: String,
    onIntentionChange: (String) -> Unit,
    onBegin: (Int) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onChooseApps: () -> Unit,
    onFixBlocking: () -> Unit,
    onChooseStory: () -> Unit,
) {
    val muted = LocalSheepColors.current.muted
    val minutes = settings.defaultDurationMin
    var showCustom by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onBegin(minutes) // begin either way; the notification is optional
    }
    fun begin() {
        val needsAsk = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else onBegin(minutes)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("The Lost Sheep", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        Text(story.reference.uppercase(), style = MaterialTheme.typography.labelSmall, color = muted)
        Spacer(Modifier.height(20.dp))

        // A small, still hillside with the flock.
        story.Scene(
            progress = { 0f },
            time = { 0f },
            celebration = { -1f },
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .clip(RoundedCornerShape(24.dp))
                .clearAndSetSemantics { contentDescription = story.phases.first().description },
        )
        TextButton(onClick = onChooseStory) {
            Text("Story: ${story.title} · Change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(12.dp))
        Text(
            formatClock(minutes * 60_000L),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { contentDescription = "$minutes minute focus session" },
        )
        Text("FOCUS", style = MaterialTheme.typography.labelSmall, color = muted)

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { m ->
                DurationChip("$m", selected = m == minutes) { onSelectDuration(m) }
            }
            DurationChip(if (minutes in presets) "Custom" else "$minutes", selected = minutes !in presets) { showCustom = true }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = intention,
            onValueChange = onIntentionChange,
            placeholder = { Text("What will you focus on? (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        PrimaryPill("Begin Focus", onClick = { begin() }, modifier = Modifier.fillMaxWidth(0.8f))

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val streak = stats.currentStreakDays
            if (streak > 0) Indicator("🔥 $streak day streak", "$streak day streak")
            Indicator("Today: ${formatDuration(stats.todayFocusMs)}", "Focused today: ${formatDuration(stats.todayFocusMs)}")
            val goal = settings.dailyGoalSessions
            Indicator("${minOf(stats.todayCompleted, goal)}/$goal", "${stats.todayCompleted} of $goal sessions today")
        }

        Spacer(Modifier.height(20.dp))
        when {
            blockedCount == 0 -> QuietLink("Choose apps to set aside while you focus", onChooseApps)
            !blockingReady -> QuietLink("App blocking needs a permission · Set up", onFixBlocking)
            else -> Text("$blockedCount ${if (blockedCount == 1) "app" else "apps"} will wait while you focus", style = MaterialTheme.typography.labelMedium, color = muted)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showCustom) {
        CustomDurationDialog(initial = minutes, onDismiss = { showCustom = false }) {
            showCustom = false
            onSelectDuration(it)
        }
    }
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) scheme.primary else Color.Transparent)
            .border(1.dp, if (selected) scheme.primary else scheme.outline, RoundedCornerShape(20.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics { contentDescription = if (label.all { it.isDigit() }) "$label minutes" else label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) scheme.onPrimary else scheme.onBackground)
    }
}

@Composable
private fun Indicator(text: String, spoken: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LocalSheepColors.current.track)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun QuietLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    }
}

// ---------------------------------------------------------------------------------------------
// Running and paused: the story fills the screen, the timer floats on top of it
// ---------------------------------------------------------------------------------------------

/** Overlays sit on the artwork, so they keep one light style in both themes. */
private val Glass = Color(0xBFFFFDF7)
private val GlassSolid = Color(0xF2FFFDF7)
private val GlassInk = Color(0xFF26332B)
private val GlassMuted = Color(0xFF5E6A5F)
private val Gold = Color(0xFFC99A3E)
private val ButtonGreen = Color(0xFF2E5A3F)

@Composable
fun FocusSessionScreen(
    session: ActiveSession,
    clock: TimeSource,
    story: FocusStory,
    blockingWarning: Boolean,
    showVerses: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEndRequest: () -> Unit,
    onDue: () -> Unit,
    onFixBlocking: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val frame = rememberSceneClock(running = !session.paused, reducedMotion = reducedMotion)

    // Everything below is computed from timestamps, never counted down.
    val remainingSec by remember(session) {
        derivedStateOf {
            frame.value
            (session.remainingMs(clock) + 999) / 1000
        }
    }
    val phase by remember(session) {
        derivedStateOf {
            frame.value
            story.phaseAt(session.progress(clock))
        }
    }
    val verse by remember(session) {
        derivedStateOf {
            frame.value
            FocusVerses.at(session.sessionId, session.elapsedMs(clock), session.plannedDurationMs)
        }
    }
    val beat by remember(session) {
        derivedStateOf {
            frame.value
            story.stateNameAt(session.progress(clock))
        }
    }

    LaunchedEffect(remainingSec, session.paused) {
        if (!session.paused && remainingSec <= 0) onDue()
    }

    val progress: () -> Float = {
        frame.value
        session.progress(clock)
    }
    // The ambient clock is the focused time itself: still while paused, continuous across restarts.
    val sceneTime: () -> Float = {
        frame.value
        if (reducedMotion) 0f else session.elapsedMs(clock) / 1000f
    }
    val dim by animateFloatAsState(if (session.paused) 0.38f else 0f, tween(700), label = "dim")
    val overlayAlpha by animateFloatAsState(if (session.paused) 0f else 1f, tween(400), label = "overlay")

    val clips = rememberStoryClips(story)
    var videoShowing by remember { mutableStateOf(false) }
    val sceneDescription = Modifier.clearAndSetSemantics { contentDescription = "${phase.description} ($beat)" }

    Box(Modifier.fillMaxSize().background(Color(0xFF62AC48))) {
        // The drawn scene, unless the story's video clips are on screen.
        if (!videoShowing) {
            story.Scene(progress = progress, time = sceneTime, celebration = { -1f }, modifier = Modifier.fillMaxSize().then(sceneDescription))
        }
        clips?.pathFor(phase.key)?.let { path ->
            StoryVideo(
                clipPath = path,
                fraction = { story.stageFraction(progress()) },
                modifier = Modifier.fillMaxSize().then(sceneDescription),
                onReady = { videoShowing = it },
            )
        }
        // A soft scrim so the status bar and timer stay readable on bright sky.
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Brush.verticalGradient(listOf(Color(0x382A3C50), Color.Transparent))),
        )
        if (dim > 0f) Box(Modifier.fillMaxSize().background(Color(0xFF17221C).copy(alpha = dim)))

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .alpha(overlayAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            FloatingTimer(remainingSec, session.intention)
            Spacer(Modifier.height(36.dp))
            StoryCaption(session.paused, phase.caption, phase.captionPersistent, phase.key)
            Spacer(Modifier.weight(1f))
            if (showVerses && !session.paused) FocusVerseCard(verse.verse, verse.alpha)
            if (blockingWarning) BlockingWarning(onFixBlocking)
            if (!session.paused) PauseButton(onPause)
            Spacer(Modifier.height(32.dp))
        }

        AnimatedVisibility(
            visible = session.paused,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            GlassCard(
                title = "Focus Paused",
                body = "${formatClock(remainingSec * 1000)} remaining",
                primary = "Resume Focus" to onResume,
                secondary = "End Session" to onEndRequest,
            )
        }
    }
}

@Composable
private fun FloatingTimer(remainingSec: Long, intention: String) {
    val finalMinute = remainingSec in 1L..60L
    Column(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = "${remainingSec / 60} minutes ${remainingSec % 60} seconds of focus remaining"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(formatClock(remainingSec * 1000), style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp), color = GlassInk)
        Text("FOCUS TIME", style = MaterialTheme.typography.labelSmall, color = GlassMuted)
        if (intention.isNotEmpty()) {
            Text(
                intention,
                style = MaterialTheme.typography.labelMedium,
                color = GlassInk,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 240.dp),
            )
        }
        // The last minute gets a quiet gold line counting down.
        Box(Modifier.padding(top = 6.dp).width(96.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (finalMinute) Gold.copy(alpha = 0.22f) else Color.Transparent)) {
            if (finalMinute) Box(Modifier.fillMaxHeight().fillMaxWidth(remainingSec / 60f).background(Gold))
        }
    }
}

/** A verse over the lower part of the scene; it comes and goes with focused time, so pausing keeps it still. */
@Composable
private fun FocusVerseCard(verse: Verse, alpha: Float) {
    if (alpha <= 0f) return
    Column(
        Modifier
            .padding(horizontal = 28.dp)
            .padding(bottom = 16.dp)
            .widthIn(max = 420.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(20.dp))
            .background(Glass)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "“${verse.text}”",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = GlassInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(verse.reference.uppercase(), style = MaterialTheme.typography.labelSmall, color = GlassMuted)
    }
}

/** One line over the scene. It fades in as its stage begins, then fades away. */
@Composable
private fun StoryCaption(paused: Boolean, caption: String?, persistent: Boolean, phaseKey: String) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(phaseKey, paused) {
        if (paused || caption == null) {
            visible = false
        } else if (persistent) {
            visible = true
        } else {
            visible = true
            delay(5_000)
            visible = false
        }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(1_400), label = "caption")
    Box(Modifier.height(36.dp).padding(horizontal = 32.dp).alpha(alpha), contentAlignment = Alignment.Center) {
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.titleLarge.copy(shadow = Shadow(Color(0x8C1E3246), blurRadius = 12f)),
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PauseButton(onPause: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Glass)
            .clickable(role = Role.Button, onClickLabel = "Pause focus", onClick = onPause)
            .semantics { contentDescription = "Pause" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(SheepIcons.Pause, contentDescription = null, tint = GlassInk, modifier = Modifier.size(22.dp))
    }
}

/** The calm card used over the scene for pause, end and recovery. */
@Composable
fun GlassCard(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(horizontal = 32.dp)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(GlassSolid)
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = GlassInk, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = GlassMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(ButtonGreen)
                .clickable(role = Role.Button, onClick = primary.second),
            contentAlignment = Alignment.Center,
        ) {
            Text(primary.first, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        TextButton(onClick = secondary.second) {
            Text(secondary.first, style = MaterialTheme.typography.labelLarge, color = GlassMuted)
        }
    }
}

@Composable
private fun BlockingWarning(onFix: () -> Unit) {
    Box(Modifier.padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(Glass)) {
        TextButton(onClick = onFix) {
            Text(
                "App blocking is off. Your timer is safe. Tap to restore.",
                style = MaterialTheme.typography.labelMedium,
                color = GlassInk,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Completed or ended
// ---------------------------------------------------------------------------------------------

@Composable
fun CompletionScreen(
    outcome: SessionOutcome,
    story: FocusStory,
    closing: Closing?,
    onSaveReflection: (intentionDone: Boolean?, reflection: String) -> Unit,
    onStartAnother: () -> Unit,
    onTakeBreak: () -> Unit,
) {
    val reducedMotion = rememberReducedMotion()
    val frame = rememberSceneClock(running = true, reducedMotion = reducedMotion)
    val celebration: () -> Float = {
        frame.value
        if (reducedMotion) 60f else ((System.currentTimeMillis() - outcome.endedAt) / 1000f).coerceAtLeast(0f)
    }
    val time: () -> Float = {
        frame.value
        if (reducedMotion) 0f else outcome.plannedMs / 1000f + celebration()
    }

    // Let the scene finish first, then the words, then the summary.
    var step by remember { mutableStateOf(if (reducedMotion) 3 else 0) }
    LaunchedEffect(outcome.sessionId) {
        if (step < 3) {
            delay(1_600); step = 1
            delay(2_000); step = 2
            delay(2_400); step = 3
        }
    }
    val minutes = outcome.focusedMs / 60_000

    val ending = rememberStoryClips(story)?.endingPath
    var videoShowing by remember { mutableStateOf(false) }
    val endingDescription = Modifier.clearAndSetSemantics { contentDescription = story.endingDescription }

    Box(Modifier.fillMaxSize().background(Color(0xFF62AC48))) {
        if (!videoShowing) {
            story.Scene(progress = { 1f }, time = time, celebration = celebration, modifier = Modifier.fillMaxSize().then(endingDescription))
        }
        if (ending != null && !reducedMotion) {
            StoryVideo(ending, fraction = { 0f }, modifier = Modifier.fillMaxSize().then(endingDescription), playThrough = true, onReady = { videoShowing = it })
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Brush.verticalGradient(listOf(Color(0x382A3C50), Color.Transparent))),
        )
        val shadow = Shadow(Color(0x8C28323C), blurRadius = 14f)
        Column(
            Modifier.fillMaxWidth().safeDrawingPadding().padding(top = 72.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(step >= 1, enter = fadeIn(tween(1_400))) {
                Text(story.completionTitle, style = MaterialTheme.typography.headlineMedium.copy(shadow = shadow), color = Color.White, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(step >= 2, enter = fadeIn(tween(1_400))) {
                Text("Well done. You stayed focused.", style = MaterialTheme.typography.bodyLarge.copy(shadow = shadow), color = Color.White, textAlign = TextAlign.Center)
            }
            if (closing != null) {
                Spacer(Modifier.height(18.dp))
                AnimatedVisibility(step >= 2, enter = fadeIn(tween(2_000, delayMillis = 900))) {
                    ClosingCard(closing)
                }
            }
        }
        AnimatedVisibility(
            step >= 3,
            enter = slideInVertically(tween(900)) { it } + fadeIn(tween(600)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .padding(start = 28.dp, end = 28.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
                Spacer(Modifier.height(16.dp))
                Text(if (minutes == 1L) "1 minute focused" else "$minutes minutes focused", style = MaterialTheme.typography.titleLarge)
                Text("Session completed ✓", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                ReflectionForm(outcome, onSaveReflection)
                Spacer(Modifier.height(16.dp))
                PrimaryPill("Start Another Session", onClick = onStartAnother, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                SecondaryPill("Take a Break", onClick = onTakeBreak, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** The closing prayer or verse, over the warm ending scene. */
@Composable
private fun ClosingCard(closing: Closing) {
    Column(
        Modifier
            .widthIn(max = 420.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (closing.reference == null) closing.text else "“${closing.text}”",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = GlassInk,
            textAlign = TextAlign.Center,
        )
        if (closing.reference != null) {
            Spacer(Modifier.height(4.dp))
            Text(closing.reference.uppercase(), style = MaterialTheme.typography.labelSmall, color = GlassMuted)
        }
    }
}

/** "Did you finish it?" and a short note for the journal. Both are optional. */
@Composable
private fun ReflectionForm(outcome: SessionOutcome, onSave: (Boolean?, String) -> Unit) {
    var done by rememberSaveable(outcome.sessionId) { mutableStateOf<Boolean?>(null) }
    var note by rememberSaveable(outcome.sessionId) { mutableStateOf("") }
    var saved by rememberSaveable(outcome.sessionId) { mutableStateOf(false) }
    val muted = LocalSheepColors.current.muted

    if (saved) {
        Text("Saved to your journal", style = MaterialTheme.typography.labelMedium, color = muted)
        return
    }
    if (outcome.intention.isNotEmpty()) {
        Text("Did you finish “${outcome.intention}”?", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DurationChip("Yes", selected = done == true) { done = true }
            DurationChip("Not yet", selected = done == false) { done = false }
        }
        Spacer(Modifier.height(10.dp))
    }
    OutlinedTextField(
        value = note,
        onValueChange = { note = it.take(500) },
        placeholder = { Text("A thought or prayer for your journal (optional)") },
        shape = RoundedCornerShape(16.dp),
        maxLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    if (done != null || note.isNotBlank()) {
        TextButton(onClick = {
            onSave(done, note)
            saved = true
        }) { Text("Save to journal") }
    }
}

@Composable
fun EndedScreen(outcome: SessionOutcome, closing: Closing?, onStartNew: () -> Unit) {
    val minutes = outcome.focusedMs / 60_000
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Session ended", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            if (minutes > 0) "You focused for $minutes ${if (minutes == 1L) "minute" else "minutes"}. Every minute counts." else "Rest, and begin again when you are ready.",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalSheepColors.current.muted,
            textAlign = TextAlign.Center,
        )
        if (closing != null) {
            Spacer(Modifier.height(24.dp))
            Text(
                if (closing.reference == null) closing.text else "“${closing.text}”",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                textAlign = TextAlign.Center,
            )
            closing.reference?.let { Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalSheepColors.current.muted) }
        }
        Spacer(Modifier.height(36.dp))
        PrimaryPill("Start New Session", onClick = onStartNew)
    }
}

@Composable
fun BreakScreen(endsAtWall: Long, onEnd: () -> Unit, onBegin: () -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAtWall) {
        while (now < endsAtWall) {
            delay(500)
            now = System.currentTimeMillis()
        }
    }
    val remaining = endsAtWall - now
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (remaining > 0) {
            Text("Rest a moment.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text(formatClock(remaining), style = MaterialTheme.typography.displayMedium)
            Text("BREAK", style = MaterialTheme.typography.labelSmall, color = LocalSheepColors.current.muted)
            Spacer(Modifier.height(36.dp))
            SecondaryPill("End Break", onClick = onEnd)
        } else {
            Text("Ready when you are.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(36.dp))
            PrimaryPill("Begin Focus", onClick = onBegin)
            TextButton(onClick = onEnd) { Text("Not yet", color = LocalSheepColors.current.muted) }
        }
    }
}
