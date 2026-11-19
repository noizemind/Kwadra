package app.clearline.lan

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

data class LanPeer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val lastSeen: Long = System.currentTimeMillis(),
)

data class LanMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val fromName: String,
    val text: String,
    val ts: Long = System.currentTimeMillis(),
    val mine: Boolean = false,
)

/**
 * LAN mesh: UDP presence beacons + TCP newline JSON chat.
 * Port 38471 UDP / 38472 TCP by default.
 */
class LanMesh(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPeers: (List<LanPeer>) -> Unit,
    private val onMessage: (LanMessage) -> Unit,
) {
    val selfId: String = UUID.randomUUID().toString().take(8)
    @Volatile var displayName: String = "Clearline"
    @Volatile var running: Boolean = false

    private val peers = ConcurrentHashMap<String, LanPeer>()
    private var beaconJob: Job? = null
    private var listenJob: Job? = null
    private var server: ServerSocket? = null
    private var udp: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    fun start(name: String) {
        if (running) return
        displayName = name.ifBlank { "Clearline" }
        running = true
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifi?.createMulticastLock("clearline-lan")?.also {
            it.setReferenceCounted(true)
            it.acquire()
        }
        scope.launch(Dispatchers.IO) {
            try {
                val srv = ServerSocket()
                srv.reuseAddress = true
                srv.bind(InetSocketAddress(TCP_PORT))
                server = srv
                listenJob = scope.launch(Dispatchers.IO) { acceptLoop(srv) }
                val sock = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(UDP_PORT))
                }
                udp = sock
                beaconJob = scope.launch(Dispatchers.IO) {
                    while (isActive && running) {
                        sendBeacon(sock)
                        prune()
                        delay(1500)
                        drainBeacons(sock)
                    }
                }
            } catch (e: Exception) {
                running = false
            }
        }
    }

    fun stop() {
        running = false
        beaconJob?.cancel()
        listenJob?.cancel()
        runCatching { server?.close() }
        runCatching { udp?.close() }
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
        peers.clear()
        onPeers(emptyList())
    }

    fun sendChat(text: String, peer: LanPeer? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val payload = JSONObject()
            .put("type", "chat")
            .put("id", UUID.randomUUID().toString())
            .put("fromId", selfId)
            .put("fromName", displayName)
            .put("text", trimmed)
            .put("ts", System.currentTimeMillis())
            .toString() + "\n"
        onMessage(
            LanMessage(
                fromId = selfId,
                fromName = displayName,
                text = trimmed,
                mine = true,
            ),
        )
        scope.launch(Dispatchers.IO) {
            val targets = if (peer != null) listOf(peer) else peers.values.toList()
            for (p in targets) {
                runCatching {
                    Socket().use { s ->
                        s.soTimeout = 2500
                        s.connect(InetSocketAddress(p.host, p.port), 2000)
                        s.getOutputStream().write(payload.toByteArray(Charsets.UTF_8))
                        s.getOutputStream().flush()
                    }
                }
            }
        }
    }

    private fun sendBeacon(sock: DatagramSocket) {
        val line = "CLPEER|$selfId|$displayName|$TCP_PORT\n"
        val data = line.toByteArray(Charsets.UTF_8)
        val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), UDP_PORT)
        runCatching { sock.send(packet) }
    }

    private fun drainBeacons(sock: DatagramSocket) {
        sock.soTimeout = 200
        val buf = ByteArray(512)
        repeat(12) {
            try {
                val packet = DatagramPacket(buf, buf.size)
                sock.receive(packet)
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                if (!text.startsWith("CLPEER|")) return@repeat
                val parts = text.split("|")
                if (parts.size < 4) return@repeat
                val id = parts[1]
                if (id == selfId) return@repeat
                val name = parts[2]
                val port = parts[3].toIntOrNull() ?: return@repeat
                val host = packet.address.hostAddress ?: return@repeat
                peers[id] = LanPeer(id = id, name = name, host = host, port = port, lastSeen = System.currentTimeMillis())
                onPeers(peers.values.sortedBy { it.name.lowercase() })
            } catch (_: Exception) {
                return
            }
        }
    }

    private fun prune() {
        val now = System.currentTimeMillis()
        var changed = false
        peers.entries.removeAll { (_, p) ->
            val stale = now - p.lastSeen > 8000
            if (stale) changed = true
            stale
        }
        // refresh lastSeen is done on beacon; also expire using stored lastSeen
        val before = peers.size
        peers.keys.toList().forEach { id ->
            val p = peers[id] ?: return@forEach
            if (now - p.lastSeen > 8000) {
                peers.remove(id)
            }
        }
        if (peers.size != before || changed) onPeers(peers.values.sortedBy { it.name.lowercase() })
    }

    private fun acceptLoop(srv: ServerSocket) {
        while (running && !srv.isClosed) {
            try {
                val client = srv.accept()
                thread(name = "cl-lan-rx", isDaemon = true) {
                    runCatching {
                        val line = client.getInputStream().bufferedReader(Charsets.UTF_8).readLine() ?: return@runCatching
                        val json = JSONObject(line)
                        if (json.optString("type") == "chat") {
                            onMessage(
                                LanMessage(
                                    id = json.optString("id", UUID.randomUUID().toString()),
                                    fromId = json.optString("fromId"),
                                    fromName = json.optString("fromName", "peer"),
                                    text = json.optString("text"),
                                    ts = json.optLong("ts", System.currentTimeMillis()),
                                    mine = false,
                                ),
                            )
                        }
                    }
                    runCatching { client.close() }
                }
            } catch (_: Exception) {
                if (!running) break
            }
        }
    }

    companion object {
        const val UDP_PORT = 38471
        const val TCP_PORT = 38472
    }
}
