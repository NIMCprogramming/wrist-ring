package io.github.zymmio.smartring.phone

import android.content.Context

object WatchState {
    const val PREFS = "wrist_state"
    const val CONNECTED = "connected"
    const val ON_WRIST = "on_wrist"
    const val UPDATED_AT = "updated_at"
    const val RECEIVED_AT = "received_at"

    fun setConnected(context: Context, connected: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean(CONNECTED, connected)
            if (!connected) {
                remove(ON_WRIST)
                remove(UPDATED_AT)
                remove(RECEIVED_AT)
            }
        }.apply()
    }

    fun shouldSilenceCall(context: Context): Boolean {
        val state = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return state.getBoolean(CONNECTED, false) &&
            state.getBoolean(ON_WRIST, false) &&
            isFresh(context)
    }

    fun isFresh(context: Context): Boolean {
        val receivedAt = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(RECEIVED_AT, 0)
        return isFresh(receivedAt)
    }

    fun isFresh(timestamp: Long) = System.currentTimeMillis() - timestamp in 0..MAX_STATE_AGE

    private const val MAX_STATE_AGE = 15 * 60 * 1000L
}
