package app.clearline.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Minimal RTP audio path: PCMU @ 8 kHz, 20 ms frames (160 samples).
 * Demo mode can loop mic → speaker locally when no remote peer is set.
 */
class RtpAudioSession {
    @Volatile var muted: Boolean = false
    @Volatile var speakerphone: Boolean = false

    private val running = AtomicBoolean(false)
    private var socket: DatagramSocket? = null
    private var remoteHost: InetAddress? = null
    private var remotePort: Int = 0
    private var localPort: Int = 0
    private var seq: Int = Random.nextInt(0, 0xffff)
    private var timestamp: Int = Random.nextInt()
    private val ssrc: Int = Random.nextInt()
    private var recordThread: Thread? = null
    private var playThread: Thread? = null
    private var demoEcho: Boolean = true

    fun start(localRtpPort: Int = 0, remote: String? = null, remoteRtpPort: Int = 0, echoDemo: Boolean = true) {
        stop()
        demoEcho = echoDemo || remote.isNullOrBlank()
        localPort = localRtpPort
        if (!remote.isNullOrBlank() && remoteRtpPort > 0) {
            remoteHost = InetAddress.getByName(remote)
            remotePort = remoteRtpPort
        }
        val sock = if (localPort > 0) DatagramSocket(localPort) else DatagramSocket()
        sock.soTimeout = 50
        socket = sock
        localPort = sock.localPort
        running.set(true)
        recordThread = thread(name = "cl-rtp-tx", isDaemon = true) { captureLoop(sock) }
        playThread = thread(name = "cl-rtp-rx", isDaemon = true) { playbackLoop(sock) }
    }

    fun localPort(): Int = localPort

    fun stop() {
        running.set(false)
        recordThread?.join(400)
        playThread?.join(400)
        recordThread = null
        playThread = null
        socket?.close()
        socket = null
        remoteHost = null
    }

    private fun captureLoop(sock: DatagramSocket) {
        val rate = 8000
        val frame = 160
        val minBuf = AudioRecord.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(frame * 4)
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf,
            )
        } catch (_: SecurityException) {
            null
        }
        if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
            // No mic permission — send comfort noise / silence RTP if remote exists.
            silenceTx(sock, frame)
            return
        }
        val pcm = ShortArray(frame)
        val ulaw = ByteArray(frame)
        val packet = ByteArray(12 + frame)
        recorder.startRecording()
        try {
            while (running.get()) {
                val n = recorder.read(pcm, 0, frame)
                if (n <= 0) continue
                if (muted) pcm.fill(0)
                G711.encode(pcm, ulaw, frame)
                if (demoEcho && remoteHost == null) {
                    // Local loop handled in playback via shared echo buffer.
                    EchoBus.push(ulaw.copyOf())
                }
                val host = remoteHost
                if (host != null && remotePort > 0) {
                    writeRtpHeader(packet)
                    System.arraycopy(ulaw, 0, packet, 12, frame)
                    sock.send(DatagramPacket(packet, packet.size, host, remotePort))
                    seq = (seq + 1) and 0xffff
                    timestamp += frame
                }
            }
        } finally {
            runCatching {
                recorder.stop()
                recorder.release()
            }
        }
    }

    private fun silenceTx(sock: DatagramSocket, frame: Int) {
        val ulaw = ByteArray(frame) { 0xff.toByte() }
        val packet = ByteArray(12 + frame)
        while (running.get()) {
            if (demoEcho && remoteHost == null) EchoBus.push(ulaw.copyOf())
            val host = remoteHost
            if (host != null && remotePort > 0) {
                writeRtpHeader(packet)
                System.arraycopy(ulaw, 0, packet, 12, frame)
                runCatching { sock.send(DatagramPacket(packet, packet.size, host, remotePort)) }
                seq = (seq + 1) and 0xffff
                timestamp += frame
            }
            Thread.sleep(20)
        }
    }

    private fun playbackLoop(sock: DatagramSocket) {
        val rate = 8000
        val frame = 160
        val minBuf = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(frame * 4)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(
                        if (speakerphone) AudioAttributes.USAGE_MEDIA
                        else AudioAttributes.USAGE_VOICE_COMMUNICATION,
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        val buf = ByteArray(12 + frame + 64)
        val pcm = ShortArray(frame)
        val packet = DatagramPacket(buf, buf.size)
        try {
            while (running.get()) {
                val ulaw: ByteArray? = if (demoEcho && remoteHost == null) {
                    EchoBus.poll() ?: run {
                        Thread.sleep(5)
                        null
                    }
                } else {
                    try {
                        sock.receive(packet)
                        if (packet.length < 13) null
                        else buf.copyOfRange(12, packet.length.coerceAtMost(12 + frame))
                    } catch (_: Exception) {
                        null
                    }
                }
                if (ulaw == null) continue
                val samples = ulaw.size.coerceAtMost(frame)
                G711.decode(ulaw, pcm, samples)
                track.write(pcm, 0, samples)
            }
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    private fun writeRtpHeader(packet: ByteArray) {
        packet[0] = 0x80.toByte()
        packet[1] = 0x00 // PT=0 PCMU
        packet[2] = ((seq shr 8) and 0xff).toByte()
        packet[3] = (seq and 0xff).toByte()
        packet[4] = ((timestamp shr 24) and 0xff).toByte()
        packet[5] = ((timestamp shr 16) and 0xff).toByte()
        packet[6] = ((timestamp shr 8) and 0xff).toByte()
        packet[7] = (timestamp and 0xff).toByte()
        packet[8] = ((ssrc shr 24) and 0xff).toByte()
        packet[9] = ((ssrc shr 16) and 0xff).toByte()
        packet[10] = ((ssrc shr 8) and 0xff).toByte()
        packet[11] = (ssrc and 0xff).toByte()
    }

    /** Tiny ring buffer for demo mic→ear echo. */
    private object EchoBus {
        private val q = ArrayDeque<ByteArray>(8)
        fun push(frame: ByteArray) = synchronized(q) {
            if (q.size >= 8) q.removeFirst()
            q.addLast(frame)
        }
        fun poll(): ByteArray? = synchronized(q) { if (q.isEmpty()) null else q.removeFirst() }
    }
}
