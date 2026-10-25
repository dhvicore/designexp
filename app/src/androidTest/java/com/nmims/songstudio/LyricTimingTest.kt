package com.nmims.songstudio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nmims.songstudio.playback.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricTimingTest {
    @Test fun highlightingTracksPauseSeekReplayAndInstrumentalGaps() {
        val cues = listOf(LyricCue(1000, 2000, "First"), LyricCue(3000, 4000, "Second"))
        assertEquals(-1, LyricTiming.active(cues, 0))
        assertEquals(0, LyricTiming.active(cues, 1000))
        assertEquals(0, LyricTiming.active(cues, 1500))
        assertEquals(-1, LyricTiming.active(cues, 2500))
        assertEquals(1, LyricTiming.active(cues, 3500))
        assertEquals(0, LyricTiming.active(cues, 1500))
        assertEquals(-1, LyricTiming.active(cues, 0))
        assertEquals(-1, LyricTiming.active(cues, 4000))
    }
    @Test fun allEightCalculusSongsHaveTheirOwnValidTimingsAndUnchangedAudio() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val songs = SunoDemo.catalog(context).filter { it.topicId == 0 || it.topicId == 17 }
        assertEquals(8, songs.size)
        assertEquals(8, songs.map { it.cues.map { cue -> cue.startMs } }.distinct().size)
        for (song in songs) {
            val lines = song.lyrics.lines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("[") && !it.startsWith("```") && it != "Verse]" }
            assertTrue(song.title, song.cues.size >= lines.size * .8)
            assertTrue(song.title, song.cues.all { it.startMs >= 0 && it.endMs > it.startMs && it.text in lines })
            assertTrue(song.title, song.cues.zipWithNext().all { (a,b) -> a.endMs <= b.startMs })
            if (song.category != "Hindi" || song.topicId != 0) assertEquals(song.title, lines, song.cues.map { it.text })
            val file = requireNotNull(SunoDemo.bundledAudio(context, song))
            assertTrue(song.title, context.assets.open(song.assetPath).use { it.readBytes() }.contentEquals(file.readBytes()))
            val player = android.media.MediaPlayer()
            try {
                player.setDataSource(file.absolutePath); player.prepare()
                assertTrue(song.title, song.cues.last().endMs <= player.duration)
            } finally { player.release() }
        }
    }

    @Test fun daltonCuesMatchSuppliedLyricsAndFitTheUnchangedRecording() = kotlinx.coroutines.runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val song = SunoDemo.catalog(context).first { it.topicId == 1001 }
        val lines = song.lyrics.lines().filter { it.isNotBlank() && !it.startsWith("[") }
        assertEquals(lines, song.cues.map { it.text })
        assertTrue(song.cues.all { it.startMs >= 0 && it.endMs > it.startMs })
        assertTrue(song.cues.zipWithNext().all { (a,b) -> a.endMs <= b.startMs })
        val file = requireNotNull(SunoDemo.bundledAudio(context, song))
        assertTrue(context.assets.open(song.assetPath).use { it.readBytes() }.contentEquals(file.readBytes()))
        val player = android.media.MediaPlayer()
        try {
            player.setDataSource(file.absolutePath); player.prepare()
            assertTrue(song.cues.last().endMs <= player.duration)
        } finally { player.release() }
    }
}
