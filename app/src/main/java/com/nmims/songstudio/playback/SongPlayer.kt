package com.nmims.songstudio.playback

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import com.nmims.songstudio.ui.Accent
import android.content.Context
import android.media.AudioManager
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nmims.songstudio.ui.Lime
import com.nmims.songstudio.ui.Panel
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SongPlayer(file: File, revision: Int, caption: String = "Vocals and music created from your educational lyrics.", cues: List<LyricCue> = emptyList(), suppliedLyrics: String = "") {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var mediaVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var ready by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableFloatStateOf(0f) }
    var seeking by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf(false) }
    var duration by remember { mutableFloatStateOf(1f) }
    var error by remember { mutableStateOf("") }
    DisposableEffect(file.absolutePath, revision) {
        val p = LocalSongEngine.create(context.applicationContext, file)
        player = p; ready = false; playing = false; error = ""; position = 0f; scrubbing = false; seeking = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                ready = state == Player.STATE_READY || state == Player.STATE_ENDED
                if (p.duration > 0) duration = p.duration.toFloat()
                if (state == Player.STATE_ENDED) { playing = false; position = duration }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                position = newPosition.positionMs.toFloat(); seeking = false
            }
            override fun onPlayerError(failure: PlaybackException) {
                ready = false; playing = false; error = "This downloaded audio could not be played. Please try reopening the song."
            }
        }
        p.addListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (event == Lifecycle.Event.ON_STOP && playing) { p.pause(); playing = false }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); p.removeListener(listener); p.release(); player = null; ready = false; playing = false }
    }
    LaunchedEffect(playing, file.absolutePath, revision) {
        while (playing) {
            if (!scrubbing && !seeking) position = player?.currentPosition?.toFloat() ?: 0f
            delay(200)
        }
    }
    fun time(ms: Float): String { val seconds = ms.toInt() / 1000; return "%d:%02d".format(seconds / 60, seconds % 60) }
    Panel(Lime) {
        Canvas(Modifier.fillMaxWidth().height(48.dp)) {
            val count = 45
            val step = size.width / count
            repeat(count) { index ->
                val envelope = (0.22f + 0.78f * kotlin.math.abs(kotlin.math.sin(index * 0.67))).toFloat()
                val half = size.height * .43f * envelope
                drawLine(Accent.copy(alpha = .35f + envelope * .35f),
                    Offset(step * index + step / 2, size.height / 2 - half),
                    Offset(step * index + step / 2, size.height / 2 + half),
                    strokeWidth = step * .44f, cap = StrokeCap.Round)
            }
        }
        Text("YOUR GENERATED SONG", style = MaterialTheme.typography.labelSmall)
        Text("Ready to listen", style = MaterialTheme.typography.titleLarge)
        Text(caption, style = MaterialTheme.typography.bodySmall)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Text("Device media volume: ${mediaVolume * 100 / maxVolume}%", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            mediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }, enabled = mediaVolume < maxVolume) { Text("Increase device volume") }
        Slider(value = position.coerceIn(0f, duration), onValueChange = { scrubbing = true; position = it }, onValueChangeFinished = {
            seeking = true
            player?.seekTo(position.toLong())
            scrubbing = false
        }, valueRange = 0f..duration, enabled = ready)
        Text("${time(position)} / ${time(duration)}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(modifier = Modifier.weight(1f).heightIn(min = 52.dp), enabled = ready, onClick = {
                player?.let { p ->
                    if (playing) p.pause() else {
                        if (position >= duration) { p.seekTo(0); position = 0f }
                        p.play()
                    }
                    playing = p.isPlaying
                }
            }) { Text(if (playing) "Pause" else "Play song") }
            OutlinedButton(enabled = ready, onClick = { player?.seekTo(0); position = 0f }) { Text("Replay") }
        }
    }
    if (cues.isNotEmpty()) {
        val active by remember(cues) { derivedStateOf { LyricTiming.active(cues, position.toInt()) } }
        val focus by remember(cues) { derivedStateOf { cues.indexOfLast { position >= it.startMs }.coerceAtLeast(0) } }
        TimedLyrics(cues, active, focus, suppliedLyrics) { cue ->
            seeking = true
            position = cue.startMs.toFloat()
            player?.seekTo(cue.startMs.toLong())
        }
    }
}
