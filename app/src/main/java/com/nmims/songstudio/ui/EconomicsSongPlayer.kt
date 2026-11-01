package com.nmims.songstudio.ui

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nmims.songstudio.economics.EconomicsAudio
import com.nmims.songstudio.economics.data.CatalogSong
import com.nmims.songstudio.playback.SongPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun EconomicsSongPlayer(lyrics: String, song: CatalogSong, topicId: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val seed = topicId * 1009 + song.title.hashCode()
    var recording by remember(lyrics, song, topicId) { mutableStateOf(EconomicsAudio.cached(context, lyrics, song, seed)) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Text("On-device study song · original melody in the selected style. Voice quality depends on the installed Android voice.", style = MaterialTheme.typography.bodySmall)
    recording?.let { SongPlayer(it.file, 0, caption = "Generated on this device", cues = it.cues, suppliedLyrics = lyrics) }
    if (busy) { LinearProgressIndicator(); Text(status) }
    Button(enabled = !busy, onClick = {
        busy = true; error = ""
        scope.launch {
            try { recording = EconomicsAudio.generate(context, lyrics, song, seed) { status = it } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not create audio." }
            finally { busy = false }
        }
    }) { Text(if (busy) "Creating song…" else if (recording == null) "Generate on device" else "Load saved song") }
    if (error.isNotBlank()) {
        Text(error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) } }) { Text("Open Android voice settings") }
    }
    if (recording == null) Text(lyrics)
}
