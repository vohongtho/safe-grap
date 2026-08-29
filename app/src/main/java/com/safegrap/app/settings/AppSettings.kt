package com.safegrap.app.settings

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("safe_gap_settings", Context.MODE_PRIVATE)

    var warningDistance: Float
        get() = prefs.getFloat("warning_distance", 25f)
        set(value) = prefs.edit().putFloat("warning_distance", value).apply()
    var dangerDistance: Float
        get() = prefs.getFloat("danger_distance", 12f)
        set(value) = prefs.edit().putFloat("danger_distance", value).apply()
    var speedAlertEnabled: Boolean
        get() = prefs.getBoolean("speed_alert", true)
        set(value) = prefs.edit().putBoolean("speed_alert", value).apply()
    var speedLimit: Int
        get() = prefs.getInt("speed_limit", 80)
        set(value) = prefs.edit().putInt("speed_limit", value).apply()
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) = prefs.edit().putBoolean("vibration", value).apply()
    var calibrationFactor: Float
        get() = prefs.getFloat("calibration_factor", 1.3f)
        set(value) = prefs.edit().putFloat("calibration_factor", value).apply()
    var calibrated: Boolean
        get() = prefs.getBoolean("calibrated", false)
        set(value) = prefs.edit().putBoolean("calibrated", value).apply()

    fun resetCalibration() {
        calibrationFactor = 1.3f
        calibrated = false
    }
}
