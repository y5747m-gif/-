/**
 * settings.js — persistent settings store (web equivalent of the Android app's
 * SettingsDataStore). Backed by localStorage; every change is broadcast to
 * subscribers so the UI stays in sync.
 */

import { emit } from './bus.js';

const STORAGE_KEY = 'vocalplayer.settings.v1';

export const THEMES = [
  { id: 'default', label: 'Default', className: 'theme-default', swatch: 'sw-default' },
  { id: 'purple', label: 'Purple', className: 'theme-purple', swatch: 'sw-purple' },
  { id: 'ocean', label: 'Ocean', className: 'theme-ocean', swatch: 'sw-ocean' },
  { id: 'sunset', label: 'Sunset', className: 'theme-sunset', swatch: 'sw-sunset' },
  { id: 'forest', label: 'Forest', className: 'theme-forest', swatch: 'sw-forest' },
  { id: 'nebula', label: 'Nebula', className: 'theme-nebula', swatch: 'sw-nebula' },
  { id: 'stars', label: 'Stars', className: 'theme-stars', swatch: 'sw-stars' },
  { id: 'custom', label: 'Custom', className: 'theme-custom', swatch: 'sw-custom' },
];

export const DEFAULT_SETTINGS = Object.freeze({
  theme: 'default',
  customBackground: null, // dataURL of custom background image (when small enough)
  vocalIsolationEnabled: false,
  vocalIsolationLevel: 1.0, // 0..1
  playbackSpeed: 1.0,
  volume: 1.0,
  showVisualizer: true,
  buttonAnimations: true,
  hapticFeedback: true,
  crossfadeEnabled: false,
  shuffleEnabled: false,
  repeatMode: 'off', // off | all | one
  visualizerMode: 'bars', // bars | wave
});

class SettingsStore {
  constructor() {
    this.settings = { ...DEFAULT_SETTINGS };
    this.listeners = new Set();
    this.load();
  }

  load() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      const saved = JSON.parse(raw);
      for (const key of Object.keys(DEFAULT_SETTINGS)) {
        if (!(key in saved)) continue;
        const def = DEFAULT_SETTINGS[key];
        const val = saved[key];
        // Nullable defaults (e.g. customBackground) accept null or their real type.
        const ok = def === null
          ? val === null || typeof val === 'string'
          : typeof val === typeof def;
        if (ok) this.settings[key] = val;
      }
      // Sanity clamps.
      this.settings.vocalIsolationLevel = clamp01(this.settings.vocalIsolationLevel);
      this.settings.playbackSpeed = Math.min(2, Math.max(0.5, this.settings.playbackSpeed));
      this.settings.volume = clamp01(this.settings.volume);
      if (!['off', 'all', 'one'].includes(this.settings.repeatMode)) this.settings.repeatMode = 'off';
      if (!THEMES.some((t) => t.id === this.settings.theme)) this.settings.theme = 'default';
    } catch {
      this.settings = { ...DEFAULT_SETTINGS };
    }
  }

  save() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.settings));
    } catch {
      // Storage full (e.g. huge custom background) — retry without it.
      try {
        const slim = { ...this.settings, customBackground: null };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(slim));
      } catch { /* give up silently */ }
    }
  }

  get(key) {
    return this.settings[key];
  }

  /** Update one or more keys; persists and notifies subscribers. */
  update(patch) {
    let changed = false;
    for (const [key, value] of Object.entries(patch)) {
      if (!(key in DEFAULT_SETTINGS)) continue;
      if (this.settings[key] !== value) {
        this.settings[key] = value;
        changed = true;
      }
    }
    if (changed) {
      this.save();
      for (const listener of this.listeners) {
        try { listener(this.settings, patch); } catch { /* listener error */ }
      }
      emit('settings', { settings: this.settings, patch });
    }
    return this.settings;
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}

function clamp01(v) {
  const n = Number(v);
  if (!Number.isFinite(n)) return 1;
  return Math.min(1, Math.max(0, n));
}

export const settings = new SettingsStore();
