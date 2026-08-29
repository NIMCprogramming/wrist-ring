#!/usr/bin/env bash
set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb"

device_for_model() {
  "$ADB" devices -l | while read -r serial details; do
    if [[ "$details" == *"model:$1 "* ]]; then
      printf '%s' "$serial"
      return
    fi
  done
}

PHONE="$(device_for_model A015)"
WATCH="$(device_for_model SM_R870)"
if [[ -z "$PHONE" || -z "$WATCH" ]]; then
  echo "CMF Phone 1 (A015) and Galaxy Watch4 (SM_R870) must both be connected." >&2
  exit 1
fi

./gradlew :phone:assembleDebug :watch:assembleDebug --quiet
"$ADB" -s "$PHONE" install -r phone/build/outputs/apk/debug/phone-debug.apk >/dev/null
"$ADB" -s "$WATCH" install -r watch/build/outputs/apk/debug/watch-debug.apk >/dev/null
"$ADB" -s "$PHONE" shell am start \
  -n io.github.zymmio.smartring/.phone.MainActivity >/dev/null
"$ADB" -s "$WATCH" shell am start \
  -n io.github.zymmio.smartring/.watch.MainActivity >/dev/null
sleep 2

assert_state() {
  local expected="$1"
  "$ADB" -s "$PHONE" logcat -c
  "$ADB" -s "$WATCH" shell am broadcast \
    -n io.github.zymmio.smartring/.watch.DebugWristStateReceiver \
    -a io.github.zymmio.smartring.DEBUG_WRIST_STATE \
    --ez monitoring "$2" --ez on_wrist "$3" >/dev/null

  for _ in {1..20}; do
    sleep 1
    "$ADB" -s "$PHONE" shell am broadcast \
      -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
      -a io.github.zymmio.smartring.DEBUG_READ_STATE >/dev/null
    if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=$expected"; then
      echo "PASS: $expected"
      return
    fi
  done

  echo "FAIL: phone did not receive $expected" >&2
  exit 1
}

assert_state on_wrist true true
assert_state off_wrist true false
assert_state unknown false false

"$ADB" -s "$PHONE" logcat -c
"$ADB" -s "$PHONE" shell am broadcast \
  -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
  -a io.github.zymmio.smartring.DEBUG_READ_STATE \
  --ez connected false >/dev/null
if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=disconnected"; then
  echo "PASS: disconnected clears wrist state"
else
  echo "FAIL: disconnected did not clear wrist state" >&2
  exit 1
fi
