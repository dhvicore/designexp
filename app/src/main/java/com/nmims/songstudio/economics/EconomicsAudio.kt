package com.nmims.songstudio.economics

import android.content.Context
import com.nmims.songstudio.economics.audio.*
import com.nmims.songstudio.economics.audio.sing.*
import com.nmims.songstudio.economics.data.CatalogSong
import com.nmims.songstudio.playback.LyricCue
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

data class EconomicsRecording(val file: File, val cues: List<LyricCue>)
object EconomicsAudio {
    private fun key(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun file(context: Context, lyrics: String, song: CatalogSong, seed: Int) =
        File(context.filesDir, "economics_audio").apply { mkdirs() }.resolve(key("v1|$seed|$song|$lyrics") + ".wav")
    fun cached(context: Context, lyrics: String, song: CatalogSong, seed: Int): EconomicsRecording? {
        val file = file(context, lyrics, song, seed)
        val timing = File(file.path + ".json")
        if (!file.exists() || !timing.exists()) return null
        return runCatching {
            val rows = JSONArray(timing.readText())
            EconomicsRecording(file, (0 until rows.length()).map { val r = rows.getJSONObject(it); LyricCue(r.getInt("start"), r.getInt("end"), r.getString("text")) })
        }.getOrNull()
    }
    /** TTS and DSP only. No server, Suno account or commercial melody download. */
    suspend fun generate(context: Context, lyrics: String, song: CatalogSong, seed: Int, status: (String) -> Unit): EconomicsRecording = withContext(Dispatchers.Main) {
        cached(context, lyrics, song, seed)?.let { return@withContext it }
        var chorus = false
        val lines = lyrics.lines().mapNotNull { raw ->
            val text = raw.trim()
            if (text.startsWith("[")) { chorus = listOf("chorus", "hook", "refrain", "ध्रुव", "धृपद", "स्थायी", "कोरस", "मुखड़ा", "मुखडा", "हुक").any { text.contains(it, true) }; null }
            else text.takeIf { it.isNotBlank() }?.let { it to chorus }
        }
        require(lines.isNotEmpty() && lines.size <= 100) { "Use between 1 and 100 lyric lines." }
        val voice = VoiceSynth(context)
        val voiceDir = File(context.cacheDir, "economics_voice").apply { mkdirs() }
        try {
            val spoken = lines.mapIndexed { i, (text, isChorus) ->
                ensureActive()
                status("Preparing ${song.lang.label} voice · line ${i + 1} of ${lines.size}")
                val wav = voiceDir.resolve(key("${song.lang.code}|$text") + ".wav")
                if (!wav.exists() || wav.length() < 44) {
                    check(voice.record(text, song.lang, wav)) { "The ${song.lang.label} voice could not generate audio. Install its voice data in Android Text-to-speech settings, then retry." }
                }
                val audio = withContext(Dispatchers.Default) { WavReader.read(wav, BeatComposer.SAMPLE_RATE) }
                check(audio != null && audio.isNotEmpty()) { wav.delete(); "The voice returned an unreadable recording. Please retry." }
                SpokenLine(audio, isChorus, i == lines.lastIndex || lines[i + 1].second != isChorus)
            }
            status("Arranging melody and mixing vocals…")
            withContext(Dispatchers.Default) {
                val result = SongRenderer.render(spoken, SongMood.of(song), seed)
                ensureActive()
                val rate = BeatComposer.SAMPLE_RATE
                val cues = lines.mapIndexed { i, line -> LyricCue(
                    (result.lineStarts[i].toLong() * 1000 / rate).toInt(),
                    ((result.lineStarts.getOrNull(i + 1) ?: result.pcm.size).toLong() * 1000 / rate).toInt(), line.first) }
                val destination = file(context, lyrics, song, seed)
                val tmp = File(destination.path + ".tmp")
                val size = result.pcm.size * 2
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                    .put("RIFF".toByteArray()).putInt(size + 36).put("WAVEfmt ".toByteArray()).putInt(16)
                    .putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
                    .put("data".toByteArray()).putInt(size).array()
                tmp.outputStream().buffered().use { out ->
                    out.write(header)
                    val buffer = ByteBuffer.allocate(8192).order(ByteOrder.LITTLE_ENDIAN)
                    for (sample in result.pcm) { if (buffer.remaining() < 2) { out.write(buffer.array(), 0, buffer.position()); buffer.clear() }; buffer.putShort(sample) }
                    out.write(buffer.array(), 0, buffer.position())
                }
                check(tmp.renameTo(destination)) { "Could not save the generated song." }
                File(destination.path + ".json").writeText(JSONArray(cues.map { JSONObject().put("start", it.startMs).put("end", it.endMs).put("text", it.text) }).toString())
                EconomicsRecording(destination, cues)
            }
        } finally { voice.release() }
    }
}
