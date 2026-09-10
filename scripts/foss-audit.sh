#!/usr/bin/env bash
# ==============================================================================
# 5G Guardian - FOSS & F-Droid Compliance Audit Script
# ==============================================================================
set -euo pipefail

FAILED=0
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "======================================================================"
echo " Starting FOSS & F-Droid Audit for 5G Guardian"
echo "======================================================================"

check_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

check_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    FAILED=1
}

# 1. License Check
echo "--- Checking Licensing ---"
if [ -f "LICENSE" ] && grep -q "GNU GENERAL PUBLIC LICENSE" "LICENSE"; then
    check_pass "LICENSE file exists and contains GNU General Public License v3."
else
    check_fail "LICENSE file is missing or not GPL v3."
fi

if [ -f "THIRD_PARTY_LICENSES.md" ]; then
    check_pass "THIRD_PARTY_LICENSES.md exists and documents all dependencies."
else
    check_fail "THIRD_PARTY_LICENSES.md is missing."
fi

# 2. Permission Check (No INTERNET)
echo "--- Checking Android Permissions ---"
if grep -q "android.permission.INTERNET" app/src/main/AndroidManifest.xml; then
    check_fail "Found android.permission.INTERNET in AndroidManifest.xml! The app must be 100% offline."
else
    check_pass "Zero INTERNET permission requested. App is completely offline."
fi

# 3. Proprietary SDKs & Trackers Scan
echo "--- Scanning for Proprietary SDKs & Trackers ---"
PROPRIETARY_PATTERNS="play-services|firebase|crashlytics|admob|analytics|facebook|sentry|mixpanel|amplitude|okhttp|retrofit|moshi|secrets-gradle-plugin|GEMINI_API_KEY"

FOUND_ISSUES=$(grep -rnI -E "$PROPRIETARY_PATTERNS" \
    --exclude-dir=".gradle" \
    --exclude-dir="build" \
    --exclude-dir=".git" \
    --exclude-dir="scripts" \
    --exclude="README.md" \
    --exclude="THIRD_PARTY_LICENSES.md" \
    app gradle || true)

if [ -n "$FOUND_ISSUES" ]; then
    check_fail "Found prohibited proprietary or tracker references:\n$FOUND_ISSUES"
else
    check_pass "No proprietary SDKs, trackers, or cloud AI services found in source or build files."
fi

# 4. Binary File Check
echo "--- Checking for Embedded Binaries ---"
SUSPICIOUS_BINS=$(find . -type f \( -name "*.jar" -o -name "*.aar" -o -name "*.so" -o -name "*.dll" -o -name "*.exe" \) \
    -not -path "*/.gradle/*" \
    -not -path "*/build/*" \
    -not -path "*/my-upload-key.jks" || true)

if [ -n "$SUSPICIOUS_BINS" ]; then
    check_fail "Found unapproved binary files in source tree:\n$SUSPICIOUS_BINS"
else
    check_pass "Zero unapproved prebuilt binary libraries (.jar, .aar, .so) in repository."
fi

# 5. Dynamic Versions Check
echo "--- Checking for Dynamic Dependency Versions ---"
if grep -rnI -E "[:\"][0-9]+.*(\+|\blatest\b)" gradle/libs.versions.toml app/build.gradle.kts; then
    check_fail "Dynamic dependency versions detected. Use strict, reproducible versions for F-Droid."
else
    check_pass "All dependencies use fixed, reproducible versions."
fi

# 6. F-Droid Metadata Check
echo "--- Checking F-Droid Metadata ---"
if [ -f "metadata/com.aistudio.fivegguardian.qywtpa.yml" ] && [ -f "fastlane/metadata/android/en-US/title.txt" ]; then
    check_pass "F-Droid YAML recipe and fastlane metadata present and aligned."
else
    check_fail "F-Droid metadata files missing."
fi

# 7. Clean Build and Verification
echo "--- Testing Reproducible Builds & Unit Tests ---"
gradle clean assembleDebug assembleRelease testDebugUnitTest lintDebug --quiet

check_pass "Build pipeline (clean, assembleDebug, assembleRelease, testDebugUnitTest, lintDebug) succeeded."

echo "======================================================================"
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}FOSS/F-Droid PRE-CHECK: PASS${NC}"
    exit 0
else
    echo -e "${RED}FOSS/F-Droid PRE-CHECK: FAIL${NC}"
    exit 1
fi
