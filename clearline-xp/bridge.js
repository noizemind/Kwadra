#!/usr/bin/env node
/**
 * Clearline LAN bridge for XP HTML (long-poll) + UDP/TCP mesh peer.
 * XP browser <-> HTTP :38480 <-> LAN UDP/TCP Clearline mesh
 */
const http = require("http");
const dgram = require("dgram");
const net = require("net");
const { URL } = require("url");
const crypto = require("crypto");
const fs = require("fs");
const path = require("path");

const UDP_PORT = 38471;
const TCP_PORT = 38472;
const HTTP_PORT = 38480;
const selfId = crypto.randomBytes(4).toString("hex");
const displayName = process.env.CL_NAME || "XP-Bridge";

const peers = new Map();
const messages = [];
let msgSeq = 0;

function addMessage(fromId, fromName, text, mine) {
  msgSeq += 1;
  const m = { id: msgSeq, fromId, fromName, text, ts: Date.now(), mine: !!mine };
  messages.push(m);
  if (messages.length > 300) messages.shift();
  return m;
}

function beacon() {
  const sock = dgram.createSocket("udp4");
  sock.bind(() => {
    sock.setBroadcast(true);
    const line = Buffer.from(`CLPEER|${selfId}|${displayName}|${TCP_PORT}\n`);
    setInterval(() => {
      sock.send(line, 0, line.length, UDP_PORT, "255.255.255.255");
    }, 1500);
  });
  const listen = dgram.createSocket({ type: "udp4", reuseAddr: true });
  listen.on("message", (msg, rinfo) => {
    const text = msg.toString("utf8").trim();
    if (!text.startsWith("CLPEER|")) return;
    const parts = text.split("|");
    if (parts.length < 4) return;
    const id = parts[1];
    if (id === selfId) return;
    peers.set(id, { id, name: parts[2], host: rinfo.address, port: Number(parts[3]) || TCP_PORT, lastSeen: Date.now() });
  });
  listen.bind(UDP_PORT);
}

function startTcp() {
  const server = net.createServer((socket) => {
    let buf = "";
    socket.on("data", (chunk) => {
      buf += chunk.toString("utf8");
      const idx = buf.indexOf("\n");
      if (idx >= 0) {
        const line = buf.slice(0, idx);
        try {
          const json = JSON.parse(line);
          if (json.type === "chat") {
            addMessage(json.fromId || "?", json.fromName || "peer", json.text || "", false);
          }
        } catch (_) {}
        socket.end();
      }
    });
  });
  server.listen(TCP_PORT);
}

function fanout(text) {
  const payload = JSON.stringify({
    type: "chat",
    id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now()),
    fromId: selfId,
    fromName: displayName,
    text,
    ts: Date.now(),
  }) + "\n";
  for (const p of peers.values()) {
    const s = net.connect({ host: p.host, port: p.port }, () => {
      s.write(payload);
      s.end();
    });
    s.on("error", () => {});
  }
}

function prune() {
  const now = Date.now();
  for (const [id, p] of peers) {
    if (now - p.lastSeen > 8000) peers.delete(id);
  }
}
setInterval(prune, 2000);

function cors(res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
}

const staticRoot = __dirname;
const server = http.createServer((req, res) => {
  cors(res);
  if (req.method === "OPTIONS") {
    res.writeHead(204);
    res.end();
    return;
  }
  const u = new URL(req.url, `http://127.0.0.1:${HTTP_PORT}`);

  if (u.pathname === "/poll") {
    const since = Number(u.searchParams.get("since") || 0);
    const out = {
      peers: [...peers.values()].map((p) => ({ id: p.id, name: p.name })),
      messages: messages.filter((m) => m.id > since),
    };
    res.writeHead(200, { "Content-Type": "text/plain; charset=utf-8" });
    res.end(JSON.stringify(out));
    return;
  }

  if (u.pathname === "/send" && req.method === "POST") {
    let body = "";
    req.on("data", (c) => (body += c));
    req.on("end", () => {
      const params = new URLSearchParams(body);
      const name = params.get("name") || displayName;
      const text = params.get("text") || "";
      if (text) {
        addMessage(selfId, name, text, true);
        fanout(text);
      }
      res.writeHead(200, { "Content-Type": "text/plain" });
      res.end("ok");
    });
    return;
  }

  let file = u.pathname === "/" ? "/index.html" : u.pathname;
  file = path.normalize(file).replace(/^(\.\.[/\\])+/, "");
  const full = path.join(staticRoot, file);
  if (!full.startsWith(staticRoot)) {
    res.writeHead(403);
    res.end("no");
    return;
  }
  fs.readFile(full, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end("missing");
      return;
    }
    const ext = path.extname(full);
    const type = ext === ".css" ? "text/css" : ext === ".js" ? "application/javascript" : "text/html";
    res.writeHead(200, { "Content-Type": type + "; charset=utf-8" });
    res.end(data);
  });
});

beacon();
startTcp();
server.listen(HTTP_PORT, "0.0.0.0", () => {
  console.log(`Clearline XP bridge http://127.0.0.1:${HTTP_PORT}  mesh udp/${UDP_PORT} tcp/${TCP_PORT}`);
});
