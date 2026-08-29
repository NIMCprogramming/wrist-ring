package io.github.zymmio.smartring.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DebugWristStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = context.getSharedPreferences(WristStateListenerService.PREFS, Context.MODE_PRIVATE)
        val value = when {
            !state.contains(WristStateListenerService.ON_WRIST) -> "unknown"
            state.getBoolean(WristStateListenerService.ON_WRIST, false) -> "on_wrist"
            else -> "off_wrist"
        }
        Log.i(TAG, "state=$value")
    }

    companion object {
        private const val TAG = "SmartRingtoneE2E"
    }
}
