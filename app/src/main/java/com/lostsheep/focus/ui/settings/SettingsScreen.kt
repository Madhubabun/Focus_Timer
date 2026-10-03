package com.lostsheep.focus.ui.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lostsheep.focus.blocking.BlockingPermissions
import com.lostsheep.focus.blocking.InstalledApp
import com.lostsheep.focus.data.AppSettings
import com.lostsheep.focus.data.BlockedAppEntity
import com.lostsheep.focus.story.Stories
import com.lostsheep.focus.story.Verses
import com.lostsheep.focus.ui.components.FullWidthDivider
import com.lostsheep.focus.ui.components.SheepIcons
import com.lostsheep.focus.ui.focus.PermissionCopy
import com.lostsheep.focus.ui.focus.PermissionExplainDialog
import com.lostsheep.focus.ui.theme.LocalSheepColors

data class PermissionStatus(val accessibility: Boolean, val usageAccess: Boolean, val notifications: Boolean) {
    val blockingReady: Boolean get() = accessibility || usageAccess
}

private fun readStatus(context: Context) = PermissionStatus(
    accessibility = BlockingPermissions.isAccessibilityEnabled(context),
    usageAccess = BlockingPermissions.hasUsageAccess(context),
    notifications = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
)

/** Re-checked every time the app comes back, e.g. from system settings. */
@Composable
fun rememberPermissionStatus(): PermissionStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readStatus(context)) }
    LifecycleResumeEffect(Unit) {
        status = readStatus(context)
        onPauseOrDispose { }
    }
    return status
}

fun openSafely(context: Context, intent: android.content.Intent, fallback: android.content.Intent? = null) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        if (fallback != null) runCatching { context.startActivity(fallback) }
    }
}

/** Asks with an explanation first, then opens the right system screen. */
@Composable
fun PermissionRequests(
    askAccessibility: Boolean,
    askUsage: Boolean,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    if (askAccessibility) {
        PermissionExplainDialog(PermissionCopy.ACCESSIBILITY_TITLE, PermissionCopy.ACCESSIBILITY_BODY, onDismiss = onDone) {
            onDone()
            openSafely(context, BlockingPermissions.accessibilitySettingsIntent())
        }
    }
    if (askUsage) {
        PermissionExplainDialog(PermissionCopy.USAGE_TITLE, PermissionCopy.USAGE_BODY, onDismiss = onDone) {
            onDone()
            openSafely(context, BlockingPermissions.usageAccessSettingsIntent(context), BlockingPermissions.usageAccessSettingsFallbackIntent())
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    blockedApps: List<BlockedAppEntity>,
    sessionActive: Boolean,
    onDuration: (Int) -> Unit,
    onDailyGoal: (Int) -> Unit,
    onSound: (Boolean) -> Unit,
    onTimerNotification: (Boolean) -> Unit,
    onVerse: (String) -> Unit,
    onChooseApps: () -> Unit,
) {
    val context = LocalContext.current
    val status = rememberPermissionStatus()
    val muted = LocalSheepColors.current.muted
    var askAccessibility by remember { mutableStateOf(false) }
    var askUsage by remember { mutableStateOf(false) }
    var showVerses by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(20.dp))

        Section("Focus")
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 25, 45, 60).forEach { m ->
                val selected = settings.defaultDurationMin == m
                TextButton(onClick = { onDuration(m) }) {
                    Text("$m min", color = if (selected) MaterialTheme.colorScheme.primary else muted)
                }
            }
        }
        Stepper("Daily goal", "${settings.dailyGoalSessions} sessions", onMinus = { onDailyGoal(settings.dailyGoalSessions - 1) }, onPlus = { onDailyGoal(settings.dailyGoalSessions + 1) })
        val story = Stories.byId(settings.storyId)
        SettingRow("Focus story", "${story.title} · ${story.reference}") {}
        Text(
            "Coming later: " + Stories.comingSoon.joinToString { it.first },
            style = MaterialTheme.typography.labelMedium,
            color = muted,
        )
        Spacer(Modifier.height(16.dp))
        FullWidthDivider()

        Section("Blocked apps")
        SettingRow(
            "Apps to set aside",
            if (blockedApps.isEmpty()) "None chosen" else blockedApps.joinToString { it.label },
            onClick = onChooseApps,
        )
        if (sessionActive) {
            Text("Changes apply from your next session.", style = MaterialTheme.typography.labelMedium, color = muted)
        }
        SettingRow(
            "Accessibility",
            if (status.accessibility) "On · apps are blocked during focus" else "Off · needed to block apps",
            onClick = { askAccessibility = true },
        )
        SettingRow(
            "Usage Access",
            if (status.usageAccess) "On · used as a fallback" else "Off · optional fallback",
            onClick = { askUsage = true },
        )
        Spacer(Modifier.height(16.dp))
        FullWidthDivider()

        Section("Sound & notifications")
        ToggleRow("Sound", "Soft wind, birds and a gentle chime at the end", settings.soundOn, onSound)
        ToggleRow("Timer in notification", "Show time left and Pause / Resume while you focus", settings.showTimerNotification, onTimerNotification)
        if (!status.notifications) {
            SettingRow("Notifications are off", "Tap to allow them in system settings") {
                openSafely(context, BlockingPermissions.appNotificationSettingsIntent(context))
            }
        }
        Spacer(Modifier.height(16.dp))
        FullWidthDivider()

        Section("Reflection")
        SettingRow("Bible verse", Verses.byId(settings.verseId).reference) { showVerses = !showVerses }
        if (showVerses) {
            Verses.all.forEach { v ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.RadioButton) { onVerse(v.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = v.id == settings.verseId, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(v.reference, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FullWidthDivider()

        Section("Privacy")
        Text(
            "The Lost Sheep has no account, no server, no analytics and no cloud sync. Your sessions, statistics, " +
                "blocked apps and preferences are stored only on this phone and are not backed up off the device.\n\n" +
                "Accessibility is used only to notice when a blocked app opens during a session you started. " +
                "Usage Access, if you allow it, is a fallback for the same purpose. Neither reads your screen or messages.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
        Spacer(Modifier.height(32.dp))
    }

    PermissionRequests(askAccessibility, askUsage) {
        askAccessibility = false
        askUsage = false
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = LocalSheepColors.current.muted, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = LocalSheepColors.current.muted)
    }
}

@Composable
private fun ToggleRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = LocalSheepColors.current.muted)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Stepper(title: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = LocalSheepColors.current.muted)
        }
        TextButton(onClick = onMinus) { Text("–", style = MaterialTheme.typography.titleLarge) }
        TextButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

// ---------------------------------------------------------------------------------------------
// Choosing apps to block
// ---------------------------------------------------------------------------------------------

@Composable
fun AppPickerScreen(
    apps: List<InstalledApp>?,
    blocked: Set<String>,
    onToggle: (InstalledApp, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val status = rememberPermissionStatus()
    var query by remember { mutableStateOf("") }
    var askAccessibility by remember { mutableStateOf(false) }
    val muted = LocalSheepColors.current.muted

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(SheepIcons.Back, contentDescription = "Back") }
            Text("Apps to set aside", style = MaterialTheme.typography.titleLarge)
        }
        if (!status.blockingReady) {
            TextButton(onClick = { askAccessibility = true }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text("Blocking needs the Accessibility permission. Tap to set up.", color = MaterialTheme.colorScheme.tertiary)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (apps == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        val games = apps.filter { it.isGame }
        val filtered = apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
        LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            if (games.isNotEmpty() && query.isBlank()) {
                item {
                    val allGames = games.all { it.packageName in blocked }
                    AppRow(label = "All games (${games.size})", detail = "Every app marked as a game", icon = null, checked = allGames) { on ->
                        games.forEach { onToggle(it, on) }
                    }
                    FullWidthDivider()
                }
            }
            items(filtered, key = { it.packageName }) { app ->
                AppRow(
                    label = app.label,
                    detail = when {
                        app.isSuggested -> "Often distracting"
                        app.isGame -> "Game"
                        else -> null
                    },
                    icon = app,
                    checked = app.packageName in blocked,
                ) { on -> onToggle(app, on) }
            }
            item { Spacer(Modifier.height(24.dp)); Text("Only apps you choose are blocked, and only during a focus session.", style = MaterialTheme.typography.labelMedium, color = muted) }
        }
    }

    PermissionRequests(askAccessibility, askUsage = false) { askAccessibility = false }
}

@Composable
private fun AppRow(label: String, detail: String?, icon: InstalledApp?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bmp = icon?.icon
        if (bmp != null) {
            val image = remember(bmp) { bmp.asImageBitmap() }
            Image(image, contentDescription = null, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelMedium, color = LocalSheepColors.current.muted)
        }
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
