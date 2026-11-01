package com.nmims.songstudio.economics.audio.sing

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Time-domain PSOLA: re-places pitch-period grains of a spoken syllable so it is held
 * at one musical pitch for a chosen length — this is what turns speech into sung notes.
 */
object Psola {
    private const val UNVOICED_HZ = 180

    fun sing(
        x: FloatArray,
        track: PitchTrack,
        syllable: Syllable,
        targetHz: Double,
        outLength: Int,
        sampleRate: Int,
    ): FloatArray {
        val out = FloatArray(outLength)
        val start = syllable.start.coerceIn(0, x.size)
        val end = syllable.end.coerceIn(start, x.size)
        if (end - start < 32 || outLength < 32) return out

        val marks = analysisMarks(x, track, start, end, sampleRate)
        if (marks.isEmpty()) return out

        val srcLen = end - start
        val onset = minOf((0.04 * sampleRate).toInt(), srcLen / 4)
        val stretch = outLength > srcLen
        val vibratoStart = 0.15 * sampleRate

        var t = 0.0
        while (t < outLength) {
            val analysisTime = when {
                !stretch -> start + t * srcLen / outLength
                t < onset -> start + t
                else -> start + onset + (t - onset) * (srcLen - onset) / (outLength - onset)
            }
            val m = nearest(marks, analysisTime)
            val sourcePeriod = track.periodAt(m).takeIf { it > 0 }
            val p = sourcePeriod ?: (sampleRate / UNVOICED_HZ)

            val vibrato = if (t > vibratoStart) 0.012 * sin(2 * PI * 5.5 * t / sampleRate) else 0.0
            val step = if (sourcePeriod != null) sampleRate / (targetHz * (1 + vibrato)) else p.toDouble()
            val gain = (step / p).coerceIn(0.5, 2.0).toFloat()

            val center = t.toInt()
            for (k in -p until p) {
                val src = m + k
                val dst = center + k
                if (src !in 0 until x.size || dst !in 0 until outLength) continue
                val w = (0.5 - 0.5 * cos(PI * (k + p) / p)).toFloat()
                out[dst] += x[src] * w * gain
            }
            t += step
        }
        fadeEdges(out, (0.006 * sampleRate).toInt())
        return out
    }

    private fun analysisMarks(x: FloatArray, track: PitchTrack, start: Int, end: Int, sampleRate: Int): IntArray {
        val marks = ArrayList<Int>()
        val firstPeriod = track.periodAt(start).takeIf { it > 0 } ?: (sampleRate / UNVOICED_HZ)
        var m = start
        for (j in start until minOf(end, start + firstPeriod)) if (abs(x[j]) > abs(x[m])) m = j
        while (m < end) {
            marks += m
            val voiced = track.periodAt(m)
            val p = if (voiced > 0) voiced else sampleRate / UNVOICED_HZ
            var next = m + p
            if (voiced > 0) {
                val lo = maxOf(m + p * 3 / 4, 0)
                val hi = minOf(m + p * 5 / 4, end - 1)
                var best = next.coerceIn(lo, maxOf(lo, hi))
                for (j in lo..hi) if (x[j] > x[best]) best = j
                next = best
            }
            if (next <= m) next = m + 1
            m = next
        }
        return marks.toIntArray()
    }

    private fun nearest(marks: IntArray, time: Double): Int {
        var lo = 0
        var hi = marks.lastIndex
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (marks[mid] < time) lo = mid + 1 else hi = mid
        }
        return if (lo > 0 && abs(marks[lo - 1] - time) < abs(marks[lo] - time)) marks[lo - 1] else marks[lo]
    }

    private fun fadeEdges(out: FloatArray, len: Int) {
        val n = minOf(len, out.size / 2)
        for (i in 0 until n) {
            val g = i.toFloat() / n
            out[i] *= g
            out[out.lastIndex - i] *= g
        }
    }
}
