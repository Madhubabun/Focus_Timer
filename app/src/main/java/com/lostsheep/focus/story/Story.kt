package com.lostsheep.focus.story

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** One chapter of a focus story, placed by session progress (0..1), never by elapsed frames. */
data class StoryPhase(
    val key: String,
    val start: Float,
    val end: Float,
    val title: String,
    /** A short, quiet line shown under the timer, or null for none. */
    val caption: String?,
    /** Persistent captions stay while the phase lasts; others fade away after a few seconds. */
    val captionPersistent: Boolean,
    /** Spoken by screen readers for the illustration. */
    val description: String,
)

/**
 * A visual focus journey. New biblical stories (David & Goliath, Noah, Daniel, the Wilderness)
 * plug in by implementing this and adding themselves to [Stories.all].
 */
interface FocusStory {
    val id: String
    val title: String
    val reference: String
    val completionTitle: String
    val phases: List<StoryPhase>

    /** Spoken by screen readers for the ending scene. */
    val endingDescription: String get() = "The shepherd returns to the flock with the lost sheep, in warm light."

    fun phaseAt(progress: Float): StoryPhase =
        phases.lastOrNull { progress >= it.start } ?: phases.first()

    /** The named story state at [progress], for screen readers and debugging (e.g. scene_042_...). */
    fun stateNameAt(progress: Float): String = phaseAt(progress).key

    /**
     * Draws the scene. Values are read through lambdas inside the draw phase, so the scene redraws
     * every frame without recomposing the screen around it.
     *
     * @param progress session progress 0..1
     * @param time ambient animation clock in seconds (frozen while paused)
     * @param celebration seconds since completion, or a negative value when not celebrating
     */
    @Composable
    fun Scene(
        progress: () -> Float,
        time: () -> Float,
        celebration: () -> Float,
        modifier: Modifier,
    )
}

object Stories {
    val all: List<FocusStory> = listOf(LostSheepStory, DavidStory, NoahStory)

    /** Shown in Settings so people know what is coming. */
    val comingSoon = listOf(
        "Daniel" to "Daniel 6",
        "Jesus in the Wilderness" to "Matthew 4",
    )

    fun byId(id: String): FocusStory = all.firstOrNull { it.id == id } ?: LostSheepStory
}
