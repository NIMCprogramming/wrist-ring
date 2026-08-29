package io.github.zymmio.smartring.watch

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.TextView
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : Activity(), SensorEventListener {
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val offBodySensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, offBodySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        super.onPause()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val onWrist = event.values[0] == 1f
        findViewById<TextView>(R.id.status).setText(
            if (onWrist) R.string.status_on_wrist else R.string.status_off_wrist,
        )

        val request = PutDataMapRequest.create(PATH).apply {
            dataMap.putBoolean(ON_WRIST, onWrist)
            dataMap.putLong(UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(request)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val PATH = "/wrist-state"
        private const val ON_WRIST = "on_wrist"
        private const val UPDATED_AT = "updated_at"
    }
}
