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
};

declare global {
  interface Window {
    clearline?: {
      configureSip: (account: SipAccount) => Promise<{ ok: boolean }>;
      registerSip: () => Promise<{ ok: boolean; demo?: boolean; error?: string }>;
      onReg: (cb: (p: { state: RegState; detail: string }) => void) => () => void;
    };
  }
}
