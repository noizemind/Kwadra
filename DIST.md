# Clearline builds

| Artifact | Status |
| --- | --- |
| **Android APK** | `./gradlew :clearline:assembleDebug` → `clearline/build/outputs/apk/debug/clearline-debug.apk` |
| **Windows** | `cd windows-clearline && npm run build && npx electron-builder --win dir --x64 -c.win.signAndEditExecutable=false` → `release/win-unpacked/Clearline.exe` |
| **XP HTML** | `cd clearline-xp && node bridge.js` → open `http://127.0.0.1:38480/` |
| **iOS IPA** | Not buildable in this Linux environment. Needs macOS + Xcode + Apple Developer signing. |

## LAN P2P messenger

Peers announce on UDP `38471` (`CLPEER|id|name|tcpPort`) and chat on TCP `38472` (newline JSON).

Works across Clearline Android, Windows Electron, and the XP HTML bridge on the same Wi‑Fi / localhost.
