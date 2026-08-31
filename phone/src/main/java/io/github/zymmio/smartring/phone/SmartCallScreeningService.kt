package io.github.zymmio.smartring.phone

import android.telecom.Call
import android.telecom.CallScreeningService

class SmartCallScreeningService : CallScreeningService() {
    override fun onScreenCall(details: Call.Details) {
        PhoneRingtone.updateForWatchState(this)
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
