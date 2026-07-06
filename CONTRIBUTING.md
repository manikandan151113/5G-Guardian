# Contributing to 5G Guardian 🛡️

Thank you for your interest in contributing to **5G Guardian**! We welcome all contributions from bug reports and documentation updates to feature implementations and bug fixes.

---

## Code of Conduct

By participating in this project, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please report unacceptable behavior to the project maintainers.

## How Can I Contribute?

### 🐛 Reporting Bugs

If you find a bug, please check the [existing issues](https://github.com/manikandan151113/5G-Guardian/issues) first. If it's not already reported:
1. Open a new issue using our **Bug Report** template.
2. Provide a clear description of the issue.
3. Include steps to reproduce, the expected behavior, and screenshots or logs (if applicable).
4. List the Android version and device model you observed the issue on.

### 💡 Requesting Features

To request a new feature:
1. Check the [Roadmap](README.md#roadmap) to see if it is already planned.
2. Open a new issue using our **Feature Request** template.
3. Describe the feature, why it would be useful, and how it should work.

### 🛠️ Submitting Code Changes

1. **Fork the Repository**: Create your own copy of the repository.
2. **Create a Branch**: Create a descriptive branch name (e.g., `feature/custom-vibration` or `fix/notification-leak`).
3. **Make Code Changes**: Write clean, readable code following Android best practices.
   - Keep components focused and reusable.
   - Follow standard Kotlin naming conventions.
4. **Write Tests**: Add unit tests or Robolectric tests in `app/src/test` for new logic or bug fixes.
5. **Verify Local Builds**: Make sure the application compiles and all tests pass locally:
   ```bash
   ./gradlew test
   ```
6. **Submit a Pull Request**: Submit your PR to the `master` branch.
   - Fill out our Pull Request template.
   - Ensure the CI build passes successfully.

## Coding Style & Guidelines

- **Kotlin**: Follow the official [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- **Jetpack Compose**: Keep state management in the ViewModel or use hoisting. Avoid putting business logic inside UI composables.
- **Resource Naming**: Use descriptive, snake_case names for resources (e.g., `ic_shield_active.xml`, `strings.xml` keys).
- **Offline First**: The app is designed to be 100% offline. Avoid introducing dependencies that require internet access or contact external servers (like analytics or proprietary crash reporters).

## F-Droid Compatibility Requirements

All contributions must respect F-Droid's requirements to ensure the app can build completely from source on their servers:
- Do not add proprietary libraries, trackers, or advertising SDKs.
- Avoid introducing mandatory Google Play Services (GMS) APIs. If GMS APIs are used, always provide a free software fallback (e.g., using Android Open Source Project equivalent APIs).
- Maintain reproducible build paths.

---

*Thank you for helping keep 5G Guardian secure, stable, and FOSS!*
