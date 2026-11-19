const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("clearline", {
  configureSip: (account) => ipcRenderer.invoke("sip:configure", account),
  registerSip: () => ipcRenderer.invoke("sip:register"),
  onReg: (cb) => {
    const handler = (_e, payload) => cb(payload);
    ipcRenderer.on("sip:reg", handler);
    return () => ipcRenderer.removeListener("sip:reg", handler);
  },
  lanStart: (name) => ipcRenderer.invoke("lan:start", name),
  lanStop: () => ipcRenderer.invoke("lan:stop"),
  lanSend: (text) => ipcRenderer.invoke("lan:send", text),
  onLanPeers: (cb) => {
    const handler = (_e, payload) => cb(payload);
    ipcRenderer.on("lan:peers", handler);
    return () => ipcRenderer.removeListener("lan:peers", handler);
  },
  onLanMessage: (cb) => {
    const handler = (_e, payload) => cb(payload);
    ipcRenderer.on("lan:message", handler);
    return () => ipcRenderer.removeListener("lan:message", handler);
  },
});
