/** Original Clearline UI tones via Web Audio — not copied from commercial softphones. */

let ctx: AudioContext | null = null;

function ac(): AudioContext {
  if (!ctx) ctx = new AudioContext();
  return ctx;
}

function beep(freq: number, dur: number, gain = 0.08, when = 0, second?: number) {
  const c = ac();
  const t0 = c.currentTime + when;
  const osc = c.createOscillator();
  const g = c.createGain();
  osc.type = "sine";
  osc.frequency.setValueAtTime(freq, t0);
  g.gain.setValueAtTime(0.0001, t0);
  g.gain.exponentialRampToValueAtTime(gain, t0 + 0.01);
  g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
  osc.connect(g);
  g.connect(c.destination);
  osc.start(t0);
  osc.stop(t0 + dur + 0.02);
  if (second != null) {
    const osc2 = c.createOscillator();
    const g2 = c.createGain();
    osc2.type = "sine";
    osc2.frequency.setValueAtTime(second, t0);
    g2.gain.setValueAtTime(0.0001, t0);
    g2.gain.exponentialRampToValueAtTime(gain * 0.9, t0 + 0.01);
    g2.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
    osc2.connect(g2);
    g2.connect(c.destination);
    osc2.start(t0);
    osc2.stop(t0 + dur + 0.02);
  }
}

const DTMF: Record<string, [number, number]> = {
  "1": [697, 1209],
  "2": [697, 1336],
  "3": [697, 1477],
  "4": [770, 1209],
  "5": [770, 1336],
  "6": [770, 1477],
  "7": [852, 1209],
  "8": [852, 1336],
  "9": [852, 1477],
  "*": [941, 1209],
  "0": [941, 1336],
  "#": [941, 1477],
};

let loopTimer: number | null = null;

export const tones = {
  tap() {
    beep(720, 0.04, 0.06);
  },
  dial() {
    beep(640, 0.04, 0.06);
    beep(880, 0.03, 0.04, 0.03);
  },
  connect() {
    beep(392, 0.12, 0.08);
    beep(523, 0.12, 0.07, 0.09);
    beep(659, 0.16, 0.06, 0.18);
  },
  hangup() {
    beep(330, 0.18, 0.08);
    beep(196, 0.22, 0.06, 0.1);
  },
  dtmf(d: string) {
    const pair = DTMF[d];
    if (!pair) return;
    beep(pair[0], 0.11, 0.05, 0, pair[1]);
  },
  startRingback() {
    this.stopLoops();
    const tick = () => {
      beep(440, 0.9, 0.05, 0, 480);
      loopTimer = window.setTimeout(tick, 3100);
    };
    tick();
  },
  startRingtone() {
    this.stopLoops();
    const tick = () => {
      beep(523, 0.22, 0.08);
      beep(659, 0.28, 0.07, 0.3);
      loopTimer = window.setTimeout(tick, 1500);
    };
    tick();
  },
  stopLoops() {
    if (loopTimer != null) {
      clearTimeout(loopTimer);
      loopTimer = null;
    }
  },
};
