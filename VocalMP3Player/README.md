# 🎤 Vocal MP3 Player

<p align="center">
  <strong>Offline MP3 Player with AI-Powered Vocal Isolation</strong>
</p>

---

## ✨ Features

### 🎵 Core Features
- **Offline MP3 Player** - Plays all music files on your device, no internet required
- **Vocal Isolation** - Remove background music and keep only vocals/speech
- **Background Playback** - Music continues playing when the app is minimized
- **Full Media Controls** - Lock screen and notification controls

### 🎨 Customization
- **8 Background Themes** - Choose from gradient presets or set a custom image
- **Animated Buttons** - Pulse, bounce, and glow effects on all controls
- **Audio Visualizer** - Real-time waveform and spectrum animations
- **Custom Backgrounds** - Use any image from your gallery as background

### ⚙️ Settings
- Toggle visualizer display
- Enable/disable button animations
- Haptic feedback control
- Playback speed adjustment (0.5x - 2.0x)
- Vocal isolation intensity control
- Crossfade between tracks

---

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Audio**: ExoPlayer (Media3)
- **Architecture**: MVVM with StateFlow
- **Storage**: DataStore Preferences
- **Image Loading**: Coil

### Project Structure
```
app/src/main/java/com/vocalplayer/app/
├── MainActivity.kt              # Entry point
├── PlayerViewModel.kt           # Main ViewModel (MVVM)
├── audio/
│   ├── AudioPlayerManager.kt    # ExoPlayer wrapper
│   ├── AudioScanner.kt          # MediaStore scanner
│   └── VocalIsolationProcessor.kt # Vocal isolation engine
├── data/
│   ├── Models.kt                # Data classes
│   └── SettingsDataStore.kt     # Preferences storage
├── service/
│   ├── MusicPlaybackService.kt  # Background service
│   └── MusicNotificationReceiver.kt
└── ui/
    ├── theme/                   # Material 3 theming
    ├── screens/
    │   ├── MainApp.kt           # Navigation & layout
    │   ├── PlayerScreen.kt      # Main player UI
    │   ├── LibraryScreen.kt     # Song browser
    │   └── SettingsScreen.kt    # Settings page
    └── components/
        ├── AnimatedButton.kt    # Pulse, Glow, Ripple buttons
        ├── AlbumArtView.kt      # Vinyl record animation
        ├── BackgroundView.kt    # Dynamic backgrounds
        └── VisualizerView.kt    # Audio visualizations
```

---

## 🔧 How Vocal Isolation Works

The app uses a **Center Channel Extraction** algorithm:

1. **Stereo Analysis**: In most recordings, vocals are panned center (equal in both channels)
2. **Side Channel Removal**: Instruments are often panned left/right
3. **Processing**: `Vocal = (Left + Right) / 2 - (Left - Right) / 2 × strength`
4. **Enhancement**: Equalizer boosts vocal frequencies (300Hz - 3.4kHz)
5. **Result**: Background music is suppressed while vocals remain

---

## 📱 Build Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34

### Steps
1. Open the project in Android Studio
2. Sync Gradle files
3. Connect your device or start an emulator
4. Click Run (▶️)

### Permissions Required
- `READ_MEDIA_AUDIO` - Access music files (Android 13+)
- `READ_EXTERNAL_STORAGE` - Access music files (Android 12-)
- `POST_NOTIFICATIONS` - Show playback notification
- `FOREGROUND_SERVICE` - Background playback

---

## 🎨 Background Themes

| Theme | Description |
|-------|-------------|
| Default | Dark gradient background |
| Purple | Animated purple gradient |
| Ocean | Deep blue ocean waves |
| Sunset | Warm sunset gradient |
| Forest | Dark forest green |
| Nebula | Space nebula effect |
| Stars | Starry night sky |
| Custom | Use your own image |

---

## 📋 Changelog

### v1.0.0
- Initial release
- MP3 playback with ExoPlayer
- Vocal isolation engine
- 8 background themes
- Custom background support
- Animated UI controls
- Audio visualizer
- Background playback service
- Settings with DataStore persistence

---

## 📄 License

This project is open source and available for personal use.

---

Made with ❤️ using Kotlin & Jetpack Compose
