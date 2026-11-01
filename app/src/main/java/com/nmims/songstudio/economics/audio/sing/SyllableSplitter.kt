package com.nmims.songstudio.economics.audio.sing

import kotlin.math.sqrt

data class Syllable(val start: Int, val end: Int)

/** Cuts spoken audio into syllable-sized pieces at the dips between loudness peaks (vowels). */
object SyllableSplitter {

    fun split(x: FloatArray, sampleRate: Int): List<Syllable> {
        val hop = sampleRate / 100
        val frames = x.size / hop
        if (frames < 3) return emptyList()

        val raw = FloatArray(frames) { f ->
            var e = 0f
            for (j in f * hop until (f + 1) * hop) e += x[j] * x[j]
            sqrt(e / hop)
        }
        val env = FloatArray(frames) { f ->
            var s = 0f
            var n = 0
            for (k in f - 2..f + 2) if (k in 0 until frames) { s += raw[k]; n++ }
            s / n
        }
        val max = env.max()
        if (max <= 0f) return emptyList()
        val silent = max * 0.08f
        val first = env.indexOfFirst { it > silent }
        val last = env.indexOfLast { it > silent }
        if (first < 0 || last <= first) return emptyList()

        val peaks = mutableListOf<Int>()
        for (f in first + 1 until last) {
            if (env[f] < max * 0.25f || env[f] < env[f - 1] || env[f] <= env[f + 1]) continue
            val previous = peaks.lastOrNull()
            if (previous == null) {
                peaks += f
                continue
            }
            var dip = env[previous]
            for (k in previous..f) dip = minOf(dip, env[k])
            val separate = f - previous >= 8 && dip < 0.8f * minOf(env[previous], env[f])
            if (separate) peaks += f
            else if (env[f] > env[previous]) peaks[peaks.lastIndex] = f
        }
        if (peaks.isEmpty()) return listOf(Syllable(first * hop, (last + 1) * hop))

        val bounds = mutableListOf(first)
        for (i in 0 until peaks.lastIndex) {
            var at = peaks[i]
            for (k in peaks[i]..peaks[i + 1]) if (env[k] < env[at]) at = k
            bounds += at
        }
        bounds += last + 1

        return bounds.zipWithNext().map { (a, b) ->
            var s = a
            var e = b
            while (s < e - 1 && env[s] < silent) s++
            while (e > s + 1 && env[e - 1] < silent) e--
            Syllable(maxOf(0, s - 1) * hop, minOf(frames, e + 1) * hop)
        }
    }
}
