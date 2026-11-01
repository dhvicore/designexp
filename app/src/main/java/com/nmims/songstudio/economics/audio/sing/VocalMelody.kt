package com.nmims.songstudio.economics.audio.sing

import com.nmims.songstudio.economics.audio.BeatComposer

/** Chooses one note per syllable: scale steps from a motif, chord tones on strong beats, a home note at section ends. */
object VocalMelody {
    const val SLOTS_PER_BAR = 8

    fun line(
        syllables: Int,
        firstBar: Int,
        slotSpacing: Int,
        chorus: Boolean,
        endOfSection: Boolean,
        harmony: BeatComposer.Harmony,
        centerMidi: Int,
        motif: IntArray,
    ): IntArray {
        if (syllables <= 0) return IntArray(0)
        val pool = (centerMidi - 5..centerMidi + 9).filter { it % 12 in harmony.scalePitchClasses }
        if (pool.isEmpty()) return IntArray(syllables) { centerMidi }

        val startTarget = if (chorus) centerMidi + 4 else centerMidi
        var idx = pool.indices.minBy { kotlin.math.abs(pool[it] - startTarget) }
        val notes = IntArray(syllables)
        for (i in 0 until syllables) {
            if (i > 0) idx = (idx + motif[(i - 1) % motif.size]).coerceIn(0, pool.lastIndex)
            val slot = i * slotSpacing
            val bar = firstBar + slot / SLOTS_PER_BAR
            val strong = slot % SLOTS_PER_BAR == 0 || slot % SLOTS_PER_BAR == SLOTS_PER_BAR / 2
            val last = i == syllables - 1
            if (strong || last) {
                val chord = harmony.chordForBar(bar)
                idx = snap(pool, idx) { it % 12 in chord }
                if (last && endOfSection && harmony.tonicPitchClass in chord) {
                    idx = snap(pool, idx) { it % 12 == harmony.tonicPitchClass }
                }
            }
            notes[i] = pool[idx]
        }
        return notes
    }

    private fun snap(pool: List<Int>, from: Int, ok: (Int) -> Boolean): Int {
        for (d in 0..pool.size) {
            if (from - d >= 0 && ok(pool[from - d])) return from - d
            if (from + d <= pool.lastIndex && ok(pool[from + d])) return from + d
        }
        return from
    }
}
