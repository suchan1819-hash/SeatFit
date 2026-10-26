package com.zeroinjury.seatfit.routine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.annotation.RawRes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * 코치 음성. 차량마다 TTS 엔진이 다를 수 있어서, 트레이너 멘트를 녹음 파일로 앱에 넣어 두고 재생한다.
 */
class Voice(context: Context) {

    private val ctx = context.applicationContext
    private var player: MediaPlayer? = null
    /** playAndWait 가 기다리는 중이면, 멈출 때 바로 풀어 준다 */
    private var waiting: (() -> Unit)? = null
    var muted = false

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    private val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attrs)
        .build()

    /** 음악이 나오고 있으면 잠깐 줄이고 코치 음성을 들려준다 */
    private fun requestFocus() { runCatching { audioManager.requestAudioFocus(focus) } }
    private fun abandonFocus() { runCatching { audioManager.abandonAudioFocusRequest(focus) } }

    /** 바로 재생 (앞의 음성은 끊는다) */
    fun play(@RawRes res: Int) {
        stop()
        if (muted) return
        runCatching {
            val mp = MediaPlayer()
            mp.setAudioAttributes(attrs)
            ctx.resources.openRawResourceFd(res).use { fd ->
                mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            mp.setOnCompletionListener { it.release(); if (player === it) player = null }
            mp.prepare()
            requestFocus()
            mp.start()
            player = mp
        }.onFailure { Log.w("SeatFit/Voice", "play failed: ${it.message}") }
    }

    /** 재생이 끝날 때까지 기다린다. 소리가 안 나는 환경에서도 멈추지 않도록 최대 시간을 둔다. */
    suspend fun playAndWait(@RawRes res: Int, maxMs: Long = 15_000) {
        stop()
        if (muted) return
        withTimeoutOrNull(maxMs) {
            suspendCancellableCoroutine { cont ->
                waiting = { if (cont.isActive) cont.resume(Unit) }
                val ok = runCatching {
                    val mp = MediaPlayer()
                    mp.setAudioAttributes(attrs)
                    ctx.resources.openRawResourceFd(res).use { fd ->
                        mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    }
                    mp.setOnCompletionListener {
                        it.release()
                        if (player === it) player = null
                        if (cont.isActive) cont.resume(Unit)
                    }
                    mp.setOnErrorListener { p, _, _ ->
                        p.release()
                        if (player === p) player = null
                        if (cont.isActive) cont.resume(Unit)
                        true
                    }
                    mp.prepare()
                    requestFocus()
                    mp.start()
                    player = mp
                }.isSuccess
                if (!ok && cont.isActive) cont.resume(Unit)
                cont.invokeOnCancellation { stop() }
            }
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        abandonFocus()
        val w = waiting
        waiting = null
        w?.invoke()
    }
}
