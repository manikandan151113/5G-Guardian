# 5G Guardian 🛡️

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![FOSS](https://img.shields.io/badge/FOSS-Free%20%26%20Open%20Source-green.svg)](https://en.wikipedia.org/wiki/Free_and_open-source_software)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

**5G Guardian** is a privacy-first, fully Free and Open Source Software (FOSS) mobile network monitoring utility for Android. It runs as a lightweight, reliable background service to continuously monitor your active cellular network type and instantly alerts you if your connection drops or falls back from high-speed **5G** to **4G/3G/2G**. 

This is particularly useful for users with unlimited 5G data plans, remote workers needing stable high-bandwidth networks, or network enthusiasts tracking carrier coverage and 5G band stability.

---

## 🎯 Key Features

- **Real-Time Network Monitoring**: Continuously tracks cellular network technology changes (NR/5G vs. LTE/4G/3G/2G) using standard Android telephony APIs.
- **Persistent Foreground Service**: Runs reliably in the background with a persistent notification, ensuring Android doesn't kill the monitor during active sessions.
- **Customizable Alert Modes**:
  - 🚨 **Continuous Alarm Mode**: Loops a loud system alarm sound until manually dismissed (essential for immediate attention).
  - 🎵 **Notification Tone Mode**: Plays a gentle, one-shot notification sound when a network fallback is detected.
- **Personalized Audio Files**: Browse, pick, and set **any custom audio file** (MP3, WAV, OGG) from your device's local storage to use as your fallback alert tone.
- **Built-in Network Simulator**: Test alert behaviors, sound playback, and notification states manually without needing a physical cell network change.
- **F-Droid Publishing Compliant**: 
  - 🔒 **100% Offline & Private**: Zero external network connections required. No servers, trackers, analytics, or ad libraries.
  - 🚫 **No Proprietary Dependencies**: All proprietary trackers, Google Mobile Services (GMS), and Firebase dependencies have been fully excluded or removed, making it perfectly prepared for F-Droid's main repository.

---

## 📸 User Interface & Visual Design

Designed strictly in accordance with modern **Material Design 3 (M3)** guidelines:
- **Dynamic Color**: Respects Material Design themes and supports eye-safe slate accents.
- **Edge-to-Edge**: Full screen layouts handling status bars and system navigation insets seamlessly.
- **Intuitive Sound Picker**: Interactive modal dialog showing standard system ringtones/alarms and a direct **Browse from device** folder picker.

---

## ⚙️ How It Works (Technical Highlights)

The app leverages Android's native hardware and software interfaces:
- **`TelephonyManager`**: Monitors network status using `TelephonyCallback` / `PhoneStateListener` for real-time cellular tech changes.
- **`CarrierConfigManager`**: Dynamically handles carrier-specific configuration states.
- **`MediaPlayer` & `RingtoneManager`**: Handles reliable audio looping, notification tones, and custom audio URI playback from device storage.
- **`SharedPreferences`**: Stores user alert preferences, alarm modes, call muting windows, and custom audio URIs securely on the device without external databases.

For a comprehensive inventory of all open-source libraries, versions, and licenses, see [THIRD_PARTY_LICENSES.md](./THIRD_PARTY_LICENSES.md).

---

## 🛠️ Build and Development Instructions

To build the project yourself or prepare it for a custom F-Droid build recipe, follow these steps:

### Prerequisites
- **Android SDK**: API Level 24 (`minSdk`) to API Level 36 (`targetSdk`).
- **JDK**: Java 11 or higher.
- **Gradle**: Kotlin DSL (`build.gradle.kts`).

### Compiling and Running
1. Clone the repository:
   ```bash
   git clone https://github.com/manikandan151113/5G-Guardian.git
   cd 5G-Guardian
   ```
2. Build the debug APK:
   ```bash
   gradle assembleDebug
   ```
3. Run the local JVM test suite (unit and Robolectric tests):
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Install the debug APK on a physical Android device:
   - Ensure USB debugging is enabled on your device.
   - Run:
     ```bash
     gradle installDebug
     ```

---

## 🤖 Preparing for F-Droid Publication

5G Guardian is built from the ground up to comply with **F-Droid's Strict Quality and Inclusion Policies**:

1. **No Proprietary Dependencies**: Checked and pruned of any proprietary closed-source SDKs (including Firebase and GMS). All libraries used are open-source and listed in `gradle/libs.versions.toml`.
2. **Standard Android Keystore**: Signed with standard signing configurations. If building for the F-Droid repository, the F-Droid build server will compile the app from source and sign it with their own F-Droid key.
3. **No Trackers or Advertisements**: The app contains absolutely zero trackers, ads, or analytics, satisfying F-Droid's privacy requirements.

### Suggested F-Droid Metadata (`.yml` recipe)
To submit this app to F-Droid, you can create a build recipe with the following parameters:

```yaml
Categories:
  - System
  - Security
  - Connectivity
License: GPL-3.0-or-later
AuthorName: Manikandan
SourceCode: https://github.com/manikandan151113/5G-Guardian
IssueTracker: https://github.com/manikandan151113/5G-Guardian/issues

RepoType: git
Repo: https://github.com/manikandan151113/5G-Guardian.git

Builds:
  - versionName: "1.1.1"
    versionCode: 3
    commit: v1.1.1
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: '1.1.1'
CurrentVersionCode: 3
```

---

## 📄 License & Legal Copy

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.**

This program is distributed in the hope that it will be useful, but **WITHOUT ANY WARRANTY**; without even the implied warranty of **MERCHANTABILITY** or **FITNESS FOR A PARTICULAR PURPOSE**. See the [LICENSE](./LICENSE) file for more details.

---

*Made with 🛡️ by the 5G Guardian Community.*
