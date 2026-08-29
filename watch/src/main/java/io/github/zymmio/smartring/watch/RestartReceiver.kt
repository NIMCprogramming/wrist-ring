package io.github.zymmio.smartring.watch

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val monitoring = context.getSharedPreferences(WristMonitorService.PREFS, Context.MODE_PRIVATE)
            .getBoolean(WristMonitorService.MONITORING, false)
        if (monitoring &&
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        ) {
            context.startForegroundService(
                Intent(context, WristMonitorService::class.java).setAction(WristMonitorService.ACTION_START),
            )
        }
    }
}
