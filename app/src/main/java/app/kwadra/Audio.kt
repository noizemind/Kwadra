package app.kwadra

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class Tones {
    @Volatile var on: Boolean = true

    fun place() {
        tone(0, 420.0, 0.07, 0.09, 0.0)
        tone(0, 630.0, 0.05, 0.04, 0.0)
    }

    fun reject() = tone(0, 180.0, 0.09, 0.07, 0.7)

    fun clear(lines: Int) {
        tone(0, 520.0, 0.10, 0.10, 0.0)
        tone(40, 660.0, 0.10, 0.09, 0.0)
        if (lines >= 2) tone(80, 790.0, 0.14, 0.08, 0.0)
    }

    fun combo(n: Int) = tone(0, 540.0 + min(n, 8) * 40.0, 0.16, 0.10, 0.0)

    fun perfect() {
        tone(0, 523.0, 0.12, 0.10, 0.0)
        tone(70, 659.0, 0.12, 0.09, 0.0)
        tone(140, 784.0, 0.18, 0.08, 0.0)
    }

    fun over() {
        tone(0, 220.0, 0.22, 0.10, 0.55)
        tone(90, 164.0, 0.28, 0.08, 0.5)
    }

    fun tap() = tone(0, 380.0, 0.04, 0.05, 0.0)

    private fun tone(delayMs: Long, freq: Double, dur: Double, gain: Double, slide: Double) {
        if (!on) return
        thread(name = "kwadra-tone", isDaemon = true) {
            if (delayMs > 0) Thread.sleep(delayMs)
            if (!on) return@thread
            val rate = 44100
            val n = maxOf(1, (dur * rate).toInt())
            val data = ByteArray(n * 2)
            var phase = 0.0
            for (i in 0 until n) {
                val t = i / rate.toDouble()
                val f = if (slide > 0) freq * slide.pow(t / dur) else freq
                phase += 2 * PI * f / rate
                val env = min(1.0, t / 0.008) * exp(-3.2 * t / dur)
                var v = (sin(phase) * gain * env * 32767).toInt()
                if (v > 32767) v = 32767
                if (v < -32767) v = -32767
                data[i * 2] = (v and 0xff).toByte()
                data[i * 2 + 1] = ((v shr 8) and 0xff).toByte()
            }
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(rate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size)
                    .build()
                track.write(data, 0, data.size)
                track.play()
                Thread.sleep((dur * 1000).toLong() + 50)
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}

class Buzz(context: Context) {
    private val vibrator = context.getSystemService(Vibrator::class.java)
    @Volatile var on: Boolean = true

    fun once(ms: Long) {
        if (!on || vibrator == null) return
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {
        }
    }

    fun pattern(first: Long, gap: Long, second: Long) {
        if (!on || vibrator == null) return
        try {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, first, gap, second), -1))
        } catch (_: Exception) {
        }
    }
}
