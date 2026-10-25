package com.nmims.songstudio.data

import android.content.Context
import org.json.JSONObject

/**
 * A single Chemistry educational-song record parsed from the bundled JSON asset.
 * All multilingual fields are stored as Maps keyed by language code ("en", "hi", "mr").
 */
data class ChemistryTopic(
    val songId: Int,
    val unit: String,
    val topicName: Map<String, String>,        // en, hi, mr
    val songTitle: Map<String, String>,         // en, hi, mr
    val artistName: Map<String, String>,        // en, hi, mr
    val lyrics: Map<String, String>,            // en, hi, mr
    val explanation: Map<String, String>,        // en, hi, mr
    val keywords: Map<String, String>,           // en, hi, mr
    val lyricsApproach: String
)

/**
 * Loads and validates the Chemistry educational-song database from the bundled JSON asset.
 *
 * Design choices:
 * - Uses Android's built-in [org.json.JSONObject] to match the existing [Curriculum] loader.
 * - All 60 records are parsed once on construction; no lazy loading is needed for 60 records.
 * - Duplicate Song_IDs are rejected at load time.
 * - Missing or blank required fields produce clear [IllegalStateException] messages.
 * - Unicode (Hindi, Marathi) text and multiline lyrics are preserved as-is from the JSON.
 *
 * This class provides the academic content catalog. It does NOT insert records into the
 * song-history database; that remains the responsibility of [SongRepository] after actual
 * song generation.
 */
class ChemistryCurriculum(context: Context) {

    /** All 60 loaded Chemistry topic records, indexed by their JSON array order. */
    val topics: List<ChemistryTopic>

    /** Distinct unit names in the order they first appear in the dataset. */
    val units: LinkedHashMap<String, String>

    /** Languages supported by the Chemistry dataset. */
    val languages = linkedMapOf("en" to "English", "hi" to "Hindi", "mr" to "Marathi")

    init {
        val raw = context.assets.open("chemistry_educationalsong_db.json")
            .bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val records = root.getJSONArray("records")
        val seenIds = mutableSetOf<Int>()
        val parsed = mutableListOf<ChemistryTopic>()

        for (i in 0 until records.length()) {
            val r = records.getJSONObject(i)
            val songId = r.getInt("Song_ID")
            check(seenIds.add(songId)) {
                "Duplicate Song_ID $songId at record index $i"
            }

            val unit = r.requireString("Unit", i)

            val topic = ChemistryTopic(
                songId = songId,
                unit = unit,
                topicName = mapOf(
                    "en" to r.requireString("Topic_English", i),
                    "hi" to r.requireString("Topic_Hindi", i),
                    "mr" to r.requireString("Topic_Marathi", i)
                ),
                songTitle = mapOf(
                    "en" to r.requireString("English_Song_Title", i),
                    "hi" to r.requireString("Hindi_Song_Title", i),
                    "mr" to r.requireString("Marathi_Song_Title", i)
                ),
                artistName = mapOf(
                    "en" to r.requireString("English_Artist_Name", i),
                    "hi" to r.requireString("Hindi_Artist_Name", i),
                    "mr" to r.requireString("Marathi_Artist_Name", i)
                ),
                lyrics = mapOf(
                    "en" to r.requireString("English_Lyrics", i),
                    "hi" to r.requireString("Hindi_Lyrics", i),
                    "mr" to r.requireString("Marathi_Lyrics", i)
                ),
                explanation = mapOf(
                    "en" to r.requireString("English_Explanation", i),
                    "hi" to r.requireString("Hindi_Explanation", i),
                    "mr" to r.requireString("Marathi_Explanation", i)
                ),
                keywords = mapOf(
                    "en" to r.requireString("English_Keywords", i),
                    "hi" to r.requireString("Hindi_Keywords", i),
                    "mr" to r.requireString("Marathi_Keywords", i)
                ),
                lyricsApproach = r.requireString("Lyrics_Approach", i)
            )
            parsed.add(topic)
        }

        topics = parsed.toList()

        // Build the unit map preserving insertion order from the dataset.
        val unitMap = linkedMapOf<String, String>()
        for (t in topics) {
            if (t.unit !in unitMap) {
                // Use the unit string itself as both key and display name.
                unitMap[t.unit] = t.unit
            }
        }
        units = unitMap
    }

    /** Retrieve a single topic by its Song_ID. */
    fun topicById(songId: Int): ChemistryTopic? = topics.firstOrNull { it.songId == songId }

    /** All topics belonging to a specific unit. */
    fun topicsForUnit(unit: String): List<ChemistryTopic> = topics.filter { it.unit == unit }

    companion object {
        /** Expected number of JSON fields per record. */
        const val EXPECTED_FIELD_COUNT = 21

        /** All JSON field names that must be present in every record. */
        val REQUIRED_FIELDS = listOf(
            "Song_ID", "Unit",
            "Topic_English", "Topic_Hindi", "Topic_Marathi",
            "English_Song_Title", "English_Artist_Name",
            "Hindi_Song_Title", "Hindi_Artist_Name",
            "Marathi_Song_Title", "Marathi_Artist_Name",
            "English_Lyrics", "Hindi_Lyrics", "Marathi_Lyrics",
            "English_Explanation", "Hindi_Explanation", "Marathi_Explanation",
            "English_Keywords", "Hindi_Keywords", "Marathi_Keywords",
            "Lyrics_Approach"
        )
    }
}

/**
 * Extract a required non-blank string from a JSON object, with a clear error message
 * identifying the record index and field name if it is missing or empty.
 */
private fun JSONObject.requireString(field: String, recordIndex: Int): String {
    check(has(field)) { "Missing field '$field' in record at index $recordIndex" }
    val value = getString(field)
    check(value.isNotBlank()) { "Blank field '$field' in record at index $recordIndex" }
    return value
}
