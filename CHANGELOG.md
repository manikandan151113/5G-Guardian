# Changelog

All notable changes to the **5G Guardian** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-07-06

### Added
- **Core Signal Monitoring**: Track NR/5G connection states vs. LTE/4G/3G/2G connections.
- **Persistent Service**: Foreground service with notification indicators to prevent system termination.
- **Customizable Alerts**: Looping alarm mode and single-shot notification tone settings.
- **Custom Sound Support**: Standard file browser to load custom alert audio files (MP3/WAV/OGG) via URI permissions.
- **Simulation Console**: Built-in mock signal and call state toggles for easy UI/alert validation.
- **FOSS Repository Polish**: Added LICENSE (GPL v3.0), contributing guidelines, code of conduct, templates, and CI/CD pipelines.

### Changed
- **Package Renaming**: Refactored namespace from `com.example` to `com.fivegguardian` for publication.
- **Aesthetic Refinements**: Fully polished UI cards, state labels, and button behaviors.
- **Dependency Cleanups**: Excluded proprietary Firebase dependencies from build scripts.

---

### Releases Reference
For downloads and details, visit the [GitHub Releases](https://github.com/manikandan151113/5G-Guardian/releases) page.
