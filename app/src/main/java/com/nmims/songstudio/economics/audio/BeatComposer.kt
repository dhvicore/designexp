package com.nmims.songstudio.economics.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class BeatStyle(val label: String, val bpm: Int) {
    LOFI("Lo-fi", 76),
    POP("Pop", 104),
    RAGA("Raga", 90),
    UPBEAT("Upbeat", 124),
    BOLLY("Bollywood", 110),
}

/**
 * Renders a seamless 4-bar instrumental loop entirely on the device.
 * The seed (from the topic id) varies the melody so every topic gets its own tune.
 */
object BeatComposer {
    const val SAMPLE_RATE = 22050
    private const val BARS = 4
    private const val STEPS_PER_BAR = 8

    private enum class Voice { KEYS, TRIANGLE, PLUCK, SQUARE }

    private class Spec(
        val chords: List<IntArray>,
        val scale: IntArray,
        val voice: Voice,
        val restChance: Float,
        val noteDecay: Double,
        val drone: IntArray? = null,
    )

    /** Key information the sung melody needs so it fits the beat. */
    class Harmony(val chords: List<IntArray>, val scalePitchClasses: Set<Int>, val tonicPitchClass: Int) {
        fun chordForBar(bar: Int): Set<Int> = chords[(bar % BARS) % chords.size].map { it % 12 }.toSet()
    }

    fun harmony(style: BeatStyle): Harmony {
        val spec = spec(style)
        return Harmony(spec.chords, spec.scale.map { it % 12 }.toSet(), spec.scale.first() % 12)
    }

    fun barSamples(bpm: Int): Int = (4 * 60.0 / bpm * SAMPLE_RATE).toInt()

    const val LOOP_BARS = BARS

    private fun spec(style: BeatStyle) = when (style) {
        BeatStyle.LOFI -> Spec(
            chords = listOf(intArrayOf(57, 60, 64, 67), intArrayOf(53, 57, 60, 64), intArrayOf(48, 52, 55, 59), intArrayOf(55, 59, 62, 65)),
            scale = intArrayOf(69, 72, 74, 76, 79, 81),
            voice = Voice.KEYS, restChance = 0.35f, noteDecay = 3.5,
        )
        BeatStyle.POP -> Spec(
            chords = listOf(intArrayOf(60, 64, 67), intArrayOf(55, 59, 62), intArrayOf(57, 60, 64), intArrayOf(53, 57, 60)),
            scale = intArrayOf(72, 74, 76, 79, 81, 84),
            voice = Voice.TRIANGLE, restChance = 0.2f, noteDecay = 5.0,
        )
        BeatStyle.RAGA -> Spec(
            chords = listOf(intArrayOf(50, 57, 62)),
            scale = intArrayOf(62, 64, 66, 69, 71, 74, 76),
            voice = Voice.PLUCK, restChance = 0.2f, noteDecay = 4.0,
            drone = intArrayOf(38, 45, 50),
        )
        BeatStyle.UPBEAT -> Spec(
            chords = listOf(intArrayOf(55, 59, 62), intArrayOf(52, 55, 59), intArrayOf(60, 64, 67), intArrayOf(62, 66, 69)),
            scale = intArrayOf(67, 69, 71, 74, 76, 79),
            voice = Voice.SQUARE, restChance = 0.15f, noteDecay = 6.0,
        )
        // A minor: Am – F – G – Em, the minor-key loop common in Bollywood dance tracks.
        BeatStyle.BOLLY -> Spec(
            chords = listOf(intArrayOf(57, 60, 64), intArrayOf(53, 57, 60), intArrayOf(55, 59, 62), intArrayOf(52, 55, 59)),
            scale = intArrayOf(69, 71, 72, 74, 76, 77, 79, 81),
            voice = Voice.PLUCK, restChance = 0.18f, noteDecay = 5.5,
        )
    }

    fun compose(style: BeatStyle, seed: Int, bpm: Int = style.bpm): ShortArray {
        val spec = spec(style)
        val beatSec = 60.0 / bpm
        val stepSec = beatSec / 2
        val barSamples = barSamples(bpm)
        val total = barSamples * BARS
        val mix = FloatArray(total)
        val rnd = Random(seed * 7919 + style.ordinal)

        for (bar in 0 until BARS) {
            val chord = spec.chords[bar % spec.chords.size]
            val start = bar * barSamples
            if (spec.drone == null) {
                chord.forEach { pad(mix, start, barSamples, hz(it), 0.05f, style == BeatStyle.LOFI) }
                val bassNote = max(36, chord.first() - 12)
                bass(mix, start, barSamples / 2, hz(bassNote))
                bass(mix, start + barSamples / 2, barSamples / 2, hz(bassNote))
            }
        }
        spec.drone?.forEach { note -> drone(mix, hz(note), if (note < 45) 0.09f else 0.06f) }

        var index = rnd.nextInt(spec.scale.size)
        val stepSamples = (stepSec * SAMPLE_RATE).toInt()
        val steps = BARS * STEPS_PER_BAR
        var step = 0
        while (step < steps) {
            val length = if (rnd.nextFloat() < 0.25f && step % 2 == 0) 2 else 1
            if (rnd.nextFloat() >= spec.restChance) {
                index = (index + rnd.nextInt(-2, 3)).coerceIn(0, spec.scale.lastIndex)
                note(mix, step * stepSamples, stepSamples * length, hz(spec.scale[index]), spec.voice, spec.noteDecay)
            }
            step += length
        }

        for (s in 0 until steps) {
            val at = s * stepSamples
            val inBar = s % STEPS_PER_BAR
            val swing = if (style == BeatStyle.LOFI && inBar % 2 == 1) (stepSamples * 0.18).toInt() else 0
            when (style) {
                BeatStyle.LOFI -> {
                    if (inBar == 0 || inBar == 5) kick(mix, at)
                    if (inBar == 2 || inBar == 6) snare(mix, at, 0.22f)
                    hat(mix, at + swing, 0.07f)
                }
                BeatStyle.POP -> {
                    if (inBar == 0 || inBar == 3 || inBar == 4) kick(mix, at)
                    if (inBar == 2 || inBar == 6) snare(mix, at, 0.3f)
                    hat(mix, at, 0.09f)
                }
                BeatStyle.UPBEAT -> {
                    if (inBar % 2 == 0) kick(mix, at)
                    if (inBar == 2 || inBar == 6) snare(mix, at, 0.32f)
                    if (inBar % 2 == 1) hat(mix, at, 0.13f, open = true)
                }
                // Dhol "chaal": low strokes on 1, the "and" of 2 and 4; sharp slaps on 2 and 4; claps on the backbeat.
                BeatStyle.BOLLY -> {
                    when (inBar) {
                        0, 3, 6 -> { kick(mix, at); dhol(mix, at, low = true) }
                        2, 4 -> dhol(mix, at, low = false)
                        7 -> dhol(mix, at, low = false, gain = 0.18f)
                    }
                    if (inBar == 2 || inBar == 6) clap(mix, at)
                    hat(mix, at, 0.06f)
                }
                BeatStyle.RAGA -> when (inBar) {
                    0, 4 -> { tablaBass(mix, at); tablaTreble(mix, at, 0.22f, ring = 10.0) }
                    2, 6 -> { tablaBass(mix, at, 0.35f); tablaTreble(mix, at, 0.2f, ring = 5.0) }
                    else -> tablaTreble(mix, at, 0.12f, ring = 16.0)
                }
            }
        }

        if (style == BeatStyle.LOFI) {
            crackle(mix, rnd)
            lowPass(mix, 0.35f)
        }
        return toPcm(mix)
    }

    private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)

    /** Adds a sample, wrapping past the end so ringing tails continue at the loop start. */
    private fun add(mix: FloatArray, i: Int, v: Float) {
        mix[i % mix.size] += v
    }

    private fun pad(mix: FloatArray, start: Int, len: Int, f: Double, gain: Float, detune: Boolean) {
        val attack = (0.3 * SAMPLE_RATE).toInt()
        val release = (0.4 * SAMPLE_RATE).toInt()
        for (i in 0 until len + release) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = when {
                i < attack -> i.toDouble() / attack
                i < len -> 1.0
                else -> 1.0 - (i - len).toDouble() / release
            }
            var v = sin(2 * PI * f * t)
            if (detune) v = 0.6 * v + 0.4 * sin(2 * PI * f * 1.003 * t)
            add(mix, start + i, (v * env * gain).toFloat())
        }
    }

    /** Tanpura-like drone snapped to whole cycles per loop so the repeat is click-free. */
    private fun drone(mix: FloatArray, f: Double, gain: Float) {
        val loopSec = mix.size.toDouble() / SAMPLE_RATE
        val fq = Math.round(f * loopSec) / loopSec
        val shimmer = Math.max(1L, Math.round(loopSec)) / loopSec
        for (i in mix.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val p = 2 * PI * fq * t
            val v = sin(p) + 0.35 * sin(2 * p) + 0.2 * sin(3 * p)
            val swell = 0.75 + 0.25 * sin(2 * PI * shimmer * t)
            mix[i] += (v * swell * gain).toFloat()
        }
    }

    private fun bass(mix: FloatArray, start: Int, len: Int, f: Double) {
        for (i in 0 until len) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = min(1.0, i / 200.0) * exp(-t * 2.2)
            add(mix, start + i, ((sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t)) * env * 0.22).toFloat())
        }
    }

    private fun note(mix: FloatArray, start: Int, len: Int, f: Double, voice: Voice, decay: Double) {
        val ring = (len * 1.6).toInt()
        for (i in 0 until ring) {
            val t = i.toDouble() / SAMPLE_RATE
            val p = 2 * PI * f * t
            val env = min(1.0, i / 120.0) * exp(-t * decay)
            val v = when (voice) {
                Voice.KEYS -> sin(p) + 0.25 * sin(2 * p) * exp(-t * 8)
                Voice.TRIANGLE -> asin(sin(p)) * 2 / PI
                Voice.PLUCK -> (sin(p) + 0.5 * sin(2 * p) + 0.33 * sin(3 * p) + 0.25 * sin(4 * p)) * exp(-t * 3) * 0.7
                Voice.SQUARE -> (sin(p) + sin(3 * p) / 3 + sin(5 * p) / 5) * 0.8
            }
            add(mix, start + i, (v * env * 0.2).toFloat())
        }
    }

    private fun kick(mix: FloatArray, start: Int) {
        var phase = 0.0
        for (i in 0 until (0.35 * SAMPLE_RATE).toInt()) {
            val t = i.toDouble() / SAMPLE_RATE
            phase += 2 * PI * (50 + 70 * exp(-t * 25)) / SAMPLE_RATE
            add(mix, start + i, (sin(phase) * exp(-t * 9) * 0.75).toFloat())
        }
    }

    private val noise = Random(42)

    private fun snare(mix: FloatArray, start: Int, gain: Float) {
        for (i in 0 until (0.2 * SAMPLE_RATE).toInt()) {
            val t = i.toDouble() / SAMPLE_RATE
            val v = (noise.nextDouble(-1.0, 1.0) * exp(-t * 20)) + 0.5 * sin(2 * PI * 180 * t) * exp(-t * 30)
            add(mix, start + i, (v * gain).toFloat())
        }
    }

    private fun hat(mix: FloatArray, start: Int, gain: Float, open: Boolean = false) {
        var prev = 0.0
        val decay = if (open) 25.0 else 60.0
        for (i in 0 until (0.12 * SAMPLE_RATE).toInt()) {
            val t = i.toDouble() / SAMPLE_RATE
            val n = noise.nextDouble(-1.0, 1.0)
            add(mix, start + i, ((n - prev) * exp(-t * decay) * gain).toFloat())
            prev = n
        }
    }

    /** Dhol stroke: a pitched membrane thump (low side) or a tight crack (high side). */
    private fun dhol(mix: FloatArray, start: Int, low: Boolean, gain: Float = 0.3f) {
        var phase = 0.0
        val base = if (low) 72.0 else 260.0
        val decay = if (low) 9.0 else 26.0
        for (i in 0 until (0.35 * SAMPLE_RATE).toInt()) {
            val t = i.toDouble() / SAMPLE_RATE
            phase += 2 * PI * (base + base * 0.8 * exp(-t * 40)) / SAMPLE_RATE
            val skin = if (low) 0.0 else noise.nextDouble(-1.0, 1.0) * exp(-t * 70) * 0.4
            add(mix, start + i, ((sin(phase) + skin) * exp(-t * decay) * gain * (if (low) 1.6f else 1f)).toFloat())
        }
    }

    private fun clap(mix: FloatArray, start: Int) {
        for (burst in 0 until 3) {
            val s = start + burst * (0.012 * SAMPLE_RATE).toInt()
            for (i in 0 until (0.12 * SAMPLE_RATE).toInt()) {
                val t = i.toDouble() / SAMPLE_RATE
                add(mix, s + i, (noise.nextDouble(-1.0, 1.0) * exp(-t * (if (burst == 2) 25 else 90)) * 0.16).toFloat())
            }
        }
    }

    private fun tablaBass(mix: FloatArray, start: Int, gain: Float = 0.5f) {
        var phase = 0.0
        for (i in 0 until (0.45 * SAMPLE_RATE).toInt()) {
            val t = i.toDouble() / SAMPLE_RATE
            phase += 2 * PI * (100 + 45 * exp(-t * 12)) / SAMPLE_RATE
            add(mix, start + i, (sin(phase) * exp(-t * 7) * gain).toFloat())
        }
    }

    private fun tablaTreble(mix: FloatArray, start: Int, gain: Float, ring: Double) {
        for (i in 0 until (0.4 * SAMPLE_RATE).toInt()) {
            val t = i.toDouble() / SAMPLE_RATE
            val v = sin(2 * PI * 520 * t) + 0.4 * sin(2 * PI * 1040 * t) + 0.2 * sin(2 * PI * 1580 * t)
            add(mix, start + i, (v * exp(-t * ring) * gain).toFloat())
        }
    }

    private fun crackle(mix: FloatArray, rnd: Random) {
        for (i in mix.indices) {
            mix[i] += (rnd.nextFloat() - 0.5f) * 0.006f
            if (rnd.nextFloat() < 0.0004f) {
                val amp = (rnd.nextFloat() - 0.5f) * 0.25f
                for (k in 0 until 40) add(mix, i + k, amp * exp(-k / 8.0).toFloat())
            }
        }
    }

    private fun lowPass(mix: FloatArray, a: Float) {
        var y = mix.last()
        for (i in mix.indices) {
            y += a * (mix[i] - y)
            mix[i] = y
        }
    }

    private fun toPcm(mix: FloatArray): ShortArray {
        Loudness.master(mix, SAMPLE_RATE, targetRms = 0.26f)
        return ShortArray(mix.size) { (mix[it] * Short.MAX_VALUE).toInt().toShort() }
    }
}
