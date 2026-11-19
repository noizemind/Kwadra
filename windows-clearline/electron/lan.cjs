const dgram = require("dgram");
const net = require("net");
const crypto = require("crypto");

const UDP_PORT = 38471;
const TCP_PORT = 38472;

function createLan(send) {
  const selfId = crypto.randomBytes(4).toString("hex");
  let displayName = "Clearline-Win";
  let running = false;
  const peers = new Map();
  let udp = null;
  let server = null;
  let beaconTimer = null;
  let pruneTimer = null;

  function publishPeers() {
    send("lan:peers", [...peers.values()]);
  }

  function start(name) {
    if (running) return { ok: true, id: selfId };
    displayName = name || displayName;
    running = true;
    server = net.createServer((socket) => {
      let buf = "";
      socket.on("data", (chunk) => {
        buf += chunk.toString("utf8");
        const i = buf.indexOf("\n");
        if (i >= 0) {
          try {
            const json = JSON.parse(buf.slice(0, i));
            if (json.type === "chat") {
              send("lan:message", {
                id: json.id || crypto.randomBytes(4).toString("hex"),
                fromId: json.fromId,
                fromName: json.fromName,
                text: json.text,
                ts: json.ts || Date.now(),
                mine: false,
              });
            }
          } catch (_) {}
          socket.end();
        }
      });
    });
    server.listen(TCP_PORT);

    udp = dgram.createSocket({ type: "udp4", reuseAddr: true });
    udp.on("message", (msg, rinfo) => {
      const text = msg.toString("utf8").trim();
      if (!text.startsWith("CLPEER|")) return;
      const parts = text.split("|");
      if (parts.length < 4) return;
      const id = parts[1];
      if (id === selfId) return;
      peers.set(id, {
        id,
        name: parts[2],
        host: rinfo.address,
        port: Number(parts[3]) || TCP_PORT,
        lastSeen: Date.now(),
      });
      publishPeers();
    });
    udp.bind(UDP_PORT, () => {
      udp.setBroadcast(true);
    });

    beaconTimer = setInterval(() => {
      const line = Buffer.from(`CLPEER|${selfId}|${displayName}|${TCP_PORT}\n`);
      try {
        udp.send(line, 0, line.length, UDP_PORT, "255.255.255.255");
      } catch (_) {}
    }, 1500);

    pruneTimer = setInterval(() => {
      const now = Date.now();
      let changed = false;
      for (const [id, p] of peers) {
        if (now - p.lastSeen > 8000) {
          peers.delete(id);
          changed = true;
        }
      }
      if (changed) publishPeers();
    }, 2000);

    return { ok: true, id: selfId };
  }

  function stop() {
    running = false;
    if (beaconTimer) clearInterval(beaconTimer);
    if (pruneTimer) clearInterval(pruneTimer);
    try { udp && udp.close(); } catch (_) {}
    try { server && server.close(); } catch (_) {}
    peers.clear();
    publishPeers();
    return { ok: true };
  }

  function sendChat(text) {
    const trimmed = String(text || "").trim();
    if (!trimmed) return { ok: false };
    const payload =
      JSON.stringify({
        type: "chat",
        id: crypto.randomBytes(6).toString("hex"),
        fromId: selfId,
        fromName: displayName,
        text: trimmed,
        ts: Date.now(),
      }) + "\n";
    send("lan:message", {
      id: crypto.randomBytes(4).toString("hex"),
      fromId: selfId,
      fromName: displayName,
      text: trimmed,
      ts: Date.now(),
      mine: true,
    });
    for (const p of peers.values()) {
      const s = net.connect({ host: p.host, port: p.port }, () => {
        s.write(payload);
        s.end();
      });
      s.on("error", () => {});
    }
    return { ok: true };
  }

  return { start, stop, sendChat, selfId };
}

module.exports = { createLan };
