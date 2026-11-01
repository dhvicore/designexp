package com.nmims.songstudio.economics.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Loads the three subjects, the song catalog and the research song list from app assets. */
object StudyRepository {

    fun load(context: Context): StudyCatalog {
        fun asset(name: String) = context.assets.open("economics/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }

        val subjects = JSONObject(asset("subjects.json")).getJSONArray("subjects").objects().map { s ->
            val subjectId = s.getString("id")
            Subject(
                id = subjectId,
                name = s.getJSONObject("name").localized(),
                units = s.getJSONArray("units").objects().map { u ->
                    val unitNo = u.getInt("number")
                    StudyUnit(
                        number = unitNo,
                        name = u.getJSONObject("name").localized(),
                        topics = u.getJSONArray("topics").objects().map { t -> topic(t, subjectId, unitNo) },
                    )
                },
            )
        }

        val catalogJson = JSONObject(asset("song_catalog.json"))
        val genres = Lang.entries.associateWith { lang ->
            val byId = catalogJson.optJSONObject(lang.code) ?: JSONObject()
            byId.keys().asSequence().map { id ->
                val g = byId.getJSONObject(id)
                Genre(
                    id = id,
                    lang = lang,
                    name = g.getString("name"),
                    instruments = g.optString("instruments"),
                    vocal = g.optString("vocal"),
                    sunoTags = g.optString("suno"),
                    songs = g.getJSONArray("songs").objects().map { song ->
                        CatalogSong(
                            title = song.getString("title"),
                            artist = song.optString("artist"),
                            year = if (song.isNull("year")) null else song.optInt("year"),
                            bpm = song.optInt("bpm", GenreStyles.defaultBpm(id)),
                            mood = song.optString("mood"),
                            publicDomain = song.optBoolean("public_domain", false),
                            genreId = id,
                            lang = lang,
                        )
                    },
                )
            }.toList()
        }

        val library = JSONArray(asset("economics_songs.json")).objects().map { s ->
            Song(
                title = s.getString("title"),
                artist = s.getString("artist"),
                year = s.getInt("year"),
                language = s.getString("language"),
                genre = s.getString("genre"),
                rank = s.getInt("rank"),
                movie = s.optString("movie", ""),
            )
        }
        return StudyCatalog(subjects, library, genres)
    }

    private fun topic(t: JSONObject, subjectId: String, unitNo: Int): Topic {
        val lyricsJson = t.getJSONObject("lyrics")
        val songsJson = t.getJSONObject("songs")
        return Topic(
            id = t.getString("id"),
            subjectId = subjectId,
            unitNo = unitNo,
            name = t.getJSONObject("name").localized(),
            description = t.getJSONObject("description").localized(),
            keywords = t.getJSONObject("keywords").localizedList(),
            formula = if (t.isNull("formula")) null else t.getString("formula"),
            lyrics = Lang.entries.mapNotNull { l -> lyricsJson.optString(l.code).takeIf { it.isNotBlank() }?.let { l to it } }.toMap(),
            songs = Lang.entries.associateWith { l ->
                songsJson.optJSONArray(l.code)?.objects()?.map {
                    SongRef(it.getString("title"), it.optString("artist"), if (it.isNull("genre")) null else it.getString("genre"))
                }.orEmpty()
            },
            reason = if (t.isNull("reason")) null else t.getString("reason"),
        )
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONObject.localized() = Localized(getString("en"), getString("hi"), getString("mr"))

    private fun JSONObject.localizedList() = LocalizedList(
        optJSONArray("en")?.strings().orEmpty(),
        optJSONArray("hi")?.strings().orEmpty(),
        optJSONArray("mr")?.strings().orEmpty(),
    )
}
