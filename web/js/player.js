/**
 * player.js — playback engine for the Vocal MP3 Player web edition.
 *
 * Graph:
 *   <audio> → MediaElementSource → vocalChain → crossfadeGain
 *           → analyser → masterGain → destination
 *
 * Web equivalents of the Android app features:
 *   - ExoPlayer                 → HTMLAudioElement + Web Audio graph
 *   - MusicPlaybackService      → Media Session API (lock-screen controls)
 *   - VocalIsolationProcessor   → vocal.js (real-time center extraction + EQ)
 *   - Crossfade setting         → gain ramps around track transitions
 */

import { settings } from './settings.js';
import { createVocalChain } from './vocal.js';
import { emit } from './bus.js';

const CROSSFADE_SECONDS = 1.2;
const RAMP_TIME = 0.4;

class Player {
  constructor() {
    this.audio = document.getElementById('audio-el');
    this.audio.preload = 'auto';

    this.tracks = [];
    this.currentIndex = -1;

    this.ctx = null;
    this.sourceNode = null;
    this.vocalChain = null;
    this.crossfadeGain = null;
    this.analyser = null;
    this.masterGain = null;

    this.isPlaying = false;
    this.isSeeking = false;
    this.fadeOutScheduled = false;

    this._wireAudioEvents();
    this._setupMediaSession();
  }

  // ------------------------------------------------------------- graph setup
  _ensureGraph() {
    if (this.ctx) {
      if (this.ctx.state === 'suspended') this.ctx.resume().catch(() => {});
      return;
    }
    const Ctor = window.AudioContext || window.webkitAudioContext;
    if (!Ctor) return; // very old browser: plain <audio> playback still works

    this.ctx = new Ctor();
    this.sourceNode = this.ctx.createMediaElementSource(this.audio);

    this.vocalChain = createVocalChain(this.ctx, this.sourceNode);
    this.crossfadeGain = this.ctx.createGain();
    this.analyser = this.ctx.createAnalyser();
    this.analyser.fftSize = 2048;
    this.analyser.smoothingTimeConstant = 0.82;
    this.masterGain = this.ctx.createGain();

    this.vocalChain.output.connect(this.crossfadeGain);
    this.crossfadeGain.connect(this.analyser);
    this.analyser.connect(this.masterGain);
    this.masterGain.connect(this.ctx.destination);

    // Apply persisted settings to the fresh graph (no ramp on first apply).
    this.masterGain.gain.value = settings.get('volume');
    this.vocalChain.setEnabled(settings.get('vocalIsolationEnabled'), settings.get('vocalIsolationLevel'), false);
  }

  // ------------------------------------------------------------- audio events
  _wireAudioEvents() {
    const audio = this.audio;

    audio.addEventListener('play', () => {
      this.isPlaying = true;
      document.body.classList.add('is-playing');
      this._emitState();
      this._updateMediaSessionPlaybackState();
    });

    audio.addEventListener('pause', () => {
      this.isPlaying = false;
      document.body.classList.remove('is-playing');
      this._emitState();
      this._updateMediaSessionPlaybackState();
    });

    audio.addEventListener('timeupdate', () => {
      if (this.isSeeking) return;
      this._maybeScheduleCrossfade();
      this._emitState();
    });

    audio.addEventListener('durationchange', () => this._emitState());
    audio.addEventListener('loadedmetadata', () => {
      const track = this.currentTrack();
      if (track && (!track.duration || Math.abs(track.duration - audio.duration) > 1)) {
        if (Number.isFinite(audio.duration)) {
          track.duration = audio.duration;
          emit('tracksupdated');
        }
      }
    });

    audio.addEventListener('ended', () => this._handleEnded());

    audio.addEventListener('playing', () => {
      this._consecutiveErrors = 0;
    });

    audio.addEventListener('error', () => {
      const track = this.currentTrack();
      if (!track) return;
      this._consecutiveErrors = (this._consecutiveErrors || 0) + 1;
      // Break the loop if every remaining track fails (all files unsupported).
      if (this._consecutiveErrors >= Math.max(1, this.tracks.length)) {
        this._consecutiveErrors = 0;
        this.stop();
        emit('toast', 'Could not play the loaded files (unsupported format?)');
        return;
      }
      emit('toast', `Could not play "${track.title}"`);
      if (this.tracks.length > 1) this.next(true);
    });
  }

  // ---------------------------------------------------------------- library
  setTracks(tracks) {
    this.tracks = tracks;
    if (this.currentIndex >= tracks.length) this.currentIndex = -1;
  }

  currentTrack() {
    return this.currentIndex >= 0 ? this.tracks[this.currentIndex] ?? null : null;
  }

  playTrack(index) {
    const track = this.tracks[index];
    if (!track) return;

    this._ensureGraph();
    this.currentIndex = index;
    this.fadeOutScheduled = false;

    // Reset crossfade gain instantly for the new track.
    if (this.crossfadeGain && this.ctx) {
      this.crossfadeGain.gain.cancelScheduledValues(this.ctx.currentTime);
      this.crossfadeGain.gain.value = 1;
    }

    this.audio.src = track.url;
    this.audio.playbackRate = settings.get('playbackSpeed');
    this.audio.play()
      .then(() => this._setMediaSessionMetadata(track))
      .catch((err) => {
        if (err?.name !== 'AbortError') emit('toast', 'Playback was blocked — tap play again');
      });
    this._emitState();
  }

  playPause() {
    const track = this.currentTrack();
    if (!track) {
      emit('needtrack');
      return;
    }
    this._ensureGraph();
    if (this.audio.paused) {
      this.audio.play().catch(() => emit('toast', 'Playback was blocked'));
    } else {
      this.audio.pause();
    }
  }

  /**
   * Advance to the next track.
   * Manual next always wraps (same as the app's playNext); automatic
   * advancement at the end of the queue honors the repeat mode.
   */
  next(auto = false) {
    const n = this.tracks.length;
    if (!n) return;

    if (settings.get('shuffleEnabled') && n > 1) {
      let idx;
      do { idx = Math.floor(Math.random() * n); } while (idx === this.currentIndex);
      this.playTrack(idx);
      return;
    }

    if (auto && settings.get('repeatMode') === 'off' && this.currentIndex >= n - 1) {
      this.stop();
      return;
    }
    this.playTrack((this.currentIndex + 1) % n);
  }

  previous() {
    if (!this.tracks.length) return;
    // More than 3s in → restart current track (same behavior as the app).
    if (this.audio.currentTime > 3) {
      this.seekTo(0);
      return;
    }
    const idx = this.currentIndex <= 0 ? this.tracks.length - 1 : this.currentIndex - 1;
    this.playTrack(idx);
  }

  stop() {
    this.audio.pause();
    this.audio.currentTime = 0;
    this.isPlaying = false;
    document.body.classList.remove('is-playing');
    this._emitState();
  }

  seekTo(seconds) {
    if (!Number.isFinite(seconds)) return;
    this.audio.currentTime = Math.max(0, seconds);
    this.fadeOutScheduled = false;
    if (this.crossfadeGain && this.ctx) {
      this.crossfadeGain.gain.cancelScheduledValues(this.ctx.currentTime);
      this.crossfadeGain.gain.value = 1;
    }
    this._emitState();
  }

  seekToFraction(fraction) {
    const duration = this.audio.duration;
    if (!Number.isFinite(duration) || duration <= 0) return;
    this.seekTo(fraction * duration);
  }

  // ---------------------------------------------------------------- settings
  setVocalIsolation(enabled) {
    settings.update({ vocalIsolationEnabled: enabled });
    if (this.vocalChain) {
      this.vocalChain.setEnabled(enabled, settings.get('vocalIsolationLevel'));
    }
    this._emitState();
  }

  setVocalIsolationLevel(level) {
    settings.update({ vocalIsolationLevel: level });
    if (this.vocalChain) this.vocalChain.setLevel(level);
  }

  setPlaybackSpeed(speed) {
    settings.update({ playbackSpeed: speed });
    this.audio.playbackRate = speed;
  }

  setVolume(volume) {
    settings.update({ volume });
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setTargetAtTime(volume, this.ctx.currentTime, 0.03);
    }
  }

  // --------------------------------------------------------------- crossfade
  _maybeScheduleCrossfade() {
    if (!settings.get('crossfadeEnabled')) return;
    if (!this.crossfadeGain || !this.ctx) return;
    const duration = this.audio.duration;
    if (!Number.isFinite(duration)) return;
    const remaining = duration - this.audio.currentTime;
    if (remaining > 0 && remaining <= CROSSFADE_SECONDS && !this.fadeOutScheduled) {
      this.fadeOutScheduled = true;
      const t = this.ctx.currentTime;
      this.crossfadeGain.gain.cancelScheduledValues(t);
      this.crossfadeGain.gain.setValueAtTime(this.crossfadeGain.gain.value, t);
      this.crossfadeGain.gain.linearRampToValueAtTime(0.001, t + Math.max(0.1, remaining - 0.05));
    }
  }

  _handleEnded() {
    if (settings.get('repeatMode') === 'one') {
      this.seekTo(0);
      this.audio.play().catch(() => {});
      return;
    }
    this.next(true);
  }

  // ------------------------------------------------------------ media session
  _setupMediaSession() {
    if (!('mediaSession' in navigator)) return;
    const ms = navigator.mediaSession;
    const guard = (fn) => {
      try { fn(); } catch { /* unsupported action */ }
    };
    guard(() => ms.setActionHandler('play', () => this.playPause()));
    guard(() => ms.setActionHandler('pause', () => this.playPause()));
    guard(() => ms.setActionHandler('previoustrack', () => this.previous()));
    guard(() => ms.setActionHandler('nexttrack', () => this.next(true)));
    guard(() => ms.setActionHandler('seekto', (details) => {
      if (details.seekTime != null) this.seekTo(details.seekTime);
    }));
  }

  _setMediaSessionMetadata(track) {
    if (!('mediaSession' in navigator)) return;
    try {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: track.title,
        artist: track.artist,
        album: track.album || 'Vocal MP3 Player',
      });
    } catch { /* metadata unsupported */ }
  }

  _updateMediaSessionPlaybackState() {
    if (!('mediaSession' in navigator)) return;
    try {
      navigator.mediaSession.playbackState = this.isPlaying ? 'playing' : 'paused';
      if (Number.isFinite(this.audio.duration) && this.audio.duration > 0) {
        navigator.mediaSession.setPositionState?.({
          duration: this.audio.duration,
          playbackRate: this.audio.playbackRate,
          position: Math.min(this.audio.currentTime, this.audio.duration),
        });
      }
    } catch { /* position state unsupported */ }
  }

  // ------------------------------------------------------------------- state
  _emitState() {
    emit('state', this.snapshot());
  }

  snapshot() {
    return {
      track: this.currentTrack(),
      currentIndex: this.currentIndex,
      isPlaying: this.isPlaying,
      position: this.audio.currentTime || 0,
      duration: Number.isFinite(this.audio.duration) ? this.audio.duration : (this.currentTrack()?.duration ?? 0),
      vocalEnabled: settings.get('vocalIsolationEnabled'),
      vocalLevel: settings.get('vocalIsolationLevel'),
      speed: this.audio.playbackRate || settings.get('playbackSpeed'),
      volume: settings.get('volume'),
      shuffle: settings.get('shuffleEnabled'),
      repeatMode: settings.get('repeatMode'),
      hasTracks: this.tracks.length > 0,
      trackCount: this.tracks.length,
    };
  }
}

export const player = new Player();
