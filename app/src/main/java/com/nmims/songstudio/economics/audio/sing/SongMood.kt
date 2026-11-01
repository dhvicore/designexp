package com.nmims.songstudio.economics.audio.sing

import com.nmims.songstudio.economics.audio.BeatStyle
import com.nmims.songstudio.economics.data.CatalogSong
import com.nmims.songstudio.economics.data.GenreStyles

/** Musical character taken from the chosen song's style and tempo (not its actual melody). */
class SongMood(val genreId: String, val style: BeatStyle, val bpm: Int, private val steps: IntArray) {

    fun verseMotif(seed: Int): IntArray = rotate(steps, seed)

    fun chorusMotif(seed: Int): IntArray = rotate(steps.map { -it }.reversed().toIntArray(), seed / 3)

    private fun rotate(a: IntArray, seed: Int): IntArray {
        val shift = Math.floorMod(seed, a.size)
        return IntArray(a.size) { a[(it + shift) % a.size] }
    }

    companion object {
        fun of(song: CatalogSong, style: BeatStyle = GenreStyles.beatStyle(song.genreId)) =
            SongMood(song.genreId, style, song.bpm.coerceIn(60, 150), GenreStyles.melodySteps(song.genreId))
    }
}
