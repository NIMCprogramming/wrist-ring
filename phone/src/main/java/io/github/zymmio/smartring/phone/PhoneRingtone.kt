package io.github.zymmio.smartring.phone

import android.content.Context
import android.media.AudioManager
import android.app.NotificationManager

object PhoneRingtone {
    private const val MUTED_BY_APP = "ringtone_muted_by_app"

    fun mute(context: Context) {
        if (!context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted) return
        val audio = context.getSystemService(AudioManager::class.java)
        if (!audio.isStreamMute(AudioManager.STREAM_RING)) {
            audio.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            context.getSharedPreferences(WatchState.PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(MUTED_BY_APP, true).apply()
        }
    }

    fun restore(context: Context) {
        val state = context.getSharedPreferences(WatchState.PREFS, Context.MODE_PRIVATE)
        if (state.getBoolean(MUTED_BY_APP, false)) {
            context.getSystemService(AudioManager::class.java)
                .adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            state.edit().remove(MUTED_BY_APP).apply()
        }
    }
}
