package io.github.zymmio.smartring.watch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.monitoring).setOnClickListener {
            if (isMonitoring()) {
                startService(
                    Intent(this, WristMonitorService::class.java).setAction(WristMonitorService.ACTION_STOP),
                )
                window.decorView.postDelayed(::showState, 200)
            } else {
                startMonitoring()
            }
        }

        startMonitoring()
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences(WristMonitorService.PREFS, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this)
        showState()
    }

    override fun onPause() {
        getSharedPreferences(WristMonitorService.PREFS, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        showState()
    }

    private fun showState() {
        val preferences = getSharedPreferences(WristMonitorService.PREFS, MODE_PRIVATE)
        val monitoring = isMonitoring()
        findViewById<TextView>(R.id.status).setText(
            when {
                !monitoring || !preferences.contains(WristMonitorService.ON_WRIST) -> R.string.status_waiting
                preferences.getBoolean(WristMonitorService.ON_WRIST, false) -> R.string.status_on_wrist
                else -> R.string.status_off_wrist
            },
        )
        findViewById<Button>(R.id.monitoring).setText(
            if (monitoring) R.string.stop_monitoring else R.string.start_monitoring,
        )
    }

    private fun isMonitoring() = getSharedPreferences(WristMonitorService.PREFS, MODE_PRIVATE)
        .getBoolean(WristMonitorService.MONITORING, false)

    private fun startMonitoring() {
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(
                Intent(this, WristMonitorService::class.java).setAction(WristMonitorService.ACTION_START),
            )
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        ) {
            startForegroundService(
                Intent(this, WristMonitorService::class.java).setAction(WristMonitorService.ACTION_START),
            )
        }
    }

    companion object {
        private const val PERMISSION_REQUEST = 1
    }
}
