#!/usr/bin/env python3
# Clearline router — one file. Python 3 stdlib.
# Works WITHOUT JavaScript (HTML forms + meta refresh).
# With JS (IE6+ / XP): live poll. Copy only this file to the router.
#
#   python3 clearline-router.py
#   http://<router-ip>:38480/

ASSETS = {"aero.css": "/* Frutiger Aero-ish, IE6-friendly: tables, no flex/grid, no CSS3-only layout */\nbody {\n  margin: 0;\n  padding: 16px 0;\n  font-family: \"Segoe UI\", Tahoma, Verdana, Arial, sans-serif;\n  font-size: 13px;\n  color: #0A3A5C;\n  background: #9ED4F7;\n}\n\n.shell {\n  background: #E8F6FF;\n  border: 2px solid #FFFFFF;\n}\n\n.topglow {\n  background: #FFFFFF;\n  height: 12px;\n}\n\n.brand {\n  background: #2EA8E5;\n  color: #FFFFFF;\n}\n\n.logo {\n  font-size: 28px;\n  font-weight: bold;\n  color: #FFFFFF;\n}\n\n.tag {\n  font-size: 11px;\n  color: #E8FBFF;\n}\n\n.status {\n  font-size: 11px;\n  color: #FFFFFF;\n  background: #0B6BB5;\n  padding: 4px 8px;\n}\n\n.side {\n  background: #D9F0FF;\n  border-right: 1px solid #FFFFFF;\n  padding: 10px;\n}\n\n.main {\n  background: #F5FBFF;\n  padding: 10px;\n}\n\n.pane-title {\n  font-weight: bold;\n  color: #0B6BB5;\n  margin: 4px 0 8px 0;\n  border-bottom: 1px solid #B5D8F0;\n  padding-bottom: 3px;\n}\n\n.peers {\n  background: #FFFFFF;\n  border: 1px solid #B5D8F0;\n  padding: 8px;\n  height: 120px;\n  overflow: auto;\n  margin-bottom: 12px;\n}\n\n.chat {\n  background: #FFFFFF;\n  border: 1px solid #B5D8F0;\n  padding: 8px;\n  height: 280px;\n  overflow: auto;\n  margin-bottom: 8px;\n}\n\n.field {\n  border: 1px solid #7FE3F0;\n  background: #FFFFFF;\n  padding: 4px;\n  font-size: 13px;\n  color: #0A3A5C;\n}\n\n.wide {\n  width: 98%;\n}\n\n.btn {\n  background: #2EA8E5;\n  color: #FFFFFF;\n  border: 1px solid #0B6BB5;\n  font-weight: bold;\n  padding: 4px 10px;\n  cursor: pointer;\n}\n\n.hint {\n  font-size: 11px;\n  color: #5A7A94;\n  margin-top: 10px;\n}\n\n.foot {\n  background: #1E9BE0;\n  color: #FFFFFF;\n  font-size: 11px;\n  padding: 6px 10px;\n  text-align: center;\n}\n\n.bubble-me {\n  color: #0B6BB5;\n  font-weight: bold;\n}\n\n.bubble-them {\n  color: #1B5F8A;\n}\n", "xp.js": "/* ES3 — IE6+ / Windows XP. Progressive enhancement only. */\nvar BRIDGE = \"\";\nvar lastId = 0;\nvar jsMode = false;\n\nfunction $(id) { return document.getElementById(id); }\n\nfunction xhr(method, url, body, ok, fail) {\n  var x = null;\n  try { x = new XMLHttpRequest(); } catch (e0) {}\n  if (!x) { try { x = new ActiveXObject(\"Msxml2.XMLHTTP\"); } catch (e1) {} }\n  if (!x) { try { x = new ActiveXObject(\"Microsoft.XMLHTTP\"); } catch (e2) {} }\n  if (!x) { if (fail) fail(\"xhr\"); return; }\n  try {\n    x.open(method, url, true);\n    if (method == \"POST\") x.setRequestHeader(\"Content-Type\", \"application/x-www-form-urlencoded\");\n    x.onreadystatechange = function () {\n      if (x.readyState == 4) {\n        if (x.status == 200 || x.status == 0) { if (ok) ok(x.responseText); }\n        else { if (fail) fail(String(x.status)); }\n      }\n    };\n    x.send(body || null);\n  } catch (e) { if (fail) fail(String(e.message || e)); }\n}\n\nfunction esc(s) {\n  return String(s).replace(/&/g,\"&amp;\").replace(/</g,\"&lt;\").replace(/>/g,\"&gt;\");\n}\n\nfunction setStatus(t) { var s = $(\"status\"); if (s) s.innerHTML = t; }\n\nfunction nick() {\n  var n = $(\"nick\");\n  return (n && n.value) ? n.value : \"Guest\";\n}\n\nfunction syncNameHidden() {\n  var h = $(\"nameField\");\n  if (h) h.value = nick();\n}\n\nfunction peersHtml(list) {\n  var el = $(\"peers\");\n  if (!el) return;\n  if (!list || !list.length) { el.innerHTML = \"none\"; return; }\n  var h = \"\";\n  for (var i = 0; i < list.length; i++) h += \"• \" + esc(list[i].name || list[i].id) + \"<br>\";\n  el.innerHTML = h;\n}\n\nfunction appendLine(from, text, mine) {\n  var box = $(\"chat\");\n  if (!box) return;\n  box.innerHTML += \"<div><b class=\\\"\" + (mine ? \"bubble-me\" : \"bubble-them\") + \"\\\">\" +\n    esc(from) + \":</b> \" + esc(text) + \"</div>\";\n  box.scrollTop = box.scrollHeight;\n}\n\nfunction disableMetaRefresh() {\n  var metas = document.getElementsByTagName(\"meta\");\n  for (var i = 0; i < metas.length; i++) {\n    var http = metas[i].httpEquiv || metas[i].getAttribute(\"http-equiv\");\n    if (http && String(http).toLowerCase() == \"refresh\") {\n      if (metas[i].parentNode) metas[i].parentNode.removeChild(metas[i]);\n      break;\n    }\n  }\n}\n\nfunction poll() {\n  xhr(\"GET\", BRIDGE + \"/poll?since=\" + lastId, null, function (txt) {\n    setStatus(\"live (JS)\");\n    try {\n      var data = (new Function(\"return (\" + txt + \");\"))();\n      if (data.peers) peersHtml(data.peers);\n      if (data.messages) {\n        for (var i = 0; i < data.messages.length; i++) {\n          var m = data.messages[i];\n          if (m.id > lastId) lastId = m.id;\n          if (!m.mine) appendLine(m.fromName || \"peer\", m.text || \"\", false);\n        }\n      }\n    } catch (e) { setStatus(\"bad data\"); }\n    window.setTimeout(poll, 1000);\n  }, function () {\n    setStatus(\"offline\");\n    window.setTimeout(poll, 2000);\n  });\n}\n\nfunction sendAjax() {\n  var inp = $(\"msg\");\n  if (!inp || !inp.value) return false;\n  var text = inp.value;\n  inp.value = \"\";\n  appendLine(nick(), text, true);\n  syncNameHidden();\n  xhr(\"POST\", BRIDGE + \"/send\",\n    \"name=\" + encodeURIComponent(nick()) + \"&text=\" + encodeURIComponent(text) + \"&ajax=1\",\n    function () {}, function () { setStatus(\"send fail\"); });\n  return false;\n}\n\nwindow.onload = function () {\n  /* If we got here, JS works — switch to live mode */\n  jsMode = true;\n  disableMetaRefresh();\n  setStatus(\"live (JS)\");\n  syncNameHidden();\n  var nickEl = $(\"nick\");\n  if (nickEl) {\n    nickEl.onchange = syncNameHidden;\n    nickEl.onblur = syncNameHidden;\n  }\n  var forms = document.getElementsByTagName(\"form\");\n  for (var i = 0; i < forms.length; i++) {\n    if (forms[i].action && String(forms[i].action).indexOf(\"/send\") >= 0) {\n      forms[i].onsubmit = function () { return sendAjax(); };\n    }\n  }\n  var msg = $(\"msg\");\n  if (msg) {\n    msg.onkeypress = function (e) {\n      e = e || window.event;\n      var k = e.keyCode || e.which;\n      if (k == 13) { sendAjax(); return false; }\n    };\n  }\n  /* seed lastId from server-rendered count if present */\n  var box = $(\"chat\");\n  if (box && box.getAttribute) {\n    var sid = box.getAttribute(\"data-last-id\");\n    if (sid) lastId = parseInt(sid, 10) || 0;\n  }\n  poll();\n};\n", "index.html": "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n<html>\n<head>\n  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n  <title>Clearline</title>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"aero.css\">\n  <!-- no-JS auto refresh -->\n  <meta http-equiv=\"refresh\" content=\"5;url=/\">\n</head>\n<body bgcolor=\"#9ED4F7\">\n  <noscript>\n    <p style=\"background:#FFF3CD;border:1px solid #F0B429;padding:8px;margin:8px;font-family:Tahoma,sans-serif;font-size:12px;\">\n      JavaScript off — OK. Page refreshes every 5 seconds. Send works via the form below.\n    </p>\n  </noscript>\n  <table width=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n    <tr><td class=\"brand\">\n      <table width=\"100%\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\">\n        <tr>\n          <td><div class=\"logo\">Clearline</div><div class=\"tag\">LAN chat · XP / router</div></td>\n          <td align=\"right\"><span id=\"status\" class=\"status\">server HTML</span></td>\n        </tr>\n      </table>\n    </td></tr>\n    <tr>\n      <td>\n        <table width=\"100%\" border=\"0\" cellpadding=\"10\" cellspacing=\"0\">\n          <tr>\n            <td width=\"30%\" valign=\"top\" class=\"side\">\n              <div class=\"pane-title\">Peers</div>\n              <div id=\"peers\" class=\"peers\"><!--PEERS--></div>\n              <div class=\"pane-title\">Your name</div>\n              <form method=\"get\" action=\"/\">\n                <input type=\"text\" name=\"nick\" id=\"nick\" class=\"field\" value=\"<!--NICK-->\" size=\"16\">\n                <input type=\"submit\" class=\"btn\" value=\"Set\">\n              </form>\n            </td>\n            <td valign=\"top\" class=\"main\">\n              <div class=\"pane-title\">Chat</div>\n              <div id=\"chat\" class=\"chat\"><!--CHAT--></div>\n              <form method=\"post\" action=\"/send\" accept-charset=\"utf-8\">\n                <input type=\"hidden\" name=\"name\" id=\"nameField\" value=\"<!--NICK-->\">\n                <input type=\"text\" name=\"text\" id=\"msg\" class=\"field wide\" value=\"\">\n                <input type=\"submit\" class=\"btn\" value=\"Send\">\n              </form>\n              <p class=\"hint\">Works without JavaScript (form + refresh). With JS on XP/IE: live updates.</p>\n            </td>\n          </tr>\n        </table>\n      </td>\n    </tr>\n  </table>\n  <script type=\"text/javascript\" src=\"xp.js\"></script>\n</body>\n</html>\n"}

import json, os, socket, sys, threading, time, uuid
from html import escape

try:
    from http.server import BaseHTTPRequestHandler, HTTPServer
    from socketserver import ThreadingMixIn
    from urllib.parse import parse_qs, urlparse, quote
except ImportError:
    print("Need Python 3"); sys.exit(1)

UDP_PORT, TCP_PORT = 38471, 38472
HTTP_PORT = int(os.environ.get("CL_PORT", "38480"))
NAME = os.environ.get("CL_NAME", "Router")
SELF = uuid.uuid4().hex[:8]

peers, peers_lock = {}, threading.Lock()
messages, msg_lock = [], threading.Lock()
msg_id = [0]


def add_msg(from_id, from_name, text, mine=False):
    with msg_lock:
        msg_id[0] += 1
        m = {"id": msg_id[0], "fromId": from_id, "fromName": from_name,
             "text": text, "ts": int(time.time()*1000), "mine": bool(mine)}
        messages.append(m)
        if len(messages) > 200:
            del messages[:-200]
        return m


def local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80)); ip = s.getsockname()[0]; s.close(); return ip
    except Exception:
        return "0.0.0.0"


def beacon_loop():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    try: sock.bind(("", UDP_PORT))
    except Exception as e: print("UDP warn:", e)
    sock.settimeout(0.4)
    line = ("CLPEER|%s|%s|%s\n" % (SELF, NAME, TCP_PORT)).encode("utf-8")
    while True:
        try: sock.sendto(line, ("255.255.255.255", UDP_PORT))
        except Exception: pass
        end = time.time() + 1.2
        while time.time() < end:
            try: data, addr = sock.recvfrom(512)
            except Exception: continue
            text = data.decode("utf-8", "ignore").strip()
            if not text.startswith("CLPEER|"): continue
            parts = text.split("|")
            if len(parts) < 4: continue
            pid = parts[1]
            if pid == SELF: continue
            with peers_lock:
                peers[pid] = {"id": pid, "name": parts[2], "host": addr[0],
                              "port": int(parts[3]) if parts[3].isdigit() else TCP_PORT,
                              "lastSeen": time.time()}


def prune_loop():
    while True:
        time.sleep(2)
        now = time.time()
        with peers_lock:
            for k in [k for k,v in peers.items() if now - v["lastSeen"] > 8]:
                del peers[k]


def heartbeat():
    while True:
        time.sleep(10)
        with peers_lock:
            n = len(peers)
            names = ", ".join(v["name"] for v in list(peers.values())[:5])
        with msg_lock:
            m = len(messages)
        print("[hb] peers=%d msgs=%d %s" % (n, m, names))


def tcp_server():
    srv = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    srv.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    srv.bind(("0.0.0.0", TCP_PORT)); srv.listen(8)
    while True:
        try: conn, _ = srv.accept()
        except Exception: continue
        threading.Thread(target=handle_tcp, args=(conn,), daemon=True).start()


def handle_tcp(conn):
    try:
        conn.settimeout(3); buf = b""
        while b"\n" not in buf:
            chunk = conn.recv(1024)
            if not chunk: break
            buf += chunk
        obj = json.loads(buf.split(b"\n",1)[0].decode("utf-8","ignore"))
        if obj.get("type") == "chat":
            add_msg(obj.get("fromId","?"), obj.get("fromName","peer"), obj.get("text",""), False)
    except Exception:
        pass
    try: conn.close()
    except Exception: pass


def fanout(text):
    payload = (json.dumps({"type":"chat","id":uuid.uuid4().hex,"fromId":SELF,
                           "fromName":NAME,"text":text,"ts":int(time.time()*1000)})+"\n").encode("utf-8")
    with peers_lock: targets = list(peers.values())
    for p in targets:
        try:
            s = socket.create_connection((p["host"], p["port"]), timeout=2)
            s.sendall(payload); s.close()
        except Exception:
            pass


def render_page(nick="Guest"):
    nick = (nick or "Guest")[:40]
    with peers_lock:
        plist = list(peers.values())
    with msg_lock:
        msgs = list(messages)
        last = msgs[-1]["id"] if msgs else 0
    if plist:
        peers_html = "<br>".join("• %s" % escape(p["name"]) for p in plist)
    else:
        peers_html = "none"
    chat_bits = []
    for m in msgs[-80:]:
        cls = "bubble-me" if m.get("mine") else "bubble-them"
        chat_bits.append(
            '<div><b class="%s">%s:</b> %s</div>'
            % (cls, escape(m.get("fromName") or "peer"), escape(m.get("text") or ""))
        )
    chat_html = "\n".join(chat_bits) if chat_bits else "<div><i>no messages yet</i></div>"
    page = ASSETS["index.html"]
    page = page.replace("<!--PEERS-->", peers_html)
    page = page.replace("<!--CHAT-->", chat_html)
    page = page.replace("<!--NICK-->", escape(nick, quote=True))
    # data-last-id for JS progressive enhancement
    page = page.replace('<div id="chat" class="chat">', '<div id="chat" class="chat" data-last-id="%d">' % last)
    return page.encode("utf-8")


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a): return
    def _cors(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
    def do_OPTIONS(self):
        self.send_response(204); self._cors(); self.end_headers()
    def do_GET(self):
        u = urlparse(self.path)
        if u.path == "/health":
            with peers_lock: n = len(peers)
            body = json.dumps({"ok": True, "name": NAME, "peers": n, "self": SELF, "nojsless": True}).encode("utf-8")
            self.send_response(200); self._cors()
            self.send_header("Content-Type", "application/json"); self.send_header("Content-Length", str(len(body)))
            self.end_headers(); self.wfile.write(body); return
        if u.path == "/poll":
            qs = parse_qs(u.query); since = int((qs.get("since") or ["0"])[0] or 0)
            with peers_lock: plist = [{"id":v["id"],"name":v["name"]} for v in peers.values()]
            with msg_lock: msgs = [m for m in messages if m["id"] > since]
            body = json.dumps({"peers": plist, "messages": msgs}).encode("utf-8")
            self.send_response(200); self._cors()
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body); return
        if u.path in ("/", "/index.html"):
            qs = parse_qs(u.query)
            nick = (qs.get("nick") or ["Guest"])[0]
            data = render_page(nick)
            self.send_response(200); self._cors()
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(data))); self.end_headers(); self.wfile.write(data); return
        key = u.path.lstrip("/")
        if key not in ASSETS:
            self.send_error(404); return
        data = ASSETS[key].encode("utf-8")
        ctype = "text/css; charset=utf-8" if key.endswith(".css") else "application/javascript; charset=utf-8"
        self.send_response(200); self._cors()
        self.send_header("Content-Type", ctype); self.send_header("Content-Length", str(len(data)))
        self.end_headers(); self.wfile.write(data)
    def do_POST(self):
        u = urlparse(self.path)
        if u.path != "/send":
            self.send_error(404); return
        n = int(self.headers.get("Content-Length", "0") or 0)
        qs = parse_qs(self.rfile.read(n).decode("utf-8", "ignore"))
        name = (qs.get("name") or [NAME])[0]
        text = (qs.get("text") or [""])[0].strip()
        ajax = (qs.get("ajax") or [""])[0]
        if text:
            add_msg(SELF, name, text, True); fanout(text)
        if ajax:
            body = b"ok"
            self.send_response(200); self._cors()
            self.send_header("Content-Type", "text/plain"); self.send_header("Content-Length", str(len(body)))
            self.end_headers(); self.wfile.write(body); return
        # no-JS form: redirect back to page with nick preserved
        loc = "/?nick=%s" % quote(name)
        self.send_response(303); self._cors()
        self.send_header("Location", loc); self.end_headers()


class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    daemon_threads = True
    allow_reuse_address = True


def main():
    for t in (beacon_loop, prune_loop, tcp_server, heartbeat):
        threading.Thread(target=t, daemon=True).start()
    ip = local_ip()
    httpd = ThreadedHTTPServer(("0.0.0.0", HTTP_PORT), Handler)
    print("=" * 48)
    print("  Clearline router (no-JS OK, XP IE OK)")
    print("  http://%s:%s/" % (ip, HTTP_PORT))
    print("  /health   name=%s" % NAME)
    print("=" * 48)
    try: httpd.serve_forever()
    except KeyboardInterrupt: print("\nbye")


if __name__ == "__main__":
    main()
