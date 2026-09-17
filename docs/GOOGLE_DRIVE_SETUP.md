# ☁️ Google Drive Authentication & Setup Guide

**syncMyShit** synchronizes your retro emulation save files with a dedicated folder on your Google Drive (`syncMyShit/`).

---

## ⚡ Method 1: Standard One-Click Sign-In (Recommended)

If your Android handheld has Google Play Services installed (standard on Odin 2, Retroid Pocket 3+/4/4Pro, Logitech G Cloud, and stock Android tablets/phones):

1. Launch **syncMyShit**.
2. Follow the 3-step setup wizard to **Step 2 (Connect Google Drive)**.
3. Tap **Sign In with Google**.
4. Select your Google account and grant file access permission.
5. You're done! **syncMyShit** will automatically create a private folder called `syncMyShit` in your Drive.

---

## 🛠️ Method 2: Custom Google Cloud OAuth 2.0 Credentials

### Why use a custom Client ID?
- If you use a **de-Googled handheld** (GrapheneOS, LineageOS, or Chinese handheld firmwares without certified Google Play Services).
- If you want **100% private API quota** without rate-limiting.
- If you want full control over your Google Cloud API access.

### Step-by-Step Instructions:

#### 1. Create a Google Cloud Project
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click the project dropdown in the top bar and click **New Project**.
3. Name it `syncMyShit-Personal` and click **Create**.

#### 2. Enable the Google Drive API
1. In the search bar at the top, type `Google Drive API` and select it.
2. Click **Enable**.

#### 3. Configure the OAuth Consent Screen
1. In the left navigation menu, go to **APIs & Services** > **OAuth consent screen**.
2. Choose **External** user type and click **Create**.
3. Fill in:
   - **App name**: `syncMyShit`
   - **User support email**: Your email address
   - **Developer contact information**: Your email address
4. Click **Save and Continue**.
5. Under **Scopes**, click **Add or Remove Scopes**, search for `drive.file`, and select:
   - `https://www.googleapis.com/auth/drive.file`
6. Under **Test users**, add your own Google email address (important while your project is in testing mode).
7. Save and finish the wizard.

#### 4. Create OAuth 2.0 Credentials (Takes 30 seconds)
1. Go to **APIs & Services** > **Credentials**.
2. Click **+ CREATE CREDENTIALS** > **OAuth client ID**.
3. Application type: Select **Android**.
   - **Package name**: `com.syncmyshit.app`
   - **SHA-1 certificate fingerprint**: `03:16:B2:C3:98:4E:4C:C4:8C:29:3C:49:B6:07:2E:54:F0:D0:A2:82`
4. Click **Create**!

> [!TIP]
> Once created, **1-Tap Google Sign-In** on your handheld will immediately work without entering any IDs or passwords! Google Play Services verifies the package and SHA-1 automatically.

#### Alternative: Web application Client ID
If you prefer not to register an Android client ID, you can create a **Web application** client ID in Google Cloud Console, copy the Client ID, and paste it into **Settings** > **Custom OAuth Client ID** in **syncMyShit**.

---

## 🔒 Privacy & Scopes

- **Backup copies**: Local `.bak` snapshots are kept in `.syncmyshit_backups/` and cloud backups in `syncMyShit/_backups/`.
- **Restricted scope (`drive.file`)**: The app requests only `drive.file` scope, meaning it only has access to files it creates itself in its own folder. It cannot access your other personal Google Drive documents or files.
