package app.clearline.sip

import app.clearline.data.CallDirection
import app.clearline.data.CallOutcome
import app.clearline.data.CallPhase
import app.clearline.data.CallRecord
import app.clearline.data.RegState
import app.clearline.data.SipAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Lightweight SIP UA inspired by classic softphone flows (REGISTER / INVITE / BYE).
 * Demo mode simulates registration and calls without a server.
 */
class SipEngine(
    private val scope: CoroutineScope,
    private val onReg: (RegState, String) -> Unit,
    private val onCallPhase: (CallPhase, String?) -> Unit,
    private val onCallEnded: (CallRecord) -> Unit,
    private val onRemoteMedia: (SdpMedia?) -> Unit = {},
) {
    var localRtpPort: Int = 4000
        private set
    private var remoteMedia: SdpMedia? = null

    private val cseq = AtomicInteger(1)
    private var socket: DatagramSocket? = null
    private var account = SipAccount()
    private var listenJob: Job? = null
    private var demoJob: Job? = null
    private var callId: String? = null
    private var peer: String = ""
    private var peerName: String = ""
    private var direction: CallDirection = CallDirection.OUTGOING
    private var connectedAt: Long? = null
    private var startedAt: Long = 0
    private var localTag: String = ""
    private var remoteTag: String = ""
    private var branch: String = ""

    fun configure(acc: SipAccount) {
        account = acc
    }

    fun start() {
        stop()
        if (account.demoMode || account.domain.isBlank() || account.username.isBlank()) {
            onReg(RegState.CONNECTING, "Demo mode")
            scope.launch {
                delay(400)
                onReg(RegState.REGISTERED, "Demo · local softphone")
            }
            return
        }
        onReg(RegState.CONNECTING, "Registering…")
        scope.launch(Dispatchers.IO) {
            try {
                val sock = DatagramSocket()
                socket = sock
                listenJob = scope.launch(Dispatchers.IO) { listenLoop(sock) }
                sendRegister(sock, auth = null)
            } catch (e: Exception) {
                onReg(RegState.FAILED, e.message ?: "Socket error")
            }
        }
    }

    fun stop() {
        demoJob?.cancel()
        listenJob?.cancel()
        socket?.close()
        socket = null
        onReg(RegState.OFFLINE, "")
    }

    fun placeCall(target: String, displayName: String) {
        peer = normalize(target)
        peerName = displayName.ifBlank { peer }
        direction = CallDirection.OUTGOING
        startedAt = System.currentTimeMillis()
        connectedAt = null
        callId = UUID.randomUUID().toString()
        localTag = randTag()
        branch = "z9hG4bK" + randTag()
        onCallPhase(CallPhase.DIALING, null)

        if (account.demoMode || account.domain.isBlank()) {
            demoJob?.cancel()
            demoJob = scope.launch {
                delay(350)
                onCallPhase(CallPhase.RINGING_OUT, null)
                delay(1800)
                connectedAt = System.currentTimeMillis()
                onCallPhase(CallPhase.ACTIVE, null)
            }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val sock = socket ?: DatagramSocket().also { socket = it }
                sendInvite(sock)
            } catch (e: Exception) {
                finish(CallOutcome.FAILED, e.message)
            }
        }
    }

    fun acceptIncoming() {
        if (direction != CallDirection.INCOMING) return
        connectedAt = System.currentTimeMillis()
        onCallPhase(CallPhase.ACTIVE, null)
        // Real 200 OK would go here when inbound SIP is wired.
    }

    fun declineOrHangup() {
        demoJob?.cancel()
        val outcome = when {
            connectedAt != null -> CallOutcome.ANSWERED
            direction == CallDirection.INCOMING -> CallOutcome.DECLINED
            else -> CallOutcome.CANCELLED
        }
        if (!account.demoMode && socket != null && callId != null) {
            scope.launch(Dispatchers.IO) {
                runCatching { sendBye(socket!!) }
                finish(outcome, null)
            }
        } else {
            finish(outcome, null)
        }
    }

    fun simulateIncoming(from: String, name: String) {
        if (callId != null) return
        peer = normalize(from)
        peerName = name
        direction = CallDirection.INCOMING
        startedAt = System.currentTimeMillis()
        connectedAt = null
        callId = UUID.randomUUID().toString()
        onCallPhase(CallPhase.RINGING_IN, null)
    }

    private fun finish(outcome: CallOutcome, detail: String?) {
        val id = callId ?: return
        val dur = connectedAt?.let { ((System.currentTimeMillis() - it) / 1000).toInt() } ?: 0
        val record = CallRecord(
            id = id,
            peer = peer,
            displayName = peerName,
            direction = direction,
            outcome = outcome,
            startedAt = startedAt,
            durationSec = dur,
        )
        callId = null
        onCallPhase(CallPhase.ENDING, detail)
        onCallEnded(record)
        scope.launch {
            delay(200)
            onCallPhase(CallPhase.IDLE, null)
        }
    }

    private suspend fun listenLoop(sock: DatagramSocket) {
        val buf = ByteArray(65535)
        while (!sock.isClosed) {
            try {
                val packet = DatagramPacket(buf, buf.size)
                sock.receive(packet)
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                handleSip(sock, text)
            } catch (_: Exception) {
                break
            }
        }
    }

    private fun handleSip(sock: DatagramSocket, msg: String) {
        val status = Regex("""SIP/2\.0\s+(\d+)""").find(msg)?.groupValues?.getOrNull(1)?.toIntOrNull()
        when (status) {
            100, 180, 183 -> onCallPhase(CallPhase.RINGING_OUT, null)
            200 -> {
                if (msg.contains("REGISTER", ignoreCase = true) || msg.lines().getOrNull(0)?.contains("REGISTER") == true) {
                    // fallthrough handled below via CSeq method sniff
                }
                val cseqLine = header(msg, "CSeq") ?: ""
                when {
                    cseqLine.contains("REGISTER", true) -> onReg(RegState.REGISTERED, "Registered")
                    cseqLine.contains("INVITE", true) -> {
                        remoteTag = header(msg, "To")?.substringAfter("tag=")?.substringBefore(";") ?: remoteTag
                        remoteMedia = SdpParse.media(msg)
                        onRemoteMedia(remoteMedia)
                        connectedAt = System.currentTimeMillis()
                        onCallPhase(CallPhase.ACTIVE, null)
                        runCatching { sendAck(sock) }
                    }
                }
            }
            401, 407 -> {
                val challenge = parseChallenge(msg) ?: return
                val cseqLine = header(msg, "CSeq") ?: return
                scope.launch(Dispatchers.IO) {
                    if (cseqLine.contains("REGISTER", true)) sendRegister(sock, challenge)
                    if (cseqLine.contains("INVITE", true)) sendInvite(sock, challenge)
                }
            }
            486, 603 -> finish(CallOutcome.DECLINED, "Busy")
            in 400..699 -> {
                val cseqLine = header(msg, "CSeq") ?: ""
                if (cseqLine.contains("REGISTER", true)) {
                    onReg(RegState.FAILED, "SIP $status")
                } else {
                    finish(CallOutcome.FAILED, "SIP $status")
                }
            }
        }
    }

    private fun sendRegister(sock: DatagramSocket, auth: DigestChallenge?) {
        val host = account.proxy.ifBlank { account.domain }
        val port = account.port
        val seq = cseq.getAndIncrement()
        val call = "reg-" + UUID.randomUUID()
        val branch = "z9hG4bK" + randTag()
        val uri = "sip:${account.domain}"
        val from = "\"${account.displayName.ifBlank { account.username }}\"<sip:${account.username}@${account.domain}>;tag=${randTag()}"
        val contact = "<sip:${account.username}@${localIp()}:${sock.localPort}>"
        val authHeader = auth?.let {
            val response = digest(it, "REGISTER", uri)
            """Authorization: Digest username="${account.username}", realm="${it.realm}", nonce="${it.nonce}", uri="$uri", response="$response", algorithm=MD5"""
        }
        val lines = buildList {
            add("REGISTER $uri SIP/2.0")
            add("Via: SIP/2.0/UDP ${localIp()}:${sock.localPort};branch=$branch;rport")
            add("Max-Forwards: 70")
            add("From: $from")
            add("To: <sip:${account.username}@${account.domain}>")
            add("Call-ID: $call")
            add("CSeq: $seq REGISTER")
            add("Contact: $contact")
            add("Expires: 600")
            add("User-Agent: Clearline/1.0")
            authHeader?.let { add(it) }
            add("Content-Length: 0")
            add("")
            add("")
        }
        send(sock, host, port, lines.joinToString("\r\n"))
    }

    private fun sendInvite(sock: DatagramSocket, auth: DigestChallenge? = null) {
        val host = account.proxy.ifBlank { account.domain }
        val port = account.port
        val seq = cseq.getAndIncrement()
        val requestUri = if (peer.contains("@")) "sip:$peer" else "sip:$peer@${account.domain}"
        val from = "\"${account.displayName.ifBlank { account.username }}\"<sip:${account.username}@${account.domain}>;tag=$localTag"
        localRtpPort = (sock.localPort + 2).coerceIn(1024, 65534)
        val sdp = buildSdp(localRtpPort)
        val authHeader = auth?.let {
            val response = digest(it, "INVITE", requestUri)
            """Authorization: Digest username="${account.username}", realm="${it.realm}", nonce="${it.nonce}", uri="$requestUri", response="$response", algorithm=MD5"""
        }
        val lines = buildList {
            add("INVITE $requestUri SIP/2.0")
            add("Via: SIP/2.0/UDP ${localIp()}:${sock.localPort};branch=$branch;rport")
            add("Max-Forwards: 70")
            add("From: $from")
            add("To: <$requestUri>")
            add("Call-ID: $callId")
            add("CSeq: $seq INVITE")
            add("Contact: <sip:${account.username}@${localIp()}:${sock.localPort}>")
            add("User-Agent: Clearline/1.0")
            add("Content-Type: application/sdp")
            authHeader?.let { add(it) }
            add("Content-Length: ${sdp.toByteArray().size}")
            add("")
            add(sdp)
        }
        send(sock, host, port, lines.joinToString("\r\n"))
        onCallPhase(CallPhase.RINGING_OUT, null)
    }

    private fun sendAck(sock: DatagramSocket) {
        val host = account.proxy.ifBlank { account.domain }
        val requestUri = if (peer.contains("@")) "sip:$peer" else "sip:$peer@${account.domain}"
        val seq = cseq.get()
        val to = "<$requestUri>" + if (remoteTag.isNotBlank()) ";tag=$remoteTag" else ""
        val msg = """
            ACK $requestUri SIP/2.0
            Via: SIP/2.0/UDP ${localIp()}:${sock.localPort};branch=$branch;rport
            Max-Forwards: 70
            From: <sip:${account.username}@${account.domain}>;tag=$localTag
            To: $to
            Call-ID: $callId
            CSeq: $seq ACK
            Content-Length: 0

        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        send(sock, host, account.port, msg)
    }

    private fun sendBye(sock: DatagramSocket) {
        val host = account.proxy.ifBlank { account.domain }
        val requestUri = if (peer.contains("@")) "sip:$peer" else "sip:$peer@${account.domain}"
        val seq = cseq.getAndIncrement()
        val to = "<$requestUri>" + if (remoteTag.isNotBlank()) ";tag=$remoteTag" else ""
        val msg = """
            BYE $requestUri SIP/2.0
            Via: SIP/2.0/UDP ${localIp()}:${sock.localPort};branch=${"z9hG4bK" + randTag()};rport
            Max-Forwards: 70
            From: <sip:${account.username}@${account.domain}>;tag=$localTag
            To: $to
            Call-ID: $callId
            CSeq: $seq BYE
            Content-Length: 0

        """.trimIndent().replace("\n", "\r\n") + "\r\n"
        send(sock, host, account.port, msg)
    }

    private fun buildSdp(rtpPort: Int): String = """
        v=0
        o=clearline ${Random.nextInt(10000, 99999)} ${Random.nextInt(10000, 99999)} IN IP4 ${localIp()}
        s=Clearline
        c=IN IP4 ${localIp()}
        t=0 0
        m=audio $rtpPort RTP/AVP 0 8 101
        a=rtpmap:0 PCMU/8000
        a=rtpmap:8 PCMA/8000
        a=rtpmap:101 telephone-event/8000
        a=sendrecv
    """.trimIndent().replace("\n", "\r\n")

    private fun send(sock: DatagramSocket, host: String, port: Int, payload: String) {
        val data = payload.toByteArray(Charsets.UTF_8)
        val addr = InetAddress.getByName(host)
        sock.send(DatagramPacket(data, data.size, addr, port))
    }

    private fun header(msg: String, name: String): String? =
        msg.lineSequence().firstOrNull { it.startsWith("$name:", ignoreCase = true) }
            ?.substringAfter(":")?.trim()

    private fun parseChallenge(msg: String): DigestChallenge? {
        val line = header(msg, "WWW-Authenticate") ?: header(msg, "Proxy-Authenticate") ?: return null
        fun cap(key: String) = Regex("""$key="([^"]+)"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)
        val realm = cap("realm") ?: return null
        val nonce = cap("nonce") ?: return null
        return DigestChallenge(realm, nonce)
    }

    private fun digest(ch: DigestChallenge, method: String, uri: String): String =
        SipCrypto.digestResponse(
            username = account.username,
            password = account.password,
            realm = ch.realm,
            nonce = ch.nonce,
            method = method,
            uri = uri,
        )

    private fun localIp(): String = try {
        DatagramSocket().use { s ->
            s.connect(InetAddress.getByName("8.8.8.8"), 53)
            s.localAddress.hostAddress ?: "127.0.0.1"
        }
    } catch (_: Exception) {
        "127.0.0.1"
    }

    private fun normalize(target: String): String = target.trim().removePrefix("sip:")

    private fun randTag(): String = Random.nextBytes(6).joinToString("") { "%02x".format(it) }

    private data class DigestChallenge(val realm: String, val nonce: String)
}
