package io.github.zymmio.smartring.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PhoneRingtone.restore(context)
        WatchState.setConnected(context, false)
    }
}
