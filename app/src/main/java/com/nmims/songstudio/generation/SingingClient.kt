package com.nmims.songstudio.generation

import android.content.Context
import com.nmims.songstudio.data.SongChoice
import com.nmims.songstudio.data.Topic
import com.nmims.songstudio.playback.AudioFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class MelodyReference(val id: String, val title: String, val bpm: Int, val sha: String)
data class AdaptedLyrics(val lyrics: String, val note: String)

data class GeneratedSong(val audio: File, val mode: String)

object SingingClient {
    private const val SERVER = "http://10.0.2.2:8787"

    private fun json(path: String, body: JSONObject? = null): JSONObject {
        val connection = URL(SERVER + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8000; connection.readTimeout = 15000
            if (body != null) {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val result = JSONObject(stream?.bufferedReader()?.use { it.readText() } ?: "{}")
            check(code in 200..299) { result.optString("message", "Music server error ($code)") }
            return result
        } finally { connection.disconnect() }
    }

    suspend fun melodies(): Map<String, MelodyReference> = withContext(Dispatchers.IO) {
        val rows = json("/melodies").getJSONArray("melodies")
        (0 until rows.length()).map { rows.getJSONObject(it) }.associate {
            it.getString("id") to MelodyReference(it.getString("id"), it.optString("title"), it.optInt("bpm", 90), it.getString("audio_sha256"))
        }
    }

    suspend fun adapt(topic: Topic, category: String, choice: SongChoice, lyrics: String, reference: Boolean): AdaptedLyrics = withContext(Dispatchers.IO) {
        val request = request(topic, category, choice, lyrics, "Moderate", reference)
        val response = json("/adapt-lyrics", request)
        AdaptedLyrics(response.getString("lyrics"), response.optString("note"))
    }

    private fun request(topic: Topic, category: String, choice: SongChoice, lyrics: String, tempo: String, reference: Boolean): JSONObject {
        val baseBpm = if (category == "Lo-fi") 75 else if (category == "Bhajan") 80 else choice.song.bpm
        val bpm = (baseBpm * when (tempo) { "Slow" -> 0.85; "Upbeat" -> 1.12; else -> 1.0 }).toInt().coerceIn(55, 160)
        return JSONObject().put("topic", topic.title).put("song", choice.song.title).put("song_id", choice.id)
            .put("lyrics", lyrics).put("style_prompt", LyricPrompts.singingStyle(category, choice))
            .put("bpm", bpm).put("category", category).put("melody_mode", if (reference) "reference" else "original")
            .put("language", when (category) { "Hindi", "Bhajan" -> "hi"; "Marathi" -> "mr"; else -> "en" })
    }

    suspend fun generate(context: Context, key: String, topic: Topic, category: String,
        choice: SongChoice, lyrics: String, tempo: String, reference: Boolean, onStatus: (String) -> Unit): GeneratedSong {
        val request = request(topic, category, choice, lyrics, tempo, reference)
        val id = try { withContext(Dispatchers.IO) { json("/generate", request).getString("job_id") } }
        catch (e: java.io.IOException) { throw IllegalStateException("Start the Music Server on your Mac, keep it running, then retry. ${e.message.orEmpty().take(80)}") }
        repeat(300) {
            val result = withContext(Dispatchers.IO) { json("/jobs/$id") }
            onStatus(result.optString("message"))
            when (result.getString("status")) {
                "failed" -> error(result.getString("message"))
                "ready" -> {
                    val path = result.getString("audio_path")
                    check(Regex("/audio/[a-f0-9]{64}\\.mp3").matches(path)) { "Invalid generated audio response." }
                    val file = withContext(Dispatchers.IO) {
                        val target = AudioFiles.file(context, key)
                        val temp = File.createTempFile("generation-", ".audio", target.parentFile)
                        val connection = URL(SERVER + path).openConnection() as HttpURLConnection
                        try {
                            connection.connectTimeout = 10000; connection.readTimeout = 60000
                            check(connection.responseCode == 200) { "The generated song could not be downloaded." }
                            connection.inputStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
                            check(temp.length() > 1000 && temp.renameTo(target)) { "Could not save the generated song." }
                            target.resolveSibling(target.name + ".mode").writeText(result.optString("mode", "original_melody"))
                            target
                        } finally { connection.disconnect(); temp.delete() }
                    }
                    return GeneratedSong(file, result.optString("mode", "original_melody"))
                }
            }
            delay(2000)
        }
        error("The model is taking too long. Retry later; a completed generation can be reused.")
    }
}
