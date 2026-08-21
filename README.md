# ⚓ ADB Captain

**A powerful, rootless ADB toolkit for Android, powered by Shizuku.**

ADB Captain is a modern, all-in-one Android debugging and device management app that brings the full power of the Android Debug Bridge (ADB) directly to your fingertips — no computer required, no root required. Built with Jetpack Compose and the Shizuku API, it lets you run real ADB commands, manage installed apps, monitor device logs, and inspect connected devices from a clean, Material 3 interface.

---

## ✨ Features

### 💻 Terminal
A fully functional ADB command terminal built right into the app.

- Execute arbitrary ADB commands (`pm`, `am`, `dumpsys`, `settings`, `input`, and more) directly on your device.
- Quick-launch command shortcuts: app list, battery status, screen size, device model, free space, top processes, and event log.
- Command history stored locally with Room — saved only when you choose, fully clearable.
- **Smart auto-complete** — while typing, history-based suggestions appear above the input.
- ANSI-aware output rendering for readable command results.

### 📱 Apps
A powerful package manager for every app on your device.

- Browse all installed applications with labels, versions, and icons.
- Search by package name and filter between **User** and **System** apps.
- Built-in **bloatware safety analysis** classifying each app as:
  - 🟢 **Safe to delete**
  - 🟠 **Delete if needed**
  - 🔴 **Critical — do not delete**
- App actions menu: **force-stop**, **clear data**, **enable/disable**, and **uninstall**.
- Disabled apps are clearly badged in the list.

### 🖥️ Devices
Complete information about your connected devices.

- Live device list with serial number, model, Android version (API level), and battery level.
- Device connection status: online, offline, unauthorized, recovery.
- **Reboot** your device (with a safety confirmation dialog).
- One-tap **screenshots**, saved to `DCIM/ADBCaptain`.

### 📋 Logcat
A live, real-time system log viewer.

- Stream `logcat` output live as it happens.
- Filter by log level (Verbose → Debug → Info → Warn → Error → Fatal) and by tag/message text.
- **Smart auto-scroll** — only follows the stream while you're at the bottom; jump back down with one tap.
- Tap or long-press any entry to copy it to the clipboard.
- Inspect PID/TID for every log entry.

### 📦 Sideload & Tools
The popular ADB toolbox, all on your phone.

- **Sideload APK** — pick an APK from storage and install it (`pm install -r`), no computer needed.
- **Screen capture** — screenshots and start/stop screen recording with an elapsed timer.
- **Device controls** — wake, unlock (dismiss keyguard), stay awake, Wi-Fi / Bluetooth / airplane-mode toggles, and a media volume slider.
- **Input & links** — type text on the device and open URLs/deep links.

### ⚙️ Settings
Tune ADB Captain to your workflow.

- Dark theme toggle.
- Command history and auto-complete toggles.
- One-tap **Shizuku status check**.
- Language selection (English / Русский).

---

## 🔒 No Root. Just Shizuku.

ADB Captain runs ADB commands using the [Shizuku](https://shizuku.rikka.app/) service, which grants apps shell-level permissions through ADB — **no root required**. To get started:

1. Install **Shizuku** from the Play Store (or via ADB on a computer).
2. Start the Shizuku service from the Shizuku app.
3. Open ADB Captain — it automatically detects Shizuku and is ready to go.

> ⚠️ Shizuku must be running for ADB Captain to function. If Shizuku is not running, the app shows a one-tap setup screen to help you enable it.

---

## 🏗️ Tech Stack

- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM with clean-architecture style layers (domain / data / presentation)
- **DI:** Hilt
- **Database:** Room (command history)
- **Storage:** DataStore Preferences (settings)
- **ADB access:** Shizuku API (`dev.rikka.shizuku`)
- **Navigation:** Jetpack Navigation Compose
- **Image loading:** Coil

---

## 🚀 Build

```bash
# Free, full-featured FOSS build (F-Droid / GitHub)
./gradlew assembleFdroidDebug

# RuStore build — identical FOSS build, no paid features
./gradlew assembleRustoreDebug

# Signed release builds (requires keystore.properties, see below)
./gradlew assembleFdroidRelease assembleRustoreRelease
```

The project has two product flavors that share the same code:

- **`fdroid`** — free, full-featured FOSS build. Ready for F-Droid and GitHub Releases.
- **`rustore`** — identical FOSS build for RuStore. No paid features, no analytics, no DonationAlerts.

> ADB Captain is fully free and open source in both variants. There are no paid ("PRO") features and no donation links — this stays true for every store.

## 🔑 Signing release builds

To sign release APKs you need a keystore. Create it once with:

```bash
keytool -genkeypair -v \
  -keystore keystore/release.jks \
  -alias adb-captain \
  -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=ADB Captain, OU=Android, O=eatenlamp, L=Moscow, C=RU"
```

Then create `keystore.properties` at the project root (it is gitignored):

```properties
storeFile=keystore/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=adb-captain
keyPassword=YOUR_KEY_PASSWORD
```

If `keystore.properties` is missing, release builds fall back to the debug key.

## 📦 Install

```bash
./gradlew installFdroidDebug
```

---

## 📄 License

ADB Captain is licensed under the **GNU Affero General Public License v3.0 or later** (AGPL-3.0-or-later).
Copyright © eatenlamp <eatenlamp@proton.me>.

The bundled fonts (Inter, JetBrains Mono) are licensed under the **SIL Open Font License 1.1** (OFL-1.1).

The project is **REUSE compliant** (https://reuse.software). License texts live in [`LICENSES/`](LICENSES) and per-file declarations are in [`REUSE.toml`](REUSE.toml). Verify with:

```bash
reuse lint
```

*ADB Captain — take the helm of your device.* ⚓
