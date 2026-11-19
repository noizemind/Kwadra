# Clearline on a router

One Python 3 file. No Node. No pip.

```bash
cd clearline-xp
python3 clearline-router.py
```

Open on any device in the LAN:

```
http://<router-ip>:38480/
```

Optional:

```bash
CL_NAME=Kitchen CL_PORT=38480 python3 clearline-router.py
```

Talks to Clearline Android / Windows LAN chat on the same Wi‑Fi.
