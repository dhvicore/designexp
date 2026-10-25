package com.nmims.songstudio.playback

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nmims.songstudio.ui.Lavender
import com.nmims.songstudio.ui.Panel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

data class SavedSunoSong(
    val topicId: Int, val category: String, val songId: String, val title: String,
    val assetPath: String, val lyrics: String, val shareUrl: String, val cues: List<LyricCue> = emptyList()
)

/** Only the explicitly supplied recordings are eligible for saved-song playback. */
object SunoDemo {
    fun catalog(context: android.content.Context): List<SavedSunoSong> {
        val rows = org.json.JSONArray(context.assets.open("demos/catalog.json").bufferedReader().use { it.readText() })
        return (0 until rows.length()).map { index ->
            val r = rows.getJSONObject(index)
            val timings = r.optJSONArray("cues")
            val cues = if (timings == null) emptyList() else (0 until timings.length()).map { n ->
                val cue = timings.getJSONObject(n)
                LyricCue(cue.getInt("startMs"), cue.getInt("endMs"), cue.getString("text"))
            }
            SavedSunoSong(r.getInt("topicId"), r.getString("category"), r.getString("songId"),
                r.getString("title"), r.getString("assetPath"), r.getString("lyrics"), r.optString("shareUrl"), cues)
        }
    }

    suspend fun bundledAudio(context: android.content.Context, song: SavedSunoSong): File? = withContext(Dispatchers.IO) {
        val input = try { context.assets.open(song.assetPath) } catch (_: FileNotFoundException) {
            return@withContext null
        }
        val target = File(context.cacheDir, song.assetPath.substringAfterLast('/'))
        val temporary = File.createTempFile("suno-demo-", ".mp3", context.cacheDir)
        try {
            input.use { source -> temporary.outputStream().use { source.copyTo(it) } }
            check(temporary.length() > 0 && temporary.renameTo(target)) { "The saved demo audio could not be prepared." }
            target
        } finally { temporary.delete() }
    }
}

@Composable
fun SunoDemoPlayer(song: SavedSunoSong) {
    val context = LocalContext.current
    var audio by remember(song.assetPath) { mutableStateOf<File?>(null) }
    var checking by remember(song.assetPath) { mutableStateOf(true) }
    var error by remember(song.assetPath) { mutableStateOf("") }
    LaunchedEffect(song.assetPath) {
        try { audio = SunoDemo.bundledAudio(context.applicationContext, song) }
        catch (_: Exception) { error = "The saved demo audio could not be opened." }
        finally { checking = false }
    }
        if (checking) Text("Checking for saved audio…")
        else if (audio == null) Text("Listen online using Suno’s player. Offline audio has not been added yet.")
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (song.shareUrl.isNotBlank()) {
        OutlinedButton(onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(song.shareUrl))) }
                .onFailure { error = "No app can open this link. Install or enable a web browser." }
        }, modifier = Modifier.fillMaxWidth()) { Text("Listen on Suno ↗") }
        Text("Opens Suno or your browser; internet is required.", style = MaterialTheme.typography.bodySmall)
        }
    audio?.let { SongPlayer(it, 0, "Saved Suno demo recording. Plays on this device without Colab.", song.cues, song.lyrics) }
}
