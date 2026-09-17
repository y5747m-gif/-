/**
 * vocal.js — Vocal Isolation processor (Web Audio implementation of the
 * Android app's VocalIsolationProcessor).
 *
 * Algorithm (mirrors the app):
 *   1. Center-channel extraction:
 *        Vocal = (L + R)/2 − strength · (L − R)/2
 *      which expands to the linear mix  L·(1−s)/2 + R·(1+s)/2  — implemented
 *      exactly with a channel splitter + gain matrix so it runs in real time.
 *      `s = CENTER_EXTRACTION_STRENGTH(0.7) × isolationLevel`.
 *   2. Equalizer shaping of the vocal band:
 *        − bass cut   ≈ −12 dB below 300 Hz
 *        − treble cut ≈ −8 dB above 3.4 kHz
 *        − vocal boost ≈ +6 dB around 1–2.5 kHz
 *      (all scaled by isolationLevel, matching getEqualizerSettings()).
 *
 * Graph:
 *   source ─┬─ dryGain ──────────────────────────────────────┐
 *           └─ splitter → gainL/gainR → sum → makeup         ├→ output
 *                                  → bassCut → trebleCut     │
 *                                  → boost → vocalGain ──────┘
 */

const CENTER_EXTRACTION_STRENGTH = 0.7; // same constant as the Android app
const VOCAL_LOW_FREQ = 300;
const VOCAL_HIGH_FREQ = 3400;
const VOCAL_BOOST_DB = 6;
const BASS_CUT_DB = -12;
const TREBLE_CUT_DB = -8;
const MAKEUP_GAIN = 1.7; // compensate mid-channel level loss

export function createVocalChain(ctx, sourceNode) {
  const T = ctx.currentTime;

  // --- Dry path ---
  const dryGain = ctx.createGain();
  dryGain.gain.value = 1;

  // --- Vocal path ---
  const splitter = ctx.createChannelSplitter(2);
  splitter.channelCount = 2;
  splitter.channelCountMode = 'clamped-max';

  const gainL = ctx.createGain();
  const gainR = ctx.createGain();
  const sum = ctx.createGain();

  // EQ stage (applied only to the extracted vocal signal)
  const bassCut = ctx.createBiquadFilter();
  bassCut.type = 'lowshelf';
  bassCut.frequency.value = VOCAL_LOW_FREQ;
  bassCut.gain.value = 0;

  const trebleCut = ctx.createBiquadFilter();
  trebleCut.type = 'highshelf';
  trebleCut.frequency.value = VOCAL_HIGH_FREQ;
  trebleCut.gain.value = 0;

  const vocalBoost = ctx.createBiquadFilter();
  vocalBoost.type = 'peaking';
  vocalBoost.frequency.value = 1800;
  vocalBoost.Q.value = 0.7;
  vocalBoost.gain.value = 0;

  const presence = ctx.createBiquadFilter();
  presence.type = 'peaking';
  presence.frequency.value = 3000;
  presence.Q.value = 1.0;
  presence.gain.value = 0;

  const makeup = ctx.createGain();
  makeup.gain.value = MAKEUP_GAIN;

  const vocalGain = ctx.createGain();
  vocalGain.gain.value = 0;

  // --- Output bus ---
  const output = ctx.createGain();

  // Wire it up.
  sourceNode.connect(dryGain);
  dryGain.connect(output);

  sourceNode.connect(splitter);
  splitter.connect(gainL, 0);
  splitter.connect(gainR, 1);
  gainL.connect(sum);
  gainR.connect(sum);
  sum.connect(bassCut);
  bassCut.connect(trebleCut);
  trebleCut.connect(vocalBoost);
  vocalBoost.connect(presence);
  presence.connect(makeup);
  makeup.connect(vocalGain);
  vocalGain.connect(output);

  function applyParams(enabled, level, ramp = true) {
    const t = ctx.currentTime;
    const set = (param, value) => {
      if (ramp) {
        param.cancelScheduledValues(t);
        param.setTargetAtTime(value, t, 0.05);
      } else {
        param.value = value;
      }
    };

    const s = CENTER_EXTRACTION_STRENGTH * level;
    // vocal = L·(1−s)/2 + R·(1+s)/2
    set(gainL.gain, (1 - s) / 2);
    set(gainR.gain, (1 + s) / 2);

    set(dryGain.gain, enabled ? 0 : 1);
    set(vocalGain.gain, enabled ? 1 : 0);

    // EQ scaled by isolation level (0 dB when disabled).
    set(bassCut.gain, enabled ? BASS_CUT_DB * level : 0);
    set(trebleCut.gain, enabled ? TREBLE_CUT_DB * level : 0);
    set(vocalBoost.gain, enabled ? VOCAL_BOOST_DB * level : 0);
    set(presence.gain, enabled ? VOCAL_BOOST_DB * 0.3 * level : 0);
  }

  const state = { enabled: false, level: 1 };

  return {
    output,
    /** Enable/disable isolation. `level` optional (0..1). */
    setEnabled(enabled, level, ramp = true) {
      state.enabled = !!enabled;
      if (typeof level === 'number' && Number.isFinite(level)) {
        state.level = Math.min(1, Math.max(0, level));
      }
      applyParams(state.enabled, state.level, ramp);
    },
    /** Update isolation strength; keeps current enable state. */
    setLevel(level, ramp = true) {
      state.level = Math.min(1, Math.max(0, level));
      applyParams(state.enabled, state.level, ramp);
    },
    get state() {
      return { ...state };
    },
  };
}
