# MusicWear Player 🎶

A sleek, Samsung-inspired media player for Wear OS, built with modern Jetpack Compose. Enjoy high-performance background playback for MP3/M4A files directly from your watch. Featuring automatic storage scanning, vivid album art extraction, and custom shuffle modes. Optimized for battery efficiency and fully compatible with Wear OS 5 and beyond.

---
### DISCLAIMER
**APP IS IN EARLY ACCESS**
---
## ✨ Features

* **Sleek Design:** Intuitive, Samsung-inspired user interface optimized for round watch screens.
* **High-Performance Playback:** Reliable, background audio playback powered by **Media3**.
* **Smart Library:** Automatic scanning of the `/sdcard/Music` directory to instantly find your tracks.
* **Visual Art:** Dynamic album art extraction that brings your music to life on the AMOLED display.
* **Custom Control:** Fully functional Play/Pause, Skip, and Shuffle modes.
* **Battery Optimized:** Intelligent lifecycle management that pauses resource-heavy UI loops when the screen is off.
* **Future-Proof:** Built on the latest APIs, ensuring compatibility with **Wear OS 5+**.

---

## 🚀 Technical Highlights

* **UI Framework:** [Jetpack Compose for Wear OS (Material 3)](https://developer.android.com/jetpack/compose/wear) for a responsive, reactive interface.
* **Audio Engine:** [Media3 / ExoPlayer](https://developer.android.com/guide/topics/media/media3) for industry-standard background service handling.
* **Image Loading:** [Coil](https://coil-kt.github.io/coil/) with custom decoders for efficient metadata image extraction.
* **Performance:** Leverages `ScalingLazyColumn` for high-FPS scrolling on low-power hardware.

---

## 🛠 Building from Source

To get the best performance, it is highly recommended to build and run the app in **Release Mode**.

---

### Prerequisites
* [Android Studio Koala](https://developer.android.com/studio) or newer.
* Android SDK Platform 30+.
* A Wear OS device (e.g., Galaxy Watch 4/5/6/7) with **Wireless Debugging** enabled.

---

### Build Steps
1. **Clone the repository.**
2. **Open the project** in Android Studio and allow Gradle to sync.
3. **Select the Build Variant:**
    * Open the **Build Variants** tab (bottom-left of Android Studio).
    * Change the `:app` module variant from `debug` to **`release`**.
4. **Compile the APK:**
    * Go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
    * Once finished, click **Locate** to find your `app-release.apk`.

---

### Installation
Connect your watch via ADB and run:
```bash
adb install app-release.apk
```

---

### Adding Music
This app scans the standard system music folder. To add songs to your watch, use ADB to push your files:

```bash
adb push my_song.mp3 /sdcard/Music/
```
After pushing files, open the app and tap the Refresh icon in the Library to update the database.

---

### 🤝 Contributing
Contributions are welcome! If you have ideas for new features or performance improvements, feel free to open an issue or submit a pull request.

---

### 📜 License
This project is licensed under the GNU General Public License v3.0 (GNU GPLv3) - see the LICENSE file for details.
