## Build Project

Clone [paritytech/polkadot-android-community](https://github.com/paritytech/polkadot-android-community)
and follow the public setup instructions in the root [README](../README.md).
Build-time configuration, signing, and distribution requirements are documented in
[docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md). Never commit keystores,
`google-services.json`, service-account files, or credentials.

## Utility Scripts

### Generate BIP39 Mnemonic
Generate a random 12-word mnemonic for testing:
```bash
python3 scripts/generate-mnemonic.py
```

# CI/CD Workflows

This document describes all continuous integration and delivery flows for the Polkadot Android application.

## Flows Overview

### 1. Pull Request Validation Flow
**Trigger:** Pull requests to any branch (except release branches)  
**Purpose:** Validate code changes through automated testing

**Steps:**
1. Check for `skip-ci` label
2. Setup Android development environment
3. Run unit tests
4. Run build

**Workflows:**
- [`pr.yml`](workflows/pr.yml)

---

### 2. Development Build Distribution Flow
**Trigger:** Manual dispatch or PR merge to `main` branch  
**Purpose:** Distribute development builds to QA team via Firebase

**Steps:**
1. Setup Android environment
2. Calculate and update build number (10100 + run_number)
3. Build app with Debug configuration
4. Upload to Firebase App Distribution
5. Notify configured groups

**Workflows:**
- [`firebase_debug_distribution.yml`](workflows/firebase_debug_distribution.yml)

**Configuration:**
- Build type: Debug
- Default groups: `dev-team`

---

### 3. Production Release Flow
**Trigger:** Manual workflow dispatch  
**Purpose:** Prepare and distribute production releases to Firebase

**Steps:**

#### Phase 1: Release Preparation
1. Validate user permissions (optional)
2. Create release branch from source ref (default: `main`)
3. Optionally bump version (major/minor/patch/no-bump)
4. Commit version changes to release branch
5. Create pull request to `main`
6. Trigger Firebase Release workflow

#### Phase 2: Firebase Distribution (triggered automatically or by PR updates)
1. Security verification (only bot-initiated PRs allowed)
2. Increment build number in Release configuration
3. Commit build number update
4. Run tests
5. Build and upload to Firebase
6. Comment on PR with build information

#### Phase 3: Backport to Source Branch (triggered after PR merge)
1. Extract source branch metadata from merged PR
2. Validate source branch exists
3. Create backport PR: `release-{version}` → `source_ref` (e.g., `main`)
4. Include incremented build numbers and any hotfixes from release branch

**Workflows:**
- [`release_prepare.yml`](workflows/release_prepare.yml) - Phase 1
- [`firebase_release_distribution.yml`](workflows/firebase_release_distribution.yml) - Phase 2 & 3

**Configuration:**
- Build type: Release
- Branches: `release-{version}` → `main` → backport to `source_ref`
- Source branch tracking: Embedded in PR metadata

---

## Version and Build Number Management

### Debug Builds
- **Version:** Read from `Versions.kt` (not changed)
- **Build number:** `10000 + github.run_number`

### Release Builds
- **Version:**
  - Format: `X.Y.Z` (major.minor.patch)
  - Updated by `release_prepare.yml` based on bump level
  - Stored in `Versions.kt` → `DefaultVersionName`
- **Build number:**
  - Auto-incremented by `firebase_release_distribution.yml`
  - Stored in `Versions.kt` → `DefaultVersionCode`

---

## Firebase Distribution

### Debug Builds
- **App ID:** `ANDROID_FIREBASE_APP_ID` (from secrets)
- **Groups:** `dev-team`
- **APK:** Debug variant with debug keystore

### Release Builds
- **App ID:** `ANDROID_FIREBASE_RELEASE_APP_ID` (from secrets)
- **Groups:** `dev-team`
- **APK:** Release variant with release keystore

---

## Security

### Release Workflows
Both `release_prepare.yml` and `firebase_release_distribution.yml` include security checks:

1. **`release_prepare.yml`:**
  - Only authorized users can run (optional, can be enabled in job condition)

2. **`firebase_release_distribution.yml`:**
  - `workflow_dispatch`: Must be triggered by `github-actions[bot]`
  - `pull_request`: PR must be created by `github-actions[bot]`

This ensures release builds can only be initiated through the official release process.

---

## Build Artifacts Storage (S3)

All Android builds are automatically uploaded to Scaleway Object Storage for archival and distribution.

**Bucket:** `polkadot-app-artefacts` (region: `fr-par`)

### File Naming Scheme

| Workflow | Path Pattern | Static Path | Example URL |
|----------|-------------|-------------|-------------|
| Debug (Firebase) | `/android/debug/polkadot-app-{version}-{build}.apk` | `/android/debug/polkadot-app.apk` | `http://polkadot-app-artefacts.s3.fr-par.scw.cloud/android/debug/polkadot-app-1.2.3-10150.apk` |
| Release (Firebase via PR) | `/android/releases/polkadot-app-{version}-{build}.apk` | `/android/releases/polkadot-app.apk` | `http://polkadot-app-artefacts.s3.fr-par.scw.cloud/android/releases/polkadot-app-1.0.0-456.apk` |
| Nightly Release | `/android/nightly/polkadot-app-{version}-{build}.apk` | `/android/nightly/polkadot-app.apk` | `http://polkadot-app-artefacts.s3.fr-par.scw.cloud/android/nightly/polkadot-app-1.0.0-1456.apk` |

**Static paths** always point to the latest build from that workflow, while **versioned paths** preserve all historical builds.

---

## Scripts

Version management scripts located in `.github/scripts/`:

- **`read_versions.py`** - Reads current version and build number from `Versions.kt`
- **`update_marketing_version.py`** - Updates version (DefaultVersionName) in `Versions.kt`
- **`update_build_number.py`** - Updates or increments build number (DefaultVersionCode) in `Versions.kt`

All scripts work with `build-logic/convention/src/main/kotlin/Versions.kt` file.
