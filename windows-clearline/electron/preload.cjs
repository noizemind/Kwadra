const { contextBridge, ipcRenderer } = require("electron");

contextBridge.exposeInMainWorld("clearline", {
  configureSip: (account) => ipcRenderer.invoke("sip:configure", account),
  registerSip: () => ipcRenderer.invoke("sip:register"),
  onReg: (cb) => {
    const handler = (_e, payload) => cb(payload);
    ipcRenderer.on("sip:reg", handler);
    return () => ipcRenderer.removeListener("sip:reg", handler);
  },
});
