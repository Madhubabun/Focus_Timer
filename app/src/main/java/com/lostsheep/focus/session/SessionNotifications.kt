package com.lostsheep.focus.session

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lostsheep.focus.MainActivity
import com.lostsheep.focus.R
import com.lostsheep.focus.blocking.BlockedActivity

/** Notifications show only the timer state; never app names or anything personal. */
object SessionNotifications {
    const val CHANNEL_SESSION = "focus_session"
    const val CHANNEL_ALERTS = "session_alerts"
    const val CHANNEL_REMINDERS = "daily_reminder"
    const val ID_SESSION = 1
    const val ID_COMPLETED = 2
    const val ID_BLOCKING = 3
    const val ID_BLOCKED_APP = 4
    const val ID_REMINDER = 5
    const val ID_SCHEDULED = 6

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SESSION, context.getString(R.string.channel_session), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
                // The app plays its own gentle chime when sound is on.
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun openApp(context: Context, requestCode: Int, extra: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (extra != null) putExtra(extra, true)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun serviceAction(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            Intent(context, FocusTimerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun buildSession(context: Context, session: ActiveSession?, clock: TimeSource, showTimer: Boolean): Notification {
        val b = NotificationCompat.Builder(context, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context, 10, MainActivity.EXTRA_FROM_NOTIFICATION))

        if (session == null || !showTimer) {
            return b.setContentTitle("Focus Session").setContentText("Stay focused.").build()
        }
        val remaining = session.remainingMs(clock)
        if (session.paused) {
            b.setContentTitle("Focus Paused")
                .setContentText("${formatClock(remaining)} remaining")
                .setShowWhen(false)
                .addAction(0, "Resume", serviceAction(context, FocusTimerService.ACTION_RESUME, 11))
        } else {
            // The system counts this down by itself, so we do not need to repost every second.
            b.setContentTitle("Focus Session")
                .setContentText("remaining")
                .setWhen(System.currentTimeMillis() + remaining)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, "Pause", serviceAction(context, FocusTimerService.ACTION_PAUSE, 12))
                .addAction(0, "End", openApp(context, 13, MainActivity.EXTRA_CONFIRM_END))
        }
        return b.build()
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun post(context: Context, id: Int, n: Notification) {
        if (!canPost(context)) return
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    fun showCompleted(context: Context, outcome: SessionOutcome, title: String) {
        ensureChannels(context)
        val minutes = outcome.focusedMs / 60_000
        post(
            context,
            ID_COMPLETED,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText("Well done. $minutes minutes focused.")
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openApp(context, 20, null))
                .build(),
        )
    }

    fun showBlockingNeedsRestore(context: Context) {
        ensureChannels(context)
        post(
            context,
            ID_BLOCKING,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("App blocking is paused")
                .setContentText("Your focus timer is still running. Turn app blocking back on in Settings.")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Your focus timer is still running. App blocking needs the Accessibility permission to be turned back on."))
                .setAutoCancel(true)
                .setContentIntent(openApp(context, 30, MainActivity.EXTRA_OPEN_SETTINGS))
                .build(),
        )
    }

    /** Fallback when only Usage Access is available and Android does not let us open a screen. */
    fun showFlockCanWait(context: Context) {
        ensureChannels(context)
        val intent = Intent(context, BlockedActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        post(
            context,
            ID_BLOCKED_APP,
            NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("The flock can wait.")
                .setContentText("You are in a focus session. Tap to return.")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(context, 40, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .build(),
        )
    }

    fun cancelSessionAlerts(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_BLOCKING)
        NotificationManagerCompat.from(context).cancel(ID_BLOCKED_APP)
    }
}
