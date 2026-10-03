package com.lostsheep.focus

import com.lostsheep.focus.story.DavidChoreography
import com.lostsheep.focus.story.DavidStory
import com.lostsheep.focus.story.LostSheepStory
import com.lostsheep.focus.story.NoahChoreography
import com.lostsheep.focus.story.NoahStory
import com.lostsheep.focus.story.Stories
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoreStoriesTest {

    @Test
    fun everyStoryCoversTheWholeSessionWithoutGaps() {
        assertEquals(listOf("lost-sheep", "david-goliath", "noah"), Stories.all.map { it.id })
        for (story in Stories.all) {
            val phases = story.phases
            assertEquals(0f, phases.first().start, 0f)
            assertEquals(1f, phases.last().end, 0f)
            phases.zipWithNext().forEach { (a, b) -> assertEquals("${story.id} ${a.key}", a.end, b.start, 0f) }
            assertTrue(story.endingDescription.isNotBlank())
        }
        assertEquals(DavidStory, Stories.byId("david-goliath"))
        assertEquals(LostSheepStory, Stories.byId("no-such-story"))
    }

    @Test
    fun davidMovesSmoothlySoPauseAndResumeNeverJump() {
        var prev = DavidChoreography.at(0f)
        var p = 0.0005f
        while (p <= 1f) {
            val s = DavidChoreography.at(p)
            assertTrue("david x at $p", abs(s.davidX - prev.davidX) * s.zoom < 0.02f)
            assertTrue("goliath x at $p", abs(s.goliathX - prev.goliathX) * s.zoom < 0.02f)
            assertTrue("camera at $p", abs(s.cameraX - prev.cameraX) * s.zoom < 0.02f)
            assertTrue("zoom at $p", abs(s.zoom - prev.zoom) < 0.02f)
            assertEquals(s, DavidChoreography.at(p)) // the same progress always draws the same frame
            prev = s
            p += 0.0005f
        }
    }

    @Test
    fun davidChoosesFiveStonesAndOneIsEnough() {
        assertEquals(0, DavidChoreography.at(0.30f).stones)
        assertTrue(DavidChoreography.at(0.45f).kneel > 0.9f)
        assertEquals(5, DavidChoreography.at(0.54f).stones)
        assertEquals(0f, DavidChoreography.at(0.80f).goliathFall, 0f)
        assertEquals(1f, DavidChoreography.at(0.95f).goliathFall, 0f)
        assertTrue(DavidChoreography.at(0.99f).cheer > 0.9f)
        assertTrue(DavidChoreography.at(0.7f).davidX < DavidChoreography.at(0.7f).goliathX)
    }

    @Test
    fun noahMovesSmoothlyAndTheStoryHappensInOrder() {
        var prev = NoahChoreography.at(0f)
        var p = 0.0005f
        while (p <= 1f) {
            val s = NoahChoreography.at(p)
            assertTrue("ark at $p", abs(s.arkX - prev.arkX) * s.zoom < 0.02f)
            assertTrue("camera at $p", abs(s.cameraX - prev.cameraX) * s.zoom < 0.02f)
            assertTrue("water at $p", abs(s.waterY - prev.waterY) < 0.01f)
            assertTrue("noah at $p", abs(s.noahX - prev.noahX) * s.zoom < 0.02f)
            assertEquals(s, NoahChoreography.at(p))
            prev = s
            p += 0.0005f
        }
        assertTrue(NoahChoreography.at(0.10f).build in 0.2f..0.6f)
        assertEquals(1f, NoahChoreography.at(0.30f).build, 0f)
        assertEquals(0f, NoahChoreography.at(0.40f).rain, 0f)
        assertTrue(NoahChoreography.at(0.60f).rain > 0.9f)
        val afloat = NoahChoreography.at(0.70f)
        assertTrue(NoahChoreography.keelY(afloat) < NoahChoreography.groundY(afloat.arkX)) // floating
        val resting = NoahChoreography.at(1f)
        assertEquals(NoahChoreography.groundY(resting.arkX), NoahChoreography.keelY(resting), 0.0001f) // on the mountain
        assertTrue(resting.rainbow > 0.9f)
    }

    @Test
    fun beatNamesAreReadable() {
        assertEquals("scene_01_with_the_sheep", DavidStory.stateNameAt(0f))
        assertEquals("scene_15_whirls_the_sling", DavidStory.stateNameAt(0.81f))
        assertEquals("scene_01_lays_the_keel", NoahStory.stateNameAt(0f))
        assertTrue(NoahStory.stateNameAt(1f).endsWith("the_rainbow"))
    }
}
