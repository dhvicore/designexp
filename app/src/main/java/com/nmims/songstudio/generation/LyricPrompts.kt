package com.nmims.songstudio.generation

import com.nmims.songstudio.data.SongPreferences
import com.nmims.songstudio.data.Topic
import com.nmims.songstudio.data.SongChoice

/** Provider-independent contract. Suraj can pass this prompt through a backend API. */
object LyricPrompts {
    fun draft(topic: Topic, category: String, choice: SongChoice): String {
        if (topic.subject == "Chemistry") {
            val language = when (category) { "Hindi" -> "hi"; "Marathi" -> "mr"; else -> "en" }
            return topic.lyrics.getValue(language)
        }
        if (topic.title == "Rolle's Theorem" && category == "Bhajan")
            return RolleLesson.bhajanDraft
        if (topic.title == "Rolle's Theorem" && category == "Hindi")
            return RolleLesson.hindiDraft
        val language = when (category) { "Hindi" -> "hi"; "Bhajan" -> "bhajan"; else -> "en" }
        val source = topic.lyrics[language].orEmpty().lines().filter { it.isNotBlank() }
        val seed = choice.song.title.fold(0) { sum, c -> (sum * 31 + c.code).and(Int.MAX_VALUE) }
        // Reference-dependent phrasing targets; exact melody alignment requires the melody dataset.
        val target = if (category == "Lo-fi" || category == "Bhajan") 10 + seed % 3 else 6 + seed % 5
        fun phrases(lines: List<String>): List<String> = lines.flatMap { line ->
            val words = line.trim().split(Regex("\\s+"))
            words.chunked(target).map { it.joinToString(" ") }
        }
        val intro = when (category) {
            "Hindi" -> if (seed % 2 == 0) "Aao milkar gyaan ko gaayein," else "Dhun ke sang yeh paath dohraayein,"
            "Bhajan" -> "Om vidya-prakashaya namah,\nGyaan ki jyoti man mein jage,"
            "Lo-fi" -> "Breathe in slowly, take your time,"
            else -> if (seed % 2 == 0) "Find the pattern, feel the beat," else "Sing the lesson, make it clear,"
        }
        val assumptions = if (topic.title == "Rolle's Theorem") when (language) {
            "en" -> "Continuous on the closed interval,\nDifferentiable inside,\nEqual values at the endpoints,\nThese conditions are our guide.\n"
            else -> "Band antaral par satat rahe,\nBheetar avkalan ho paaye,\nDono siron par maan samaan,\nTabhi yeh niyam lag paaye.\n"
        } else ""
        val chorus = phrases(source.takeLast(2)).let { if (seed % 2 == 0) it else it + it.takeLast(1) }
        return "[Intro]\n$intro\n\n[Verse]\n$assumptions${phrases(source).joinToString("\n")}\n\n[Chorus]\n${chorus.joinToString("\n")}"
    }

    fun singingStyle(category: String, choice: SongChoice): String {
        val language = when (category) { "Hindi", "Bhajan" -> "Hindi"; "Marathi" -> "Marathi"; else -> "English" }
        val description = when (category) {
            "Bhajan" -> "devotional bhajan, Hindi with Sanskrit invocation, reverent melodic singing, harmonium, tabla, tanpura"
            "Lo-fi" -> "lo-fi chill song, gentle melodic $language vocals, soft keys, warm bass, relaxed drums"
            else -> "${choice.style.description}, ${choice.song.mood}, ${choice.style.instruments}, clear $language singing"
        }
        return "$description, educational lyrics, catchy chorus, natural sung vocals".take(512)
    }

    fun educational(topic: Topic, preferences: SongPreferences, languageName: String): String = """
        Write an original educational song for a college student.
        Topic: ${topic.title}
        Source explanation: ${topic.concept}
        Formula/conclusion: ${topic.formula}
        Language: $languageName
        Music: ${preferences.style.description}; ${preferences.style.instruments}
        Tempo preference: ${preferences.tempo}
        Student learning focus: ${preferences.focus}
        Structure: short verse, repeating concept chorus, second verse, chorus.
        Preserve mathematical meaning and state every required theorem condition.
        Explain symbols in singable words; show exact notation separately.
        Do not invent facts or copy a reference song's lyrics or melody.
        Flag missing source information for human review.
    """.trimIndent()

    fun music(preferences: SongPreferences, languageName: String): String =
        "${preferences.style.description}, ${preferences.reference.mood}, " +
            "${preferences.style.instruments}, $languageName vocals, ${preferences.tempo}, " +
            "clear pronunciation, original melody, educational song, memorable repeated chorus"

    fun draft(topic: Topic, language: String): String {
        val source = topic.lyrics[language].orEmpty()
        // Keep the original data intact; correct the missing theorem assumptions in this draft.
        val conditions = if (topic.title == "Rolle's Theorem" && language == "en")
            "Continuous on the closed interval, differentiable inside,\nEqual endpoint values — these conditions are our guide.\n" else ""
        return "[Verse]\n$conditions$source\n\n[Chorus]\n" + source.lines().takeLast(2).joinToString("\n")
    }
}
