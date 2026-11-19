export type SipAccount = {
  displayName: string;
  username: string;
  password: string;
  domain: string;
  proxy: string;
  port: number;
  demoMode: boolean;
};

export type Contact = {
  id: string;
  name: string;
  uri: string;
  note: string;
};

export type CallDirection = "OUTGOING" | "INCOMING";
export type CallOutcome = "ANSWERED" | "MISSED" | "DECLINED" | "FAILED" | "CANCELLED";
export type RegState = "OFFLINE" | "CONNECTING" | "REGISTERED" | "FAILED";
export type CallPhase = "IDLE" | "DIALING" | "RINGING_OUT" | "RINGING_IN" | "ACTIVE" | "HELD" | "ENDING";

export type CallRecord = {
  id: string;
  peer: string;
  displayName: string;
  direction: CallDirection;
  outcome: CallOutcome;
  startedAt: number;
  durationSec: number;
};

export type ActiveCall = {
  id: string;
  peer: string;
  displayName: string;
  direction: CallDirection;
  phase: CallPhase;
  muted: boolean;
  onSpeaker: boolean;
  held: boolean;
  startedAt: number;
  connectedAt?: number;
};

export type LanPeer = { id: string; name: string; host: string; port: number };
export type LanMessage = {
  id: string;
  fromId: string;
  fromName: string;
  text: string;
  ts: number;
  mine: boolean;
};

export type AppState = {
  account: SipAccount;
  contacts: Contact[];
  recents: CallRecord[];
  regState: RegState;
  regDetail: string;
  activeCall: ActiveCall | null;
  tab: number;
  dialBuffer: string;
  search: string;
  lanOn: boolean;
  lanPeers: LanPeer[];
  lanMessages: LanMessage[];
  lanDraft: string;
  splash: boolean;
};

declare global {
  interface Window {
    clearline?: {
      configureSip: (account: SipAccount) => Promise<{ ok: boolean }>;
      registerSip: () => Promise<{ ok: boolean; demo?: boolean; error?: string }>;
      onReg: (cb: (p: { state: RegState; detail: string }) => void) => () => void;
      lanStart: (name: string) => Promise<{ ok: boolean; id?: string }>;
      lanStop: () => Promise<{ ok: boolean }>;
      lanSend: (text: string) => Promise<{ ok: boolean }>;
      onLanPeers: (cb: (peers: LanPeer[]) => void) => () => void;
      onLanMessage: (cb: (msg: LanMessage) => void) => () => void;
    };
  }
}
