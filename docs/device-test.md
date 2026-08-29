# First device test

Devices:

- CMF Phone 1, Android 16
- Samsung Galaxy Watch4

## Wrist-state test

Run the automated phone-watch communication test first:

```sh
./scripts/device-e2e.sh
```

The physical sensor still needs these short manual checks:

1. Install both modules with the same build variant.
2. Open the watch app.
3. Put on and unlock the watch. Confirm both apps show `On wrist`.
4. Remove the watch. Confirm both apps show `Off wrist`.
5. Repeat ten times and record late or wrong updates.
6. Disconnect Bluetooth and record what each app shows.
7. Close the watch app. Confirm its active notification remains visible.
8. Put on and remove the watch again. Confirm phone updates continue.
9. Press `Stop` in the watch app. Confirm the phone shows `Unknown` after reopening it.
10. Disable Bluetooth and Wi-Fi on the watch. Confirm the phone shows `Watch disconnected`.

## Call test gate

Do not add automatic call silencing until the wrist-state test is reliable.
After call screening is added, test these cases:

| Watch state | Expected phone result | Expected watch result |
| --- | --- | --- |
| On wrist | No ringtone; normal call screen | User's existing alert setting |
| Off wrist | Existing phone ring mode | User's existing alert setting |
| Disconnected | Existing phone ring mode | No assumption |
| Unknown | Existing phone ring mode | No assumption |

Also test one saved contact and one unknown number. The test fails if the app
changes the phone's saved ring volume or any watch alert setting.
