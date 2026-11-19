# Clearline on a router / Windows XP

**One file.** Copy `clearline-router.py` to the router (Python 3). No pip, no Node.

```bash
python3 clearline-router.py
```

Open: `http://<router-ip>:38480/`

## Without JavaScript

Works. Server HTML + POST form + auto-refresh every 5 seconds.

## With JavaScript (Windows XP / IE6+)

ES3-only live updates (ActiveX XHR). No modern JS APIs.

```bash
CL_NAME=Kitchen CL_PORT=38480 python3 clearline-router.py
```

`/health` — status JSON. Meshes with Clearline Android / Windows on the same Wi‑Fi.
