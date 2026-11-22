# Play Store Swapper

One-tap root tool that replaces the stock Google Play Store on Android 5.x with the patched
**Android Play Store Revival** client from [Lyano Market](https://market.lyano.ovh/), installs it
as a system app in `/system/priv-app/Phonesky`, removes the old `.odex` files, and reboots.

The revival client only works when it is installed as a system app — this tool does that.

## What it does

Tapping **Replace Play Store & Reboot** runs a root shell script that:

1. Remounts `/system` read-write (with fallbacks for Samsung's block-device mount)
2. Uninstalls the current Play Store and wipes its data (`/data/app/com.android.vending-*`,
   `/data/data/com.android.vending`, dalvik-cache entries)
3. Deletes `/system/priv-app/Phonesky` (including old `.odex`/oat files), recreates it and copies
   the bundled bootstrapper APK to `/system/priv-app/Phonesky/Phonesky.apk` (`root:root 644`)
4. Fixes the SELinux context with `restorecon` (fallback `chcon u:object_r:system_file:s0`)
5. Syncs and reboots

The full script output is shown in the app, so a failure (e.g. `/system` stays read-only or
runs out of space) is visible before anything is rebooted.

## Bundled Play Store

The APK embedded in `assets/playstore.apk` is the Google Play Store **5.12.10** client patched
by the [Android Play Store Revival (Lyano Market)](https://market.lyano.ovh/) project
(formerly Android Market Reborn). It is not the stock Google-signed client. All credit for the
patched store goes to Lyano; this repository only automates installing it as a system app on a
rooted device.

## Requirements

- Rooted device with a working `su` (built and tested against Android 5.1.1, e.g. Galaxy S6 edge /
  Galaxy A3 — targetSdk 22, minSdk 21)
- Enough free space on `/system` for the ~15 MB APK
- Recommended: a TWRP backup of `/system` before running it

## Usage

1. Install `PlayStoreSwapper.apk` (enable "Unknown sources" or use `adb install -r PlayStoreSwapper.apk`)
2. Open **Play Store Swapper** and grant the root prompt
3. Tap **Replace Play Store & Reboot**
4. After the reboot the first boot takes a while (the new APK gets dex-optimized)

The helper app is debug-signed and standalone; it can be uninstalled afterwards.

## Build

Built without Gradle, using the Android SDK build tools directly:

```bash
# edit SDK paths at the top of build.sh if needed
./build.sh
```

The script runs `aapt2 link` → `javac` → `d8` → zips `classes.dex` → `zipalign` → `apksigner`,
and produces `PlayStoreSwapper.apk` signed with the local debug keystore.

## Notes / disclaimer

- This modifies `/system` and wipes Play Store data (accounts themselves live in GMS and are
  unaffected, but the store starts fresh).
- It is unofficial and not affiliated with Google or Lyano. Use at your own risk; you are
  responsible for backing up your device.
- A newer Play Store may later self-update into `/data/app`; that is normal and does not remove
  the system install.
