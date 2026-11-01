package com.nmims.songstudio.economics.data

data class LyricSection(val title: String, val lines: List<String>, val isChorus: Boolean = false)

data class FittedLyrics(val sections: List<LyricSection>, val syllablesPerLine: Int) {
    val lines: List<String> get() = sections.flatMap { it.lines }

    /** Lyrics with [Section] tags, the format music generators such as Suno expect. */
    fun asText(): String = sections.joinToString("\n\n") { s -> "[${s.title}]\n" + s.lines.joinToString("\n") }
}

/**
 * Builds a topic's lyrics and fits them to the chosen song: its section pattern (verse/chorus,
 * refrain/antara, verse/hook) and a line length that suits its tempo. Template-based, no AI model.
 */
object LyricsFitter {

    fun fit(topic: Topic, lang: Lang, song: CatalogSong, variant: Int = 0): FittedLyrics {
        val target = syllableTarget(song)
        val base = baseSections(topic, lang)
        val verses = base.filter { !it.isChorus }.ifEmpty { generated(topic, lang).filter { !it.isChorus } }
        val chorus = base.firstOrNull { it.isChorus }?.takeIf { variant == 0 } ?: hook(topic, lang, variant)
        val words = Words.of(lang)

        val fittedVerses = verses.map { it.copy(lines = reflow(it.lines, target).take(6)) }.take(3)
        val fittedChorus = chorus.copy(lines = reflow(chorus.lines, target).take(4))

        val sections = when {
            GenreStyles.isDevotional(song.genreId) -> buildList {
                add(fittedChorus.copy(title = words.refrain))
                fittedVerses.forEachIndexed { i, v ->
                    add(v.copy(title = "${words.stanza} ${i + 1}"))
                    add(fittedChorus.copy(title = words.refrain))
                }
            }
            GenreStyles.isRap(song.genreId) -> buildList {
                fittedVerses.forEachIndexed { i, v ->
                    add(v.copy(title = "${words.verse} ${i + 1}"))
                    add(fittedChorus.copy(title = words.hook))
                }
            }
            else -> buildList {
                fittedVerses.forEachIndexed { i, v ->
                    add(v.copy(title = "${words.verse} ${i + 1}"))
                    if (i == 0 || i == fittedVerses.lastIndex) add(fittedChorus.copy(title = words.chorus))
                }
            }
        }
        return FittedLyrics(sections, target)
    }

    /** Roughly two bars per line: more syllables for fast songs and rap, fewer for slow ones. */
    fun syllableTarget(song: CatalogSong): Int {
        val base = when {
            song.bpm >= 120 -> 14
            song.bpm >= 100 -> 12
            song.bpm >= 85 -> 11
            else -> 9
        }
        return if (GenreStyles.isRap(song.genreId)) base + 4 else base
    }

    fun reflow(lines: List<String>, target: Int): List<String> {
        val split = lines.flatMap { splitLong(it, target) }
        val out = mutableListOf<String>()
        for (line in split) {
            val last = out.lastOrNull()
            if (last != null && syllables(last) < target * 0.45 && syllables(last) + syllables(line) <= target * 1.2) {
                out[out.lastIndex] = "$last $line"
            } else {
                out += line
            }
        }
        return out
    }

    private fun splitLong(line: String, target: Int): List<String> {
        if (syllables(line) <= target * 1.5) return listOf(line)
        val words = line.split(" ").filter { it.isNotBlank() }
        if (words.size < 4) return listOf(line)
        val total = syllables(line)
        var running = 0
        var best = words.size / 2
        var bestScore = Int.MAX_VALUE
        for (i in 1 until words.size) {
            running += syllables(words[i - 1])
            val score = kotlin.math.abs(running - total / 2) - (if (words[i - 1].endsWith(",") || words[i - 1].endsWith("—")) 3 else 0)
            if (score < bestScore) { bestScore = score; best = i }
        }
        val first = words.take(best).joinToString(" ")
        val second = words.drop(best).joinToString(" ")
        return splitLong(first, target) + splitLong(second, target)
    }

    /** Syllable estimate for Latin and Devanagari text. */
    fun syllables(text: String): Int {
        var count = 0
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        for (word in words) {
            if (word.any { it in 'ऀ'..'ॿ' }) {
                for ((i, c) in word.withIndex()) {
                    val independentVowel = c in 'ऄ'..'औ'
                    val consonant = c in 'क'..'ह' || c in 'क़'..'य़'
                    val halfConsonant = consonant && word.getOrNull(i + 1) == '्'
                    if (independentVowel || (consonant && !halfConsonant)) count++
                }
            } else {
                val w = word.lowercase().filter { it.isLetter() }
                if (w.isEmpty()) continue
                var groups = Regex("[aeiouy]+").findAll(w).count()
                if (w.length > 2 && w.endsWith("e") && !w.endsWith("le") && groups > 1) groups--
                count += maxOf(1, groups)
            }
        }
        return count
    }

    private fun baseSections(topic: Topic, lang: Lang): List<LyricSection> =
        topic.lyrics[lang]?.let(::parse)?.takeIf { it.isNotEmpty() } ?: generated(topic, lang)

    private fun parse(text: String): List<LyricSection> {
        val sections = mutableListOf<LyricSection>()
        var title = ""
        var lines = mutableListOf<String>()
        fun close() {
            if (lines.isNotEmpty()) {
                val chorus = Regex("chorus|hook|मुखड़ा|मुखडा|धृपद|ध्रुवपद|स्थायी|कोरस", RegexOption.IGNORE_CASE).containsMatchIn(title)
                sections += LyricSection(title, lines, chorus)
            }
        }
        for (raw in text.lines()) {
            val line = raw.trim()
            when {
                line.startsWith("[") && line.endsWith("]") -> {
                    close()
                    title = line.removePrefix("[").removeSuffix("]")
                    lines = mutableListOf()
                }
                line.isNotEmpty() -> lines += line
            }
        }
        close()
        return sections
    }

    private fun generated(topic: Topic, lang: Lang): List<LyricSection> {
        val keywords = topic.keywords[lang].take(4)
        val words = Words.of(lang)
        val verse1 = topic.description[lang].split(Regex("(?<=[.।!?])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val verse2 = keywords.map { words.keywordLine(it) }.ifEmpty { listOfNotNull(topic.formula?.let { words.formulaLine(it) }) }
        return listOfNotNull(
            LyricSection(words.verse, verse1),
            verse2.takeIf { it.isNotEmpty() }?.let { LyricSection(words.verse, it) },
            hook(topic, lang, 0),
        )
    }

    private fun hook(topic: Topic, lang: Lang, variant: Int): LyricSection {
        val words = Words.of(lang)
        val name = topic.name[lang]
        val (open, close) = words.hooks[Math.floorMod(variant, words.hooks.size)]
        val middle = topic.keywords[lang].take(4).joinToString(", ").ifBlank { topic.formula ?: name }
        return LyricSection(words.chorus, listOf(open.replace("{name}", name), middle, close), isChorus = true)
    }

    private class Words(
        val verse: String,
        val chorus: String,
        val hook: String,
        val refrain: String,
        val stanza: String,
        val hooks: List<Pair<String, String>>,
        val keywordLine: (String) -> String,
        val formulaLine: (String) -> String,
    ) {
        companion object {
            fun of(lang: Lang) = when (lang) {
                Lang.EN -> Words(
                    "Verse", "Chorus", "Hook", "Refrain", "Verse",
                    listOf(
                        "{name}, {name} — sing it, learn it, keep it!" to "That's the rhythm of the lesson, that's the beat we need.",
                        "Hey, it's {name} — say it loud and clear," to "Sing it one more time and it stays right here.",
                        "{name} on the beat, {name} in my mind," to "Learn it with a song and you'll never fall behind.",
                    ),
                    { kw -> "$kw — a key idea to know." },
                    { f -> "Write it down: $f" },
                )
                Lang.HI -> Words(
                    "अंतरा", "मुखड़ा", "हुक", "स्थायी", "अंतरा",
                    listOf(
                        "{name}, {name} — गाओ, समझो, याद रखो!" to "यही है पढ़ाई की ताल, यही है सीख हमारी।",
                        "सुनो जी, {name} — दिल से समझो आज," to "गाते-गाते याद रहे, यही है इसका राज़।",
                        "{name} की बात, सुर में साथ-साथ," to "एक बार गाओ फिर से, पक्की हो हर बात।",
                    ),
                    { kw -> "$kw — इस विषय की अहम बात।" },
                    { f -> "सूत्र याद रखो: $f" },
                )
                Lang.MR -> Words(
                    "कडवे", "धृपद", "हुक", "ध्रुवपद", "कडवे",
                    listOf(
                        "{name}, {name} — गा, समजून घे, लक्षात ठेव!" to "हाच अभ्यासाचा ताल, हीच आपली शिकवण.",
                        "ऐका रे, {name} — मनापासून समजू," to "गाता-गाता लक्षात राहील, चला पुन्हा गाऊ.",
                        "{name} ची गोष्ट, सुरात सांगू आता," to "एकदा गाऊ पुन्हा, पक्की होईल प्रत्येक बाब.",
                    ),
                    { kw -> "$kw — या विषयातील महत्त्वाची संकल्पना." },
                    { f -> "सूत्र लक्षात ठेवा: $f" },
                )
            }
        }
    }
}
