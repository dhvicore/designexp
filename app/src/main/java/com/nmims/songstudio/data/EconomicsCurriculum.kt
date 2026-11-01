package com.nmims.songstudio.data

import android.content.Context
import com.nmims.songstudio.economics.data.*

/** Krishhma's Economics/Management contribution, isolated from the existing curricula. */
class EconomicsCurriculum(context: Context) {
    val catalog = StudyRepository.load(context)
    private val source = catalog.subject("economics")
    private val originals = source.topics.mapIndexed { i, topic -> (2000 + i) to topic }.toMap()
    val units = source.units.associate { "Unit ${it.number}" to it.name.en }
    fun lang(category: String) = when(category) { "Hindi" -> Lang.HI; "Marathi" -> Lang.MR; else -> Lang.EN }
    val topics = originals.map { (id, t) ->
        Topic(id, "Unit ${t.unitNo}", t.name.en, t.description.en,
            t.formula ?: t.keywords.en.joinToString(", "),
            Lang.entries.associate { l -> l.code to LyricsFitter.fit(t, l, songs(id, l, false).first()).asText() },
            Lang.entries.associate { it.code to songs(id, it, false).first().title }, "Economics",
            Lang.entries.associate { it.code to songs(id, it, false).first().artist },
            Lang.entries.associate { it.code to t.description[it] })
    }
    private fun songs(id: Int, lang: Lang, all: Boolean): List<CatalogSong> {
        val recommended = originals.getValue(id).songs[lang].orEmpty().map { catalog.asCatalogSong(it, lang) }
        val library = catalog.genres(lang).flatMap { it.songs }
        return (recommended + if(all || recommended.isEmpty()) library else emptyList()).distinctBy { it.title }
    }
    fun options(id: Int, category: String, all: Boolean): List<SongChoice> = songs(id, lang(category), all).map { song ->
        val genre = catalog.genre(song.lang, song.genreId)
        SongChoice(ReferenceSong(song.title, song.artist, song.bpm, song.mood),
            MusicStyle(song.genreId, genre?.name ?: song.genreId, genre?.instruments.orEmpty(), genre?.vocal.orEmpty(), emptyList()))
    }
    fun song(category: String, choice: SongChoice) = CatalogSong(choice.song.title, choice.song.artist, null,
        choice.song.bpm, choice.song.mood, false, choice.style.id, lang(category))
    fun draft(id: Int, category: String, choice: SongChoice) =
        LyricsFitter.fit(originals.getValue(id), lang(category), song(category, choice)).asText()
}
