#!/usr/bin/env bash
# Build SoundbarRemote.apk with plain Android SDK tools (no Gradle).
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
BT="$SDK/build-tools/36.0.0"
JAR="$SDK/platforms/android-34/android.jar"
OUT=build
APK=SoundbarRemote.apk
KS="$OUT/../debug.keystore"

rm -rf "$OUT"; mkdir -p "$OUT/res" "$OUT/gen" "$OUT/classes" "$OUT/dex"

echo "[1/6] aapt2 compile"
"$BT/aapt2" compile --dir res -o "$OUT/res/res.zip"

echo "[2/6] aapt2 link"
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml \
  --min-sdk-version 21 --target-sdk-version 34 \
  --version-code 1 --version-name 1.0 \
  --java "$OUT/gen" -o "$OUT/base.apk" "$OUT/res/res.zip"

echo "[3/6] javac"
javac -nowarn -source 8 -target 8 -classpath "$JAR" -d "$OUT/classes" \
  $(find src "$OUT/gen" -name '*.java') 2>&1 | grep -v 'bootstrap classpath\|^1 warning\|source value 8\|target value 8\|To suppress warnings' || true

echo "[4/6] d8"
"$BT/d8" --release --lib "$JAR" --min-api 21 --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

echo "[5/6] package + zipalign"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
jar uf "$OUT/unsigned.apk" -C "$OUT/dex" classes.dex
"$BT/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "[6/6] sign"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias sbremote -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Soundbar Remote" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
  --out "$APK" "$OUT/aligned.apk"
"$BT/apksigner" verify "$APK" && echo "OK -> $(pwd)/$APK"
