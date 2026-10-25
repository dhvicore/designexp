package com.nmims.songstudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Topic(
    val id: Int, val unit: String, val title: String, val concept: String,
    val formula: String, val lyrics: Map<String, String>, val reference: Map<String, String>,
    val subject: String = "Calculus", val artists: Map<String, String> = emptyMap(),
    val explanations: Map<String, String> = emptyMap()
)
data class ReferenceSong(val title: String, val artist: String, val bpm: Int, val mood: String)
data class MusicStyle(
    val id: String, val name: String, val instruments: String,
    val description: String, val songs: List<ReferenceSong>
)
data class SongPreferences(
    val language: String, val style: MusicStyle, val reference: ReferenceSong,
    val focus: String, val tempo: String
)

/** Local curriculum imported from the existing Python prototype, not fabricated syllabus data. */
class Curriculum(context: Context) {
    private val assets = context.assets
    private fun read(name: String): String = assets.open(name).bufferedReader().use { it.readText() }
    private val catalog = JSONObject(read("song_catalog.json"))
    val topics: List<Topic> = JSONArray(read("topics.json")).let { rows ->
        (0 until rows.length()).map { i ->
            val t = rows.getJSONObject(i)
            val fields = mapOf("en" to "lyrics_en", "hi" to "lyrics_hi", "mr" to "lyrics_mr",
                "gu" to "lyrics_gu", "sa" to "lyrics_bhajan_sa", "bhajan" to "lyrics_bhajan")
            Topic(i, t.getString("unit"), t.getString("topic"), t.getString("concept"),
                t.getString("formula"), fields.mapValues { (_, field) ->
                    val lines = t.optJSONArray(field) ?: JSONArray()
                    (0 until lines.length()).joinToString("\n") { lines.getString(it) }
                }, mapOf("en" to t.optString("ref_en"), "hi" to t.optString("ref_hi")))
        }.filter { it.unit.startsWith("Unit ") }
    }
    val chemistry = ChemistryCurriculum(context)
    val chemistryTopics = chemistry.topics.map { t ->
        Topic(1000 + t.songId, t.unit, t.topicName.getValue("en"),
            t.explanation.getValue("en"), t.keywords.getValue("en"),
            t.lyrics.mapValues { it.value.replace("\\n", "\n") }, t.songTitle,
            "Chemistry", t.artistName, t.explanation)
    }
    fun topicsForSubject(subject: String) = if (subject == "Chemistry") chemistryTopics else topics
    fun unitsForSubject(subject: String): Map<String, String> =
        if (subject == "Chemistry") chemistry.units else units

    val languages = linkedMapOf("en" to "English", "hi" to "Hindi", "mr" to "Marathi",
        "gu" to "Gujarati", "sa" to "Sanskrit")
    val collections = linkedMapOf("en" to "Hollywood / English", "hi" to "Bollywood / Hindi",
        "mr" to "Marathi", "gu" to "Gujarati", "sa" to "Sanskrit / Devotional")
    val units = linkedMapOf("Unit 1" to "Differential Calculus", "Unit 2" to "Partial Differentiation",
        "Unit 3" to "Integral Calculus", "Unit 4" to "Multiple Integrals", "Unit 5" to "Vector Calculus")

    fun styles(language: String): List<MusicStyle> {
        val genres = catalog.getJSONObject("genres").getJSONObject(language)
        val existing = genres.keys().asSequence().map { key ->
            val g = genres.getJSONObject(key)
            val songs = g.getJSONArray("songs")
            MusicStyle(key, g.getString("name"), g.getString("instruments"), g.getString("suno"),
                (0 until songs.length()).map { i ->
                    val s = songs.getJSONObject(i)
                    ReferenceSong(s.getString("title"), s.getString("artist"), s.optInt("bpm", 90), s.optString("mood"))
                })
        }.toList()
        return existing + MusicStyle("lofi", "Lo-fi", "soft keys, relaxed drums, warm ambient textures",
            "gentle lo-fi educational song", existing.flatMap { it.songs }.distinctBy { it.title })
    }
}
