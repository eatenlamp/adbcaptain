# ADB Captain

An ADB toolkit for Android powered by Shizuku. No root.

App for debugging and managing Android devices: ADB commands, app management, logs, device info. Jetpack Compose, Material 3.

<p align="center">
  <img src="https://github.com/user-attachments/assets/8ee96161-fa27-43b7-8946-da28c2ee092d" width="18%">
  <img src="https://github.com/user-attachments/assets/03b29df5-a536-458b-b103-9d645aac851e" width="18%">
  <img src="https://github.com/user-attachments/assets/1d1ccfd2-1241-4cee-aae3-2174829ca4fe" width="18%">
  <img src="https://github.com/user-attachments/assets/a71df247-26de-4b27-9c65-e4323bdc5729" width="18%">
  <img src="https://github.com/user-attachments/assets/8e766ea4-5dd1-4f26-bef4-5bd42d28d6d4" width="18%">
</p>

---

## Features

**Terminal** - run ADB commands (`pm`, `am`, `dumpsys`, `settings`, `input`, etc.). Quick commands: app list, battery, screen size, model, free space, top processes, event log (events buffer), log tail. Command history in Room, auto-complete, ANSI support. **Tabs** let you keep several sessions open, each with its own output buffer and its own command still running. **History Groups** sort the history into Frequent, Today, Yesterday, This week and Older.

**Apps** - list of installed apps with icons and versions. Search by package, filter user/system. Bloatware safety rating: 🟢 safe, 🟠 if needed, 🔴 critical. Actions: force-stop, clear data, enable/disable, uninstall, quick toggles for Wi-Fi, Bluetooth and airplane mode, App Ops. **Intent Launcher** builds an intent by hand with a live preview of the command it will run, plus presets for URLs, text, dialling and settings. App Inspector shows activities, services, receivers and providers with the permission each requires, splits permissions into dangerous and normal, and reports the install source.

**Devices** - serial, model, Android version, battery. Connection status. Reboot. Screenshots to `DCIM/ADBCaptain`.

**Logcat** - live logs. Filter by level and tag. Auto-scroll. Copy entries. PID/TID. Save the filtered view to a file. Filter by app with a tap on the PID, or let Logcat follow whatever app is in the foreground. **Timeline** merges the main and events buffers into one stream with chips to switch between them. Share the filtered view as a Markdown file straight into a bug report.

**Debug** - one place for diagnosing a problem. Bug Pack collects a shareable ZIP (system info, package and activity dumps, main and events logcat, processes, packages, battery, memory, CPU) with a checkbox per section. Debug Session records elapsed time, the apps that came to the foreground and buffers logcat while you reproduce the bug. Macros store a chain of commands under a name and run it in one tap. Crash logs are grouped per app with full stack traces.

**Process Manager** - the real process table with PID, PPID, user, memory and command line rather than just the top by CPU. Search by name, PID or user, and force-stop any app from the list.

**Command Palette** - the search icon in the top bar reaches every screen, every macro and common shell commands, and runs them in the terminal right away.

**Sideload & Tools** - install APK (`pm install -r`). Screenshots and screen recording. Wake, unlock, stay awake, Wi-Fi / Bluetooth / airplane mode, volume. Text input, open links.

**Appearance** - built on Material 3 Expressive, with morphing progress indicators and rounder shapes across every screen.

**Settings** - dark theme, history and auto-complete, Shizuku check, languages.

---

## Shizuku

ADB Captain runs through [Shizuku](https://shizuku.rikka.app/) - shell permissions via ADB, no root needed.

1. Install Shizuku (F-Droid or GitHub).
2. Start the Shizuku service.
3. Open ADB Captain - Shizuku is detected automatically.

> **Note:** Shizuku must be running. If not, the app shows a setup screen.

---

## Translations

All translations except English are AI-generated and may be inaccurate. Report issues via [Issues](../../issues).

---

## Author

ADB Captain is created and maintained by **eatenlamp**.

- GitHub: <https://github.com/eatenlamp>
- Project: <https://github.com/eatenlamp/ADBCaptain>
- Contact: eatenlamp@proton.me

Every source file carries an ownership notice. Removing, hiding or replacing
that notice is a violation of the license terms.

---

## License

AGPL-3.0-or-later. Copyright © 2026 eatenlamp. See [LICENSE](LICENSE).
