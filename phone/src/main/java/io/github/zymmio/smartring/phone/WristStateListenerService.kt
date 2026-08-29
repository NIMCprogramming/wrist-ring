package io.github.zymmio.smartring.phone

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WristStateListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == PATH }
            .forEach {
                val data = DataMapItem.fromDataItem(it.dataItem).dataMap
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean(ON_WRIST, data.getBoolean(ON_WRIST))
                    .putLong(UPDATED_AT, data.getLong(UPDATED_AT))
                    .apply()
            }
    }

    companion object {
        const val PATH = "/wrist-state"
        const val PREFS = "wrist_state"
        const val ON_WRIST = "on_wrist"
        const val UPDATED_AT = "updated_at"
    }
}
