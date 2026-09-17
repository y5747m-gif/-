# Vocal MP3 Player - Build Guide

## 📋 Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK 17**
- **Android SDK 34** (API level 34)
- **Gradle 8.2** (will be downloaded automatically)

## 🚀 How to Build

### Option 1: Android Studio (Recommended)
1. Open Android Studio
2. Click **File → Open**
3. Navigate to the `VocalMP3Player` folder and select it
4. Wait for Gradle sync to complete
5. Click the green **Run (▶)** button
6. Select your device/emulator

### Option 2: Command Line
```bash
cd VocalMP3Player
# Make gradlew executable (Mac/Linux)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

## 📱 First Launch
1. Grant the **Music/Audio** permission when prompted
2. The app will scan your device for MP3 files
3. Tap a song from the Library tab to start playing
4. Tap the 🎤 button to enable **Vocal Isolation**

## 🎤 Vocal Isolation Tips
- Works best with **stereo** recordings where vocals are centered
- Adjust the **isolation level** slider for best results
- Some tracks may sound better at lower isolation levels
- Works offline — no internet needed!

## 🎨 Customizing Backgrounds
1. Go to **Settings** tab
2. Under **Background**, tap a preset theme
3. Or tap **Custom** to choose an image from your gallery
4. The background animates smoothly between transitions
