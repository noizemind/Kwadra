/* ES3-safe JS for IE6+ / Windows XP */
var BRIDGE = "http://127.0.0.1:38480";
var lastId = 0;
var myName = "XP-Desk";

function $(id) {
  return document.getElementById(id);
}

function setStatus(t) {
  var s = $("status");
  if (s) s.innerHTML = t;
}

function xhrGet(url, ok, fail) {
  var x = null;
  try { x = new XMLHttpRequest(); } catch (e1) {}
  if (!x) {
    try { x = new ActiveXObject("Msxml2.XMLHTTP"); } catch (e2) {}
  }
  if (!x) {
    try { x = new ActiveXObject("Microsoft.XMLHTTP"); } catch (e3) {}
  }
  if (!x) {
    if (fail) fail("no xhr");
    return;
  }
  try {
    x.open("GET", url, true);
    x.onreadystatechange = function () {
      if (x.readyState == 4) {
        if (x.status == 200 || x.status == 0) {
          if (ok) ok(x.responseText);
        } else {
          if (fail) fail(String(x.status));
        }
      }
    };
    x.send(null);
  } catch (e) {
    if (fail) fail(String(e.message || e));
  }
}

function xhrPost(url, body, ok, fail) {
  var x = null;
  try { x = new XMLHttpRequest(); } catch (e1) {}
  if (!x) {
    try { x = new ActiveXObject("Msxml2.XMLHTTP"); } catch (e2) {}
  }
  if (!x) {
    try { x = new ActiveXObject("Microsoft.XMLHTTP"); } catch (e3) {}
  }
  if (!x) {
    if (fail) fail("no xhr");
    return;
  }
  try {
    x.open("POST", url, true);
    x.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    x.onreadystatechange = function () {
      if (x.readyState == 4) {
        if (x.status == 200 || x.status == 0) {
          if (ok) ok(x.responseText);
        } else {
          if (fail) fail(String(x.status));
        }
      }
    };
    x.send(body);
  } catch (e) {
    if (fail) fail(String(e.message || e));
  }
}

function appendChat(from, text, mine) {
  var box = $("chat");
  if (!box) return;
  var cls = mine ? "bubble-me" : "bubble-them";
  var line = "<div><span class=\"" + cls + "\">" + escapeHtml(from) + ":</span> " + escapeHtml(text) + "</div>";
  box.innerHTML = box.innerHTML + line;
  box.scrollTop = box.scrollHeight;
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;");
}

function renderPeers(list) {
  var el = $("peers");
  if (!el) return;
  if (!list || list.length == 0) {
    el.innerHTML = "No peers yet.";
    return;
  }
  var html = "";
  for (var i = 0; i < list.length; i++) {
    html += "• " + escapeHtml(list[i].name || list[i].id) + "<br>";
  }
  el.innerHTML = html;
}

function poll() {
  xhrGet(BRIDGE + "/poll?since=" + lastId + "&name=" + encodeURIComponent(myName), function (txt) {
    setStatus("bridge: online");
    try {
      // Minimal JSON-ish parser for known shape without JSON.parse (IE7-)
      // Prefer eval-safe Function for our bridge only.
      var data = (new Function("return (" + txt + ");"))();
      if (data.peers) renderPeers(data.peers);
      if (data.messages) {
        for (var i = 0; i < data.messages.length; i++) {
          var m = data.messages[i];
          if (m.id > lastId) lastId = m.id;
          appendChat(m.fromName || "peer", m.text || "", !!m.mine);
        }
      }
    } catch (e) {
      setStatus("bridge: bad payload");
    }
    window.setTimeout(poll, 1200);
  }, function () {
    setStatus("bridge: offline (start bridge.js)");
    window.setTimeout(poll, 2500);
  });
}

function sendChat() {
  var inp = $("msg");
  if (!inp) return false;
  var text = inp.value;
  if (!text) return false;
  inp.value = "";
  appendChat(myName, text, true);
  xhrPost(
    BRIDGE + "/send",
    "name=" + encodeURIComponent(myName) + "&text=" + encodeURIComponent(text),
    function () {},
    function () { setStatus("bridge: send failed"); }
  );
  return false;
}

function fakeDial() {
  var n = $("num");
  var v = n ? n.value : "";
  appendChat("system", "SIP dial '" + v + "' needs Clearline Android/Windows app.", false);
  return false;
}

window.onload = function () {
  appendChat("system", "Clearline XP shell ready.", false);
  poll();
};
