package com.nmims.songstudio

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nmims.songstudio.playback.LocalSongEngine
import com.nmims.songstudio.playback.SunoDemo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class BufferedPlaybackTest {
    @Test fun originalFilesPlayOnTheActualAppEngineWithoutAudioUnderruns() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val songs = SunoDemo.catalog(context).sortedBy { if (it.topicId == 1001) 0 else 1 }
        for (song in songs) {
            val file = requireNotNull(SunoDemo.bundledAudio(context, song))
            val ready = CountDownLatch(1)
            val ended = CountDownLatch(1)
            val error = AtomicReference<String?>(null)
            val underruns = AtomicInteger(0)
            lateinit var player: ExoPlayer
            instrumentation.runOnMainSync {
                player = LocalSongEngine.create(context, file)
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) ready.countDown()
                        if (state == Player.STATE_ENDED) ended.countDown()
                    }
                    override fun onPlayerError(failure: PlaybackException) { error.set(failure.message); ready.countDown(); ended.countDown() }
                })
                player.addAnalyticsListener(object : AnalyticsListener {
                    override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) { underruns.incrementAndGet() }
                })
            }
            try {
                assertTrue(song.title, ready.await(10, TimeUnit.SECONDS))
                assertNull(error.get())
                instrumentation.runOnMainSync { player.play() }
                var previous = 0L
                repeat(if (song.topicId == 1001) 20 else 3) {
                    SystemClock.sleep(500)
                    var current = 0L
                    instrumentation.runOnMainSync { assertTrue(song.title, player.isPlaying); current = player.currentPosition }
                    assertTrue("Playback stalled: ${song.title} at $current", current > previous)
                    previous = current
                }
                instrumentation.runOnMainSync { player.pause(); assertFalse(player.isPlaying); player.seekTo(player.duration - 700); player.play() }
                assertTrue("Did not finish ${song.title}", ended.await(10, TimeUnit.SECONDS))
                assertNull(error.get())
                assertEquals("Audio underruns: ${song.title}", 0, underruns.get())
            } finally { instrumentation.runOnMainSync { player.release() } }
        }
    }
}
