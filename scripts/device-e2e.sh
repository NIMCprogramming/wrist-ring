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

echo "Building debug apps..."
./gradlew :phone:assembleDebug :watch:assembleDebug --quiet
echo "Installing phone app..."
"$ADB" -s "$PHONE" install -r phone/build/outputs/apk/debug/phone-debug.apk >/dev/null
echo "Installing watch app..."
"$ADB" -s "$WATCH" install -r watch/build/outputs/apk/debug/watch-debug.apk >/dev/null
echo "Starting apps..."
"$ADB" -s "$PHONE" shell am start \
  -n io.github.zymmio.smartring/.phone.MainActivity >/dev/null
"$ADB" -s "$WATCH" shell am start \
  -n io.github.zymmio.smartring/.watch.MainActivity >/dev/null
sleep 2

assert_state() {
  local expected="$1"
  local expected_silence="$4"
  echo "Testing $expected..."
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
    if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=$expected silence=$expected_silence"; then
      echo "PASS: $expected, silence=$expected_silence"
      return
    fi
  done

  echo "FAIL: phone did not receive $expected" >&2
  exit 1
}

assert_state on_wrist true true true

echo "Testing ringtone mute and restore..."
"$ADB" -s "$PHONE" shell cmd notification allow_dnd io.github.zymmio.smartring
"$ADB" -s "$PHONE" shell am broadcast \
  -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
  -a io.github.zymmio.smartring.DEBUG_READ_STATE \
  --ez connected true --ez on_wrist true \
  --es ringtone screen >/dev/null
if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=on_wrist silence=true ringMuted=true"; then
  echo "PASS: on-wrist call muted the ringtone"
else
  echo "FAIL: ringtone was not muted" >&2
  exit 1
fi
"$ADB" -s "$PHONE" shell am broadcast \
  -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
  -a io.github.zymmio.smartring.DEBUG_READ_STATE \
  --es ringtone restore >/dev/null
if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "ringMuted=false"; then
  echo "PASS: call end restored the ringtone"
else
  echo "FAIL: ringtone was not restored" >&2
  exit 1
fi

echo "Testing cold-start state recovery..."
"$ADB" -s "$PHONE" logcat -c
"$ADB" -s "$PHONE" shell pm clear io.github.zymmio.smartring >/dev/null
"$ADB" -s "$PHONE" shell am start \
  -n io.github.zymmio.smartring/.phone.MainActivity >/dev/null
for _ in {1..20}; do
  sleep 1
  "$ADB" -s "$PHONE" shell am broadcast \
    -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
    -a io.github.zymmio.smartring.DEBUG_READ_STATE >/dev/null
  if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=on_wrist silence=true"; then
    echo "PASS: cold start restored on_wrist"
    break
  fi
done
if ! "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=on_wrist silence=true"; then
  echo "FAIL: cold start did not restore on_wrist" >&2
  exit 1
fi

"$ADB" -s "$PHONE" logcat -c
"$ADB" -s "$PHONE" shell am broadcast \
  -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
  -a io.github.zymmio.smartring.DEBUG_READ_STATE \
  --ez stale true >/dev/null
if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=on_wrist silence=false"; then
  echo "PASS: stale on_wrist, silence=false"
else
  echo "FAIL: stale wrist state was trusted" >&2
  exit 1
fi

assert_state off_wrist true false false
assert_state unknown false false false

"$ADB" -s "$PHONE" logcat -c
"$ADB" -s "$PHONE" shell am broadcast \
  -n io.github.zymmio.smartring/.phone.DebugWristStateReceiver \
  -a io.github.zymmio.smartring.DEBUG_READ_STATE \
  --ez connected false >/dev/null
if "$ADB" -s "$PHONE" logcat -d -s SmartRingtoneE2E:I '*:S' | grep -q "state=disconnected silence=false"; then
  echo "PASS: disconnected clears wrist state"
else
  echo "FAIL: disconnected did not clear wrist state" >&2
  exit 1
fi
