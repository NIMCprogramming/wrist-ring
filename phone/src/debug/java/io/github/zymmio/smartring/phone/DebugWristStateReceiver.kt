package io.github.zymmio.smartring.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

class DebugWristStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(CONNECTED)) {
            WatchState.setConnected(context, intent.getBooleanExtra(CONNECTED, false))
        }
        val state = context.getSharedPreferences(WristStateListenerService.PREFS, Context.MODE_PRIVATE)
        if (intent.hasExtra(ON_WRIST)) {
            state.edit()
                .putBoolean(WatchState.ON_WRIST, intent.getBooleanExtra(ON_WRIST, false))
                .putLong(WatchState.RECEIVED_AT, System.currentTimeMillis())
                .apply()
        }
        if (intent.getBooleanExtra(STALE, false)) {
            state.edit().putLong(WatchState.RECEIVED_AT, 0).apply()
        }
        when (intent.getStringExtra(RINGTONE)) {
            "screen" -> PhoneRingtone.updateForWatchState(context)
            "restore" -> PhoneRingtone.restore(context)
        }
        val value = when {
            state.contains(WatchState.CONNECTED) && !state.getBoolean(WatchState.CONNECTED, false) -> "disconnected"
            !state.contains(WristStateListenerService.ON_WRIST) -> "unknown"
            state.getBoolean(WristStateListenerService.ON_WRIST, false) -> "on_wrist"
            else -> "off_wrist"
        }
        val ringMuted = context.getSystemService(AudioManager::class.java).isStreamMute(AudioManager.STREAM_RING)
        Log.i(TAG, "state=$value silence=${WatchState.shouldSilenceCall(context)} ringMuted=$ringMuted")
    }

    companion object {
        private const val TAG = "SmartRingtoneE2E"
        private const val CONNECTED = "connected"
        private const val STALE = "stale"
        private const val RINGTONE = "ringtone"
        private const val ON_WRIST = "on_wrist"
    }
}
