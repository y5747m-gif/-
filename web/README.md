# 🎤 Vocal MP3 Player — Web Edition

The Vocal MP3 Player as a **website**: a fully client-side, offline-capable MP3
player with real-time vocal isolation. It is a faithful web port of the Android
app in this repository — same features, same algorithm, same design language.

Everything runs locally in the browser. Audio files are never uploaded anywhere.

## ▶ Run it

No build step, no dependencies for the site itself:

```bash
cd web
node server.js          # serves on http://0.0.0.0:8080
# or any static file server, e.g.:
#   python3 -m http.server 8080
#   npx serve .
```

Then open the shown URL and add music.

## ✨ Features (mirrors the Android app)

| Feature | Web implementation |
|---|---|
| Offline MP3 playback | `HTMLAudioElement` + Web Audio graph; files loaded via picker, folder picker, or drag & drop |
| **Vocal isolation** | Real-time center-channel extraction: `Vocal = (L+R)/2 − strength·(L−R)/2` via a channel-splitter gain matrix, plus vocal-band EQ (bass cut −12 dB < 300 Hz, treble cut −8 dB > 3.4 kHz, vocal boost +6 dB) — the same algorithm & constants as `VocalIsolationProcessor.kt` |
| Isolation intensity | 0–100 % slider (scales extraction strength and EQ) |
| Background playback controls | Media Session API (lock screen / notification play·pause·prev·next·seek) |
| 8 background themes + custom image | Animated CSS backgrounds matching `BackgroundView.kt` |
| Animated buttons | CSS pulse/glow animations, toggleable |
| Audio visualizer | `AnalyserNode` → canvas, spectrum bars ⇄ waveform (click the visualizer to switch) |
| Playback speed 0.5×–2× | Presets + slider |
| Crossfade | Gain ramp around track transitions |
| Shuffle / Repeat off·all·one | Same semantics as the app (manual next wraps, auto-advance honors repeat) |
| Settings persistence | `localStorage` (web equivalent of DataStore) |
| Library search & sort | Title / Artist / Album / Duration |
| Metadata | Built-in ID3v1/v2.2/v2.3/v2.4 parser (`js/id3.js`), filename fallback |

Keyboard shortcuts: `Space` play/pause · `←`/`→` seek 5 s · `Shift+←/→` prev/next track.

## 📁 Layout

```
web/
├── index.html          # Player / Library / Settings screens, mini player, nav
├── css/styles.css      # Neon theme, 8 animated backgrounds, components
├── js/
│   ├── app.js          # UI wiring, navigation, settings screens
│   ├── player.js       # Playback engine (graph, crossfade, Media Session)
│   ├── vocal.js        # Vocal-isolation DSP chain
│   ├── visualizer.js   # Canvas spectrum/waveform visualizer
│   ├── library.js      # File intake, tags, durations, search/sort
│   ├── settings.js     # Persisted settings store
│   ├── id3.js          # Dependency-free ID3 tag parser (+ self-test)
│   └── bus.js          # Tiny event bus
├── server.js           # Zero-dependency static server (dev/preview)
└── favicon.ico
```

## 🧪 Tests

```bash
node js/id3.js --self-test   # ID3 parser unit tests
```

The repo additionally ships a jsdom end-to-end suite (`e2e-dom`) used during
development that boots the real page and exercises playback, vocal isolation,
themes, search, persistence, and repeat/shuffle semantics.

## 🎤 How vocal isolation works

Identical to the Android app:

1. Vocals are usually panned center, i.e. equal in both channels.
2. `mid = (L+R)/2`, `side = (L−R)/2`; instruments panned left/right live in `side`.
3. `Vocal = mid − strength·side` with `strength = 0.7 × level` (linear, so it is
   implemented exactly with a gain matrix and runs in real time).
4. EQ suppresses bands where vocals rarely sit and boosts 1–3 kHz.

Works best with stereo recordings where vocals are centered; adjust the
isolation slider to taste.
