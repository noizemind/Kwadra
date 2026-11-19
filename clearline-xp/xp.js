/* ES3 — IE6+ / OpenWrt uhttpd browsers */
var BRIDGE = ""; /* same host */
var lastId = 0;

function $(id) { return document.getElementById(id); }

function xhr(method, url, body, ok, fail) {
  var x = null;
  try { x = new XMLHttpRequest(); } catch (e0) {}
  if (!x) { try { x = new ActiveXObject("Msxml2.XMLHTTP"); } catch (e1) {} }
  if (!x) { try { x = new ActiveXObject("Microsoft.XMLHTTP"); } catch (e2) {} }
  if (!x) { if (fail) fail("xhr"); return; }
  try {
    x.open(method, url, true);
    if (method == "POST") x.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    x.onreadystatechange = function () {
      if (x.readyState == 4) {
        if (x.status == 200 || x.status == 0) { if (ok) ok(x.responseText); }
        else { if (fail) fail(String(x.status)); }
      }
    };
    x.send(body || null);
  } catch (e) { if (fail) fail(String(e.message || e)); }
}

function esc(s) {
  return String(s).replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;");
}

function setStatus(t) { var s = $("status"); if (s) s.innerHTML = t; }

function line(from, text, mine) {
  var box = $("chat");
  if (!box) return;
  box.innerHTML += "<div><b class=\"" + (mine ? "bubble-me" : "bubble-them") + "\">" +
    esc(from) + ":</b> " + esc(text) + "</div>";
  box.scrollTop = box.scrollHeight;
}

function peersHtml(list) {
  var el = $("peers");
  if (!el) return;
  if (!list || !list.length) { el.innerHTML = "none"; return; }
  var h = "";
  for (var i = 0; i < list.length; i++) h += "• " + esc(list[i].name || list[i].id) + "<br>";
  el.innerHTML = h;
}

function nick() {
  var n = $("nick");
  return (n && n.value) ? n.value : "Guest";
}

function poll() {
  xhr("GET", BRIDGE + "/poll?since=" + lastId, null, function (txt) {
    setStatus("online");
    try {
      var data = (new Function("return (" + txt + ");"))();
      if (data.peers) peersHtml(data.peers);
      if (data.messages) {
        for (var i = 0; i < data.messages.length; i++) {
          var m = data.messages[i];
          if (m.id > lastId) lastId = m.id;
          if (!m.mine) line(m.fromName || "peer", m.text || "", false);
        }
      }
    } catch (e) { setStatus("bad data"); }
    window.setTimeout(poll, 1000);
  }, function () {
    setStatus("offline");
    window.setTimeout(poll, 2000);
  });
}

function sendChat() {
  var inp = $("msg");
  if (!inp || !inp.value) return false;
  var text = inp.value;
  inp.value = "";
  line(nick(), text, true);
  xhr("POST", BRIDGE + "/send",
    "name=" + encodeURIComponent(nick()) + "&text=" + encodeURIComponent(text),
    function () {}, function () { setStatus("send fail"); });
  return false;
}

window.onload = function () {
  line("system", "Clearline LAN ready", false);
  var msg = $("msg");
  if (msg) {
    msg.onkeypress = function (e) {
      e = e || window.event;
      var k = e.keyCode || e.which;
      if (k == 13) { sendChat(); return false; }
    };
  }
  poll();
};
