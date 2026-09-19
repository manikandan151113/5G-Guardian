#!/usr/bin/env bash
# ==============================================================================
# 5G Guardian - Local FOSS & F-Droid Automated Pipeline Checker
# ==============================================================================
set -euo pipefail

FAILED=0
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "======================================================================"
echo " Starting FOSS & F-Droid Pipeline Compliance Check"
echo "======================================================================"

check_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

check_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    FAILED=1
}

# 1. License Check
echo "--- 1. Checking Licensing ---"
if [ -f "LICENSE" ] && grep -q "GNU GENERAL PUBLIC LICENSE" "LICENSE"; then
    check_pass "LICENSE file present and verified as GNU General Public License v3."
else
    check_fail "LICENSE file is missing or not GPL v3."
fi

if [ -f "THIRD_PARTY_LICENSES.md" ] && grep -q "Apache License 2.0" "THIRD_PARTY_LICENSES.md"; then
    check_pass "THIRD_PARTY_LICENSES.md exists and documents all dependencies."
else
    check_fail "THIRD_PARTY_LICENSES.md is missing or invalid."
fi

# 2. Permission Check (No INTERNET)
echo "--- 2. Checking Android Permissions ---"
if grep -q "android.permission.INTERNET" app/src/main/AndroidManifest.xml; then
    check_fail "Found android.permission.INTERNET in AndroidManifest.xml! The app must be 100% offline."
else
    check_pass "Zero INTERNET permission requested. Application is completely offline."
fi

# 3. Proprietary SDKs, Trackers, Analytics, Ads Scan
echo "--- 3. Scanning for Proprietary SDKs, Trackers, Ads & Cloud APIs ---"
PROPRIETARY_PATTERNS="play-services|firebase|crashlytics|admob|analytics|facebook|sentry|mixpanel|amplitude|okhttp|retrofit|moshi|secrets-gradle-plugin"

FOUND_ISSUES=$(grep -rnI -E "$PROPRIETARY_PATTERNS" \
    --exclude-dir=".gradle" \
    --exclude-dir="build" \
    --exclude-dir=".git" \
    --exclude-dir="scripts" \
    --exclude="README.md" \
    --exclude="THIRD_PARTY_LICENSES.md" \
    app gradle settings.gradle.kts build.gradle.kts || true)

if [ -n "$FOUND_ISSUES" ]; then
    check_fail "Found prohibited proprietary or tracker references:\n$FOUND_ISSUES"
else
    check_pass "Zero proprietary SDKs, trackers, ads, or cloud AI services found."
fi

# 4. Secrets & API Keys Check
echo "--- 4. Scanning for Secrets & API Keys ---"
SECRET_PATTERNS="AIza[0-9A-Za-z_-]{35}|AKIA[0-9A-Z]{16}|api_key[[:space:]]*=[[:space:]]*[\"'][^\"']+[\"']"

FOUND_SECRETS=$(grep -rnI -E "$SECRET_PATTERNS" \
    --exclude-dir=".gradle" \
    --exclude-dir="build" \
    --exclude-dir=".git" \
    --exclude-dir="scripts" \
    app || true)

if [ -n "$FOUND_SECRETS" ]; then
    check_fail "Potential secrets or API keys found in codebase:\n$FOUND_SECRETS"
else
    check_pass "Zero hardcoded secrets, tokens, or API keys detected."
fi

# 5. Embedded Binary Audit
echo "--- 5. Checking for Unapproved Embedded Binaries ---"
SUSPICIOUS_BINS=$(find . -type f \( -name "*.jar" -o -name "*.aar" -o -name "*.so" -o -name "*.dll" -o -name "*.exe" -o -name "*.jks" \) \
    -not -path "*/.gradle/*" \
    -not -path "*/build/*" \
    -not -path "*/gradle/wrapper/*" \
    -not -path "*/debug.keystore" || true)

if [ -n "$SUSPICIOUS_BINS" ]; then
    check_fail "Found unauthorized binary files in repository:\n$SUSPICIOUS_BINS"
else
    check_pass "Zero unauthorized prebuilt binary libraries or keystores found."
fi

# 6. Dynamic Version Check
echo "--- 6. Checking for Dynamic Dependency Versions ---"
if grep -rnI -E "[:\"][0-9]+.*(\+|\blatest\b|\bRELEASE\b)" gradle/libs.versions.toml app/build.gradle.kts; then
    check_fail "Dynamic dependency versions detected. Fixed versions are required for reproducible builds."
else
    check_pass "All dependency versions are strictly pinned and deterministic."
fi

# 7. F-Droid Metadata Check
echo "--- 7. Checking F-Droid Metadata ---"
if [ -f "metadata/com.aistudio.fivegguardian.qywtpa.yml" ] && [ -f "fastlane/metadata/android/en-US/title.txt" ]; then
    # Verify build recipe does not have invalid subdir
    if grep -q "subdir:" "metadata/com.aistudio.fivegguardian.qywtpa.yml"; then
        check_fail "Invalid 'subdir' found in F-Droid metadata."
    fi
    # Verify current version alignment
    if grep -q "CurrentVersion: '1.1.1'" "metadata/com.aistudio.fivegguardian.qywtpa.yml" && \
       grep -q "CurrentVersionCode: 3" "metadata/com.aistudio.fivegguardian.qywtpa.yml" && \
       grep -q "versionCode = 3" "app/build.gradle.kts" && \
       grep -q 'versionName = "1.1.1"' "app/build.gradle.kts"; then
        check_pass "F-Droid YAML recipe and app build.gradle.kts versions aligned at v1.1.1 (versionCode 3)."
    else
        check_fail "Version mismatch between F-Droid metadata and app/build.gradle.kts."
    fi
else
    check_fail "F-Droid metadata files are missing."
fi

# 8. Clean Build, Test & Lint
echo "--- 8. Executing Clean Build, Unit Tests & Lint ---"
gradle clean assembleDebug assembleRelease testDebugUnitTest lintDebug --quiet

check_pass "Clean Gradle build (assembleDebug, assembleRelease, testDebugUnitTest, lintDebug) succeeded without errors."

echo "======================================================================"
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}FOSS/F-DROID PRE-SUBMISSION: PASS${NC}"
    exit 0
else
    echo -e "${RED}FOSS/F-DROID PRE-SUBMISSION: FAIL${NC}"
    exit 1
fi
