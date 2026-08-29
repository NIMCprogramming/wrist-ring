# First device test

Devices:

- CMF Phone 1, Android 16
- Samsung Galaxy Watch4

## Wrist-state test

1. Install both modules with the same build variant.
2. Open the watch app.
3. Put on and unlock the watch. Confirm both apps show `On wrist`.
4. Remove the watch. Confirm both apps show `Off wrist`.
5. Repeat ten times and record late or wrong updates.
6. Disconnect Bluetooth and record what each app shows.
7. Close the watch app and check whether updates stop.

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
