package com.lostsheep.focus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lostsheep.focus.story.Stories
import com.lostsheep.focus.story.Verses
import com.lostsheep.focus.story.closingFor
import com.lostsheep.focus.ui.components.SheepIcons
import com.lostsheep.focus.ui.focus.BreakScreen
import com.lostsheep.focus.ui.focus.CompletionScreen
import com.lostsheep.focus.ui.focus.EndConfirmDialog
import com.lostsheep.focus.ui.focus.EndedScreen
import com.lostsheep.focus.ui.focus.FocusHome
import com.lostsheep.focus.ui.focus.FocusSessionScreen
import com.lostsheep.focus.ui.focus.RecoveryDialog
import com.lostsheep.focus.ui.journey.JourneyScreen
import com.lostsheep.focus.ui.settings.AppPickerScreen
import com.lostsheep.focus.ui.settings.PermissionRequests
import com.lostsheep.focus.ui.settings.SettingsScreen
import com.lostsheep.focus.ui.settings.rememberPermissionStatus
import com.lostsheep.focus.ui.stats.StatsScreen
import com.lostsheep.focus.ui.theme.LocalSheepColors

enum class Tab(val label: String, val icon: ImageVector) {
    Focus("Focus", SheepIcons.Focus),
    Journey("My Journey", SheepIcons.Journey),
    Stats("Stats", SheepIcons.Stats),
    Settings("Settings", SheepIcons.Settings),
}

/** Requests coming from outside Compose (notification taps, the blocked screen). */
class ExternalRequests {
    val confirmEnd = mutableStateOf(false)
    val openSettings = mutableStateOf(false)
    val showRecovery = mutableStateOf(false)
}

@Composable
fun AppRoot(requests: ExternalRequests, vm: FocusViewModel = viewModel()) {
    val session by vm.session.collectAsStateWithLifecycle()
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val blockedApps by vm.blockedApps.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val installedApps by vm.installedApps.collectAsStateWithLifecycle()
    val intention by vm.intentionDraft.collectAsStateWithLifecycle()
    val permissions = rememberPermissionStatus()

    var tab by rememberSaveable { mutableStateOf(Tab.Focus) }
    var pickingApps by rememberSaveable { mutableStateOf(false) }
    var askBlockingPermission by rememberSaveable { mutableStateOf(false) }

    val active = session?.takeIf { it.isActive }
    val story = Stories.byId(active?.storyId ?: settings.storyId)
    val immersive = active != null || outcome != null

    LaunchedEffect(requests.openSettings.value) {
        if (requests.openSettings.value) {
            tab = Tab.Settings
            requests.openSettings.value = false
        }
    }
    LaunchedEffect(immersive) { if (immersive) tab = Tab.Focus }
    BackHandler(enabled = pickingApps) { pickingApps = false }
    BackHandler(enabled = !pickingApps && !immersive && tab != Tab.Focus) { tab = Tab.Focus }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // During a session the story runs edge to edge, under the status bar.
        contentWindowInsets = if (immersive) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (!immersive && !pickingApps) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = LocalSheepColors.current.muted,
                                unselectedTextColor = LocalSheepColors.current.muted,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (pickingApps) {
                LaunchedEffect(Unit) { vm.loadInstalledApps() }
                AppPickerScreen(
                    apps = installedApps,
                    blocked = blockedApps.map { it.packageName }.toSet(),
                    onToggle = { app, on -> vm.setBlocked(app, on) },
                    onBack = { pickingApps = false },
                )
                return@Box
            }
            when (tab) {
                Tab.Focus -> FocusTab(
                    vmState = FocusTabState(active, outcome, settings, stats, blockedApps.size, permissions.blockingReady, intention),
                    vm = vm,
                    story = story,
                    onChooseApps = { pickingApps = true },
                    onFixBlocking = { askBlockingPermission = true },
                    onEndRequest = { requests.confirmEnd.value = true },
                )
                Tab.Journey -> JourneyScreen(stats.totalCompleted, history, Verses.byId(settings.verseId), isSystemInDarkTheme())
                Tab.Stats -> StatsScreen(stats, settings.dailyGoalSessions)
                Tab.Settings -> SettingsScreen(
                    settings = settings,
                    blockedApps = blockedApps,
                    sessionActive = active != null,
                    onDuration = vm::setDefaultDuration,
                    onDailyGoal = vm::setDailyGoal,
                    onSound = vm::setSound,
                    onTimerNotification = vm::setShowTimerNotification,
                    onVerse = vm::setVerse,
                    onVersesWhileFocusing = vm::setVersesWhileFocusing,
                    onClosingWords = vm::setClosingWords,
                    onReminder = vm::setReminder,
                    onSaveSchedule = vm::saveSchedule,
                    onDeleteSchedule = vm::deleteSchedule,
                    onChooseApps = { pickingApps = true },
                )
            }
        }
    }

    // Dialogs that can appear over anything.
    if (active != null && requests.showRecovery.value && !requests.confirmEnd.value) {
        RecoveryDialog(
            session = active,
            clock = vm.clock,
            story = story,
            onResume = {
                requests.showRecovery.value = false
                if (active.paused) vm.resume() else vm.acknowledgeRecovery()
            },
            onEnd = {
                requests.showRecovery.value = false
                requests.confirmEnd.value = true
            },
        )
    }
    if (active != null && requests.confirmEnd.value) {
        EndConfirmDialog(
            session = active,
            clock = vm.clock,
            onKeep = {
                requests.confirmEnd.value = false
                vm.acknowledgeRecovery()
            },
            onEnd = {
                requests.confirmEnd.value = false
                vm.end()
            },
        )
    } else if (active == null && requests.confirmEnd.value) {
        requests.confirmEnd.value = false
    }
    PermissionRequests(askAccessibility = askBlockingPermission, askUsage = false) { askBlockingPermission = false }
}

private data class FocusTabState(
    val session: com.lostsheep.focus.session.ActiveSession?,
    val outcome: com.lostsheep.focus.session.SessionOutcome?,
    val settings: com.lostsheep.focus.data.AppSettings,
    val stats: com.lostsheep.focus.data.FocusStats,
    val blockedCount: Int,
    val blockingReady: Boolean,
    val intention: String,
)

private enum class FocusMode { Idle, Session, Completed, Ended, Break }

@Composable
private fun FocusTab(
    vmState: FocusTabState,
    vm: FocusViewModel,
    story: com.lostsheep.focus.story.FocusStory,
    onChooseApps: () -> Unit,
    onFixBlocking: () -> Unit,
    onEndRequest: () -> Unit,
) {
    val (session, outcome, settings, stats, blockedCount, blockingReady, intention) = vmState
    val onBreak = settings.breakEndsAtWall > 0L
    val mode = when {
        session != null -> FocusMode.Session
        outcome?.completed == true -> FocusMode.Completed
        outcome != null -> FocusMode.Ended
        onBreak -> FocusMode.Break
        else -> FocusMode.Idle
    }
    AnimatedContent(
        targetState = mode,
        transitionSpec = { fadeIn(tween(700)) togetherWith fadeOut(tween(500)) },
        label = "focusMode",
    ) { m ->
        when (m) {
            FocusMode.Session -> if (session != null) {
                FocusSessionScreen(
                    session = session,
                    clock = vm.clock,
                    story = story,
                    blockingWarning = session.selectedBlockedApps.isNotEmpty() && !blockingReady,
                    showVerses = settings.versesWhileFocusing,
                    onPause = vm::pause,
                    onResume = vm::resume,
                    onEndRequest = onEndRequest,
                    onDue = { vm.completeIfDue() },
                    onFixBlocking = onFixBlocking,
                )
            }
            FocusMode.Completed -> if (outcome != null) {
                CompletionScreen(
                    outcome = outcome,
                    story = story,
                    closing = closingFor(settings.closingWords, outcome.sessionId),
                    onSaveReflection = { done, note -> vm.saveReflection(outcome.sessionId, done, note) },
                    onStartAnother = {
                        val minutes = (outcome.plannedMs / 60_000).toInt()
                        vm.dismissOutcome()
                        vm.begin(minutes)
                    },
                    onTakeBreak = vm::startBreak,
                )
            }
            FocusMode.Ended -> if (outcome != null) {
                EndedScreen(outcome, closingFor(settings.closingWords, outcome.sessionId), onStartNew = vm::dismissOutcome)
            }
            FocusMode.Break -> BreakScreen(
                endsAtWall = settings.breakEndsAtWall,
                onEnd = vm::endBreak,
                onBegin = { vm.begin(settings.defaultDurationMin) },
            )
            FocusMode.Idle -> FocusHome(
                settings = settings,
                stats = stats,
                story = story,
                blockedCount = blockedCount,
                blockingReady = blockingReady,
                intention = intention,
                onIntentionChange = vm::setIntentionDraft,
                onBegin = vm::begin,
                onSelectDuration = vm::setDefaultDuration,
                onChooseApps = onChooseApps,
                onFixBlocking = onFixBlocking,
            )
        }
    }
}
