# Release Process for 5G Guardian 🚀

This document outlines the standard release process for packaging, signing, and releasing new versions of **5G Guardian** to GitHub and F-Droid.

---

## 1. Versioning Strategy

We use [Semantic Versioning (SemVer)](https://semver.org/):
- **MAJOR** version for incompatible API or structural changes.
- **MINOR** version for functional additions in a backwards-compatible manner.
- **PATCH** version for backwards-compatible bug fixes.

Both `versionCode` (integer) and `versionName` (string) must be incremented in `app/build.gradle.kts` before releasing.

---

## 2. Release Checklist

### Step 1: Update Build Configuration
In `app/build.gradle.kts`:
- Increment `versionCode` (e.g., `1` -> `2`).
- Update `versionName` (e.g., `"1.0"` -> `"1.1"`).
- Commit these changes with the message `Release vX.Y.Z`.

### Step 2: Update Changelog
Update [CHANGELOG.md](CHANGELOG.md) to summarize all additions, changes, and fixes under a new version heading with the release date.

### Step 3: Local Verification
Run standard tests to ensure the release package is fully stable:
```bash
./gradlew test
```
Verify the build works:
```bash
./gradlew assembleRelease
```

### Step 4: Create Tag and Push
Tag the release commit:
```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin master --tags
```

---

## 3. Signing Release Builds

For public distribution on GitHub, the APK must be signed.

### Local Signing
You can configure signing keys in your local environment by creating a `.env` file containing:
```properties
KEYSTORE_PATH=/path/to/keystore.jks
STORE_PASSWORD=your_keystore_password
KEY_PASSWORD=your_key_password
```
The Gradle build file automatically reads these environment variables.

### GitHub Actions Signing
If you configure secret variables in your GitHub repository, the Release workflow will automatically sign the APK and attach it to the release assets:
- `KEYSTORE_BASE64`: Base64 encoded keystore file.
- `KEYSTORE_PASSWORD`: Keystore store password.
- `KEY_ALIAS`: Keystore key alias.
- `KEY_PASSWORD`: Keystore key password.

---

## 4. F-Droid Integration

F-Droid builds from source on their own servers and signs the app with their own key.

1. Ensure all changes compile with open-source dependencies.
2. Update the F-Droid metadata recipe in the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository to point to the new tag.
3. The F-Droid build server will automatically pick up the new git tag, build it, and publish it to the F-Droid client app within a few days.
