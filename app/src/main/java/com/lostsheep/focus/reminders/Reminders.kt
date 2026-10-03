package com.lostsheep.focus.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lostsheep.focus.LostSheepApp
import com.lostsheep.focus.MainActivity
import com.lostsheep.focus.R
import com.lostsheep.focus.data.AppSettings
import com.lostsheep.focus.data.FocusSchedule
import com.lostsheep.focus.session.SessionNotifications
import com.lostsheep.focus.story.Verses
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.launch

/**
 * The daily reminder and scheduled focus sessions. Each keeps one alarm for its next time and
 * sets the following one when it fires, so changing the clock or the settings never stacks alarms.
 */
object Reminders {
    private const val REQ_REMINDER = 40
    private const val REQ_SCHEDULE = 41
    const val EXTRA_SCHEDULE_ID = "schedule_id"

    /** The next reminder strictly after [now]. */
    fun nextReminder(minuteOfDay: Int, now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(now.zone)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Re-arms both alarms from the current settings. Safe to call any time. */
    fun sync(context: Context, settings: AppSettings) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now()

        val reminder = pending(context, REQ_REMINDER, Intent(context, ReminderReceiver::class.java))
        am.cancel(reminder)
        if (settings.reminderOn) {
            val at = nextReminder(settings.reminderMinute, now).toInstant().toEpochMilli()
            // A reminder may arrive a few minutes late; that keeps it gentle on the battery.
            runCatching { am.setWindow(AlarmManager.RTC_WAKEUP, at, 10 * 60_000L, reminder) }
        }

        val next = FocusSchedule.next(settings.schedules, now)
        val intent = Intent(context, ScheduledFocusReceiver::class.java)
        am.cancel(pending(context, REQ_SCHEDULE, intent))
        if (next != null) {
            val (schedule, time) = next
            val pi = pending(context, REQ_SCHEDULE, intent.putExtra(EXTRA_SCHEDULE_ID, schedule.id))
            val at = time.toInstant().toEpochMilli()
            runCatching {
                if (canUseExactAlarms(context)) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                } else {
                    am.setWindow(AlarmManager.RTC_WAKEUP, at, 60_000L, pi)
                }
            }
        }
    }

    /** Exact timing lets a scheduled session start on the minute and show its timer straight away. */
    fun canUseExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }

    private fun pending(context: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    internal fun openApp(context: Context, code: Int): PendingIntent = PendingIntent.getActivity(
        context,
        code,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    internal fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
}

/** A gentle nudge with a verse, skipped on days a session is already done. */
class ReminderReceiver : BroadcastReceiver() {
    @Suppress("MissingPermission") // checked in canNotify
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as LostSheepApp).container
        val pending = goAsync()
        container.scope.launch {
            try {
                val settings = container.settings.settings.value
                val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val doneToday = container.db.sessions().completedSince(startOfDay) > 0
                if (settings.reminderOn && !doneToday && !container.sessionManager.hasActiveSession && Reminders.canNotify(context)) {
                    val verse = Verses.pick(Verses.focus, LocalDate.now().toString())
                    val n = NotificationCompat.Builder(context, SessionNotifications.CHANNEL_REMINDERS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Time to focus")
                        .setContentText("“${verse.text}” ${verse.reference}")
                        .setStyle(NotificationCompat.BigTextStyle().bigText("“${verse.text}”\n${verse.reference}"))
                        .setContentIntent(Reminders.openApp(context, 62))
                        .setAutoCancel(true)
                        .build()
                    NotificationManagerCompat.from(context).notify(SessionNotifications.ID_REMINDER, n)
                }
                Reminders.sync(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Starts a scheduled session, unless one is already running, then arms the next one. */
class ScheduledFocusReceiver : BroadcastReceiver() {
    @Suppress("MissingPermission") // checked in canNotify
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as LostSheepApp).container
        val pending = goAsync()
        container.scope.launch {
            try {
                val settings = container.settings.settings.value
                val schedule = settings.schedules.firstOrNull { it.id == intent.getStringExtra(Reminders.EXTRA_SCHEDULE_ID) && it.enabled }
                if (schedule != null && !container.sessionManager.hasActiveSession) {
                    val started = container.startFocus(schedule.durationMin)
                    // Without exact-alarm access Android may not let the timer notification start from
                    // the background; this one tells the person their session has begun.
                    if (started && !container.sessionManager.serviceRunning && Reminders.canNotify(context)) {
                        val n = NotificationCompat.Builder(context, SessionNotifications.CHANNEL_ALERTS)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle("Scheduled focus has begun")
                            .setContentText("${schedule.durationMin} minutes. Tap to open.")
                            .setContentIntent(Reminders.openApp(context, 63))
                            .setAutoCancel(true)
                            .build()
                        NotificationManagerCompat.from(context).notify(SessionNotifications.ID_SCHEDULED, n)
                    }
                }
                Reminders.sync(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}
