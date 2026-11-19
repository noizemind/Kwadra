# Clearline on a router

**One file.** Copy `clearline-router.py` to the router (Python 3). No pip, no Node, no extra HTML files.

```bash
python3 clearline-router.py
```

Open: `http://<router-ip>:38480/`

```bash
CL_NAME=Kitchen CL_PORT=38480 python3 clearline-router.py
```

Optional check: `http://<router-ip>:38480/health`

Meshes with Clearline Android / Windows LAN chat on the same Wi‑Fi.
