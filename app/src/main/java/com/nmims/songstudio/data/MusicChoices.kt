package com.nmims.songstudio.data

data class SongChoice(val song: ReferenceSong, val style: MusicStyle) {
    val id: String get() = song.title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

/** Topic-dependent recommendations from the team's existing song metadata. */
object MusicChoices {
    val categories = listOf("Hindi", "English", "Bhajan", "Lo-fi")

    fun options(data: Curriculum, topic: Topic, category: String, all: Boolean = false): List<SongChoice> {
        if (topic.subject == "Chemistry") {
            val language = when (category) { "Hindi" -> "hi"; "Marathi" -> "mr"; else -> "en" }
            val style = MusicStyle("chemistry-reference", "Educational song", "",
                "clear melodic educational singing", emptyList())
            return listOf(SongChoice(ReferenceSong(topic.reference.getValue(language),
                topic.artists.getValue(language), 90, "Curriculum song reference"), style))
        }
        val language = if (category == "Hindi" || category == "Bhajan") "hi" else "en"
        var pool = data.styles(language).filter { it.id != "lofi" }.flatMap { style ->
            style.songs.map { SongChoice(it, style) }
        }
        pool = when (category) {
            "Bhajan" -> listOf(SongChoice(ReferenceSong("Vaishnav Jan To", "Traditional bhajan", 70, "devotional"), data.styles("hi").first { it.id == "bhajan" })) + pool.filter { it.style.id == "bhajan" } + data.styles("sa").filter { it.id != "lofi" }.flatMap { style -> style.songs.map { SongChoice(it, style) } }
            "Hindi" -> pool.filter { it.style.id != "bhajan" } + listOf(
                SongChoice(ReferenceSong("Chikni Chameli", "Bollywood dance reference", 120, "energetic"), data.styles("hi").first { it.id == "party" }),
                SongChoice(ReferenceSong("Chammak Challo", "Bollywood dance reference", 110, "rhythmic"), data.styles("hi").first { it.id == "party" })
            )
            "Lo-fi" -> pool.filter { it.style.id in listOf("acoustic", "rnb", "pop", "hiphop") }.map {
                SongChoice(it.song, it.style.copy(name = "Lo-fi", description = "soft lo-fi study song with warm intimate singing", instruments = "soft piano, mellow bass, gentle drums"))
            }
            else -> pool
        }.distinctBy { it.song.title }
        if (all) return pool
        val preferred = topic.reference[language].orEmpty()
        val mapped = pool.firstOrNull { preferred.contains(it.song.title, ignoreCase = true) }
        val recommended = pool.shuffled(kotlin.random.Random(topic.id * 1009 + category.hashCode()))
        val savedId = when (topic.id) {
            0 -> mapOf("English" to "levitating", "Hindi" to "kala-chashma", "Bhajan" to "raghupati-raghav-raja-ram", "Lo-fi" to "perfect")[category]
            17 -> mapOf("English" to "shape-of-you", "Hindi" to "chaiyya-chaiyya", "Bhajan" to "om-jai-jagdish-hare", "Lo-fi" to "sunflower")[category]
            else -> null
        }
        val ready = pool.firstOrNull { it.id == savedId } ?: if (category == "Bhajan") pool.firstOrNull { it.id == listOf("raghupati-raghav-raja-ram", "om-jai-jagdish-hare", "vaishnav-jan-to")[topic.id % 3] } else null
        return (listOfNotNull(ready, mapped) + recommended).distinctBy { it.song.title }.take(4)
    }
}
