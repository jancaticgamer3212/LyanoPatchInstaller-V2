#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SDK="C:/Android/Sdk"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35/android.jar"
KS="$HOME/.android/debug.keystore"

rm -rf build
mkdir -p build/classes build/dex

echo "== aapt2 link"
"$BT/aapt2.exe" link -o build/app.unsigned.apk \
  -I "$PLATFORM" \
  --manifest AndroidManifest.xml \
  -A assets \
  --min-sdk-version 8 --target-sdk-version 22

echo "== javac"
javac -source 8 -target 8 -bootclasspath "$PLATFORM" -d build/classes \
  src/com/qoder/playstoreswap/MainActivity.java

echo "== d8"
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 8 \
  --lib "$PLATFORM" --output build/dex $(find build/classes -name '*.class')

echo "== add classes.dex"
python - <<'PY'
import zipfile, shutil
shutil.copyfile('build/app.unsigned.apk', 'build/app.dex.apk')
with zipfile.ZipFile('build/app.dex.apk', 'a', zipfile.ZIP_DEFLATED) as z:
    z.write('build/dex/classes.dex', 'classes.dex')
PY

echo "== zipalign"
"$BT/zipalign.exe" -f 4 build/app.dex.apk build/app.aligned.apk

echo "== sign"
rm -f LyanoPatchInstaller-V2.apk
java -jar "$BT/lib/apksigner.jar" sign \
  --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out LyanoPatchInstaller-V2.apk build/app.aligned.apk

echo "== verify"
java -jar "$BT/lib/apksigner.jar" verify --verbose LyanoPatchInstaller-V2.apk | head -6
ls -l LyanoPatchInstaller-V2.apk
