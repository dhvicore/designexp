package com.nmims.songstudio.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nmims.songstudio.ui.*

data class LyricCue(val startMs: Int, val endMs: Int, val text: String)

object LyricTiming {
    fun active(cues: List<LyricCue>, positionMs: Int): Int {
        val index = cues.indexOfLast { positionMs >= it.startMs }
        return if (index >= 0 && positionMs < cues[index].endMs) index else -1
    }
}

@Composable
fun TimedLyrics(cues: List<LyricCue>, active: Int, focus: Int, suppliedLyrics: String, onSeek: (LyricCue) -> Unit) {
    Panel(Lavender) {
        Text("READ ALONG", style = MaterialTheme.typography.labelSmall)
        Text("Follow the lyrics", style = MaterialTheme.typography.titleLarge)
        Text("The current line highlights as it plays. Tap any line to jump to it. Automatic timing may be approximate.", style = MaterialTheme.typography.bodySmall)
        val first = (focus - 1).coerceIn(0, (cues.size - 3).coerceAtLeast(0))
        Column(Modifier.fillMaxWidth().heightIn(min = 150.dp).background(Color.White.copy(alpha = .65f), RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cues.drop(first).take(3).forEachIndexed { offset, cue ->
                val selected = first + offset == active
                Text(cue.text, color = if (selected) Accent else Ink.copy(alpha = .65f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().clickable { onSeek(cue) }.padding(vertical = 4.dp))
            }
        }
        var expanded by remember { mutableStateOf(false) }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide full lyrics" else "Show full lyrics") }
        if (expanded) {
            val full = remember(cues, suppliedLyrics) {
                val lines = suppliedLyrics.lines().map { it.trim() }.filter {
                    it.isNotBlank() && !it.startsWith("[") && !it.startsWith("```") && it != "Verse]"
                }.ifEmpty { cues.map { it.text } }
                var next = 0
                lines.map { line ->
                    val index = next.takeIf { it < cues.size && cues[it].text == line }
                    if (index != null) next++
                    line to index
                }
            }
            full.forEach { (line, index) ->
                Text(line, fontWeight = if (index == active) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == active) Accent else Ink,
                    modifier = Modifier.fillMaxWidth().clickable(enabled = index != null) { index?.let { onSeek(cues[it]) } }.padding(vertical = 6.dp))
            }
        }
    }
}
