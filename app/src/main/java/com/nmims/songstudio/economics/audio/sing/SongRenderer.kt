package com.nmims.songstudio.economics.audio.sing

import com.nmims.songstudio.economics.audio.BeatComposer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** One lyric line's spoken recording (null if the voice could not produce it). */
class SpokenLine(val audio: FloatArray?, val chorus: Boolean, val endOfSection: Boolean)

class RenderedSong(val pcm: ShortArray, val lineStarts: IntArray) {
    val lineCount get() = lineStarts.size

    fun lineAt(sample: Int): Int {
        var current = -1
        for (i in lineStarts.indices) if (lineStarts[i] <= sample) current = i
        return current
    }
}

/** Sings each spoken line on a melody over the generated beat and mixes everything into one track. */
object SongRenderer {
    private const val RATE = BeatComposer.SAMPLE_RATE

    fun render(lines: List<SpokenLine>, mood: SongMood, seed: Int): RenderedSong {
        val style = mood.style
        val bar = BeatComposer.barSamples(mood.bpm)
        val slot = bar / VocalMelody.SLOTS_PER_BAR
        val harmony = BeatComposer.harmony(style)
        val verseMotif = mood.verseMotif(seed)
        val chorusMotif = mood.chorusMotif(seed)

        class Analysed(val audio: FloatArray, val track: PitchTrack, val syllables: List<Syllable>)
        val analysed = lines.map { line ->
            line.audio?.let { a -> Analysed(a, PitchTracker.track(a, RATE), SyllableSplitter.split(a, RATE)) }
        }
        val voiced = analysed.filterNotNull().flatMap { it.track.voicedPeriods(0, it.audio.size) }
        val speechHz = PitchTracker.medianHz(voiced, RATE) ?: 200.0
        val centerMidi = (69 + 12 * ln(speechHz / 440.0) / ln(2.0)).roundToInt().plus(2).coerceIn(45, 72)

        data class Placed(val start: Int, val samples: FloatArray)
        val placed = ArrayList<Placed>()
        val lineStarts = IntArray(lines.size)
        var cursorBar = 1

        lines.forEachIndexed { i, line ->
            lineStarts[i] = cursorBar * bar
            val a = analysed[i]
            if (a == null || a.syllables.isEmpty()) {
                cursorBar += 2
                return@forEachIndexed
            }
            val n = a.syllables.size
            val spacing = if (n <= 4) 2 else 1
            val bars = maxOf(1, ceil((n * spacing + 2) / VocalMelody.SLOTS_PER_BAR.toDouble()).toInt())
            val notes = VocalMelody.line(
                syllables = n,
                firstBar = cursorBar,
                slotSpacing = spacing,
                chorus = line.chorus,
                endOfSection = line.endOfSection,
                harmony = harmony,
                centerMidi = centerMidi,
                motif = if (line.chorus) chorusMotif else verseMotif,
            )
            val lineSlots = bars * VocalMelody.SLOTS_PER_BAR
            a.syllables.forEachIndexed { s, syllable ->
                val startSlot = s * spacing
                val slots = if (s == n - 1) minOf(lineSlots - startSlot, spacing + 2) else spacing
                val length = (slots * slot * 0.92).toInt()
                val hz = 440.0 * 2.0.pow((notes[s] - 69) / 12.0)
                val sung = Psola.sing(a.audio, a.track, syllable, hz, length, RATE)
                placed += Placed(cursorBar * bar + startSlot * slot, sung)
            }
            cursorBar += bars
        }

        val totalBars = cursorBar + 1
        val total = totalBars * bar
        val vocal = FloatArray(total)
        placed.forEach { p ->
            for (k in p.samples.indices) if (p.start + k < total) vocal[p.start + k] += p.samples[k]
        }
        normalize(vocal, 1f)
        echo(vocal, (0.12 * RATE).toInt(), 0.22f)
        normalize(vocal, 1f)

        val loop = BeatComposer.compose(style, seed, mood.bpm)
        com.nmims.songstudio.economics.audio.Loudness.master(vocal, RATE, targetRms = 0.24f)
        val mix = FloatArray(total) { i -> loop[i % loop.size] / 32768f * 0.34f + vocal[i] * 0.8f }
        com.nmims.songstudio.economics.audio.Loudness.master(mix, RATE, targetRms = 0.3f)
        for (i in 0 until bar) mix[total - 1 - i] *= i.toFloat() / bar
        return RenderedSong(ShortArray(total) { (mix[it] * Short.MAX_VALUE).toInt().toShort() }, lineStarts)
    }

    private fun echo(x: FloatArray, delay: Int, feedback: Float) {
        for (i in delay until x.size) x[i] += x[i - delay] * feedback
    }

    private fun normalize(x: FloatArray, peak: Float) {
        val max = x.maxOfOrNull { abs(it) } ?: return
        if (max <= 1e-6f) return
        val g = peak / max
        for (i in x.indices) x[i] *= g
    }
}
