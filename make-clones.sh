#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

BT="C:/Android/Sdk/build-tools/35.0.0"
KS="$HOME/.android/debug.keystore"

VERSIONS=("$@")
if [ ${#VERSIONS[@]} -eq 0 ]; then
  VERSIONS=(5.12.10 4.4.22 3.4.7)
fi

for v in "${VERSIONS[@]}"; do
  echo "== $v: patch manifest"
  rm -f "build/clone-$v-unsigned.apk" "build/clone-$v-aligned.apk" "assets/clone-$v.apk"
  python tools/axml_patch.py "assets/$v.apk" "build/clone-$v-unsigned.apk"

  echo "== $v: align + sign"
  "$BT/zipalign.exe" -f -p 4 "build/clone-$v-unsigned.apk" "build/clone-$v-aligned.apk"
  java -jar "$BT/lib/apksigner.jar" sign \
    --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey \
    --out "assets/clone-$v.apk" "build/clone-$v-aligned.apk"
  java -jar "$BT/lib/apksigner.jar" verify "assets/clone-$v.apk" >/dev/null
  echo "== $v: $(du -h "assets/clone-$v.apk" | cut -f1)"
done
