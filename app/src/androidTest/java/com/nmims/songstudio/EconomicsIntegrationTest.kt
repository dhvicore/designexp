package com.nmims.songstudio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nmims.songstudio.data.*
import com.nmims.songstudio.economics.EconomicsAudio
import com.nmims.songstudio.economics.audio.sing.WavReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EconomicsIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun allEconomicsTopicsHaveThreeLanguageChoicesAndLyrics() {
        val data = Curriculum(context)
        assertEquals(38, data.topics.size)
        assertEquals(60, data.chemistryTopics.size)
        assertEquals(32, data.economics.topics.size)
        assertEquals(7, data.unitsForSubject("Economics").size)
        assertEquals(130, (data.topics + data.chemistryTopics + data.economics.topics).map { it.id }.distinct().size)
        for(topic in data.economics.topics) for(category in listOf("English", "Hindi", "Marathi")) {
            val options = MusicChoices.options(data, topic, category)
            assertTrue(options.isNotEmpty())
            assertEquals(options.size, options.map { it.id }.distinct().size)
            assertTrue(options.all { it.id.isNotBlank() })
            val lyrics = data.economics.draft(topic.id, category, options.first())
            assertTrue(lyrics.length > 50)
            if(category != "English") assertTrue(lyrics.any { it in '\u0900'..'\u097f' })
            assertTrue(topic.concept.isNotBlank())
        }
    }
    @Test fun phoneVoiceRendersPlayableWavWithLineTimingAndCache() = runBlocking {
        val data = Curriculum(context)
        val topic = data.economics.topics.first()
        val choice = data.economics.options(topic.id, "English", false).first()
        val song = data.economics.song("English", choice)
        val lyrics = "[Verse]\nResources are scarce, choices we make.\n[Chorus]\nLearn economics with every step we take."
        val recording = EconomicsAudio.generate(context, lyrics, song, 83412) {}
        assertEquals(2, recording.cues.size)
        assertTrue(recording.cues[0].startMs > 0)
        assertEquals(recording.cues[0].endMs, recording.cues[1].startMs)
        val samples = WavReader.read(recording.file, 22050)!!
        assertTrue(samples.size > 22050 * 3)
        assertTrue(samples.maxOf { kotlin.math.abs(it) } > .1f)
        assertEquals(recording.file, EconomicsAudio.cached(context, lyrics, song, 83412)!!.file)
    }
    @Test fun firstFullEconomicsLessonGeneratesAndPlaysOnMedia3() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val data = Curriculum(context)
        val topic = data.economics.topics.first()
        for(category in listOf("English", "Hindi", "Marathi")) {
            val choice = data.economics.options(topic.id, category, false).first()
            val song = data.economics.song(category, choice)
            val draft = data.economics.draft(topic.id, category, choice)
            val recording = EconomicsAudio.generate(context, draft, song, topic.id * 1009 + song.title.hashCode()) {}
            assertTrue(category, recording.cues.size >= 4)
            val ready = java.util.concurrent.CountDownLatch(1)
            var error: String? = null
            lateinit var player: androidx.media3.exoplayer.ExoPlayer
            instrumentation.runOnMainSync {
                player = com.nmims.songstudio.playback.LocalSongEngine.create(context, recording.file)
                player.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) { if(state == androidx.media3.common.Player.STATE_READY) ready.countDown() }
                    override fun onPlayerError(failure: androidx.media3.common.PlaybackException) { error = failure.message; ready.countDown() }
                })
            }
            try {
                assertTrue(category, ready.await(10, java.util.concurrent.TimeUnit.SECONDS))
                assertNull(error)
                instrumentation.runOnMainSync { player.play() }
                android.os.SystemClock.sleep(2500)
                instrumentation.runOnMainSync {
                    assertTrue(category, player.isPlaying)
                    assertTrue(category, player.currentPosition > 1000)
                    assertTrue(category, player.duration >= recording.cues.last().endMs - 100)
                }
            } finally { instrumentation.runOnMainSync { player.release() } }
        }
    }

}
