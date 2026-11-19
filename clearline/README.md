# Clearline (Android module)

Original SIP softphone for Android, developed inside the Kwadra monorepo as `:clearline`.

The standalone Android repository target is **https://github.com/noizemind/Clearline** (create the empty repo, then the agent can push).

## Features

- Demo mode (default): simulated register & calls
- SIP REGISTER / INVITE / BYE + MD5 digest when demo is off
- G.711 µ-law RTP with demo mic→ear echo
- Foreground call notification
- Contacts, dialer, recents, mute / hold / speaker

## Build

```bash
export ANDROID_HOME=/path/to/Android/Sdk
./gradlew :clearline:testDebugUnitTest :clearline:assembleDebug
```

APK: `clearline/build/outputs/apk/debug/clearline-debug.apk`
