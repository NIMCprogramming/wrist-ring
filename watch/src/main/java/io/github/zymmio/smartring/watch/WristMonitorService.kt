package io.github.zymmio.smartring.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class WristMonitorService : Service(), SensorEventListener {
    private val handler = Handler(Looper.getMainLooper())
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val offBodySensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        startForeground(
            NOTIFICATION_ID,
            android.app.Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_watch)
                .setContentTitle(getString(R.string.monitoring_title))
                .setContentText(getString(R.string.monitoring_text))
                .setContentIntent(openApp)
                .setOngoing(true)
                .build(),
        )

        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(MONITORING, true).apply()
        sensorManager.unregisterListener(this)
        sensorManager.registerListener(this, offBodySensor, SensorManager.SENSOR_DELAY_NORMAL)
        handler.removeCallbacks(heartbeat)
        handler.postDelayed(heartbeat, HEARTBEAT_INTERVAL)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val onWrist = event.values[0] == 1f
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ON_WRIST, onWrist).apply()
        sendState(monitoring = true, onWrist = onWrist)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopMonitoring() {
        handler.removeCallbacks(heartbeat)
        sensorManager.unregisterListener(this)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply()
        sendState(monitoring = false, onWrist = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendState(monitoring: Boolean, onWrist: Boolean) {
        val request = PutDataMapRequest.create(PATH).apply {
            dataMap.putBoolean(MONITORING, monitoring)
            dataMap.putBoolean(ON_WRIST, onWrist)
            dataMap.putLong(UPDATED_AT, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(this).putDataItem(request)
    }

    private val heartbeat = object : Runnable {
        override fun run() {
            val state = getSharedPreferences(PREFS, MODE_PRIVATE)
            if (state.contains(ON_WRIST)) {
                sendState(monitoring = true, onWrist = state.getBoolean(ON_WRIST, false))
            }
            handler.postDelayed(this, HEARTBEAT_INTERVAL)
        }
    }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val PREFS = "wrist_state"
        const val MONITORING = "monitoring"
        const val ON_WRIST = "on_wrist"

        private const val PATH = "/wrist-state"
        private const val UPDATED_AT = "updated_at"
        private const val CHANNEL = "wrist_monitoring"
        private const val NOTIFICATION_ID = 1
        private const val HEARTBEAT_INTERVAL = 5 * 60 * 1000L
    }
}
