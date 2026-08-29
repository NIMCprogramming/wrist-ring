# Architecture

## Data flow

```text
Galaxy Watch4 off-body sensor
        |
        | state changes only
        v
Wear OS Data Layer: /wrist-state
        |
        v
CMF Phone local wrist state
        |
        | incoming call
        v
Android CallScreeningService
        |
        +-- on wrist and connected -> silence this phone call
        +-- off wrist or unknown   -> keep normal phone behavior
```

`SmartCallScreeningService` keeps the default call screen and call log. Android's
`silenceCall` flag also suppresses the Wear OS alert, so the service leaves that
flag off. When `WatchState` is connected and on wrist, it temporarily mutes only
the phone ring audio stream. `CallStateReceiver` restores the previous unmuted
state when the call ends. If the phone was already muted, the app does not unmute it.

## Message contract

Path: `/wrist-state`

| Field | Type | Meaning |
| --- | --- | --- |
| `on_wrist` | Boolean | `true` when the off-body sensor reports on body |
| `monitoring` | Boolean | `true` while the watch foreground service is active |
| `updated_at` | Long | Watch wall-clock time in Unix milliseconds |

The phone tracks reachable Wear OS nodes separately from wrist state. A node
disconnect clears the saved wrist state. Unknown, disconnected, or invalid
state must fail safely by allowing the normal ringtone.

## Battery rule

The watch foreground service sends a Data Layer item only when sensor state
changes. It does not poll every 10 seconds. The phone will track connection separately. A slow
health message may be added only if device tests show that connection events
are not reliable enough.

## Implementation order

1. Prove on-wrist and off-wrist messages on the two real devices.
2. Prove reliable connected and disconnected state without polling.
3. Add the call-screening role and silence one call only when state is safe.
4. Test saved contacts. Add Contacts permission only if Android requires it.
5. Measure battery use before adding background work.
