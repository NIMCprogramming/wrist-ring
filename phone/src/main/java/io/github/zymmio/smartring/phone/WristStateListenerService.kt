package io.github.zymmio.smartring.phone

import android.content.Context
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

class WristStateListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == PATH }
            .forEach {
                WatchState.setConnected(this, true)
                saveDataItem(this, it.dataItem)
            }
    }

    override fun onPeerConnected(peer: Node) {
        WatchState.setConnected(this, true)
    }

    override fun onPeerDisconnected(peer: Node) {
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { WatchState.setConnected(this, it.isNotEmpty()) }
            .addOnFailureListener { WatchState.setConnected(this, false) }
    }

    companion object {
        const val PATH = "/wrist-state"
        const val PREFS = WatchState.PREFS
        const val ON_WRIST = WatchState.ON_WRIST
        const val UPDATED_AT = WatchState.UPDATED_AT
        private const val MONITORING = "monitoring"

        fun saveDataItem(context: Context, item: DataItem) {
            val data = DataMapItem.fromDataItem(item).dataMap
            context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().apply {
                if (data.getBoolean(MONITORING) && WatchState.isFresh(data.getLong(UPDATED_AT))) {
                    putBoolean(ON_WRIST, data.getBoolean(ON_WRIST))
                    putLong(UPDATED_AT, data.getLong(UPDATED_AT))
                    putLong(WatchState.RECEIVED_AT, System.currentTimeMillis())
                } else {
                    remove(ON_WRIST)
                    remove(UPDATED_AT)
                    remove(WatchState.RECEIVED_AT)
                }
            }.apply()
        }
    }
}
