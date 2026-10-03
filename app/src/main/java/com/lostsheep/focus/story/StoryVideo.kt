package com.lostsheep.focus.story

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

/**
 * Optional painted video for a story: one short clip per stage, bundled in the APK at
 * `assets/stories/<storyId>/<phaseKey>.mp4`, plus an optional `complete.mp4` for the ending.
 *
 * The clips never run on their own clock. Each one is stretched across its stage and stepped to
 * the frame that matches the session's progress, so pausing freezes the picture and resuming,
 * or reopening the app an hour later, shows exactly the right moment.
 */
class StoryClips(private val storyId: String, private val phaseKeys: Set<String>, private val ending: Boolean) {
    fun pathFor(phaseKey: String): String? = if (phaseKey in phaseKeys) "stories/$storyId/$phaseKey.mp4" else null
    val endingPath: String? get() = if (ending) "stories/$storyId/complete.mp4" else null

    companion object {
        /** Returns null unless every stage has a clip, in which case the drawn scene is used instead. */
        fun find(context: Context, story: FocusStory): StoryClips? {
            val files = runCatching { context.assets.list("stories/${story.id}")?.toSet() }.getOrNull() ?: return null
            val keys = story.phases.map { it.key }
            if (keys.any { "$it.mp4" !in files }) return null
            return StoryClips(story.id, keys.toSet(), "complete.mp4" in files)
        }
    }
}

/** How far through its own stage the session is, 0..1. */
fun FocusStory.stageFraction(progress: Float): Float {
    val ph = phaseAt(progress)
    val span = ph.end - ph.start
    return if (span <= 0f) 0f else ((progress - ph.start) / span).coerceIn(0f, 1f)
}

@Composable
fun rememberStoryClips(story: FocusStory): StoryClips? {
    val context = LocalContext.current
    return remember(story.id) { StoryClips.find(context, story) }
}

/**
 * Shows [clipPath] cropped to fill, at the frame for [fraction] (0..1 through the clip).
 * With [playThrough] the clip simply plays once at normal speed (the ending).
 * [onReady] reports when the first frame is on screen, so the drawn scene can stop rendering.
 */
@Composable
fun StoryVideo(
    clipPath: String,
    fraction: () -> Float,
    modifier: Modifier = Modifier,
    playThrough: Boolean = false,
    onReady: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val player = remember { MediaPlayer() }
    var surface by remember { mutableStateOf<Surface?>(null) }
    var prepared by remember { mutableStateOf<String?>(null) }
    var shown by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf<TextureView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.release() }
            surface?.release()
        }
    }

    // Load the clip for this stage whenever the stage changes.
    LaunchedEffect(clipPath, surface) {
        val s = surface ?: return@LaunchedEffect
        shown = false
        onReady(false)
        prepared = null
        val ok = runCatching {
            player.reset()
            context.assets.openFd(clipPath).use { fd -> player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            player.setSurface(s)
            player.setVolume(0f, 0f) // the app's own ambient sound plays instead
            player.isLooping = false
            player.setOnVideoSizeChangedListener { _, w, h -> view?.let { cropToFill(it, w, h) } }
        }.isSuccess && withContext(Dispatchers.IO) { runCatching { player.prepare() }.isSuccess }
        if (!ok) return@LaunchedEffect
        prepared = clipPath
        if (playThrough) {
            player.start()
        } else {
            player.seekTo(frameAt(player, fraction()), MediaPlayer.SEEK_CLOSEST)
        }
        delay(120)
        shown = true
        onReady(true)
    }

    // Step to the right frame a few times a second. Never plays on its own.
    LaunchedEffect(clipPath, prepared) {
        if (prepared != clipPath || playThrough) return@LaunchedEffect
        var last = -1L
        while (true) {
            val target = frameAt(player, fraction())
            if (abs(target - last) >= 40L) {
                runCatching { player.seekTo(target, MediaPlayer.SEEK_CLOSEST) }
                last = target
            }
            delay(200)
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                view = this
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        surface = Surface(st)
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        if (player.videoWidth > 0) cropToFill(this@apply, player.videoWidth, player.videoHeight)
                    }
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        runCatching { player.setSurface(null) }
                        surface?.release()
                        surface = null
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                }
            }
        },
        modifier = modifier.alpha(if (shown) 1f else 0f),
    )
}

private fun frameAt(player: MediaPlayer, fraction: Float): Long {
    val duration = runCatching { player.duration }.getOrDefault(0)
    if (duration <= 0) return 0L
    return (fraction.coerceIn(0f, 1f) * max(0, duration - 50)).toLong()
}

/** Scales the video to cover the whole view, cropping the edges, like centerCrop. */
private fun cropToFill(view: TextureView, videoW: Int, videoH: Int) {
    val vw = view.width.toFloat()
    val vh = view.height.toFloat()
    if (vw <= 0f || vh <= 0f || videoW <= 0 || videoH <= 0) return
    val scale = max(vw / videoW, vh / videoH)
    val sx = videoW * scale / vw
    val sy = videoH * scale / vh
    view.setTransform(Matrix().apply { setScale(sx, sy, vw / 2f, vh / 2f) })
}
