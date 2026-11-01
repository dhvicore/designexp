package com.nmims.songstudio.economics.audio

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.math.tanh

/** Final mastering: compress the dynamics, raise the level, then soft-limit so the song plays loud without clipping. */
object Loudness {
    private const val THRESHOLD = 0.2f
    private const val RATIO = 4f

    fun master(x: FloatArray, sampleRate: Int, targetRms: Float = 0.3f) {
        if (x.isEmpty()) return
        val peak = x.maxOf { abs(it) }
        if (peak < 1e-6f) return
        for (i in x.indices) x[i] /= peak

        val attack = exp(-1.0 / (0.005 * sampleRate)).toFloat()
        val release = exp(-1.0 / (0.15 * sampleRate)).toFloat()
        var env = 0f
        for (i in x.indices) {
            val level = abs(x[i])
            env = if (level > env) attack * env + (1 - attack) * level else release * env + (1 - release) * level
            if (env > THRESHOLD) {
                val compressed = THRESHOLD + (env - THRESHOLD) / RATIO
                x[i] *= compressed / env
            }
        }

        val rms = rms(x)
        if (rms < 1e-6f) return
        var gain = targetRms / rms
        repeat(3) {
            val reached = limitedRms(x, gain)
            if (reached > 1e-6f) gain *= (targetRms / reached).coerceIn(1f, 2f)
        }
        for (i in x.indices) x[i] = limit(x[i] * gain)
    }

    /** Soft limiter: nearly linear for normal levels, rounding peaks off just below full scale. */
    private fun limit(v: Float): Float = tanh(v.toDouble()).toFloat() * 0.97f

    private fun limitedRms(x: FloatArray, gain: Float): Float {
        var sum = 0.0
        for (v in x) {
            val y = limit(v * gain)
            sum += y * y
        }
        return sqrt(sum / x.size).toFloat()
    }

    fun rms(x: FloatArray): Float = if (x.isEmpty()) 0f else sqrt(x.fold(0.0) { a, v -> a + v * v } / x.size).toFloat()
}
