package io.github.zymmio.smartring.phone

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        val state = getSharedPreferences(WristStateListenerService.PREFS, MODE_PRIVATE)
        val known = state.contains(WristStateListenerService.ON_WRIST)
        findViewById<TextView>(R.id.status).text = when {
            !known -> getString(R.string.status_unknown)
            state.getBoolean(WristStateListenerService.ON_WRIST, false) -> getString(R.string.status_on_wrist)
            else -> getString(R.string.status_off_wrist)
        }
    }
}
