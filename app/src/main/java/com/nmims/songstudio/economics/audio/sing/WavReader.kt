package com.nmims.songstudio.economics.audio.sing

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads a 16-bit (or 8-bit) PCM WAV file as mono floats resampled to [targetRate]. */
object WavReader {

    fun read(file: File, targetRate: Int): FloatArray? = decode(file.readBytes(), targetRate)

    fun decode(bytes: ByteArray, targetRate: Int): FloatArray? {
        if (bytes.size < 12) return null
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null

        var pos = 12
        var channels = 1
        var rate = 0
        var bits = 16
        var dataStart = -1
        var dataSize = 0
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = buf.getInt(pos + 4)
            val body = pos + 8
            when (id) {
                "fmt " -> {
                    channels = buf.getShort(body + 2).toInt()
                    rate = buf.getInt(body + 4)
                    bits = buf.getShort(body + 14).toInt()
                }
                "data" -> {
                    dataStart = body
                    dataSize = if (size <= 0 || body + size > bytes.size) bytes.size - body else size
                }
            }
            if (dataStart >= 0) break
            if (size < 0) return null
            pos = body + size + (size and 1)
        }
        if (dataStart < 0 || rate <= 0 || channels <= 0) return null

        val bytesPerSample = bits / 8
        if (bytesPerSample != 1 && bytesPerSample != 2) return null
        val frames = dataSize / (bytesPerSample * channels)
        val mono = FloatArray(frames) { f ->
            var sum = 0f
            for (c in 0 until channels) {
                val at = dataStart + (f * channels + c) * bytesPerSample
                sum += if (bytesPerSample == 2) buf.getShort(at) / 32768f else ((bytes[at].toInt() and 0xFF) - 128) / 128f
            }
            sum / channels
        }
        return resample(mono, rate, targetRate)
    }

    fun resample(x: FloatArray, from: Int, to: Int): FloatArray {
        if (from == to || x.isEmpty()) return x
        val outLen = (x.size.toLong() * to / from).toInt()
        val step = from.toDouble() / to
        return FloatArray(outLen) { i ->
            val p = i * step
            val k = p.toInt()
            val frac = (p - k).toFloat()
            val a = x[minOf(k, x.lastIndex)]
            val b = x[minOf(k + 1, x.lastIndex)]
            a + (b - a) * frac
        }
    }
}
