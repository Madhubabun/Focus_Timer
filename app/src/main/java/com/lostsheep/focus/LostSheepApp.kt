package com.lostsheep.focus

import android.app.Application
import android.content.Context
import com.lostsheep.focus.data.BlockedAppEntity
import com.lostsheep.focus.data.FocusSessionEntity
import com.lostsheep.focus.data.LostSheepDatabase
import com.lostsheep.focus.data.SettingsStore
import com.lostsheep.focus.session.AndroidTimeSource
import com.lostsheep.focus.session.SessionManager
import com.lostsheep.focus.session.SessionNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Hand-wired dependencies; everything lives on the device. */
class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = LostSheepDatabase.create(context)
    val settings = SettingsStore(context)
    val clock = AndroidTimeSource(context)
    val sessionManager = SessionManager(context, db, settings, clock, scope)

    val blockedApps: StateFlow<List<BlockedAppEntity>> =
        db.blockedApps().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<FocusSessionEntity>> =
        db.sessions().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun setBlocked(packageName: String, label: String, blocked: Boolean) {
        scope.launch {
            if (blocked) db.blockedApps().insert(BlockedAppEntity(packageName, label))
            else db.blockedApps().delete(packageName)
        }
    }
}

class LostSheepApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        SessionNotifications.ensureChannels(this)
    }
}
