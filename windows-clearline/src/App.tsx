import { useEffect, useMemo, useState } from "react";
import { tones } from "./lib/tones";
import type {
  ActiveCall,
  AppState,
  CallOutcome,
  CallRecord,
  Contact,
  SipAccount,
} from "./lib/types";

const SEED: Contact[] = [
  { id: "1", name: "Echo Lab", uri: "echo@demo.clearline", note: "Demo loopback" },
  { id: "2", name: "Desk Phone", uri: "1001@office.local", note: "Office PBX" },
  { id: "3", name: "Night Desk", uri: "ops@night.clearline", note: "On-call" },
];

function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function uid() {
  return crypto.randomUUID();
}

function formatDuration(sec: number) {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

function regLabel(state: AppState["regState"], detail: string) {
  if (state === "OFFLINE") return "Offline";
  if (state === "CONNECTING") return "Connecting…";
  if (state === "FAILED") return `Failed · ${detail}`;
  return detail || "Online";
}

const defaultAccount: SipAccount = {
  displayName: "",
  username: "",
  password: "",
  domain: "",
  proxy: "",
  port: 5060,
  demoMode: true,
};

export function App() {
  const [state, setState] = useState<AppState>(() => ({
    account: load("cl.account", defaultAccount),
    contacts: load("cl.contacts", SEED),
    recents: load("cl.recents", [] as CallRecord[]),
    regState: "CONNECTING",
    regDetail: "Demo mode",
    activeCall: null,
    tab: 0,
    dialBuffer: "",
    search: "",
  }));
  const [edit, setEdit] = useState<Contact | null>(null);
  const [draftAcc, setDraftAcc] = useState(state.account);
  const [now, setNow] = useState(Date.now());

  useEffect(() => {
    localStorage.setItem("cl.account", JSON.stringify(state.account));
  }, [state.account]);
  useEffect(() => {
    localStorage.setItem("cl.contacts", JSON.stringify(state.contacts));
  }, [state.contacts]);
  useEffect(() => {
    localStorage.setItem("cl.recents", JSON.stringify(state.recents));
  }, [state.recents]);

  useEffect(() => {
    const t = window.setTimeout(() => {
      setState((s) => ({ ...s, regState: "REGISTERED", regDetail: "Demo · local softphone" }));
    }, 400);
    const off = window.clearline?.onReg((p) => {
      setState((s) => ({ ...s, regState: p.state, regDetail: p.detail }));
    });
    return () => {
      clearTimeout(t);
      off?.();
    };
  }, []);

  useEffect(() => {
    if (!state.activeCall || (state.activeCall.phase !== "ACTIVE" && state.activeCall.phase !== "HELD")) return;
    const id = window.setInterval(() => setNow(Date.now()), 500);
    return () => clearInterval(id);
  }, [state.activeCall?.phase, state.activeCall?.connectedAt]);

  const filtered = useMemo(() => {
    const q = state.search.trim().toLowerCase();
    if (!q) return state.contacts;
    return state.contacts.filter((c) => c.name.toLowerCase().includes(q) || c.uri.toLowerCase().includes(q));
  }, [state.contacts, state.search]);

  function endCall(outcome: CallOutcome) {
    const call = state.activeCall;
    if (!call) return;
    tones.stopLoops();
    tones.hangup();
    const dur = call.connectedAt ? Math.floor((Date.now() - call.connectedAt) / 1000) : 0;
    const record: CallRecord = {
      id: call.id,
      peer: call.peer,
      displayName: call.displayName,
      direction: call.direction,
      outcome,
      startedAt: call.startedAt,
      durationSec: dur,
    };
    setState((s) => ({
      ...s,
      activeCall: { ...call, phase: "ENDING" },
      recents: [record, ...s.recents].slice(0, 100),
    }));
    window.setTimeout(() => setState((s) => ({ ...s, activeCall: null })), 220);
  }

  function placeCall(uri: string, name = "") {
    if (!uri.trim() || state.activeCall) return;
    tones.dial();
    const display = name || state.contacts.find((c) => c.uri.toLowerCase() === uri.toLowerCase())?.name || uri;
    const call: ActiveCall = {
      id: uid(),
      peer: uri.trim(),
      displayName: display,
      direction: "OUTGOING",
      phase: "DIALING",
      muted: false,
      onSpeaker: false,
      held: false,
      startedAt: Date.now(),
    };
    setState((s) => ({ ...s, activeCall: call, dialBuffer: "", tab: 0 }));
    window.setTimeout(() => {
      tones.startRingback();
      setState((s) => (s.activeCall ? { ...s, activeCall: { ...s.activeCall, phase: "RINGING_OUT" } } : s));
    }, 350);
    window.setTimeout(() => {
      tones.stopLoops();
      tones.connect();
      setState((s) =>
        s.activeCall
          ? { ...s, activeCall: { ...s.activeCall, phase: "ACTIVE", connectedAt: Date.now() } }
          : s,
      );
    }, 2150);
  }

  function demoIncoming() {
    if (state.activeCall) return;
    const from = state.contacts[0] || SEED[0];
    tones.startRingtone();
    setState((s) => ({
      ...s,
      activeCall: {
        id: uid(),
        peer: from.uri,
        displayName: from.name,
        direction: "INCOMING",
        phase: "RINGING_IN",
        muted: false,
        onSpeaker: false,
        held: false,
        startedAt: Date.now(),
      },
    }));
  }

  async function saveAccount() {
    setState((s) => ({ ...s, account: draftAcc, regState: "CONNECTING", regDetail: "Registering…" }));
    tones.connect();
    await window.clearline?.configureSip(draftAcc);
    if (draftAcc.demoMode || !draftAcc.domain || !draftAcc.username) {
      setState((s) => ({ ...s, regState: "REGISTERED", regDetail: "Demo · local softphone" }));
      return;
    }
    await window.clearline?.registerSip();
  }

  const call = state.activeCall;

  return (
    <div className="app">
      {state.tab === 0 && (
        <div className="pane">
          <header className="hero">
            <div>
              <h1>Clearline</h1>
              <p>{regLabel(state.regState, state.regDetail)}</p>
            </div>
            <div className="hero-actions">
              <button className="round-btn" onClick={() => setEdit({ id: uid(), name: "", uri: "", note: "" })}>
                +
              </button>
            </div>
          </header>
          <input
            className="search"
            placeholder="Search people"
            value={state.search}
            onChange={(e) => setState((s) => ({ ...s, search: e.target.value }))}
          />
          <div className="list">
            {filtered.map((c) => (
              <div className="row" key={c.id} onClick={() => setEdit(c)}>
                <div className="avatar">{c.name.slice(0, 1).toUpperCase()}</div>
                <div className="row-body">
                  <strong>{c.name}</strong>
                  <span>{c.uri}</span>
                </div>
                <button
                  className="call-chip"
                  onClick={(e) => {
                    e.stopPropagation();
                    placeCall(c.uri, c.name);
                  }}
                >
                  ☎
                </button>
              </div>
            ))}
            {!filtered.length && <div className="empty">No contacts yet</div>}
          </div>
        </div>
      )}

      {state.tab === 1 && (
        <div className="pane dialer">
          <div className="brand">Clearline</div>
          <div className="sub">Dial anyone on your SIP line</div>
          <div className="buffer">{state.dialBuffer || " "}</div>
          <div className="keys">
            {["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"].map((k) => (
              <button
                key={k}
                className="key"
                onClick={() => {
                  tones.dtmf(k);
                  setState((s) => ({ ...s, dialBuffer: s.dialBuffer + k }));
                }}
              >
                {k}
              </button>
            ))}
          </div>
          <div className="dial-actions">
            <button
              className="round-btn"
              onClick={() => setState((s) => ({ ...s, dialBuffer: s.dialBuffer.slice(0, -1) }))}
            >
              ⌫
            </button>
            <button className="hang-green" onClick={() => placeCall(state.dialBuffer)}>
              ☎
            </button>
            <span style={{ width: 44 }} />
          </div>
        </div>
      )}

      {state.tab === 2 && (
        <div className="pane">
          <header className="hero">
            <div>
              <h1>Clearline</h1>
              <p>Recent calls</p>
            </div>
          </header>
          <div className="list">
            {state.recents.map((r) => (
              <div className="row" key={r.id} onClick={() => placeCall(r.peer, r.displayName)}>
                <div className="avatar" style={{ background: r.outcome === "ANSWERED" ? undefined : "linear-gradient(135deg,#c45c4a,#1a3442)" }}>
                  ☎
                </div>
                <div className="row-body">
                  <strong>{r.displayName}</strong>
                  <span>
                    {r.direction === "INCOMING" ? "Incoming" : "Outgoing"} · {r.outcome.toLowerCase()}
                    {r.durationSec ? ` · ${formatDuration(r.durationSec)}` : ""}
                  </span>
                </div>
                <span className="meta">{new Date(r.startedAt).toLocaleString()}</span>
              </div>
            ))}
            {!state.recents.length && <div className="empty">Calls you make will land here</div>}
          </div>
        </div>
      )}

      {state.tab === 3 && (
        <div className="pane settings">
          <header className="hero">
            <div>
              <h1>Clearline</h1>
              <p>SIP account · MicroSIP-style fields</p>
            </div>
          </header>
          {(
            [
              ["Display name", "displayName"],
              ["Username", "username"],
              ["Password", "password"],
              ["Domain", "domain"],
              ["Proxy (optional)", "proxy"],
            ] as const
          ).map(([label, key]) => (
            <div className="field" key={key}>
              <label>{label}</label>
              <input
                type={key === "password" ? "password" : "text"}
                value={draftAcc[key]}
                onChange={(e) => setDraftAcc({ ...draftAcc, [key]: e.target.value })}
              />
            </div>
          ))}
          <div className="field">
            <label>Port</label>
            <input
              type="number"
              value={draftAcc.port}
              onChange={(e) => setDraftAcc({ ...draftAcc, port: Number(e.target.value) || 5060 })}
            />
          </div>
          <div className="toggle" onClick={() => setDraftAcc({ ...draftAcc, demoMode: !draftAcc.demoMode })}>
            <div>
              <strong>Demo mode</strong>
              <div className="meta">Simulate register & calls without a SIP server</div>
            </div>
            <div className={`switch ${draftAcc.demoMode ? "on" : ""}`}>
              <i />
            </div>
          </div>
          <button className="btn primary" onClick={saveAccount}>
            Save & register
          </button>
          <button className="btn ghost" onClick={demoIncoming}>
            Simulate incoming call
          </button>
          <p className="note">
            Clearline is an original softphone. SIP flows follow classic REGISTER/INVITE/BYE patterns used by open
            softphones such as MicroSIP — the interface and sounds are Clearline’s own.
          </p>
        </div>
      )}

      <nav className="nav">
        {[
          ["People", 0],
          ["Dial", 1],
          ["Recents", 2],
          ["Setup", 3],
        ].map(([label, idx]) => (
          <button
            key={label}
            className={state.tab === idx ? "on" : ""}
            onClick={() => {
              tones.tap();
              setState((s) => ({ ...s, tab: idx as number }));
            }}
          >
            {label}
          </button>
        ))}
      </nav>

      {call && (
        <div className="call-overlay">
          <div className="eyebrow">Clearline</div>
          <div className="call-avatar">{call.displayName.slice(0, 1).toUpperCase()}</div>
          <h2>{call.displayName}</h2>
          <div className="peer">{call.peer}</div>
          <div className="phase">
            {call.phase === "DIALING" && "Calling…"}
            {call.phase === "RINGING_OUT" && "Ringing…"}
            {call.phase === "RINGING_IN" && "Incoming call"}
            {call.phase === "ACTIVE" &&
              formatDuration(Math.floor((now - (call.connectedAt || now)) / 1000))}
            {call.phase === "HELD" && "On hold"}
            {call.phase === "ENDING" && "Call ended"}
          </div>
          {call.phase === "RINGING_IN" ? (
            <div className="call-controls">
              <button className="hang-red" onClick={() => endCall("DECLINED")}>
                ✕
              </button>
              <button
                className="hang-green"
                onClick={() => {
                  tones.stopLoops();
                  tones.connect();
                  setState((s) =>
                    s.activeCall
                      ? { ...s, activeCall: { ...s.activeCall, phase: "ACTIVE", connectedAt: Date.now() } }
                      : s,
                  );
                }}
              >
                ☎
              </button>
            </div>
          ) : (
            <>
              <div className="call-controls">
                <button
                  className="soft-act"
                  onClick={() => {
                    tones.tap();
                    setState((s) =>
                      s.activeCall ? { ...s, activeCall: { ...s.activeCall, muted: !s.activeCall.muted } } : s,
                    );
                  }}
                >
                  <span className="icon">{call.muted ? "🔇" : "🎙"}</span>
                  {call.muted ? "Unmute" : "Mute"}
                </button>
                <button
                  className="soft-act"
                  onClick={() => {
                    tones.tap();
                    setState((s) =>
                      s.activeCall
                        ? {
                            ...s,
                            activeCall: {
                              ...s.activeCall,
                              held: !s.activeCall.held,
                              phase: !s.activeCall.held ? "HELD" : "ACTIVE",
                            },
                          }
                        : s,
                    );
                  }}
                >
                  <span className="icon">{call.held ? "▶" : "⏸"}</span>
                  {call.held ? "Resume" : "Hold"}
                </button>
                <button
                  className="soft-act"
                  onClick={() => {
                    tones.tap();
                    setState((s) =>
                      s.activeCall ? { ...s, activeCall: { ...s.activeCall, onSpeaker: !s.activeCall.onSpeaker } } : s,
                    );
                  }}
                >
                  <span className="icon">🔊</span>
                  Speaker
                </button>
              </div>
              <button
                className="hang-red"
                onClick={() => endCall(call.connectedAt ? "ANSWERED" : "CANCELLED")}
              >
                ✕
              </button>
            </>
          )}
        </div>
      )}

      {edit && (
        <div className="modal-back" onClick={() => setEdit(null)}>
          <div className="sheet" onClick={(e) => e.stopPropagation()}>
            <h3>Contact</h3>
            <div className="field">
              <label>Name</label>
              <input value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} />
            </div>
            <div className="field">
              <label>SIP URI / number</label>
              <input value={edit.uri} onChange={(e) => setEdit({ ...edit, uri: e.target.value })} />
            </div>
            <div className="field">
              <label>Note</label>
              <input value={edit.note} onChange={(e) => setEdit({ ...edit, note: e.target.value })} />
            </div>
            <button
              className="btn primary"
              onClick={() => {
                if (!edit.name.trim() || !edit.uri.trim()) return;
                tones.tap();
                setState((s) => {
                  const others = s.contacts.filter((c) => c.id !== edit.id);
                  return {
                    ...s,
                    contacts: [...others, { ...edit, name: edit.name.trim(), uri: edit.uri.trim() }].sort((a, b) =>
                      a.name.localeCompare(b.name),
                    ),
                  };
                });
                setEdit(null);
              }}
            >
              Save
            </button>
            {state.contacts.some((c) => c.id === edit.id) && (
              <button
                className="btn ghost"
                onClick={() => {
                  setState((s) => ({ ...s, contacts: s.contacts.filter((c) => c.id !== edit.id) }));
                  setEdit(null);
                }}
              >
                Delete
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
