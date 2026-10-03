package com.lostsheep.focus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lostsheep.focus.LostSheepApp
import com.lostsheep.focus.MainActivity
import com.lostsheep.focus.R
import com.lostsheep.focus.data.FocusStats
import com.lostsheep.focus.data.FocusStatsCalculator
import com.lostsheep.focus.data.formatDuration
import com.lostsheep.focus.session.formatClock
import kotlinx.coroutines.launch

/** What the home-screen widget says. Kept apart from Android so it can be tested. */
data class WidgetText(val progress: String, val detail: String, val button: String) {
    companion object {
        fun of(stats: FocusStats, goal: Int, remainingMs: Long?, paused: Boolean): WidgetText {
            if (remainingMs != null) {
                return WidgetText(
                    progress = if (paused) "Focus paused" else "Focusing",
                    detail = "${formatClock(remainingMs)} left",
                    button = "Open",
                )
            }
            val done = stats.todayCompleted
            val streak = stats.currentStreakDays
            return WidgetText(
                progress = if (done >= goal) "Goal reached · $done of $goal" else "$done of $goal sessions today",
                detail = buildString {
                    if (streak > 0) append("🔥 $streak day streak · ")
                    append(formatDuration(stats.todayFocusMs)).append(" focused")
                },
                button = "Begin Focus",
            )
        }
    }
}

/** Today's progress toward the daily goal, the streak, and a one-tap start. */
class FocusWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        val container = (context.applicationContext as LostSheepApp).container
        container.scope.launch {
            try {
                val stats = FocusStatsCalculator.compute(container.db.sessions().all())
                val settings = container.settings.settings.value
                val session = container.sessionManager.session.value?.takeIf { it.isActive }
                val text = WidgetText.of(
                    stats,
                    settings.dailyGoalSessions,
                    session?.remainingMs(container.clock),
                    session?.paused == true,
                )
                ids.forEach { manager.updateAppWidget(it, views(context, text, start = session == null)) }
            } finally {
                pending.finish()
            }
        }
    }

    private fun views(context: Context, text: WidgetText, start: Boolean): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_focus).apply {
            setTextViewText(R.id.widget_progress, text.progress)
            setTextViewText(R.id.widget_streak, text.detail)
            setTextViewText(R.id.widget_button, text.button)
            setOnClickPendingIntent(R.id.widget_root, open(context, 60, null))
            setOnClickPendingIntent(R.id.widget_button, open(context, 61, if (start) MainActivity.EXTRA_START_FOCUS else null))
        }

    private fun open(context: Context, requestCode: Int, extra: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (extra != null) putExtra(extra, true) else putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        /** Redraws every placed widget, e.g. after a session starts or ends. */
        fun refresh(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, FocusWidget::class.java))
                if (ids.isEmpty()) return
                context.sendBroadcast(
                    Intent(context, FocusWidget::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }
}
