#!/usr/bin/env python3
# Clearline router bridge — Python 3 stdlib only (OpenWrt / busybox-friendly).
# Usage:  python3 clearline-router.py
# Open:   http://<router-ip>:38480/
#
# LAN mesh: UDP 38471 presence, TCP 38472 chat (same as Clearline apps).

from __future__ import print_function
import json
import os
import socket
import sys
import threading
import time
import uuid

try:
    from http.server import BaseHTTPRequestHandler, HTTPServer
    from socketserver import ThreadingMixIn
    from urllib.parse import parse_qs, urlparse
except ImportError:
    print("Need Python 3")
    sys.exit(1)

UDP_PORT = 38471
TCP_PORT = 38472
HTTP_PORT = int(os.environ.get("CL_PORT", "38480"))
NAME = os.environ.get("CL_NAME", "Router")
SELF = uuid.uuid4().hex[:8]
ROOT = os.path.dirname(os.path.abspath(__file__))

peers = {}
peers_lock = threading.Lock()
messages = []
msg_lock = threading.Lock()
msg_id = 0


def add_msg(from_id, from_name, text, mine=False):
    global msg_id
    with msg_lock:
        msg_id += 1
        m = {
            "id": msg_id,
            "fromId": from_id,
            "fromName": from_name,
            "text": text,
            "ts": int(time.time() * 1000),
            "mine": bool(mine),
        }
        messages.append(m)
        if len(messages) > 200:
            del messages[0 : len(messages) - 200]
        return m


def local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "0.0.0.0"


def beacon_loop():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try:
        sock.bind(("", UDP_PORT))
    except Exception as e:
        print("UDP bind warn:", e)
    sock.settimeout(0.4)
    line = ("CLPEER|%s|%s|%s\n" % (SELF, NAME, TCP_PORT)).encode("utf-8")
    while True:
        try:
            sock.sendto(line, ("255.255.255.255", UDP_PORT))
        except Exception:
            pass
        end = time.time() + 1.2
        while time.time() < end:
            try:
                data, addr = sock.recvfrom(512)
            except Exception:
                continue
            try:
                text = data.decode("utf-8", "ignore").strip()
            except Exception:
                continue
            if not text.startswith("CLPEER|"):
                continue
            parts = text.split("|")
            if len(parts) < 4:
                continue
            pid = parts[1]
            if pid == SELF:
                continue
            with peers_lock:
                peers[pid] = {
                    "id": pid,
                    "name": parts[2],
                    "host": addr[0],
                    "port": int(parts[3]) if parts[3].isdigit() else TCP_PORT,
                    "lastSeen": time.time(),
                }


def prune_loop():
    while True:
        time.sleep(2)
        now = time.time()
        with peers_lock:
            dead = [k for k, v in peers.items() if now - v["lastSeen"] > 8]
            for k in dead:
                del peers[k]


def tcp_server():
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("0.0.0.0", TCP_PORT))
    srv.listen(8)
    while True:
        try:
            conn, _ = srv.accept()
        except Exception:
            continue
        threading.Thread(target=handle_tcp, args=(conn,), daemon=True).start()


def handle_tcp(conn):
    try:
        conn.settimeout(3)
        buf = b""
        while b"\n" not in buf:
            chunk = conn.recv(1024)
            if not chunk:
                break
            buf += chunk
        line = buf.split(b"\n", 1)[0].decode("utf-8", "ignore")
        obj = json.loads(line)
        if obj.get("type") == "chat":
            add_msg(obj.get("fromId", "?"), obj.get("fromName", "peer"), obj.get("text", ""), False)
    except Exception:
        pass
    try:
        conn.close()
    except Exception:
        pass


def fanout(text):
    payload = (
        json.dumps(
            {
                "type": "chat",
                "id": uuid.uuid4().hex,
                "fromId": SELF,
                "fromName": NAME,
                "text": text,
                "ts": int(time.time() * 1000),
            }
        )
        + "\n"
    ).encode("utf-8")
    with peers_lock:
        targets = list(peers.values())
    for p in targets:
        try:
            s = socket.create_connection((p["host"], p["port"]), timeout=2)
            s.sendall(payload)
            s.close()
        except Exception:
            pass


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        return

    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def do_OPTIONS(self):
        self.send_response(204)
        self._cors()
        self.end_headers()

    def do_GET(self):
        u = urlparse(self.path)
        if u.path == "/poll":
            qs = parse_qs(u.query)
            since = int((qs.get("since") or ["0"])[0] or 0)
            with peers_lock:
                plist = [{"id": v["id"], "name": v["name"]} for v in peers.values()]
            with msg_lock:
                msgs = [m for m in messages if m["id"] > since]
            body = json.dumps({"peers": plist, "messages": msgs}).encode("utf-8")
            self.send_response(200)
            self._cors()
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        # static files next to this script
        path = u.path if u.path != "/" else "/index.html"
        path = path.replace("..", "")
        full = os.path.join(ROOT, path.lstrip("/"))
        if not os.path.isfile(full):
            self.send_error(404)
            return
        ext = os.path.splitext(full)[1].lower()
        ctype = "text/html; charset=utf-8"
        if ext == ".css":
            ctype = "text/css; charset=utf-8"
        elif ext == ".js":
            ctype = "application/javascript; charset=utf-8"
        with open(full, "rb") as f:
            data = f.read()
        self.send_response(200)
        self._cors()
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        u = urlparse(self.path)
        if u.path != "/send":
            self.send_error(404)
            return
        n = int(self.headers.get("Content-Length", "0") or 0)
        raw = self.rfile.read(n).decode("utf-8", "ignore")
        qs = parse_qs(raw)
        name = (qs.get("name") or [NAME])[0]
        text = (qs.get("text") or [""])[0].strip()
        if text:
            add_msg(SELF, name, text, True)
            fanout(text)
        body = b"ok"
        self.send_response(200)
        self._cors()
        self.send_header("Content-Type", "text/plain")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True
    allow_reuse_address = True


def main():
    for t in (beacon_loop, prune_loop, tcp_server):
        threading.Thread(target=t, daemon=True).start()
    ip = local_ip()
    httpd = ThreadedHTTPServer(("0.0.0.0", HTTP_PORT), Handler)
    print("Clearline router bridge")
    print("  open  http://%s:%s/" % (ip, HTTP_PORT))
    print("  also  http://127.0.0.1:%s/" % HTTP_PORT)
    print("  mesh  udp/%s tcp/%s  name=%s" % (UDP_PORT, TCP_PORT, NAME))
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nbye")


if __name__ == "__main__":
    main()
