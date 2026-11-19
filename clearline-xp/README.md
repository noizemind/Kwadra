# Clearline XP

HTML 4.01 + ES3 JavaScript softphone shell that runs on **Windows XP** browsers (IE6/7/8).

LAN chat talks to nearby Clearline Android/Windows peers through `bridge.js`
(UDP presence + TCP chat + HTTP long-poll for the XP page).

## Run

```bash
node bridge.js
```

Open `http://127.0.0.1:38480/` in IE on XP (or any browser).

## Limits

- SIP calling is not available in IE — use Clearline Android / Windows for voice.
- IPA (iOS) builds need macOS + Xcode signing; this Linux agent cannot produce `.ipa`.
