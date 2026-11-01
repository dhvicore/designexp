package com.nmims.songstudio.economics.data

enum class Lang(val label: String, val localeTag: String, val code: String) {
    EN("English", "en-IN", "en"),
    HI("हिंदी", "hi-IN", "hi"),
    MR("मराठी", "mr-IN", "mr"),
}

data class Localized(val en: String, val hi: String, val mr: String) {
    operator fun get(lang: Lang): String = when (lang) {
        Lang.EN -> en
        Lang.HI -> hi
        Lang.MR -> mr
    }
}

data class LocalizedList(val en: List<String>, val hi: List<String>, val mr: List<String>) {
    operator fun get(lang: Lang): List<String> = when (lang) {
        Lang.EN -> en
        Lang.HI -> hi
        Lang.MR -> mr
    }
}

/** A song a topic was matched with in the research datasets. */
data class SongRef(val title: String, val artist: String, val genre: String?)

data class Topic(
    val id: String,
    val subjectId: String,
    val unitNo: Int,
    val name: Localized,
    val description: Localized,
    val keywords: LocalizedList,
    val formula: String?,
    val lyrics: Map<Lang, String>,
    val songs: Map<Lang, List<SongRef>>,
    val reason: String?,
)

data class StudyUnit(val number: Int, val name: Localized, val topics: List<Topic>)

data class Subject(val id: String, val name: Localized, val units: List<StudyUnit>) {
    val topics: List<Topic> = units.flatMap { it.topics }
    fun unit(number: Int): StudyUnit? = units.firstOrNull { it.number == number }
}

/** A style family in the song picker (e.g. Bollywood pop, bhajan). */
data class Genre(
    val id: String,
    val lang: Lang,
    val name: String,
    val instruments: String,
    val vocal: String,
    val sunoTags: String,
    val songs: List<CatalogSong>,
)

/** A song students can choose as the tune reference for their study song. */
data class CatalogSong(
    val title: String,
    val artist: String,
    val year: Int?,
    val bpm: Int,
    val mood: String,
    val publicDomain: Boolean,
    val genreId: String,
    val lang: Lang,
)

/** The 90-song research list shown in the library. */
data class Song(
    val title: String,
    val artist: String,
    val year: Int,
    val language: String,
    val genre: String,
    val rank: Int,
    val movie: String,
)

class StudyCatalog(
    val subjects: List<Subject>,
    val librarySongs: List<Song>,
    private val genres: Map<Lang, List<Genre>>,
) {
    fun subject(id: String): Subject = subjects.firstOrNull { it.id == id } ?: subjects.first()

    fun topic(id: String): Topic? = subjects.firstNotNullOfOrNull { s -> s.topics.firstOrNull { it.id == id } }

    fun genres(lang: Lang): List<Genre> = genres[lang].orEmpty()

    fun genre(lang: Lang, id: String): Genre? = genres(lang).firstOrNull { it.id == id }

    /** Resolves a dataset song to a catalog entry, or builds one from the language's default style. */
    fun asCatalogSong(ref: SongRef, lang: Lang): CatalogSong {
        genres(lang).flatMap { it.songs }.firstOrNull { it.title.equals(ref.title, ignoreCase = true) }?.let { return it }
        val libraryGenre = ref.genre ?: librarySongs.firstOrNull { it.title.equals(ref.title, ignoreCase = true) }?.genre
        val genreId = GenreStyles.catalogIdFor(libraryGenre, lang) ?: genres(lang).firstOrNull()?.id ?: "pop"
        return CatalogSong(ref.title, ref.artist, null, GenreStyles.defaultBpm(genreId), "", false, genreId, lang)
    }

    fun topicsUsingSong(subject: Subject, title: String): List<Topic> =
        subject.topics.filter { t -> t.songs.values.flatten().any { it.title.equals(title, ignoreCase = true) } }
}

/** A song made by the app: Topic → Language → reference song → fitted lyrics → singer → audio. */
data class GeneratedSong(
    val topicId: String,
    val language: Lang,
    val referenceSong: CatalogSong,
    val lyrics: String,
    val stylePrompt: String,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
