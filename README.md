# Quick Call Scan & Detect Caller

AI-powered Caller ID, spam detection, live camera OCR number scanning from cards, posters, and screens with Truecaller-style floating overlay and automated GitHub self-updates.

---

## 🚀 GitHub Integration & Self-Update System

This application includes a complete, secure self-updating architecture integrated with GitHub Releases and GitHub Actions:

### 1. How the Self-Update Works
1. **GitHub Actions Workflow** (`.github/workflows/release.yml`):
   - Whenever you push a Git tag formatted like `v1.0.1`, GitHub Actions builds the Android Release APK automatically.
   - It runs the complete Gradle compilation, calculates the SHA-256 fingerprint, and publishes the `.apk` directly to a GitHub Release.
   - The release only publishes if the Gradle build succeeds.
2. **Version Descriptor** (`version.json`):
   - Contains `versionCode`, `versionName`, `apkUrl`, `releaseNotes`, and `minimumSupportedVersion`.
   - Hosted at your public GitHub endpoint:
     `https://raw.githubusercontent.com/<username>/<repo>/main/version.json`
     or on GitHub Pages.
3. **In-App Updater** (`Settings -> Check for Updates`):
   - Compares remote `versionCode` against the installed app `versionCode`.
   - Downloads the APK directly over secure HTTPS with real-time progress.
   - **Security Inspection**: Before installation, the app validates:
     - APK Package Name matches the running app.
     - Version code is strictly $\ge$ installed version (blocks downgrades).
     - Cryptographic signing certificate matches the currently installed keystore signature.
   - Launches Android's official Package Installer UI via `FileProvider` (`REQUEST_INSTALL_PACKAGES`).

---

## 🛠️ Step-by-Step GitHub Setup Guide

### Step 1: Connect AI Studio to Your GitHub Repository
1. In the AI Studio top/settings bar, open the **Export / Push to GitHub** option.
2. Select your repository: `jeebanrai05-eng/Quick-scan-call`.
3. Commit and push the project files to the `main` branch.

### Step 2: Running the Release Workflow

#### Option A: Manual Trigger from your Android Phone or Browser
1. Open GitHub on your Android phone browser or GitHub Mobile app.
2. Go to: `https://github.com/jeebanrai05-eng/Quick-scan-call/actions`.
3. Tap **Build & Release Android APK**.
4. Tap the **Run workflow** dropdown button.
5. Select the `main` branch, enter the release tag (e.g. `v1.0.2`), and tap **Run workflow**.

#### Option B: Automatic Push Trigger via Git Tag
1. `version.json` in the repository is configured for `v1.0.2`:
   ```json
   {
     "versionCode": 3,
     "versionName": "1.0.2",
     "apkUrl": "https://github.com/jeebanrai05-eng/Quick-scan-call/releases/download/v1.0.2/app-release.apk",
     "releaseNotes": "• Smart Speed SMS & WhatsApp Template Builder: 1-tap fast replies for scanned business numbers\n• Batch Numbers Vault: Copy all, organize, and export scanned numbers\n• In-App Self-Update Dashboard Banner with instant background discovery\n• Fixed OCR parsing for international area codes and bracketed country prefixes (+44 (0)20, 1800/800 toll-free)\n• Enhanced 120 FPS AMOLED Dark theme with battery optimizations",
     "minimumSupportedVersion": 1
   }
   ```
2. Create and push a version tag to trigger GitHub Actions:
   ```bash
   git tag v1.0.2
   git push origin v1.0.2
   ```
3. GitHub Actions will trigger automatically, build the release APK with JDK 17, compute its SHA-256 fingerprint, and publish the release asset under GitHub Releases!
4. All installed apps checking for updates will discover version `1.0.2` and offer 1-tap download & install via the Home Screen update banner or Settings updater.

---

## 🔒 Security Guarantees
- **No Hardcoded Tokens:** Zero GitHub personal access tokens or secrets are stored in the APK.
- **Downgrade Protection:** Older or compromised version codes are rejected immediately.
- **Signature Integrity:** The installed app checks the APK signer before delegating to the OS package installer.
- **Non-Silent Installation:** Always prompts the user via the Android system package manager.
