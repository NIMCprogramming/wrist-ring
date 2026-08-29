package io.github.zymmio.smartring.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class DebugWristStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val request = PutDataMapRequest.create(PATH).apply {
            dataMap.putBoolean(MONITORING, intent.getBooleanExtra(MONITORING, false))
            dataMap.putBoolean(ON_WRIST, intent.getBooleanExtra(ON_WRIST, false))
            dataMap.putLong(UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
    }

    companion object {
        private const val PATH = "/wrist-state"
        private const val MONITORING = "monitoring"
        private const val ON_WRIST = "on_wrist"
        private const val UPDATED_AT = "updated_at"
    }
}
