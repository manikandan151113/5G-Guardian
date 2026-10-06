# 5G Guardian 📡

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform: Android](https://img.shields.io/badge/Platform-Android%2024%2B-green.svg)](https://developer.android.com)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![FOSS: F-Droid Ready](https://img.shields.io/badge/FOSS-F--Droid%20Compliant-brightgreen.svg)](https://f-droid.org)
[![Tests: Robolectric](https://img.shields.io/badge/Tests-Robolectric%20Passed-success.svg)](https://robolectric.org)

**5G Guardian** is a lightweight, privacy-first, and fully Free and Open Source Software (FOSS) mobile network monitoring utility for Android. Operating as an efficient, persistent foreground service, it continuously monitors your active cellular connection technology and delivers instantaneous audible and visual alerts whenever your connection drops from high-speed **5G (NR)** down to **4G (LTE), 3G, or 2G**.

Designed specifically for users on unmetered/unlimited 5G data tiers, remote professionals demanding uninterrupted broadband connectivity, and telecommunications enthusiasts monitoring carrier cell handovers.

---

## 📑 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [How It Works](#how-it-works)
- [Technical Requirements](#technical-requirements)
- [Building and Installation](#building-and-installation)
- [Testing & Quality Assurance](#testing--quality-assurance)
- [F-Droid Packaging & Compliance](#f-droid-packaging--compliance)
- [Security & Privacy Audit](#security--privacy-audit)
- [Limitations & Roadmap](#limitations--roadmap)
- [Community & Contributing](#community--contributing)
- [License](#license)
- [Author](#author)

---

## 🔍 Overview

Modern mobile carriers often implement aggressive carrier aggregation fallback mechanisms. When a 5G signal dips in signal strength or experiences cell congestion, mobile devices frequently switch silently to 4G LTE or legacy 3G networks without notifying the user. For users operating on 5G-only promotional plans or high-throughput workflows, this silent transition can lead to unexpected metered billing charges, degraded latency, or dropped data pipelines.

**5G Guardian** solves this by establishing an offline, hardware-aware monitoring pipeline directly on the Android device. It captures low-level telephony state transitions, analyzes carrier network generation states, and triggers immediate alarms before significant data is consumed over fallback networks.

---

## ✨ Key Features

- **Real-Time Network Generation Tracking:** Continuously monitors cellular network status using Android Telephony APIs, classifying connections into `5G (NR)`, `4G (LTE)`, `Other (3G/2G)`, or `Offline`.
- **Persistent Background Monitoring:** Runs as an optimized Android `ForegroundService` with custom notification priority channels (`fiveg_guardian_monitoring` and `fiveg_guardian_alert`) to prevent operating system task-killer termination.
- **Configurable Alert Behaviors:**
  - **Continuous Looping Alarm Mode:** Plays a persistent alarm audio stream with wake lock protection until explicitly acknowledged and dismissed by the user.
  - **Quiet Push Notification Mode:** Dispatches a gentle, non-disruptive system notification sound upon network downgrade.
- **Custom Local Audio Playback:** Import and play any audio file (`.mp3`, `.wav`, `.ogg`) stored on the local device filesystem via Android Storage Access Framework (SAF).
- **Intelligent Phone Call State Detection:** Automatically pauses and suppresses alarms during incoming and active voice calls (`CALL_STATE_RINGING`, `CALL_STATE_OFFHOOK`) using `PhoneStateListener`, enforcing a 30-second post-call cooldown window to prevent disruptive sirens during calls.
- **Deep Sleep & WakeLock Resiliency:** Employs `PowerManager.PARTIAL_WAKE_LOCK` and `MediaPlayer.setWakeMode` to ensure notifications fire reliably even when the device display is locked and CPU is in low-power Doze state.
- **Integrated Simulation Harness:** Built-in simulation control panel allowing developers and users to verify alarm behaviors, audio playback, and notification dismissal without requiring real cell-tower handovers.
- **Zero Proprietary Trackers:** 100% offline architecture with no analytics, no advertisements, and zero Google Mobile Services (GMS) or Firebase dependencies.

---

## 🏗️ System Architecture

The application is structured around a reactive, decoupled Android lifecycle architecture:

```
┌────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI                   │
│   (MainActivity, Theme, Dynamic Color, Edge-to-Edge)   │
└──────────────────────────┬─────────────────────────────┘
                           │ Observes StateFlow
┌──────────────────────────▼─────────────────────────────┐
│                     MainViewModel                      │
│        (UI State Management, Action Delegation)        │
└──────────────────────────┬─────────────────────────────┘
                           │ Dispatches Commands
┌──────────────────────────▼─────────────────────────────┐
│                 NetworkMonitorManager                  │
│  (Kotlin Coroutines StateFlow Coordinator, Prefs)      │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
┌──────────────▼─────────────┐ ┌──────────▼──────────────┐
│   NetworkMonitorService    │ │       AlarmPlayer       │
│  - TelephonyCallback       │ │  - MediaPlayer          │
│  - PhoneStateListener      │ │  - AudioFocusRequest    │
│  - Notification Channels   │ │  - Partial WakeLock     │
└────────────────────────────┘ └─────────────────────────┘
```

### Component Roles

1. **`NetworkMonitorManager`**: Central reactive singleton holding immutable `StateFlow` streams for network state, call status, alarm playback state, switch event history, and user preferences.
2. **`NetworkMonitorService`**: Bound and started foreground service maintaining the lifecycle of telephony listener registrations and dispatching system notifications.
3. **`AlarmPlayer`**: Hardware audio controller managing audio focus acquisition (`AudioManager`), safe wake lock release cycles (`PowerManager.WakeLock`), and looping media playback.
4. **`MainActivity` & `MainViewModel`**: Declarative UI layer built with Jetpack Compose and Material 3, reacting to state changes and providing user controls for sound selection, sensitivity, and simulation.

---

## 📁 Project Structure

```text
5G-Guardian/
├── .github/                               # GitHub community templates and security policy
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   ├── CONTRIBUTING.md
│   ├── pull_request_template.md
│   └── SECURITY.md
├── app/
│   ├── build.gradle.kts                   # Application Gradle configuration (SDK 24-36)
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml        # Permissions, service and activity manifests
│   │   │   ├── java/com/example/
│   │   │   │   ├── AlarmPlayer.kt         # Audio focus, wake lock & MediaPlayer controller
│   │   │   │   ├── MainActivity.kt        # Jetpack Compose UI & screen components
│   │   │   │   ├── MainViewModel.kt       # Android ViewModel managing UI state
│   │   │   │   ├── NetworkMonitorManager.kt # Central StateFlow coordinator & state machine
│   │   │   │   ├── NetworkMonitorService.kt # Foreground service & Telephony listeners
│   │   │   │   └── ui/theme/              # Material Design 3 theme, colors, typography
│   │   │   └── res/                       # Drawables, mipmaps, strings, and XML rules
│   │   └── test/
│   │       └── java/com/example/          # Automated Robolectric unit test suite
│   └── proguard-rules.pro                 # R8 / ProGuard optimization rules
├── fastlane/                              # Fastlane automated build and deployment specs
├── metadata/                              # Fastlane / F-Droid metadata and store descriptions
├── gradle/                                # Gradle wrapper and libs.versions.toml catalog
├── build.gradle.kts                       # Top-level project Gradle build configuration
├── settings.gradle.kts                    # Project modules and plugin management
├── LICENSE                                # GNU General Public License v3.0
├── THIRD_PARTY_LICENSES.md                # Comprehensive third-party open-source license audit
└── README.md                              # Project documentation
```

---

## ⚙️ How It Works

1. **Service Registration:** When the user engages the monitor toggle, `NetworkMonitorService` starts as an ongoing Android `ForegroundService` with notification channel priority.
2. **Hardware Telephony Hooking:**
   - **Android 12+ (API 31+):** Uses `TelephonyManager.registerTelephonyCallback()` listening for `DisplayInfoListener` and `ServiceStateListener`.
   - **Android 7.0 to 11 (API 24 to 30):** Employs `PhoneStateListener` with `LISTEN_DATA_CONNECTION_STATE` and `LISTEN_CALL_STATE`.
3. **State Transition Evaluation:** Telephony callbacks evaluate network override states (distinguishing between true Standalone/Non-Standalone 5G vs LTE fallback). When a transition from `TYPE_5G` to `TYPE_4G` or lower occurs:
   - The event is logged in the reactive switch timeline.
   - The app verifies call state; if the user is in a voice call (`OFFHOOK` / `RINGING`), the audible alarm is automatically suppressed.
   - If idle, `AlarmPlayer` acquires a non-reference-counted `PARTIAL_WAKE_LOCK`, requests audio focus via `AudioManager`, and triggers audio playback.
4. **User Dismissal:** Tapping the persistent notification action or the in-app dismiss control releases the wake lock and halts playback.

---

## 📋 Technical Requirements

| Component | Specification |
|:---|:---|
| **Operating System** | Android 7.0 (Nougat, API Level 24) or higher |
| **Target SDK** | Android 15 / 16 (API Level 36) |
| **Programming Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose + Material Design 3 (M3) |
| **JDK Requirement** | OpenJDK 11 or higher |
| **Build System** | Gradle (Kotlin DSL) |

---

## 🛠️ Building and Installation

### 1. Prerequisites
Ensure you have the Android SDK and OpenJDK 11+ configured in your local environment.

### 2. Clone the Repository
```bash
git clone https://github.com/manikandan151113/5G-Guardian.git
cd 5G-Guardian
```

### 3. Build Debug APK
```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows (PowerShell)
.\gradlew.bat assembleDebug
```
The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.

### 4. Install onto Device
Enable **USB Debugging** on your Android device and execute:
```bash
.\gradlew.bat installDebug
```

---

## 🧪 Testing & Quality Assurance

5G Guardian includes automated unit tests leveraging **Robolectric** to simulate Android runtime environments on the local JVM:

```bash
# Execute local JVM unit and Robolectric tests
.\gradlew.bat :app:testDebugUnitTest
```

### Test Suite Coverage
- **State Transition Testing:** Validates accurate classification across `TYPE_5G`, `TYPE_4G`, and legacy fallback.
- **Alarm Decision Logic:** Verifies alarm trigger conditions and user preferences.
- **Call State Cooldown:** Ensures siren suppression during phone calls and verifies 30-second post-call cooldown.
- **Simulation Harness:** Validates manual test mode triggers and state flow emission.

---

## 📦 F-Droid Packaging & Compliance

5G Guardian is built from the ground up to satisfy the strict inclusion criteria of the official **F-Droid** repository:

- ✅ **Zero Proprietary SDKs:** Free of Google Play Services, Firebase, or proprietary binary blobs.
- ✅ **100% Offline Operation:** No internet permission (`android.permission.INTERNET`) requested in `AndroidManifest.xml`.
- ✅ **Clean Dependency Tree:** All third-party libraries use compatible permissive licenses documented in [THIRD_PARTY_LICENSES.md](./THIRD_PARTY_LICENSES.md).
- ✅ **Reproducible Build Ready:** Build configuration supports standard F-Droid build recipes.

### Sample F-Droid Build Recipe (`metadata/com.aistudio.fivegguardian.qywtpa.yml`)
```yaml
Categories:
  - System
  - Security
  - Connectivity
License: GPL-3.0-or-later
AuthorName: Manikandan K
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

## 🔒 Security & Privacy Audit

- **No Remote Telemetry:** The application makes zero HTTP/HTTPS network calls. All logs, settings, and network events remain strictly on the local device.
- **Local Storage Isolation:** User preferences are persisted via Android's private `SharedPreferences` partition, inaccessible to external applications without root access.
- **Permission Minimization:** Only essential telephony state (`READ_PHONE_STATE`, `ACCESS_NETWORK_STATE`), foreground service, and wake lock permissions are declared.

---

## 🗺️ Limitations & Roadmap

- **Carrier NSA / SA Indicator Discrepancies:** Certain carrier configurations on older modems report 5G NSA as LTE anchor bands until active data transfers begin.
- **Dual SIM Enhancements:** Future releases will incorporate dedicated per-SIM monitoring selection for multi-SIM hardware.
- **Battery Optimization Whitelist Prompt:** Streamlining the OEM-specific battery optimization exemption workflow.

---

## 🤝 Community & Contributing

Contributions, feature discussions, and bug reports are warmly welcome!
- Review our [Contributing Guidelines](.github/CONTRIBUTING.md) before submitting a pull request.
- Read our [Security Policy](.github/SECURITY.md) for responsible vulnerability disclosure.

---

## 📄 License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License as published by the Free Software Foundation**, either version 3 of the License, or (at your option) any later version.

See the [LICENSE](./LICENSE) file for complete details.

---

## 👨‍💻 Author

**Manikandan K**  
Computer Science and Engineering Student (Cyber Security Specialization)  
* SRM Madurai College for Engineering and Technology  
* GitHub: [@manikandan151113](https://github.com/manikandan151113)  
* LinkedIn: [Manikandan K](https://www.linkedin.com/in/manikandan-k-9559a7329)  
* Email: [manikandankumaresan151113@gmail.com](mailto:manikandankumaresan151113@gmail.com)
