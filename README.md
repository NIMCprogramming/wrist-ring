# Wrist Ring

Wrist Ring silences incoming calls on the phone while a paired Wear OS watch is
on the user's wrist. The watch keeps its existing sound and vibration settings.

The first tested devices are CMF Phone 1 and Samsung Galaxy Watch4.

## Project modules

- `phone`: receives wrist state and controls only the phone ringtone.
- `watch`: reads the low-latency off-body sensor and sends state changes.
- `docs`: records product rules, architecture, and the first device test.

There is no shared module yet. The Wear OS Data Layer message is the small
boundary between the two apps.

## Open in Android Studio

1. Open this directory as a project.
2. Use Java 17 for Gradle.
3. Let Android Studio sync Gradle dependencies.
4. Run `phone` on the CMF Phone 1.
5. Run `watch` on the paired Galaxy Watch4.

The phone keeps its normal call screen. On-wrist calls stay quiet on the phone
while the watch still alerts. Off-wrist, unknown, stale, and disconnected states
use the phone's normal ringtone behavior.

## Command-line check

```sh
./gradlew lint test
```

## Release signing

Release builds use `release.keystore` and `keystore.properties` in the project
root. Both files are local and ignored by Git. Back them up securely; updates
cannot be signed without the same key.

Debug builds work without these local files. A release built without them is
unsigned and cannot be installed directly.

```sh
./gradlew :phone:assembleRelease :watch:assembleRelease
```

With the CMF Phone 1 and Galaxy Watch4 connected through ADB, run the automated
Data Layer test:

```sh
./scripts/device-e2e.sh
```

This test installs debug builds and checks on-wrist, off-wrist, and stopped
monitoring messages. It does not test the physical off-body sensor.
