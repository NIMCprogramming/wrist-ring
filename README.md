# Smart Ringtone

Smart Ringtone is an Android experiment for a CMF Phone 1 and Galaxy Watch4.
It will silence an incoming call on the phone when the watch is on the user's
wrist. It must not change the watch's sound or vibration settings.

## Project modules

- `phone`: receives wrist state and will make the per-call sound decision.
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

The current skeleton only proves wrist-state delivery. Call silencing is held
back until that delivery and the real call-alert behavior are tested.

## Command-line check

```sh
./gradlew lint test
```

With the CMF Phone 1 and Galaxy Watch4 connected through ADB, run the automated
Data Layer test:

```sh
./scripts/device-e2e.sh
```

This test installs debug builds and checks on-wrist, off-wrist, and stopped
monitoring messages. It does not test the physical off-body sensor.
