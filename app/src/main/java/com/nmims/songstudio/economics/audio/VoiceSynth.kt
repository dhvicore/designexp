package com.nmims.songstudio.economics.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.nmims.songstudio.economics.data.Lang
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/** Records lyric lines with the phone's text-to-speech voice into WAV files. */
class VoiceSynth(context: Context) {
    private enum class State { CONNECTING, READY, FAILED }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var state = State.CONNECTING
    private val readyWaiters = mutableListOf<CancellableContinuation<Boolean>>()
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()
    private val ids = AtomicInteger()

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}

        override fun onDone(utteranceId: String) {
            pending.remove(utteranceId)?.resume(true)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) {
            pending.remove(utteranceId)?.resume(false)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            pending.remove(utteranceId)?.resume(false)
        }
    }

    init {
        connect()
    }

    private fun connect() {
        state = State.CONNECTING
        tts?.shutdown()
        tts = TextToSpeech(appContext) { status ->
            main.post {
                state = if (status == TextToSpeech.SUCCESS) State.READY else State.FAILED
                tts?.setOnUtteranceProgressListener(listener)
                val waiters = readyWaiters.toList()
                readyWaiters.clear()
                waiters.forEach { it.resume(state == State.READY) }
            }
        }
    }

    /** Must be called from the main thread. */
    private suspend fun ready(): Boolean {
        if (state == State.READY) return true
        if (state == State.FAILED) connect()
        return withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                readyWaiters += cont
                cont.invokeOnCancellation { main.post { readyWaiters.remove(cont) } }
            }
        } ?: false
    }

    /** Writes [text] spoken in [lang] to [file]; returns false if no voice could produce it. */
    suspend fun record(text: String, lang: Lang, file: File): Boolean {
        if (!ready()) return false
        val engine = tts ?: return false
        val locale = Locale.forLanguageTag(lang.localeTag)
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return false
        preferOnDeviceVoice(engine, locale)

        val id = "line-${ids.incrementAndGet()}"
        val ok = withTimeoutOrNull(25_000) {
            suspendCancellableCoroutine { cont ->
                pending[id] = cont
                cont.invokeOnCancellation { pending.remove(id) }
                if (engine.synthesizeToFile(text, Bundle(), file, id) != TextToSpeech.SUCCESS) {
                    pending.remove(id)?.resume(false)
                }
            }
        }
        if (ok != true) {
            engine.stop()
            file.delete()
        }
        return ok == true
    }

    /** Network voices fail without internet, so pick an installed voice for the language when one exists. */
    private fun preferOnDeviceVoice(engine: TextToSpeech, locale: Locale) {
        val candidates = try {
            engine.voices.orEmpty().filter { it.locale.language == locale.language }
        } catch (_: RuntimeException) {
            return
        }
        val local = candidates
            .filter { !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features }
            .sortedWith(compareBy({ it.locale.country != locale.country }, { it.latency }, { -it.quality }))
            .firstOrNull()
        if (local != null) engine.voice = local
    }

    fun stop() {
        if (state == State.READY) tts?.stop()
        pending.keys.toList().forEach { pending.remove(it)?.resume(false) }
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
