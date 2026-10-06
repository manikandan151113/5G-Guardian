# Contributing to 5G Guardian

Thank you for your interest in contributing to **5G Guardian**! As a privacy-first, Free and Open Source Software (FOSS) utility, we welcome code contributions, issue reports, documentation improvements, and localization.

---

## Code of Conduct

We are committed to providing a friendly, safe, and welcoming environment for all contributors, regardless of experience level or background. Please be respectful and constructive in all discussions.

---

## Development Workflow

### 1. Fork and Clone
1. Fork the repository on GitHub: `https://github.com/manikandan151113/5G-Guardian`
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/5G-Guardian.git
   cd 5G-Guardian
   ```
3. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

### 2. Environment Setup
* **Android Studio:** Ladybug (2024.2+) or newer recommended.
* **JDK:** OpenJDK 11 or higher.
* **Android SDK:** API Level 24 (`minSdk`) to API Level 36 (`targetSdk`).

### 3. FOSS & Privacy Principles (Strictly Enforced)
Because 5G Guardian is built for distribution via **F-Droid**, every contribution must adhere to strict FOSS standards:
* **No Closed-Source SDKs:** Do not add proprietary libraries, Google Mobile Services (GMS), Firebase, or non-free tracking SDKs.
* **No Unsolicited Network Calls:** The app must remain 100% offline. Do not add analytics, remote crash reporting, or external API pings.
* **Permissive or GPLv3 Compatible Licenses:** Any third-party dependency must use an approved open-source license (MIT, Apache-2.0, BSD, GPLv3).

### 4. Running Tests
Before submitting a PR, ensure all local unit tests and Robolectric suites pass:
```bash
# Linux/macOS
./gradlew :app:testDebugUnitTest

# Windows
.\gradlew.bat :app:testDebugUnitTest
```

### 5. Submitting a Pull Request
1. Commit your changes with clear, imperative commit messages:
   ```bash
   git commit -m "telephony: improve 5G NSA fallback detection during low-power state"
   ```
2. Push the branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
3. Open a Pull Request targeting the `main` branch of `manikandan151113/5G-Guardian`.
4. Fill out the Pull Request template completely.

---

## Reporting Issues

* **Bug Reports:** Use our Bug Report template. Include device manufacturer, Android OS version, active cellular carrier, and reproduction steps.
* **Feature Requests:** Use our Feature Request template to outline the proposed enhancement and its user benefit.
