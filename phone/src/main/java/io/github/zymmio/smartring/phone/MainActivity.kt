package io.github.zymmio.smartring.phone

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.wearable.Wearable

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener {
                WatchState.setConnected(this, it.isNotEmpty())
                showState()
            }
            .addOnFailureListener {
                WatchState.setConnected(this, false)
                showState()
            }
        showState()
    }

    private fun showState() {
        val state = getSharedPreferences(WristStateListenerService.PREFS, MODE_PRIVATE)
        val known = state.contains(WristStateListenerService.ON_WRIST)
        findViewById<TextView>(R.id.status).text = when {
            state.contains(WatchState.CONNECTED) && !state.getBoolean(WatchState.CONNECTED, false) ->
                getString(R.string.status_disconnected)
            !known -> getString(R.string.status_unknown)
            state.getBoolean(WristStateListenerService.ON_WRIST, false) -> getString(R.string.status_on_wrist)
            else -> getString(R.string.status_off_wrist)
        }
    }
}
