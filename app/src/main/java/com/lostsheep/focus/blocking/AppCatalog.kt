package com.lostsheep.focus.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isGame: Boolean,
    val isSuggested: Boolean,
    val icon: Bitmap?,
)

object AppCatalog {
    /** Common distractions, shown first when installed. */
    val suggestedPackages = linkedMapOf(
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube",
        "com.whatsapp" to "WhatsApp",
        "com.facebook.katana" to "Facebook",
        "com.reddit.frontpage" to "Reddit",
        "com.twitter.android" to "X",
        "com.netflix.mediaclient" to "Netflix",
        "com.android.chrome" to "Chrome",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.snapchat.android" to "Snapchat",
    )

    /** Launchable apps on this device. Call off the main thread. */
    fun load(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val iconPx = (40 * context.resources.displayMetrics.density).toInt()
        return pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    isGame = info.category == ApplicationInfo.CATEGORY_GAME,
                    isSuggested = info.packageName in suggestedPackages,
                    icon = runCatching { pm.getApplicationIcon(info).toBitmap(iconPx, iconPx) }.getOrNull(),
                )
            }
            .sortedWith(compareByDescending<InstalledApp> { it.isSuggested }.thenBy { it.label.lowercase() })
    }
}
