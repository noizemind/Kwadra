package app.clearline.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Original Clearline tones — not copied from any commercial softphone.
 * Soft teal-ish timbre: warmer midrange, short decays, no Skype-like motifs.
 */
class ClearTones {
    @Volatile var enabled: Boolean = true
    @Volatile private var loopId: Long = 0

    fun tap() = oneshot(listOf(Tone(720.0, 0.035, 0.07)))

    fun dial() = oneshot(listOf(Tone(640.0, 0.04, 0.06), Tone(880.0, 0.03, 0.04, delay = 30)))

    fun connect() = oneshot(
        listOf(
            Tone(392.0, 0.12, 0.09),
            Tone(523.0, 0.12, 0.08, delay = 90),
            Tone(659.0, 0.16, 0.07, delay = 180),
        ),
    )

    fun hangup() = oneshot(
        listOf(
            Tone(330.0, 0.18, 0.09, slide = 0.55),
            Tone(196.0, 0.22, 0.07, delay = 100, slide = 0.7),
        ),
    )

    fun busy() = oneshot(
        listOf(
            Tone(480.0, 0.35, 0.08),
            Tone(480.0, 0.35, 0.08, delay = 500),
        ),
    )

    fun dtmf(digit: Char) {
        val (a, b) = DTMF[digit] ?: return
        oneshot(listOf(Tone(a, 0.12, 0.05, second = b)))
    }

    fun startRingback() {
        val id = ++loopId
        thread(name = "cl-ringback", isDaemon = true) {
            while (enabled && loopId == id) {
                play(Tone(440.0, 0.9, 0.06, second = 480.0))
                if (loopId != id) break
                Thread.sleep(2200)
            }
        }
    }

    fun startRingtone() {
        val id = ++loopId
        thread(name = "cl-ringtone", isDaemon = true) {
            while (enabled && loopId == id) {
                play(Tone(523.0, 0.22, 0.09))
                if (loopId != id) break
                Thread.sleep(80)
                play(Tone(659.0, 0.28, 0.08))
                if (loopId != id) break
                Thread.sleep(900)
            }
        }
    }

    fun stopLoops() {
        loopId++
    }

    private data class Tone(
        val freq: Double,
        val dur: Double,
        val gain: Double,
        val delay: Long = 0,
        val slide: Double = 0.0,
        val second: Double? = null,
    )

    private fun oneshot(tones: List<Tone>) {
        if (!enabled) return
        thread(name = "cl-tone", isDaemon = true) {
            for (t in tones) {
                if (t.delay > 0) Thread.sleep(t.delay)
                if (!enabled) return@thread
                play(t)
            }
        }
    }

    private fun play(t: Tone) {
        val rate = 44100
        val n = maxOf(1, (t.dur * rate).toInt())
        val data = ShortArray(n)
        var p1 = 0.0
        var p2 = 0.0
        for (i in 0 until n) {
            val x = i / rate.toDouble()
            val f1 = if (t.slide > 0) t.freq * Math.pow(t.slide, x / t.dur) else t.freq
            p1 += 2 * PI * f1 / rate
            var sample = sin(p1)
            if (t.second != null) {
                p2 += 2 * PI * t.second / rate
                sample = (sample + sin(p2)) * 0.5
            }
            val env = min(1.0, x / 0.01) * exp(-2.8 * x / t.dur)
            data[i] = (sample * env * t.gain * Short.MAX_VALUE).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION_SIGNALLING)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(data, 0, data.size)
        track.play()
        Thread.sleep((t.dur * 1000).toLong() + 20)
        track.stop()
        track.release()
    }

    companion object {
        private val DTMF = mapOf(
            '1' to (697.0 to 1209.0),
            '2' to (697.0 to 1336.0),
            '3' to (697.0 to 1477.0),
            '4' to (770.0 to 1209.0),
            '5' to (770.0 to 1336.0),
            '6' to (770.0 to 1477.0),
            '7' to (852.0 to 1209.0),
            '8' to (852.0 to 1336.0),
            '9' to (852.0 to 1477.0),
            '*' to (941.0 to 1209.0),
            '0' to (941.0 to 1336.0),
            '#' to (941.0 to 1477.0),
        )
    }
}
