package io.github.zymmio.smartring.watch

import android.app.AlarmManager
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
    private var hasFreshReading = false
    private var sensorRegistered = false
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
        if (!sensorRegistered) {
            startSensor()
        } else if (hasFreshReading) {
            sendState(monitoring = true, onWrist = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(ON_WRIST, false))
        }
        scheduleHeartbeat()
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val onWrist = event.values[0] == 1f
        hasFreshReading = true
        handler.removeCallbacks(retrySensor)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(ON_WRIST, onWrist).apply()
        sendState(monitoring = true, onWrist = onWrist)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        cancelHeartbeat()
        handler.removeCallbacks(retrySensor)
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopMonitoring() {
        cancelHeartbeat()
        handler.removeCallbacks(retrySensor)
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

    private fun scheduleHeartbeat() {
        getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + HEARTBEAT_INTERVAL,
            heartbeatIntent(),
        )
    }

    private fun cancelHeartbeat() {
        getSystemService(AlarmManager::class.java).cancel(heartbeatIntent())
    }

    private fun heartbeatIntent() = PendingIntent.getService(
        this,
        1,
        Intent(this, WristMonitorService::class.java).setAction(ACTION_HEARTBEAT),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun startSensor() {
        hasFreshReading = false
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(ON_WRIST).apply()
        sendState(monitoring = false, onWrist = false)
        sensorManager.unregisterListener(this)
        sensorRegistered = sensorManager.registerListener(
            this,
            offBodySensor,
            SensorManager.SENSOR_DELAY_NORMAL,
        )
        handler.removeCallbacks(retrySensor)
        handler.postDelayed(retrySensor, SENSOR_RETRY_INTERVAL)
    }

    private val retrySensor = object : Runnable {
        override fun run() {
            if (hasFreshReading) return
            sensorManager.unregisterListener(this@WristMonitorService)
            sensorRegistered = sensorManager.registerListener(
                this@WristMonitorService,
                offBodySensor,
                SensorManager.SENSOR_DELAY_NORMAL,
            )
            handler.postDelayed(this, SENSOR_RETRY_INTERVAL)
        }
    }

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        private const val ACTION_HEARTBEAT = "heartbeat"
        const val PREFS = "wrist_state"
        const val MONITORING = "monitoring"
        const val ON_WRIST = "on_wrist"

        private const val PATH = "/wrist-state"
        private const val UPDATED_AT = "updated_at"
        private const val CHANNEL = "wrist_monitoring"
        private const val NOTIFICATION_ID = 1
        private const val HEARTBEAT_INTERVAL = 5 * 60 * 1000L
        private const val SENSOR_RETRY_INTERVAL = 10 * 1000L
    }
}
