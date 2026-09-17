/**
 * app.js — UI wiring for the Vocal MP3 Player web edition
 * (mirrors MainActivity/MainApp/PlayerScreen/LibraryScreen/SettingsScreen).
 */

import { player } from './player.js';
import { library, formatDuration } from './library.js';
import { settings, THEMES } from './settings.js';
import { initVisualizer } from './visualizer.js';
import { emit, on } from './bus.js';

const $ = (sel) => document.querySelector(sel);

// ------------------------------------------------------------------ helpers
function toast(message) {
  const el = $('#toast');
  el.textContent = message;
  el.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => { el.hidden = true; }, 2400);
}

function haptic() {
  if (settings.get('hapticFeedback')) {
    try { navigator.vibrate?.(12); } catch { /* unsupported */ }
  }
}

function setRangeFill(input) {
  const min = Number(input.min) || 0;
  const max = Number(input.max) || 100;
  const val = Number(input.value);
  const pct = max > min ? ((val - min) / (max - min)) * 100 : 0;
  input.style.setProperty('--fill', `${pct}%`);
}

// ---------------------------------------------------------------- navigation
const screens = { player: $('#screen-player'), library: $('#screen-library'), settings: $('#screen-settings') };
let currentScreen = 'player';

function navigate(screen) {
  if (!screens[screen]) return;
  currentScreen = screen;
  for (const [id, el] of Object.entries(screens)) {
    el.classList.toggle('is-active', id === screen);
  }
  document.querySelectorAll('.nav-item').forEach((btn) => {
    btn.classList.toggle('is-active', btn.dataset.nav === screen);
  });
  updateMiniPlayer(player.snapshot());
  if (screen === 'library') renderLibrary();
}

document.querySelectorAll('.nav-item').forEach((btn) => {
  btn.addEventListener('click', () => { haptic(); navigate(btn.dataset.nav); });
});
on('navigate', (screen) => navigate(screen));

// ------------------------------------------------------------- player screen
const els = {
  modeIndicator: $('#mode-indicator'),
  btnVocal: $('#btn-vocal-toggle'),
  btnShuffle: $('#btn-shuffle'),
  btnRepeat: $('#btn-repeat'),
  btnPrev: $('#btn-prev'),
  btnPlay: $('#btn-play'),
  btnNext: $('#btn-next'),
  iconPlay: $('#icon-play'),
  iconPause: $('#icon-pause'),
  iconRepeat: $('#icon-repeat'),
  iconRepeatOne: $('#icon-repeat-one'),
  trackTitle: $('#track-title'),
  trackArtist: $('#track-artist'),
  trackAlbum: $('#track-album'),
  seekBar: $('#seek-bar'),
  timeCurrent: $('#time-current'),
  timeTotal: $('#time-total'),
  vocalControls: $('#vocal-controls'),
  vocalLevel: $('#vocal-level'),
  vocalLevelLabel: $('#vocal-level-label'),
  speedLabel: $('#speed-label'),
  speedSlider: $('#speed-slider'),
  speedPresets: $('#speed-presets'),
  volumeSlider: $('#volume-slider'),
  volumeLabel: $('#volume-label'),
  miniPlayer: $('#mini-player'),
  miniTitle: $('#mini-title'),
  miniArtist: $('#mini-artist'),
  miniPlay: $('#mini-play'),
  miniIconPlay: $('#mini-icon-play'),
  miniIconPause: $('#mini-icon-pause'),
  miniProgressFill: $('#mini-progress-fill'),
  libraryList: $('#library-list'),
  libraryEmpty: $('#library-empty'),
  libraryCount: $('#library-count'),
};

function wireControlButton(btn, handler) {
  btn.addEventListener('click', () => { haptic(); handler(); });
}

wireControlButton(els.btnPlay, () => player.playPause());
wireControlButton(els.btnNext, () => player.next(false));
wireControlButton(els.btnPrev, () => player.previous());

wireControlButton(els.btnVocal, () => {
  const enabled = !settings.get('vocalIsolationEnabled');
  player.setVocalIsolation(enabled);
  toast(enabled ? '🎤 Vocal isolation ON' : '🎵 Normal mode');
});

wireControlButton(els.btnShuffle, () => {
  const shuffle = !settings.get('shuffleEnabled');
  settings.update({ shuffleEnabled: shuffle });
  toast(shuffle ? 'Shuffle on' : 'Shuffle off');
});

wireControlButton(els.btnRepeat, () => {
  const order = ['off', 'all', 'one'];
  const nextMode = order[(order.indexOf(settings.get('repeatMode')) + 1) % order.length];
  settings.update({ repeatMode: nextMode });
  const label = nextMode === 'off' ? 'Repeat off' : nextMode === 'all' ? 'Repeat all' : 'Repeat one';
  toast(label);
});

on('needtrack', () => {
  navigate('library');
  toast('Add some music first');
});

// Seek bar (drag-friendly)
els.seekBar.addEventListener('input', () => {
  player.isSeeking = true;
  setRangeFill(els.seekBar);
  const duration = player.snapshot().duration;
  els.timeCurrent.textContent = formatDuration((Number(els.seekBar.value) / 1000) * duration);
});
els.seekBar.addEventListener('change', () => {
  player.seekToFraction(Number(els.seekBar.value) / 1000);
  player.isSeeking = false;
});

// Vocal level
els.vocalLevel.addEventListener('input', () => {
  const level = Number(els.vocalLevel.value) / 100;
  setRangeFill(els.vocalLevel);
  els.vocalLevelLabel.textContent = `${Math.round(level * 100)}%`;
  player.setVocalIsolationLevel(level);
});

// Speed
function applySpeedUI(speed) {
  els.speedLabel.textContent = `${Number(speed.toFixed(2)).toString()}x`;
  els.speedSlider.value = speed;
  setRangeFill(els.speedSlider);
  els.speedPresets.querySelectorAll('.chip').forEach((chip) => {
    chip.classList.toggle('is-active', Math.abs(Number(chip.dataset.speed) - speed) < 0.001);
  });
}
els.speedSlider.addEventListener('input', () => {
  player.setPlaybackSpeed(Number(els.speedSlider.value));
  applySpeedUI(Number(els.speedSlider.value));
});
els.speedPresets.querySelectorAll('.chip').forEach((chip) => {
  chip.addEventListener('click', () => {
    haptic();
    const speed = Number(chip.dataset.speed);
    player.setPlaybackSpeed(speed);
    applySpeedUI(speed);
  });
});

// Volume
els.volumeSlider.addEventListener('input', () => {
  const v = Number(els.volumeSlider.value) / 100;
  setRangeFill(els.volumeSlider);
  els.volumeLabel.textContent = `${Math.round(v * 100)}%`;
  player.setVolume(v);
});

// Visualizer mode: click the canvas to toggle wave/bars
$('#visualizer').addEventListener('click', () => {
  const viz = initVisualizerOnce();
  if (!viz) return;
  const nextMode = viz.mode === 'bars' ? 'wave' : 'bars';
  viz.setMode(nextMode);
  toast(nextMode === 'wave' ? 'Visualizer: waveform' : 'Visualizer: spectrum');
});

let vizInstance = null;
function initVisualizerOnce() {
  if (!vizInstance) vizInstance = initVisualizer();
  return vizInstance;
}

// --------------------------------------------------------------- state sync
function updatePlayerUI(s) {
  // Track info
  const track = s.track;
  els.trackTitle.textContent = track?.title || 'No Track Selected';
  els.trackArtist.textContent = track?.artist || (s.hasTracks ? '' : 'Add songs from the Library');
  els.trackAlbum.textContent = track?.album || '';
  document.title = track ? `${track.title} — Vocal MP3 Player` : 'Vocal MP3 Player';

  // Play/pause icons
  els.iconPlay.hidden = s.isPlaying;
  els.iconPause.hidden = !s.isPlaying;
  els.btnPlay.setAttribute('aria-label', s.isPlaying ? 'Pause' : 'Play');

  // Vocal UI
  els.btnVocal.setAttribute('aria-pressed', String(s.vocalEnabled));
  els.btnVocal.classList.toggle('vocal-active', s.vocalEnabled);
  els.modeIndicator.textContent = s.vocalEnabled ? '🎤 Vocal Isolation ON' : '🎵 Normal Mode';
  els.modeIndicator.classList.toggle('vocal-on', s.vocalEnabled);
  els.vocalControls.hidden = !s.vocalEnabled;
  if (document.activeElement !== els.vocalLevel) {
    els.vocalLevel.value = Math.round(s.vocalLevel * 100);
    els.vocalLevelLabel.textContent = `${Math.round(s.vocalLevel * 100)}%`;
    setRangeFill(els.vocalLevel);
  }

  // Shuffle/repeat
  els.btnShuffle.setAttribute('aria-pressed', String(s.shuffle));
  els.btnRepeat.setAttribute('aria-pressed', String(s.repeatMode !== 'off'));
  els.iconRepeat.hidden = s.repeatMode === 'one';
  els.iconRepeatOne.hidden = s.repeatMode !== 'one';

  // Progress
  if (!player.isSeeking) {
    const fraction = s.duration > 0 ? Math.min(1, s.position / s.duration) : 0;
    els.seekBar.value = Math.round(fraction * 1000);
    setRangeFill(els.seekBar);
    els.timeCurrent.textContent = formatDuration(s.position);
  }
  els.timeTotal.textContent = formatDuration(s.duration || null);

  // Speed & volume UI (reflect persisted values; don't fight active drags)
  if (document.activeElement !== els.speedSlider) applySpeedUI(s.speed);
  if (document.activeElement !== els.volumeSlider) {
    els.volumeSlider.value = Math.round(s.volume * 100);
    els.volumeLabel.textContent = `${Math.round(s.volume * 100)}%`;
    setRangeFill(els.volumeSlider);
  }

  updateMiniPlayer(s);
  library._updateNowPlayingRow();
}

function updateMiniPlayer(s) {
  const showMini = s.track && currentScreen !== 'player';
  els.miniPlayer.hidden = !showMini;
  if (!showMini) return;
  els.miniTitle.textContent = s.track.title;
  els.miniArtist.textContent = s.track.artist;
  els.miniIconPlay.hidden = s.isPlaying;
  els.miniIconPause.hidden = !s.isPlaying;
  const pct = s.duration > 0 ? Math.min(100, (s.position / s.duration) * 100) : 0;
  els.miniProgressFill.style.width = `${pct}%`;
}

els.miniPlayer.addEventListener('click', (e) => {
  if (e.target.closest('#mini-play')) return;
  navigate('player');
});
els.miniPlay.addEventListener('click', (e) => {
  e.stopPropagation();
  haptic();
  player.playPause();
});

// Throttle state updates while playing (position changes ~4x/sec from timeupdate).
let stateRafPending = false;
on('state', (s) => {
  if (stateRafPending) return;
  stateRafPending = true;
  requestAnimationFrame(() => {
    stateRafPending = false;
    updatePlayerUI(player.snapshot());
  });
});

// ------------------------------------------------------------------ library
function renderLibrary() {
  const hasTracks = library.tracks.length > 0;
  const visibleCount = hasTracks ? library.visibleTracks().length : 0;
  els.libraryEmpty.hidden = hasTracks;
  $('#library-no-results').hidden = !(hasTracks && visibleCount === 0);
  els.libraryList.hidden = !hasTracks;
  els.libraryCount.textContent = `${library.tracks.length} song${library.tracks.length === 1 ? '' : 's'}`;
  if (hasTracks) library.render(els.libraryList);
}

on('tracksupdated', () => {
  renderLibrary();
});

const searchInput = $('#search-input');
searchInput.addEventListener('input', () => {
  $('#search-clear').hidden = !searchInput.value;
  library.setSearch(searchInput.value);
  renderLibrary();
});
$('#search-clear').addEventListener('click', () => {
  searchInput.value = '';
  $('#search-clear').hidden = true;
  library.setSearch('');
  renderLibrary();
});

document.querySelectorAll('#sort-chips .chip').forEach((chip) => {
  chip.addEventListener('click', () => {
    haptic();
    document.querySelectorAll('#sort-chips .chip').forEach((c) => c.classList.remove('is-active'));
    chip.classList.add('is-active');
    library.setSort(chip.dataset.sort);
    renderLibrary();
  });
});

const fileInput = $('#file-input');
const folderInput = $('#folder-input');
$('#btn-add-files').addEventListener('click', () => fileInput.click());
$('#btn-empty-add').addEventListener('click', () => fileInput.click());
$('#btn-add-folder').addEventListener('click', () => {
  if (typeof folderInput.webkitdirectory === 'undefined') {
    toast('Folder picking is not supported in this browser — use + instead');
    fileInput.click();
    return;
  }
  folderInput.click();
});
$('#btn-refresh').addEventListener('click', () => {
  haptic();
  renderLibrary();
  toast('Library refreshed');
});

fileInput.addEventListener('change', async () => {
  await library.addFiles(fileInput.files);
  fileInput.value = '';
  renderLibrary();
});
folderInput.addEventListener('change', async () => {
  await library.addFiles(folderInput.files);
  folderInput.value = '';
  renderLibrary();
});

// Drag & drop anywhere
const dropOverlay = $('#drop-overlay');
let dragDepth = 0;
window.addEventListener('dragenter', (e) => {
  e.preventDefault();
  dragDepth++;
  dropOverlay.hidden = false;
});
window.addEventListener('dragover', (e) => e.preventDefault());
window.addEventListener('dragleave', (e) => {
  e.preventDefault();
  dragDepth = Math.max(0, dragDepth - 1);
  if (dragDepth === 0) dropOverlay.hidden = true;
});
window.addEventListener('drop', async (e) => {
  e.preventDefault();
  dragDepth = 0;
  dropOverlay.hidden = true;
  if (e.dataTransfer?.files?.length) {
    await library.addFiles(e.dataTransfer.files);
    renderLibrary();
  }
});

// ----------------------------------------------------------------- settings
function applyTheme() {
  const theme = THEMES.find((t) => t.id === settings.get('theme')) || THEMES[0];
  document.body.classList.remove(...THEMES.map((t) => t.className));
  document.body.classList.add(theme.className);

  const bgCustom = document.querySelector('.bg-custom');
  const custom = settings.get('customBackground');
  if (theme.id === 'custom' && custom) {
    bgCustom.style.backgroundImage = `url("${custom}")`;
  } else {
    bgCustom.style.backgroundImage = '';
  }

  // Theme grid selection state
  document.querySelectorAll('.theme-thumb').forEach((thumb) => {
    thumb.classList.toggle('is-selected', thumb.dataset.theme === theme.id);
  });
  $('#custom-bg-row').hidden = !(theme.id === 'custom' && custom);
  if (custom) $('#custom-bg-preview').style.backgroundImage = `url("${custom}")`;
}

function buildThemeGrid() {
  const grid = $('#theme-grid');
  grid.innerHTML = '';
  for (const theme of THEMES) {
    const thumb = document.createElement('div');
    thumb.className = 'theme-thumb';
    thumb.dataset.theme = theme.id;

    const swatch = document.createElement('button');
    swatch.className = `theme-swatch ${theme.swatch}`;
    swatch.setAttribute('aria-label', `${theme.label} background`);
    if (theme.id === 'custom') {
      swatch.innerHTML = '<svg viewBox="0 0 24 24" class="icon"><path d="M19,7v10H5V7h14m0,-2H5a2,2 0 0,0 -2,2v10a2,2 0 0,0 2,2h14a2,2 0 0,0 2,-2V7a2,2 0 0,0 -2,-2zM9.5,9.5A1.5,1.5 0 1,1 8,11a1.5,1.5 0 0,1 1.5,-1.5zM5,17l3.5,-4.5 2.5,3 3.5,-4.5L19,17H5z"/></svg>';
    }
    swatch.addEventListener('click', () => {
      haptic();
      if (theme.id === 'custom') {
        if (settings.get('customBackground')) {
          settings.update({ theme: 'custom' });
        } else {
          $('#bg-input').click();
        }
      } else {
        settings.update({ theme: theme.id });
      }
      applyTheme();
    });

    const label = document.createElement('div');
    label.className = 'theme-label';
    label.textContent = theme.label;

    thumb.append(swatch, label);
    grid.appendChild(thumb);
  }
}

const bgInput = $('#bg-input');
bgInput.addEventListener('change', () => {
  const file = bgInput.files?.[0];
  if (!file || !file.type.startsWith('image/')) return;
  const reader = new FileReader();
  reader.onload = () => {
    const dataUrl = String(reader.result);
    // Only persist reasonably small images; bigger ones stay session-only.
    const persistable = dataUrl.length < 2_500_000 ? dataUrl : null;
    settings.update({ theme: 'custom', customBackground: persistable });
    // Always show it for this session even if too big to persist.
    sessionCustomBg = dataUrl;
    applyTheme();
    if (!persistable) toast('Custom background set for this session');
  };
  reader.readAsDataURL(file);
  bgInput.value = '';
});

let sessionCustomBg = null;
// Patch applyTheme to prefer session image when persistence was skipped.
const _applyTheme = applyTheme;
applyTheme = function patchedApplyTheme() {
  _applyTheme();
  const theme = THEMES.find((t) => t.id === settings.get('theme')) || THEMES[0];
  if (theme.id === 'custom' && sessionCustomBg && !settings.get('customBackground')) {
    document.querySelector('.bg-custom').style.backgroundImage = `url("${sessionCustomBg}")`;
    $('#custom-bg-row').hidden = false;
    $('#custom-bg-preview').style.backgroundImage = `url("${sessionCustomBg}")`;
  }
};

$('#btn-remove-custom-bg').addEventListener('click', () => {
  sessionCustomBg = null;
  settings.update({ theme: 'default', customBackground: null });
  applyTheme();
});

// Toggle switches
function bindSwitch(id, key, onChange) {
  const input = $(id);
  input.checked = settings.get(key);
  input.addEventListener('change', () => {
    haptic();
    settings.update({ [key]: input.checked });
    onChange?.(input.checked);
  });
}

bindSwitch('#set-visualizer', 'showVisualizer');
bindSwitch('#set-vocal-default', 'vocalIsolationEnabled', (checked) => {
  // Single source of truth: the player applies it immediately too.
  player.setVocalIsolation(checked);
});
bindSwitch('#set-animations', 'buttonAnimations');
bindSwitch('#set-haptics', 'hapticFeedback');
bindSwitch('#set-crossfade', 'crossfadeEnabled');

on('settings', ({ patch }) => {
  // Keep switches in sync when settings change elsewhere (e.g. player screen).
  if ('vocalIsolationEnabled' in patch) $('#set-vocal-default').checked = patch.vocalIsolationEnabled;
  if ('showVisualizer' in patch) $('#set-visualizer').checked = patch.showVisualizer;
  if ('buttonAnimations' in patch) document.body.classList.toggle('no-animations', !patch.buttonAnimations);
  if ('theme' in patch || 'customBackground' in patch) applyTheme();
});

on('toast', toast);

// Keyboard shortcuts
window.addEventListener('keydown', (e) => {
  const target = e.target;
  if (target instanceof Element && target.closest('input, textarea, select, button')) return;
  switch (e.code) {
    case 'Space':
      e.preventDefault();
      player.playPause();
      break;
    case 'ArrowRight':
      if (e.shiftKey) player.next(false);
      else player.seekTo(player.audio.currentTime + 5);
      break;
    case 'ArrowLeft':
      if (e.shiftKey) player.previous();
      else player.seekTo(Math.max(0, player.audio.currentTime - 5));
      break;
    default:
      break;
  }
});

// ------------------------------------------------------------------ startup
// Debug/test hook (harmless in production, used by automated checks).
window.__vocalDebug = { player, library, settings };

buildThemeGrid();
document.body.classList.toggle('no-animations', !settings.get('buttonAnimations'));
applyTheme();
initVisualizerOnce();
renderLibrary();
updatePlayerUI(player.snapshot());

// Warn on close while playing (background playback keeps going in the tab).
window.addEventListener('beforeunload', (e) => {
  if (player.isPlaying) {
    e.preventDefault();
    e.returnValue = '';
  }
});
