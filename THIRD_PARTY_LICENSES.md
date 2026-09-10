# Third-Party Licenses

This document lists all third-party libraries and dependencies used by **5G Guardian**, their versions, licenses, project URLs, and packaging type.

All third-party dependencies are distributed under permissive Free and Open Source Software (FOSS) licenses (Apache 2.0, MIT, EPL 1.0) compatible with the **GNU General Public License v3.0 or later (GPL-3.0-or-later)** under which 5G Guardian is licensed.

---

## Direct Application Dependencies (Runtime)

| Dependency | Version | License | Project URL | Type |
| :--- | :--- | :--- | :--- | :--- |
| **AndroidX Core KTX** (`androidx.core:core-ktx`) | `1.18.0` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/core) | Direct / AAR |
| **AndroidX Activity Compose** (`androidx.activity:activity-compose`) | `1.10.1` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/activity) | Direct / AAR |
| **AndroidX Lifecycle Runtime KTX** (`androidx.lifecycle:lifecycle-runtime-ktx`) | `2.8.7` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/lifecycle) | Direct / AAR |
| **AndroidX Lifecycle ViewModel Compose** (`androidx.lifecycle:lifecycle-viewmodel-compose`) | `2.8.7` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/lifecycle) | Direct / AAR |
| **AndroidX Lifecycle Runtime Compose** (`androidx.lifecycle:lifecycle-runtime-compose`) | `2.8.7` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/lifecycle) | Direct / AAR |
| **AndroidX Compose BOM** (`androidx.compose:compose-bom`) | `2024.09.00` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/compose) | Direct / BOM |
| **AndroidX Compose UI** (`androidx.compose.ui:ui`) | Managed by BOM | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/compose-ui) | Direct / AAR |
| **AndroidX Compose UI Graphics** (`androidx.compose.ui:ui-graphics`) | Managed by BOM | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/compose-ui) | Direct / AAR |
| **AndroidX Compose UI Tooling Preview** (`androidx.compose.ui:ui-tooling-preview`) | Managed by BOM | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/compose-ui) | Direct / AAR |
| **AndroidX Compose Material 3** (`androidx.compose.material3:material3`) | Managed by BOM | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/compose-material3) | Direct / AAR |
| **AndroidX Compose Material Icons Core** (`androidx.compose.material:material-icons-core`) | Managed by BOM | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx/releases/compose-material) | Direct / AAR |
| **Kotlinx Coroutines Core** (`org.jetbrains.kotlinx:kotlinx-coroutines-core`) | `1.10.2` | Apache License 2.0 | [github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Direct / JAR |
| **Kotlinx Coroutines Android** (`org.jetbrains.kotlinx:kotlinx-coroutines-android`) | `1.10.2` | Apache License 2.0 | [github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Direct / AAR |

---

## Test Dependencies (Not Packaged into Release APK)

| Dependency | Version | License | Project URL | Type |
| :--- | :--- | :--- | :--- | :--- |
| **JUnit 4** (`junit:junit`) | `4.13.2` | Eclipse Public License 1.0 | [junit.org/junit4](https://junit.org/junit4/) | Test / Direct |
| **AndroidX Test Ext JUnit** (`androidx.test.ext:junit`) | `1.3.0` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx) | Test / Direct |
| **AndroidX Test Core** (`androidx.test:core`) | `1.6.1` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx) | Test / Direct |
| **AndroidX Test Runner** (`androidx.test:runner`) | `1.6.2` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx) | Test / Direct |
| **Espresso Core** (`androidx.test.espresso:espresso-core`) | `3.7.0` | Apache License 2.0 | [developer.android.com/jetpack/androidx](https://developer.android.com/jetpack/androidx) | Test / Direct |
| **Kotlinx Coroutines Test** (`org.jetbrains.kotlinx:kotlinx-coroutines-test`) | `1.10.2` | Apache License 2.0 | [github.com/Kotlin/kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | Test / Direct |
| **Robolectric** (`org.robolectric:robolectric`) | `4.16.1` | MIT License | [robolectric.org](https://robolectric.org/) | Test / Direct |
| **Roborazzi** (`io.github.takahirom.roborazzi`) | `1.59.0` | Apache License 2.0 | [github.com/takahirom/roborazzi](https://github.com/takahirom/roborazzi) | Test / Direct |

---

## Transitive Dependencies

All transitive runtime dependencies are open-source components of the **Android Open Source Project (AOSP)**, **AndroidX**, or the **Kotlin Standard Library**, licensed under the **Apache License 2.0**.

---

## Excluded Proprietary and Tracking Dependencies

The following proprietary, cloud-reliant, or tracking libraries have been audited and explicitly verified as **NOT PRESENT** in this repository:

- ❌ **Google Play Services** (No GMS location, auth, or mobile services)
- ❌ **Firebase** (No Crashlytics, Analytics, AI, or Core)
- ❌ **Google Cloud / Gemini SDKs** (No remote AI SDKs or cloud keys)
- ❌ **Analytics / Tracking SDKs** (No Sentry, Mixpanel, Amplitude, AppsFlyer, etc.)
- ❌ **Advertising Networks** (No AdMob, Unity Ads, etc.)
- ❌ **Network Client Libraries** (No Retrofit, OkHttp, Moshi — app is 100% offline-first)
- ❌ **Bundled Binaries** (Zero proprietary `.jar`, `.aar`, `.so` files checked into source)
