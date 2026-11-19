const { app, BrowserWindow, ipcMain, shell } = require("electron");
const path = require("path");
const dgram = require("dgram");
const crypto = require("crypto");

let mainWindow = null;
let sipSocket = null;
let account = {
  displayName: "",
  username: "",
  password: "",
  domain: "",
  proxy: "",
  port: 5060,
  demoMode: true,
};
let cseq = 1;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 420,
    height: 780,
    minWidth: 380,
    minHeight: 640,
    backgroundColor: "#E7F2F4",
    title: "Clearline",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  const dist = path.join(__dirname, "..", "dist", "index.html");
  mainWindow.loadFile(dist);
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: "deny" };
  });
}

function sendToUi(channel, payload) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send(channel, payload);
  }
}

function md5(s) {
  return crypto.createHash("md5").update(s).digest("hex");
}

function localIp() {
  return "127.0.0.1";
}

function sendSip(host, port, message) {
  return new Promise((resolve, reject) => {
    const sock = sipSocket || dgram.createSocket("udp4");
    sipSocket = sock;
    const buf = Buffer.from(message, "utf8");
    sock.send(buf, port, host, (err) => (err ? reject(err) : resolve()));
  });
}

ipcMain.handle("sip:configure", async (_e, acc) => {
  account = { ...account, ...acc };
  return { ok: true };
});

ipcMain.handle("sip:register", async () => {
  if (account.demoMode || !account.domain || !account.username) {
    sendToUi("sip:reg", { state: "REGISTERED", detail: "Demo · local softphone" });
    return { ok: true, demo: true };
  }
  sendToUi("sip:reg", { state: "CONNECTING", detail: "Registering…" });
  try {
    if (!sipSocket) {
      sipSocket = dgram.createSocket("udp4");
      sipSocket.on("message", (msg) => {
        const text = msg.toString("utf8");
        const m = text.match(/SIP\/2\.0\s+(\d+)/);
        const code = m ? Number(m[1]) : 0;
        if (code === 200 && /CSeq:.*REGISTER/i.test(text)) {
          sendToUi("sip:reg", { state: "REGISTERED", detail: "Registered" });
        } else if (code === 401 || code === 407) {
          // Minimal digest retry omitted in this shell; surface challenge.
          sendToUi("sip:reg", { state: "FAILED", detail: `Auth challenge ${code}` });
        } else if (code >= 400) {
          sendToUi("sip:reg", { state: "FAILED", detail: `SIP ${code}` });
        }
      });
    }
    const host = account.proxy || account.domain;
    const seq = cseq++;
    const callId = "reg-" + crypto.randomBytes(8).toString("hex");
    const branch = "z9hG4bK" + crypto.randomBytes(4).toString("hex");
    const uri = `sip:${account.domain}`;
    const msg = [
      `REGISTER ${uri} SIP/2.0`,
      `Via: SIP/2.0/UDP ${localIp()}:5060;branch=${branch};rport`,
      "Max-Forwards: 70",
      `From: "${account.displayName || account.username}"<sip:${account.username}@${account.domain}>;tag=${crypto.randomBytes(3).toString("hex")}`,
      `To: <sip:${account.username}@${account.domain}>`,
      `Call-ID: ${callId}`,
      `CSeq: ${seq} REGISTER`,
      `Contact: <sip:${account.username}@${localIp()}:5060>`,
      "Expires: 600",
      "User-Agent: Clearline/1.0",
      "Content-Length: 0",
      "",
      "",
    ].join("\r\n");
    await sendSip(host, account.port || 5060, msg);
    return { ok: true };
  } catch (e) {
    sendToUi("sip:reg", { state: "FAILED", detail: String(e.message || e) });
    return { ok: false, error: String(e.message || e) };
  }
});

app.whenReady().then(createWindow);
app.on("window-all-closed", () => {
  if (sipSocket) sipSocket.close();
  if (process.platform !== "darwin") app.quit();
});
