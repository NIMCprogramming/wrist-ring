package io.github.zymmio.smartring.phone

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.wearable.Wearable

class MainActivity : Activity(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.call_screening).setOnClickListener { enableCallScreening() }
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences(WatchState.PREFS, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this)
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener {
                val connected = it.isNotEmpty()
                WatchState.setConnected(this, connected)
                if (connected) refreshWristState()
            }
            .addOnFailureListener {
                WatchState.setConnected(this, false)
            }
        showState()
        showRoleState()
    }

    override fun onPause() {
        getSharedPreferences(WatchState.PREFS, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        showState()
    }

    private fun showState() {
        val state = getSharedPreferences(WristStateListenerService.PREFS, MODE_PRIVATE)
        val known = state.contains(WristStateListenerService.ON_WRIST)
        findViewById<TextView>(R.id.status).text = when {
            state.contains(WatchState.CONNECTED) && !state.getBoolean(WatchState.CONNECTED, false) ->
                getString(R.string.status_disconnected)
            !known || !WatchState.isFresh(this) -> getString(R.string.status_unknown)
            state.getBoolean(WristStateListenerService.ON_WRIST, false) -> getString(R.string.status_on_wrist)
            else -> getString(R.string.status_off_wrist)
        }
    }

    private fun refreshWristState() {
        Wearable.getDataClient(this).dataItems.addOnSuccessListener { items ->
            items.use {
                it.filter { item -> item.uri.path == WristStateListenerService.PATH }
                    .forEach { item -> WristStateListenerService.saveDataItem(this, item) }
            }
        }
    }

    private fun enableCallScreening() {
        val permissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (permissions.isNotEmpty()) {
            requestPermissions(permissions.toTypedArray(), PERMISSIONS_REQUEST)
            return
        }
        if (!getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return
        }
        requestCallScreeningRole()
    }

    private fun requestCallScreeningRole() {
        val roles = getSystemService(RoleManager::class.java)
        if (roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            !roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        ) {
            startActivityForResult(
                roles.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                ROLE_REQUEST,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST &&
            checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        ) {
            enableCallScreening()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ROLE_REQUEST) showRoleState()
    }

    private fun showRoleState() {
        val roles = getSystemService(RoleManager::class.java)
        val roleHeld = roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        val permissionsGranted = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val policyAccessGranted = getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        val enabled = roleHeld && permissionsGranted && policyAccessGranted
        findViewById<TextView>(R.id.call_screening_status).setText(
            when {
                enabled -> R.string.call_screening_enabled
                !permissionsGranted -> R.string.call_screening_permission_needed
                !policyAccessGranted -> R.string.call_screening_policy_needed
                else -> R.string.call_screening_disabled
            },
        )
        findViewById<Button>(R.id.call_screening).visibility = if (enabled) View.GONE else View.VISIBLE
    }

    companion object {
        private const val PERMISSIONS_REQUEST = 1
        private const val ROLE_REQUEST = 2
    }
}
