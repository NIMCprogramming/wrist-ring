package io.github.zymmio.smartring.phone

import android.telecom.Call
import android.telecom.CallScreeningService

class SmartCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        if (WatchState.shouldSilenceCall(this)) PhoneRingtone.mute(this) else PhoneRingtone.restore(this)
        respondToCall(
            details,
            CallResponse.Builder()
                .setSilenceCall(false)
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build(),
        )
    }
}
