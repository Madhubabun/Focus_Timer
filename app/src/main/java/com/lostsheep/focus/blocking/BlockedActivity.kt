package com.lostsheep.focus.blocking

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lostsheep.focus.LostSheepApp
import com.lostsheep.focus.MainActivity
import com.lostsheep.focus.session.formatClock
import com.lostsheep.focus.story.Stories
import com.lostsheep.focus.ui.components.PrimaryPill
import com.lostsheep.focus.ui.theme.LocalSheepColors
import com.lostsheep.focus.ui.theme.LostSheepTheme
import kotlinx.coroutines.delay

/** "The flock can wait." A gentle pause, not a punishment. */
class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val manager = (application as LostSheepApp).container.sessionManager

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = returnToFocus()
        })

        setContent {
            LostSheepTheme {
                val session by manager.session.collectAsStateWithLifecycle()
                val active = session?.takeIf { it.isActive }
                LaunchedEffect(active == null) { if (active == null) finish() }
                if (active == null) return@LostSheepTheme

                var tick by remember { mutableLongStateOf(0L) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1_000)
                        tick++
                    }
                }
                val remaining = run { tick; active.remainingMs(manager.clock) }
                val story = Stories.byId(active.storyId)

                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("The flock can wait.", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(28.dp))
                    story.Scene(
                        progress = { 0.93f }, // the shepherd carrying the sheep home
                        time = { 0f },
                        celebration = { -1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clearAndSetSemantics { contentDescription = "The Good Shepherd carrying a sheep" },
                    )
                    Spacer(Modifier.height(28.dp))
                    Text("Focus session remaining: ${formatClock(remaining)}", style = MaterialTheme.typography.bodyLarge, color = LocalSheepColors.current.muted)
                    Spacer(Modifier.height(32.dp))
                    PrimaryPill("Return to Focus", onClick = ::returnToFocus, modifier = Modifier.fillMaxWidth(0.8f))
                }
            }
        }
    }

    private fun returnToFocus() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_FROM_NOTIFICATION, true),
        )
        finish()
    }
}
