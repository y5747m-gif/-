# Vocal MP3 Player

Offline MP3 player with vocal isolation — an Android app plus a web edition.

| Part | Path | What it is |
|---|---|---|
| 📱 Android app | [`VocalMP3Player/`](VocalMP3Player/) | Kotlin + Jetpack Compose + ExoPlayer. See its [README](VocalMP3Player/README.md) and [build guide](VocalMP3Player/BUILD_GUIDE.md). |
| 🌐 Web edition | [`web/`](web/) | Dependency-free website port with the same features and vocal-isolation algorithm. See its [README](web/README.md). |

## Quick start (website)

```bash
cd web
node server.js        # http://0.0.0.0:8080
```

Open the URL, add audio files (picker, folder, or drag & drop), and press the
🎤 button to enable vocal isolation. Everything plays locally — no uploads.

## Quick start (Android)

```bash
cd VocalMP3Player
./gradlew assembleDebug   # APK: app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 + Android SDK 34 (or open the project in Android Studio).
