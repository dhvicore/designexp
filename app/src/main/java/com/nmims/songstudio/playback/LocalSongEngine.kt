package com.nmims.songstudio.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider
import java.io.File

/** Plays the original local file. No generation, transcoding or network access. */
@androidx.annotation.OptIn(UnstableApi::class)
object LocalSongEngine {
    fun create(context: Context, file: File): ExoPlayer {
        val renderers = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioOutputPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setAudioTrackBufferSizeProvider(DefaultAudioTrackBufferSizeProvider.Builder()
                        .setMinPcmBufferDurationUs(1_000_000)
                        .setMaxPcmBufferDurationUs(2_000_000).build())
                    .build()
        }
        val load = DefaultLoadControl.Builder().setBufferDurationsMs(5000, 20000, 1000, 1500).build()
        return ExoPlayer.Builder(context, renderers).setLoadControl(load).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            volume = 1f
            setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(file)))
            prepare()
        }
    }
}
