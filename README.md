ADB Captain

An ADB toolkit for Android powered by Shizuku. No root.

App for debugging and managing Android devices: ADB commands, app management, logs, device info. Jetpack Compose, Material 3.
Features

Screenshots
<p align="center">
  <img src="https://github.com/user-attachments/assets/8ee96161-fa27-43b7-8946-da28c2ee092d" width="18%">
  <img src="https://github.com/user-attachments/assets/03b29df5-a536-458b-b103-9d645aac851e" width="18%">
  <img src="https://github.com/user-attachments/assets/1d1ccfd2-1241-4cee-aae3-2174829ca4fe" width="18%">
  <img src="https://github.com/user-attachments/assets/a71df247-26de-4b27-9c65-e4323bdc5729" width="18%">
  <img src="https://github.com/user-attachments/assets/8e766ea4-5dd1-4f26-bef4-5bd42d28d6d4" width="18%">
</p>

Terminal — run ADB commands (pm, am, dumpsys, settings, input, etc.). Quick commands: app list, battery, screen size, model, free space, top processes, event log. Command history in Room, auto-complete, ANSI support.

Apps — list of installed apps with icons and versions. Search by package, filter user/system. Bloatware safety rating: 🟢 safe, 🟠 if needed, 🔴 critical. Actions: force-stop, clear data, enable/disable, uninstall.

Devices — serial, model, Android version, battery. Connection status. Reboot. Screenshots to DCIM/ADBCaptain.

Logcat — live logs. Filter by level and tag. Auto-scroll. Copy entries. PID/TID.

Sideload & Tools — install APK (pm install -r). Screenshots and screen recording. Wake, unlock, stay awake, Wi-Fi / Bluetooth / airplane mode, volume. Text input, open links.

Settings — dark theme, history and auto-complete, Shizuku check, languages.
Shizuku

ADB Captain runs through Shizuku - shell permissions via ADB, no root needed.

    Install Shizuku (F-Droid or Github).

    Start the Shizuku service.

    Open ADB Captain — Shizuku is detected automatically.

Shizuku must be running. If not, the app shows a setup screen.

Translations

All translations except English are AI-generated and may be inaccurate. Report issues via issues.
