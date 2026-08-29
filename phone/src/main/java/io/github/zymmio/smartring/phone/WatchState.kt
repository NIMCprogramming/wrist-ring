package io.github.zymmio.smartring.phone

import android.content.Context

object WatchState {
    const val PREFS = "wrist_state"
    const val CONNECTED = "connected"
    const val ON_WRIST = "on_wrist"
    const val UPDATED_AT = "updated_at"

    fun setConnected(context: Context, connected: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean(CONNECTED, connected)
            if (!connected) {
                remove(ON_WRIST)
                remove(UPDATED_AT)
            }
        }.apply()
    }
}
