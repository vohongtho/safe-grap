package com.safegrap.app.alerts

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.safegrap.app.settings.AppSettings

class AlertController(context: Context, private val settings: AppSettings) {
    private val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
    private var lastAlertAt = 0L
    private var lastState: AlertState? = null

    fun notify(state: AlertState) {
        if (state in setOf(AlertState.SAFE, AlertState.NO_VEHICLE)) { lastState = state; return }
        val now = System.currentTimeMillis()
        val interval = when (state) {
            AlertState.COLLISION -> 900L
            AlertState.DANGER -> 1800L
            AlertState.WARNING -> 4500L
            AlertState.SPEEDING -> 6000L
            AlertState.CAMERA_INVALID -> 8000L
            AlertState.VEHICLE_MOVED -> Long.MAX_VALUE
            else -> 5000L
        }
        if (state == lastState && now - lastAlertAt < interval) return
        lastState = state; lastAlertAt = now
        if (settings.soundEnabled) {
            val toneId = if (state == AlertState.COLLISION) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP2
            tone.startTone(toneId, if (state == AlertState.COLLISION) 500 else 220)
        }
        if (settings.vibrationEnabled && vibrator.hasVibrator()) {
            val pattern = if (state == AlertState.COLLISION) longArrayOf(0, 180, 90, 180) else longArrayOf(0, 130)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    fun release() = tone.release()
}
