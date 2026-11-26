// Upbeat, bouncy kitchen music — 115 BPM, C-G-Am-F, major pentatonic pluck melody
// Web Audio only, no external files

class UpbeatMusicEngine {
  constructor() {
    this.ctx = null;
    this.masterGain = null;
    this.filter = null;
    this.isMuted = false;
    this.isPlaying = false;
    this.timerId = null;
    this.beat = 0;

    // 115 BPM → bar = 4 beats = 4*(60/115) ≈ 2.087 s
    this.BPM = 115;
    this.beatDur = 60 / this.BPM;        // seconds per beat
    this.barDur  = this.beatDur * 4;     // 4 beats per bar

    // C  G  Am  F chord roots (bass octave 2)
    this.chordRoots = [65.41, 49.00, 55.00, 43.65];

    // Major pentatonic scale in C: C D E G A (two octaves)
    this.pentatonic = [523.25, 587.33, 659.25, 783.99, 880.00,
                       1046.50, 783.99, 659.25, 523.25, 587.33,
                       783.99, 523.25, 880.00, 659.25, 783.99, 587.33];

    try {
      this.isMuted = localStorage.getItem('music-muted') === 'true';
    } catch { /* ignore */ }
  }

  _initCtx() {
    if (this.ctx) return true;
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return false;
    this.ctx = new AC();
    this.masterGain = this.ctx.createGain();
    this.masterGain.gain.value = this.isMuted ? 0 : 0.10;

    this.filter = this.ctx.createBiquadFilter();
    this.filter.type = 'lowpass';
    this.filter.frequency.value = 3200;
    this.filter.Q.value = 0.8;

    this.filter.connect(this.masterGain);
    this.masterGain.connect(this.ctx.destination);
    return true;
  }

  async start() {
    if (!this._initCtx()) return false;
    if (this.ctx.state === 'suspended') {
      try { await this.ctx.resume(); } catch { return false; }
    }
    if (this.ctx.state !== 'running') return false;
    if (!this.isPlaying) {
      this.isPlaying = true;
      this._scheduleBar(this.ctx.currentTime);
    }
    return true;
  }

  _osc(freq, type, t0, dur, vol) {
    const o = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    o.type = type;
    o.frequency.setValueAtTime(freq, t0);
    g.gain.setValueAtTime(0.0001, t0);
    g.gain.linearRampToValueAtTime(vol, t0 + 0.012);
    g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
    o.connect(g); g.connect(this.filter);
    o.start(t0); o.stop(t0 + dur + 0.02);
  }

  _noise(t0, dur, vol) {
    const bufLen = Math.ceil(this.ctx.sampleRate * dur);
    const buf = this.ctx.createBuffer(1, bufLen, this.ctx.sampleRate);
    const data = buf.getChannelData(0);
    for (let i = 0; i < bufLen; i++) data[i] = Math.random() * 2 - 1;
    const src = this.ctx.createBufferSource();
    src.buffer = buf;
    const bp = this.ctx.createBiquadFilter();
    bp.type = 'bandpass';
    bp.frequency.value = 8000;
    bp.Q.value = 0.6;
    const g = this.ctx.createGain();
    g.gain.setValueAtTime(vol, t0);
    g.gain.exponentialRampToValueAtTime(0.0001, t0 + dur);
    src.connect(bp); bp.connect(g); g.connect(this.masterGain);
    src.start(t0); src.stop(t0 + dur + 0.01);
  }

  _kick(t) {
    const o = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    o.type = 'sine';
    o.frequency.setValueAtTime(160, t);
    o.frequency.exponentialRampToValueAtTime(50, t + 0.12);
    g.gain.setValueAtTime(0.42, t);
    g.gain.exponentialRampToValueAtTime(0.0001, t + 0.18);
    o.connect(g); g.connect(this.masterGain);
    o.start(t); o.stop(t + 0.22);
  }

  _scheduleBar(t) {
    if (!this.ctx || this.ctx.state !== 'running' || this.isMuted) return;

    const bar  = this.beat % 4;
    const root = this.chordRoots[bar];
    const bd   = this.beatDur;
    const mel  = this.pentatonic;
    const mOff = (this.beat * 4) % mel.length;

    // Bouncy bass
    this._osc(root,       'triangle', t,          bd * 0.45, 0.22);
    this._osc(root * 2,   'triangle', t + bd,     bd * 0.35, 0.14);
    this._osc(root,       'triangle', t + bd * 2, bd * 0.45, 0.20);
    this._osc(root * 1.5, 'triangle', t + bd * 3, bd * 0.35, 0.12);

    // Staccato pluck melody
    for (let i = 0; i < 4; i++) {
      const freq = mel[(mOff + i) % mel.length];
      this._osc(freq,     'square', t + i * bd, bd * 0.22, 0.055);
      this._osc(freq * 2, 'sine',   t + i * bd, bd * 0.18, 0.018);
    }

    // Chord pad
    const third = root * 4 * 1.26;
    const fifth  = root * 4 * 1.498;
    this._osc(root * 4, 'sine', t, this.barDur * 0.9, 0.055);
    this._osc(third,    'sine', t, this.barDur * 0.9, 0.040);
    this._osc(fifth,    'sine', t, this.barDur * 0.9, 0.035);

    // Kick on beats 1 & 3
    this._kick(t);
    this._kick(t + bd * 2);

    // Hi-hat every beat + off-beat
    for (let i = 0; i < 4; i++) {
      this._noise(t + i * bd,          0.08, 0.025);
      this._noise(t + i * bd + bd * 0.5, 0.06, 0.012);
    }

    this.beat++;
    this.timerId = window.setTimeout(
      () => this._scheduleBar(t + this.barDur),
      (this.barDur - 0.08) * 1000
    );
  }

  setMuted(muted) {
    this.isMuted = muted;
    try { localStorage.setItem('music-muted', String(muted)); } catch { /* ignore */ }
    if (this.masterGain && this.ctx) {
      const now = this.ctx.currentTime;
      this.masterGain.gain.cancelScheduledValues(now);
      this.masterGain.gain.setTargetAtTime(muted ? 0 : 0.10, now, 0.06);
    }
    if (!muted) this.start();
    return this.isMuted;
  }

  toggleMute() {
    return this.setMuted(!this.isMuted);
  }
}

export const musicEngine = new UpbeatMusicEngine();
