package com.nmims.songstudio.economics.audio.sing

import kotlin.math.sqrt

/** Pitch period per 10 ms frame (0 = unvoiced), found with a simplified YIN search on a 2x-decimated signal. */
class PitchTrack(val hop: Int, private val periods: IntArray) {
    fun periodAt(sample: Int): Int {
        if (periods.isEmpty()) return 0
        return periods[(sample / hop).coerceIn(0, periods.lastIndex)]
    }

    fun voicedPeriods(from: Int, to: Int): List<Int> =
        (from / hop until minOf(periods.size, to / hop + 1)).map { periods[it] }.filter { it > 0 }
}

object PitchTracker {
    private const val MIN_HZ = 70
    private const val MAX_HZ = 400
    private const val THRESHOLD = 0.15f

    fun track(x: FloatArray, sampleRate: Int): PitchTrack {
        val hop = sampleRate / 100
        val y = FloatArray(x.size / 2) { (x[2 * it] + x[2 * it + 1]) * 0.5f }
        val rate = sampleRate / 2
        val minLag = rate / MAX_HZ
        val maxLag = rate / MIN_HZ
        val window = maxLag * 2
        val peak = x.maxOfOrNull { kotlin.math.abs(it) } ?: 0f
        val silence = peak * 0.04f
        val frames = x.size / hop
        val periods = IntArray(frames)
        val diff = FloatArray(maxLag + 1)

        for (f in 0 until frames) {
            val start = f * hop / 2
            if (start + window + maxLag >= y.size) break
            var energy = 0f
            for (j in start until start + window) energy += y[j] * y[j]
            if (sqrt(energy / window) < silence) continue

            for (tau in 1..maxLag) {
                var d = 0f
                for (j in start until start + window) {
                    val delta = y[j] - y[j + tau]
                    d += delta * delta
                }
                diff[tau] = d
            }
            var running = 0f
            var chosen = 0
            for (tau in 1..maxLag) {
                running += diff[tau]
                val normalized = if (running == 0f) 1f else diff[tau] * tau / running
                if (tau >= minLag && normalized < THRESHOLD) {
                    var best = tau
                    while (best + 1 <= maxLag && diff[best + 1] < diff[best]) best++
                    chosen = best
                    break
                }
            }
            periods[f] = chosen * 2
        }
        return PitchTrack(hop, periods)
    }

    fun medianHz(periods: List<Int>, sampleRate: Int): Double? {
        if (periods.isEmpty()) return null
        val sorted = periods.sorted()
        return sampleRate.toDouble() / sorted[sorted.size / 2]
    }
}
