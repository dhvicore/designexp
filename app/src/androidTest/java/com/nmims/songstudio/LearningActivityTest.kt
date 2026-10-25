package com.nmims.songstudio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nmims.songstudio.data.Curriculum
import com.nmims.songstudio.ui.PracticeActivities
import com.nmims.songstudio.ui.PracticeBlank
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningActivityTest {
    @Test fun allTopicsHaveUsablePracticeAndOneCorrectFormula() {
        val data = Curriculum(InstrumentationRegistry.getInstrumentation().targetContext)
        for (topic in data.topics) {
            val lesson = PracticeActivities.lesson(topic)
            assertTrue(topic.title, lesson.blanks.isNotEmpty())
            assertTrue(topic.title, lesson.blanks.all { it.prompt.contains("___") && it.answer.isNotBlank() })
            assertTrue(topic.title, lesson.orderedLines.size >= 2)
            val choices = PracticeActivities.formulaOptions(topic, data.topics)
            assertEquals(topic.title, 4, choices.size)
            assertEquals(4, choices.distinct().size)
            assertEquals(1, choices.count { it == topic.formula })
            assertNotEquals(lesson.orderedLines, PracticeActivities.mixedLines(lesson.orderedLines, topic.id))
        }
    }
    @Test fun rolleAcceptsIndependentConditionChecksInAnySequenceButRequiresConclusionLast() {
        val data = Curriculum(InstrumentationRegistry.getInstrumentation().targetContext)
        val lines = PracticeActivities.lesson(data.topics.first { it.id == 0 }).orderedLines
        assertTrue(PracticeActivities.correctOrder(0, listOf(lines[2], lines[0], lines[1], lines[3]), lines))
        assertFalse(PracticeActivities.correctOrder(0, listOf(lines[3], lines[0], lines[1], lines[2]), lines))
        assertFalse(PracticeActivities.correctOrder(0, listOf(lines[0], lines[0], lines[1], lines[3]), lines))
        assertEquals("continuous", PracticeActivities.normalize(" Continuous! "))
        assertTrue(PracticeActivities.accepts(PracticeBlank("", "one"), "1"))
        assertTrue(PracticeActivities.accepts(PracticeBlank("", "zero"), "0"))
        assertTrue(PracticeActivities.accepts(PracticeBlank("", "plus"), "+"))
        assertFalse(PracticeActivities.accepts(PracticeBlank("", "plus"), "-"))
    }
}
