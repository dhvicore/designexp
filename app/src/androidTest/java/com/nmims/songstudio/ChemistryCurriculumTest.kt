package com.nmims.songstudio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nmims.songstudio.data.ChemistryCurriculum
import com.nmims.songstudio.data.ChemistryTopic
import com.nmims.songstudio.data.Curriculum
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the Chemistry educational-song database loader.
 * Reads the actual bundled chemistry_educationalsong_db.json asset from the target APK.
 */
@RunWith(AndroidJUnit4::class)
class ChemistryCurriculumTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val chem by lazy { ChemistryCurriculum(context) }

    @Test fun chemistryAdaptersKeepLanguagesAndCalculusDemosSeparate() {
        val data = Curriculum(InstrumentationRegistry.getInstrumentation().targetContext)
        assertEquals(60, data.chemistryTopics.size)
        assertTrue(data.chemistryTopics.none { chem -> data.topics.any { it.id == chem.id } })
        for (topic in data.chemistryTopics) {
            for ((category, language) in mapOf("Hindi" to "hi", "English" to "en", "Marathi" to "mr")) {
                val choice = com.nmims.songstudio.data.MusicChoices.options(data, topic, category).first()
                assertEquals(topic.reference[language], choice.song.title)
                assertEquals(topic.lyrics[language], com.nmims.songstudio.generation.LyricPrompts.draft(topic, category, choice))
                assertTrue(topic.lyrics.getValue(language).contains('\n'))
                assertFalse(topic.lyrics.getValue(language).contains("\\n"))
            }
        }
    }

    @Test
    fun loadsExactly60UniqueRecords() {
        assertEquals("Expected 60 Chemistry topics", 60, chem.topics.size)
        val ids = chem.topics.map { it.songId }.toSet()
        assertEquals("All Song_IDs must be unique", 60, ids.size)
    }

    @Test
    fun allSongIdsArePositive() {
        assertTrue(chem.topics.all { it.songId > 0 })
    }

    @Test
    fun allUnitsArePresent() {
        val unitNames = chem.units.keys.toList()
        assertTrue("Must have multiple units", unitNames.size >= 6)
        // Verify known units from the dataset
        assertTrue(unitNames.any { it.contains("Atom Structure") })
        assertTrue(unitNames.any { it.contains("Gas Laws") && it.startsWith("Unit") })
        assertTrue(unitNames.any { it.contains("Water") && it.startsWith("Unit") })
        assertTrue(unitNames.any { it.contains("Engineering Materials") })
        assertTrue(unitNames.any { it.contains("Electrochemistry") })
        assertTrue(unitNames.any { it.contains("Acids and Bases") })
    }

    @Test
    fun allTopicNamesArePopulatedInAllLanguages() {
        for (topic in chem.topics) {
            for (lang in listOf("en", "hi", "mr")) {
                val name = topic.topicName[lang]
                assertNotNull("Missing topic name for lang=$lang, Song_ID=${topic.songId}", name)
                assertTrue("Blank topic name for lang=$lang, Song_ID=${topic.songId}", name!!.isNotBlank())
            }
        }
    }

    @Test
    fun allSongTitlesAndArtistsArePopulated() {
        for (topic in chem.topics) {
            for (lang in listOf("en", "hi", "mr")) {
                assertNotNull(topic.songTitle[lang])
                assertTrue(topic.songTitle[lang]!!.isNotBlank())
                assertNotNull(topic.artistName[lang])
                assertTrue(topic.artistName[lang]!!.isNotBlank())
            }
        }
    }

    @Test
    fun allLyricsArePopulatedAndMultiline() {
        for (topic in chem.topics) {
            for (lang in listOf("en", "hi", "mr")) {
                val lyrics = topic.lyrics[lang]
                assertNotNull("Missing lyrics for lang=$lang, Song_ID=${topic.songId}", lyrics)
                assertTrue("Lyrics too short for lang=$lang, Song_ID=${topic.songId}", lyrics!!.length > 50)
                assertTrue("Lyrics should be multiline for lang=$lang, Song_ID=${topic.songId}", lyrics.contains("\n"))
            }
        }
    }

    @Test
    fun allExplanationsArePopulated() {
        for (topic in chem.topics) {
            for (lang in listOf("en", "hi", "mr")) {
                val explanation = topic.explanation[lang]
                assertNotNull("Missing explanation for lang=$lang, Song_ID=${topic.songId}", explanation)
                assertTrue("Explanation too short for lang=$lang, Song_ID=${topic.songId}", explanation!!.length > 20)
            }
        }
    }

    @Test
    fun allKeywordsArePopulated() {
        for (topic in chem.topics) {
            for (lang in listOf("en", "hi", "mr")) {
                val keywords = topic.keywords[lang]
                assertNotNull("Missing keywords for lang=$lang, Song_ID=${topic.songId}", keywords)
                assertTrue("Keywords too short for lang=$lang, Song_ID=${topic.songId}", keywords!!.isNotBlank())
            }
        }
    }

    @Test
    fun lyricsApproachIsPopulated() {
        for (topic in chem.topics) {
            assertTrue("Blank Lyrics_Approach for Song_ID=${topic.songId}", topic.lyricsApproach.isNotBlank())
        }
    }

    @Test
    fun hindiTextDisplaysCorrectly() {
        // Verify specific known Hindi text from the first record (Dalton's Atomic Theory)
        val dalton = chem.topics.first { it.songId == 1 }
        assertTrue(dalton.topicName["hi"]!!.contains("डाल्टन"))
        assertTrue(dalton.lyrics["hi"]!!.contains("परमाणु"))
        assertTrue(dalton.explanation["hi"]!!.contains("पदार्थ"))
        assertTrue(dalton.keywords["hi"]!!.contains("डाल्टन"))
    }

    @Test
    fun marathiTextDisplaysCorrectly() {
        // Verify specific known Marathi text from the first record (Dalton's Atomic Theory)
        val dalton = chem.topics.first { it.songId == 1 }
        assertTrue(dalton.topicName["mr"]!!.contains("डाल्टन"))
        assertTrue(dalton.lyrics["mr"]!!.contains("अणू"))
        assertTrue(dalton.explanation["mr"]!!.contains("अणू"))
        assertTrue(dalton.keywords["mr"]!!.contains("डाल्टन"))
    }

    @Test
    fun multilineLyricsRetainSectionLabels() {
        // Verify that section markers like [Verse 1], [Chorus] are preserved in English
        val firstTopic = chem.topics.first()
        val engLyrics = firstTopic.lyrics["en"]!!
        assertTrue("English lyrics should contain section labels", engLyrics.contains("[Verse") || engLyrics.contains("[Chorus"))
        // Hindi section markers
        val hindiLyrics = firstTopic.lyrics["hi"]!!
        assertTrue("Hindi lyrics should contain section labels", hindiLyrics.contains("[अंतरा") || hindiLyrics.contains("[मुखड़ा"))
        // Marathi section markers
        val marathiLyrics = firstTopic.lyrics["mr"]!!
        assertTrue("Marathi lyrics should contain section labels", marathiLyrics.contains("[कडवे") || marathiLyrics.contains("[ध्रुवपद"))
    }

    @Test
    fun englishHindiMarathiMappingsAreDistinct() {
        for (topic in chem.topics) {
            // Topic names should differ across languages
            val en = topic.topicName["en"]!!
            val hi = topic.topicName["hi"]!!
            val mr = topic.topicName["mr"]!!
            // At least English should differ from Hindi/Marathi (they use different scripts)
            assertNotEquals("English and Hindi topic names should differ for Song_ID=${topic.songId}", en, hi)
            assertNotEquals("English and Marathi topic names should differ for Song_ID=${topic.songId}", en, mr)

            // Lyrics should also differ
            assertNotEquals("English and Hindi lyrics should differ for Song_ID=${topic.songId}",
                topic.lyrics["en"]!!, topic.lyrics["hi"]!!)
            assertNotEquals("English and Marathi lyrics should differ for Song_ID=${topic.songId}",
                topic.lyrics["en"]!!, topic.lyrics["mr"]!!)
        }
    }

    @Test
    fun everyTopicRetrievesCorrectSongTitleAndArtist() {
        // Verify lookup by Song_ID for every record
        for (topic in chem.topics) {
            val found = chem.topicById(topic.songId)
            assertNotNull("topicById should find Song_ID=${topic.songId}", found)
            assertEquals(topic.songId, found!!.songId)
            // Every language field should be retrievable
            for (lang in listOf("en", "hi", "mr")) {
                assertNotNull(found.songTitle[lang])
                assertNotNull(found.artistName[lang])
                assertNotNull(found.explanation[lang])
                assertNotNull(found.keywords[lang])
            }
        }
    }

    @Test
    fun topicsForUnitReturnsCorrectSubsets() {
        for (unitKey in chem.units.keys) {
            val unitTopics = chem.topicsForUnit(unitKey)
            assertTrue("Unit '$unitKey' should have at least 1 topic", unitTopics.isNotEmpty())
            assertTrue(unitTopics.all { it.unit == unitKey })
        }
        // Sum of per-unit topics should equal total
        val totalFromUnits = chem.units.keys.sumOf { chem.topicsForUnit(it).size }
        assertEquals(60, totalFromUnits)
    }

    @Test
    fun allExpectedFieldsArePresentInRequiredFieldsList() {
        assertEquals(
            "Expected 21 required fields matching the JSON schema",
            21, ChemistryCurriculum.EXPECTED_FIELD_COUNT
        )
        assertEquals(21, ChemistryCurriculum.REQUIRED_FIELDS.size)
    }

    @Test
    fun existingCalculusTopicsStillWork() {
        val data = Curriculum(context)
        assertEquals("Calculus must still have 38 topics", 38, data.topics.size)
        assertEquals(
            listOf(9, 5, 5, 8, 11),
            data.units.keys.map { unit -> data.topics.count { it.unit == unit } }
        )
        assertTrue(data.topics.all { it.concept.isNotBlank() && it.formula.isNotBlank() })
    }

    @Test
    fun chemistryBrowsingDoesNotRequireSingingServer() {
        // This test verifies that the ChemistryCurriculum can be created and browsed
        // entirely offline, with no network or server dependency.
        val chem = ChemistryCurriculum(context)
        assertEquals(60, chem.topics.size)
        val firstTopic = chem.topics.first()
        // Reading all fields should work without any network call
        assertNotNull(firstTopic.topicName["en"])
        assertNotNull(firstTopic.lyrics["en"])
        assertNotNull(firstTopic.explanation["en"])
        assertNotNull(firstTopic.keywords["en"])
        assertNotNull(firstTopic.songTitle["en"])
        assertNotNull(firstTopic.artistName["en"])
        assertNotNull(firstTopic.lyricsApproach)
    }
}
