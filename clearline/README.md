# Clearline

Original SIP softphone for **Android** and **Windows**. Contacts, dialer, recents, in-call controls, and SIP account fields in the MicroSIP tradition (domain, proxy, digest auth) — with Clearline’s own interface and generated tones.

## Android

Module `:clearline` in this repo.

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew :clearline:testDebugUnitTest :clearline:assembleDebug
```

APK: `clearline/build/outputs/apk/debug/clearline-debug.apk`

Demo mode is on by default — register/calls work without a SIP server. Turn it off in Setup and fill username, password, domain to talk to a real PBX.

## Windows

```bash
cd windows-clearline
npm install
npm test
npm run build
npm start
```

Same demo softphone UI. Electron can also send a UDP REGISTER when demo mode is off.

## Sounds

Ringback, ringtone, connect, hangup, and DTMF are synthesized at runtime (Android `AudioTrack` / Web Audio). No third-party softphone assets are bundled.
