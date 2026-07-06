# Security Policy for 5G Guardian 🛡_

## Supported Versions

Only the latest release of 5G Guardian is actively supported with security updates. We recommend always running the latest version available on GitHub Releases or F-Droid.

| Version | Supported |
| ------- | --------- |
| v1.0.x  | ✅ Yes     |
| < v1.0  | ❌ No      |

## Reporting a Vulnerability

We take the security of 5G Guardian seriously. Since the application handles network telemetry and call state tracking, maintaining absolute privacy and offline security is our primary focus.

If you discover a security vulnerability, please do **not** open a public issue. Instead, report it privately to our development team:

1. Email the details to **manikandan151113@gmail.com**.
2. Include a detailed description of the vulnerability, steps to reproduce, and any proof of concept or potential impact.
3. We will acknowledge receipt of your report within 48 hours and work with you to resolve it.
4. Once resolved, we will publish an update and coordinate a public disclosure if appropriate.

## Security Design Principles

5G Guardian is designed to protect your privacy and ensure secure execution:
- **Offline First**: The app makes absolutely zero external network requests and does not contact any servers. Your location and cellular metadata never leave the device.
- **Zero Proprietary SDKs**: No Google Mobile Services (GMS), Firebase Analytics, or third-party tracking libraries are included, minimizing the attack surface.
- **Minimal Service Exporting**: All background services and broadcast receivers are non-exported (`android:exported="false"`), preventing other apps on the device from triggering or intercepting monitor events.
- **Local Data Only**: Database logging is stored locally using Android's Room library in protected app-private storage.
