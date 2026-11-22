# LyanoPatchInstaller V2

Installs a patched **Android Play Store Revival** client from
[Lyano Market](https://market.lyano.ovh/), with two methods:

- **Root** — backs up the current Play Store, replaces it in `/system`, removes the old
  `.odex` files, wipes its data and reboots.
- **No root** — installs the same build as an ordinary app under the renamed package
  `com.lyano.playstore.patch`, so the existing Play Store stays installed. Both stores coexist.

The helper runs on **Android 2.2+ (API 8)**. Each build's own minimum Android version is shown in
the app, and all three bundled builds work with either method.

## Root method

Pick a build, choose **Root — replace system Play Store**, then tap
**Replace Play Store & Reboot**. The app runs a root shell script that:

1. Remounts `/system` read-write (with fallbacks for Samsung's block-device mount)
2. Backs up the original Play Store APK to `/sdcard/LyanoPatchInstaller/backup/` (an existing
   backup is never overwritten) and records its original path
3. Uninstalls the current Play Store and wipes its data (`/data/app/com.android.vending-*`,
   `/data/data/com.android.vending`, dalvik-cache entries)
4. Deletes the stock APK (`/system/priv-app/Phonesky*` on Android 4.4+, `/system/app/Phonesky.apk`
   or `Vending.apk` on older versions, including `.odex` files) and copies the selected APK into
   place (`root:root 644`)
5. Fixes the SELinux context with `restorecon` (fallback `chcon u:object_r:system_file:s0`)
6. Syncs and reboots

The full script output is shown in the app, so a failure (e.g. `/system` stays read-only or
runs out of space) is visible before anything is rebooted.

### Restore original Play Store

The **Restore Original Play Store** button (root) removes the installed Play Store, puts the
backed-up APK back at its recorded path (or the default system path), fixes permissions and
SELinux context, and reboots. The patched store's data is wiped; accounts live in Google Play
services and are unaffected.

## No-root method

Choose **No root — install as second store**. The app writes the renamed clone of the selected
build to `Download/LyanoPlayStore-<version>-clone.apk` and opens the system package installer.
Tap **Install**, wait, and a second Play Store appears next to the original one.

The clones are prebuilt in `assets/clone-<version>.apk`. Compared to the original APKs they:

- use the package name `com.lyano.playstore.patch` instead of `com.android.vending`
- declare renamed permissions (e.g. `com.lyano.playstore.patch.BILLING`) so there is no
  `INSTALL_FAILED_DUPLICATE_PERMISSION` clash with the real store
- expose renamed content-provider authorities (`com.lyano.playstore.patch.finsky.*`)
- are re-signed with a debug key (the original signature cannot be reused)
- all share `versionCode 999000000`, so one build can replace another

Limitations of the no-root install:

- The clone is a normal app, so it lacks the privileged `INSTALL_PACKAGES` permission. Browsing,
  searching and library features work, but downloading/updating apps will likely fail.
- Google services may reject or restrict a re-signed, renamed client.
- Both launchers show the Play Store icon; the clone is labelled **Play Store**.

## Bundled Play Store builds

The APKs in `assets/` are Google Play Store clients patched by the
[Android Play Store Revival (Lyano Market)](https://market.lyano.ovh/) project (formerly
Android Market Reborn). They are not the stock Google-signed clients. All credit for the patched
store goes to Lyano; this repository only automates installing one of them.

| Version  | Size    | Min Android | Era            | Notes                    |
|----------|---------|-------------|----------------|--------------------------|
| 5.12.10  | ~14.9MB | 2.3 (API 9) | late 2015      | Newest, default          |
| 4.4.22   | ~6MB    | 2.2 (API 8) | 2013           | Older client             |
| 3.4.7    | ~3.7MB  | 2.2 (API 8) | 2011–2012      | Oldest / lightest client |

The helper itself needs Android 2.2+; a build whose minimum is higher (5.12.10 needs 2.3) only
works on devices at or above that version. The app shows these minimums next to each build and
also displays the current device's Android version.

## Requirements

- Android 2.2+ (API 8) device; built and tested against Android 5.1.1 (Galaxy S6 edge / Galaxy A3)
- Root with a working `su` **only** for the root method
- Enough free space on `/system` for the selected APK (root method)
- Recommended: a TWRP backup of `/system` before using the root method

## Usage

1. Install `LyanoPatchInstaller-V2.apk` (enable "Unknown sources" or use
   `adb install -r LyanoPatchInstaller-V2.apk`)
2. Open **LyanoPatchInstaller V2**
3. Choose **Root** or **No root**
4. Select the patched build (each line shows its minimum Android version and era)
5. Tap the button
   - Root: grant the root prompt, wait for the log to finish, the device reboots (first boot takes
     a while while the new APK is optimized)
   - No root: confirm **Install** in the system installer; no reboot needed
6. To undo a root install, tap **Restore Original Play Store** and confirm

The helper app is debug-signed and standalone; it can be uninstalled afterwards. The backup in
`/sdcard/LyanoPatchInstaller/backup/` stays until you delete it.

## Build

Built without Gradle, using the Android SDK build tools directly:

```bash
# edit SDK paths at the top of build.sh if needed
bash make-clones.sh     # builds + signs assets/clone-<version>.apk for all three builds
./build.sh              # packages the helper app -> LyanoPatchInstaller-V2.apk
```

`build.sh` runs `aapt2 link` → `javac` → `d8` → zips `classes.dex` → `zipalign` → `apksigner`.
`make-clones.sh` uses `tools/axml_patch.py`, which binary-patches `AndroidManifest.xml` inside the
APK (package name, versionCode, permissions, provider authorities, component names) without
rebuilding resources, then re-signs with the local debug keystore.

## Notes / disclaimer

- The root method modifies `/system` and wipes Play Store data (accounts themselves live in GMS
  and are unaffected, but the store starts fresh). The original APK is backed up to
  `/sdcard/LyanoPatchInstaller/backup/` first and can be restored from the app.
- Restoring only reverts the system Play Store; a no-root clone (`com.lyano.playstore.patch`)
  is left installed and has to be uninstalled separately.
- Unofficial and not affiliated with Google or Lyano. Use at your own risk; you are responsible
  for backing up your device.
- A newer Play Store may later self-update into `/data/app`; that is normal and does not remove
  the system install.
