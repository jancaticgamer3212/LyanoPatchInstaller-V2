#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

SDK="C:/Android/Sdk"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35/android.jar"
KS="$HOME/.android/debug.keystore"

MIN_SDK="${MIN_SDK:-21}"
OUT="${OUT:-LyanoPatchInstaller-V2.apk}"

rm -rf build
mkdir -p build/classes build/dex

MANIFEST=AndroidManifest.xml
MANIFEST_SDK=$(sed -n 's/.*android:minSdkVersion="\([0-9]*\)".*/\1/p' AndroidManifest.xml)
if [ "$MIN_SDK" != "$MANIFEST_SDK" ]; then
  sed "s/android:minSdkVersion=\"$MANIFEST_SDK\"/android:minSdkVersion=\"$MIN_SDK\"/" \
    AndroidManifest.xml > build/AndroidManifest.xml
  MANIFEST=build/AndroidManifest.xml
fi

echo "== aapt2 link (minSdk $MIN_SDK)"
"$BT/aapt2.exe" link -o build/app.unsigned.apk \
  -I "$PLATFORM" \
  --manifest "$MANIFEST" \
  -A assets \
  --min-sdk-version "$MIN_SDK" --target-sdk-version 22

echo "== javac"
javac -source 8 -target 8 -bootclasspath "$PLATFORM" -d build/classes \
  src/com/qoder/playstoreswap/MainActivity.java

echo "== d8"
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api "$MIN_SDK" \
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
rm -f "$OUT"
java -jar "$BT/lib/apksigner.jar" sign \
  --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out "$OUT" build/app.aligned.apk

echo "== verify"
java -jar "$BT/lib/apksigner.jar" verify --verbose "$OUT" | head -6
ls -l "$OUT"
