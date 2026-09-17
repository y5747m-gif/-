/**
 * library.js — the music library (web equivalent of the app's AudioScanner +
 * LibraryScreen). Files are chosen locally (picker, folder picker, or
 * drag & drop), tagged via id3.js, and never leave the browser.
 */

import { parseTags, tagsFromFilename } from './id3.js';
import { player } from './player.js';
import { emit, on } from './bus.js';

const AUDIO_EXT = /\.(mp3|wav|ogg|oga|m4a|aac|flac|opus|webm|mp4)$/i;

let nextId = 1;

function isAudioFile(file) {
  if (file.type && file.type.startsWith('audio/')) return true;
  if (file.type && file.type.startsWith('video/') && AUDIO_EXT.test(file.name)) return true;
  return AUDIO_EXT.test(file.name);
}

class Library {
  constructor() {
    this.tracks = [];
    this.searchQuery = '';
    this.sortBy = 'title';
    this._probeQueue = [];
    this._probing = false;
    this._seen = new Set();

    on('state', () => this._updateNowPlayingRow());
  }

  // ------------------------------------------------------------ file intake
  async addFiles(fileList) {
    const files = Array.from(fileList || []).filter(isAudioFile);
    if (!files.length) {
      emit('toast', 'No audio files found in that selection');
      return 0;
    }

    let added = 0;
    const newTracks = [];
    for (const file of files) {
      const key = `${file.name}|${file.size}|${file.lastModified || 0}`;
      if (this._seen.has(key)) continue;
      this._seen.add(key);

      const fallback = tagsFromFilename(file.name);
      const track = {
        id: nextId++,
        file,
        url: URL.createObjectURL(file),
        title: fallback.title || file.name,
        artist: fallback.artist || 'Unknown Artist',
        album: fallback.album || '',
        duration: null,
      };
      newTracks.push(track);
      added++;
    }

    if (!added) {
      emit('toast', 'Those songs are already in your library');
      return 0;
    }

    this.tracks.push(...newTracks);
    this._syncPlayer();
    emit('tracksupdated');
    emit('toast', added === 1 ? 'Added 1 song' : `Added ${added} songs`);

    // Enrich metadata in the background.
    this._parseTagsFor(newTracks);
    for (const t of newTracks) this._probeQueue.push(t);
    this._pumpProbing();

    return added;
  }

  async _parseTagsFor(tracks) {
    const CONCURRENCY = 4;
    let i = 0;
    const worker = async () => {
      while (i < tracks.length) {
        const idx = i++;
        const track = tracks[idx];
        try {
          const tags = await parseTags(track.file);
          let changed = false;
          if (tags.title) { track.title = tags.title; changed = true; }
          if (tags.artist) { track.artist = tags.artist; changed = true; }
          if (tags.album) { track.album = tags.album; changed = true; }
          if (changed) emit('tracksupdated');
        } catch { /* keep filename-based tags */ }
      }
    };
    await Promise.all(Array.from({ length: Math.min(CONCURRENCY, tracks.length) }, worker));
  }

  /** Probe durations one at a time so we don't hammer the decoder. */
  _pumpProbing() {
    if (this._probing) return;
    this._probing = true;

    const probe = new Audio();
    probe.preload = 'metadata';

    const next = () => {
      const track = this._probeQueue.shift();
      if (!track) {
        this._probing = false;
        probe.src = '';
        return;
      }
      let settled = false;
      const finish = () => {
        if (settled) return;
        settled = true;
        probe.onloadedmetadata = null;
        probe.onerror = null;
        if (Number.isFinite(probe.duration) && probe.duration > 0) {
          track.duration = probe.duration;
          emit('tracksupdated');
        }
        // Give the event loop a breath, then continue.
        setTimeout(next, 0);
      };
      probe.onloadedmetadata = finish;
      probe.onerror = finish;
      probe.src = track.url;
      // Safety valve for files that never fire metadata.
      setTimeout(finish, 8000);
    };
    next();
  }

  clear() {
    for (const t of this.tracks) URL.revokeObjectURL(t.url);
    this.tracks = [];
    this._seen.clear();
    this._probeQueue = [];
    this._syncPlayer();
    emit('tracksupdated');
  }

  _syncPlayer() {
    player.setTracks(this.tracks);
  }

  // ---------------------------------------------------------------- queries
  setSearch(query) {
    this.searchQuery = (query || '').trim().toLowerCase();
  }

  setSort(sortBy) {
    this.sortBy = sortBy;
  }

  visibleTracks() {
    const q = this.searchQuery;
    let list = this.tracks;
    if (q) {
      list = list.filter((t) =>
        t.title.toLowerCase().includes(q) ||
        t.artist.toLowerCase().includes(q) ||
        (t.album || '').toLowerCase().includes(q));
    }
    const sorted = [...list];
    switch (this.sortBy) {
      case 'artist': sorted.sort((a, b) => a.artist.localeCompare(b.artist)); break;
      case 'album': sorted.sort((a, b) => (a.album || '').localeCompare(b.album || '')); break;
      case 'duration': sorted.sort((a, b) => (a.duration ?? Infinity) - (b.duration ?? Infinity)); break;
      default: sorted.sort((a, b) => a.title.localeCompare(b.title));
    }
    return sorted;
  }

  // ------------------------------------------------------------- rendering
  render(container) {
    const visible = this.visibleTracks();
    const currentId = player.currentTrack()?.id;
    container.innerHTML = '';

    if (!this.tracks.length) return; // empty state handled by app.js

    const frag = document.createDocumentFragment();
    visible.forEach((track) => {
      const row = document.createElement('div');
      row.className = 'track-item';
      row.dataset.id = String(track.id);
      if (track.id === currentId) row.classList.add('is-current');

      const index = document.createElement('div');
      index.className = 'track-index';
      const num = document.createElement('span');
      num.className = 'idx-num';
      num.textContent = String(this.tracks.indexOf(track) + 1);
      const bars = document.createElement('span');
      bars.className = 'eq-bars';
      bars.innerHTML = '<span></span><span></span><span></span>';
      index.append(num, bars);

      const meta = document.createElement('div');
      meta.className = 'track-meta';
      const name = document.createElement('div');
      name.className = 'track-name';
      name.textContent = track.title;
      const sub = document.createElement('div');
      sub.className = 'track-sub';
      const artistSpan = document.createElement('span');
      artistSpan.textContent = track.artist;
      sub.appendChild(artistSpan);
      if (track.album) {
        const albumSpan = document.createElement('span');
        albumSpan.className = 'album-part';
        albumSpan.textContent = ` • ${track.album}`;
        sub.appendChild(albumSpan);
      }
      meta.append(name, sub);

      const time = document.createElement('div');
      time.className = 'track-time';
      time.textContent = formatDuration(track.duration);

      row.append(index, meta, time);
      row.addEventListener('click', () => {
        const idx = this.tracks.findIndex((t) => t.id === track.id);
        if (idx >= 0) player.playTrack(idx);
      });
      frag.appendChild(row);
    });
    container.appendChild(frag);
    this._updateNowPlayingRow();
  }

  _updateNowPlayingRow() {
    const currentId = player.currentTrack()?.id;
    document.querySelectorAll('.track-item').forEach((row) => {
      const id = Number(row.dataset.id);
      row.classList.toggle('is-current', id === currentId);
      row.classList.toggle('is-playing-item', id === currentId && player.isPlaying);
    });
  }
}

export function formatDuration(seconds) {
  if (seconds == null || !Number.isFinite(seconds)) return '--:--';
  const total = Math.max(0, Math.round(seconds));
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export const library = new Library();
