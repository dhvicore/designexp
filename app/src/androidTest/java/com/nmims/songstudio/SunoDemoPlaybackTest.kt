package com.nmims.songstudio

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nmims.songstudio.data.Curriculum
import com.nmims.songstudio.data.MusicChoices
import com.nmims.songstudio.playback.SunoDemo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SunoDemoPlaybackTest {
    @Test fun allEightRecordingsAreSelectableAndPlayToTheirEndAfterSeeking() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val curriculum = Curriculum(context)
        val songs = SunoDemo.catalog(context)
        assertEquals(8, songs.size)
        assertEquals(8, songs.map { Triple(it.topicId, it.category, it.songId) }.distinct().size)
        assertEquals(8, songs.map { it.assetPath }.distinct().size)
        for (song in songs) {
            val topic = curriculum.topics.first { it.id == song.topicId }
            assertEquals("Saved option should be first for ${song.title}", song.songId,
                MusicChoices.options(curriculum, topic, song.category).first().id)
            assertTrue(song.lyrics.isNotBlank())
            val file = requireNotNull(SunoDemo.bundledAudio(context, song))
            val completed = CountDownLatch(1)
            val playbackError = AtomicReference<String?>(null)
            val player = MediaPlayer()
            try {
                player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                player.setOnErrorListener { _, what, extra ->
                    playbackError.set("MediaPlayer error $what / $extra"); completed.countDown(); true
                }
                player.setOnCompletionListener { completed.countDown() }
                player.setDataSource(file.absolutePath); player.prepare()
                assertTrue(song.assetPath, player.duration > 10000)
                println("SUNO_DEMO ${song.assetPath}: duration_ms=${player.duration}, bytes=${file.length()}")
                player.start(); SystemClock.sleep(450)
                assertTrue(song.assetPath, player.isPlaying)
                assertTrue(player.currentPosition > 100)
                player.pause(); assertFalse(player.isPlaying)
                player.seekTo(player.duration - 700); player.start()
                assertTrue("Did not reach end: ${song.assetPath}", completed.await(8, TimeUnit.SECONDS))
                assertNull(playbackError.get()); assertFalse(player.isPlaying)
            } finally { player.release() }
        }
    }
}
