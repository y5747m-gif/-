/**
 * visualizer.js — real-time audio visualizer (web equivalent of the app's
 * VisualizerView). Two modes:
 *   "bars" — frequency spectrum (mirrors SpectrumVisualizer)
 *   "wave" — oscilloscope waveform (mirrors WaveformVisualizer)
 * When audio isn't playing it draws a gentle idle animation, like the app.
 */

import { player } from './player.js';
import { settings } from './settings.js';
import { on } from './bus.js';

const CYAN = [0, 229, 255];
const PURPLE = [187, 134, 252];

class Visualizer {
  constructor(canvas) {
    this.canvas = canvas;
    this.ctx2d = canvas.getContext ? canvas.getContext('2d') : null;
    this.mode = settings.get('visualizerMode') || 'bars';
    this.enabled = settings.get('showVisualizer');
    this.running = false;
    this._raf = 0;
    this._freqData = null;
    this._timeData = null;

    on('state', (s) => {
      this.enabled = settings.get('showVisualizer');
      this._syncVisibility();
      if (s.isPlaying && !this.running) this.start();
    });

    on('settings', () => {
      this.enabled = settings.get('showVisualizer');
      this._syncVisibility();
    });

    this._syncVisibility();
    this.start(); // idle animation until playback begins
  }

  setMode(mode) {
    this.mode = mode === 'wave' ? 'wave' : 'bars';
    settings.update({ visualizerMode: this.mode });
  }

  _syncVisibility() {
    const wrap = this.canvas.parentElement;
    if (wrap) wrap.style.display = this.enabled ? '' : 'none';
  }

  start() {
    if (this.running) return;
    this.running = true;
    const loop = () => {
      this._raf = requestAnimationFrame(loop);
      if (document.hidden || !this.enabled) return;
      this._draw();
    };
    loop();
  }

  _ensureBuffers(analyser) {
    if (!this._freqData || this._freqData.length !== analyser.frequencyBinCount) {
      this._freqData = new Uint8Array(analyser.frequencyBinCount);
      this._timeData = new Uint8Array(analyser.fftSize);
    }
  }

  _draw() {
    const { canvas, ctx2d } = this;
    if (!ctx2d) return; // canvas unsupported in this environment
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const cssW = canvas.clientWidth || 320;
    const cssH = canvas.clientHeight || 60;
    if (canvas.width !== Math.round(cssW * dpr) || canvas.height !== Math.round(cssH * dpr)) {
      canvas.width = Math.round(cssW * dpr);
      canvas.height = Math.round(cssH * dpr);
    }
    const w = canvas.width;
    const h = canvas.height;
    ctx2d.clearRect(0, 0, w, h);

    const analyser = player.analyser;
    const playing = player.isPlaying;

    if (analyser && playing) {
      this._ensureBuffers(analyser);
      if (this.mode === 'wave') {
        analyser.getByteTimeDomainData(this._timeData);
        this._drawWave(this._timeData, w, h);
      } else {
        analyser.getByteFrequencyData(this._freqData);
        this._drawBars(this._freqData, w, h, true);
      }
    } else {
      this._drawIdle(w, h);
    }
  }

  _drawIdle(w, h) {
    const t = performance.now() / 1000;
    const bars = 40;
    const gap = w / bars;
    const barW = gap * 0.5;
    for (let i = 0; i < bars; i++) {
      const amp = 0.05 + 0.035 * Math.sin(t * 1.4 + i * 0.45) + 0.02 * Math.sin(t * 0.7 + i * 1.1);
      const bh = Math.max(2, amp * h);
      const x = i * gap + (gap - barW) / 2;
      const y = (h - bh) / 2;
      ctx2dFillRoundRect(this.ctx2d, x, y, barW, bh, barW / 2, mixColor(CYAN, PURPLE, i / bars, 0.5));
    }
  }

  _drawBars(freq, w, h, live) {
    const bars = 48;
    // Use the lower ~60% of the spectrum where music content lives.
    const usable = Math.floor(freq.length * 0.6);
    const gap = w / bars;
    const barW = Math.max(2, gap * 0.55);
    for (let i = 0; i < bars; i++) {
      // Sample with a slight curve so low bins don't dominate visually.
      const bin = Math.floor(Math.pow(i / bars, 1.6) * usable);
      const v = freq[bin] / 255;
      const bh = Math.max(2, v * h * 0.96);
      const x = i * gap + (gap - barW) / 2;
      const y = h - bh;
      const grad = this.ctx2d.createLinearGradient(0, y, 0, h);
      const c = mixColor(CYAN, PURPLE, i / bars, 1);
      grad.addColorStop(0, c);
      grad.addColorStop(1, mixColor(CYAN, PURPLE, i / bars, 0.25));
      ctx2dFillRoundRect(this.ctx2d, x, y, barW, bh, barW / 2, grad);
    }
    void live;
  }

  _drawWave(time, w, h) {
    const ctx = this.ctx2d;
    const grad = ctx.createLinearGradient(0, 0, w, 0);
    grad.addColorStop(0, 'rgba(0,229,255,0.95)');
    grad.addColorStop(0.5, 'rgba(187,134,252,0.95)');
    grad.addColorStop(1, 'rgba(0,229,255,0.95)');

    ctx.lineWidth = Math.max(1.5, h * 0.03);
    ctx.strokeStyle = grad;
    ctx.lineJoin = 'round';
    ctx.beginPath();
    const step = Math.max(1, Math.floor(time.length / w));
    for (let x = 0; x < w; x++) {
      const v = time[Math.min(time.length - 1, x * step)] / 128 - 1;
      const y = h / 2 + v * h * 0.46;
      if (x === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Soft mirror glow below center line.
    ctx.globalAlpha = 0.25;
    ctx.beginPath();
    for (let x = 0; x < w; x += 2) {
      const v = time[Math.min(time.length - 1, x * step)] / 128 - 1;
      const y = h / 2 - v * h * 0.35;
      if (x === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();
    ctx.globalAlpha = 1;
  }
}

function mixColor(a, b, t, alpha) {
  const r = Math.round(a[0] + (b[0] - a[0]) * t);
  const g = Math.round(a[1] + (b[1] - a[1]) * t);
  const bl = Math.round(a[2] + (b[2] - a[2]) * t);
  return `rgba(${r},${g},${bl},${alpha})`;
}

function ctx2dFillRoundRect(ctx, x, y, w, h, r, style) {
  const radius = Math.min(r, w / 2, h / 2);
  ctx.fillStyle = style;
  ctx.beginPath();
  if (ctx.roundRect) {
    ctx.roundRect(x, y, w, h, radius);
  } else {
    ctx.moveTo(x + radius, y);
    ctx.arcTo(x + w, y, x + w, y + h, radius);
    ctx.arcTo(x + w, y + h, x, y + h, radius);
    ctx.arcTo(x, y + h, x, y, radius);
    ctx.arcTo(x, y, x + w, y, radius);
    ctx.closePath();
  }
  ctx.fill();
}

export function initVisualizer() {
  const canvas = document.getElementById('visualizer');
  if (!canvas) return null;
  return new Visualizer(canvas);
}
