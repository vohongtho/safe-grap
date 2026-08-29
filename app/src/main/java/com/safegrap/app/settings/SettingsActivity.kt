package com.safegrap.app.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.safegrap.app.R
import com.safegrap.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings(this)
        binding.warningDistanceInput.setText(settings.warningDistance.toString())
        binding.dangerDistanceInput.setText(settings.dangerDistance.toString())
        binding.speedLimitInput.setText(settings.speedLimit.toString())
        binding.speedAlertSwitch.isChecked = settings.speedAlertEnabled
        binding.soundSwitch.isChecked = settings.soundEnabled
        binding.vibrationSwitch.isChecked = settings.vibrationEnabled
        binding.resetCalibrationButton.setOnClickListener {
            settings.resetCalibration()
            Toast.makeText(this, R.string.calibration_saved, Toast.LENGTH_SHORT).show()
        }
        binding.saveButton.setOnClickListener { save() }
    }

    private fun save() {
        val warning = binding.warningDistanceInput.text?.toString()?.toFloatOrNull()
        val danger = binding.dangerDistanceInput.text?.toString()?.toFloatOrNull()
        val limit = binding.speedLimitInput.text?.toString()?.toIntOrNull()
        if (warning == null || danger == null || danger < 2f || danger >= warning || warning > 100f || limit == null || limit !in 10..200) {
            Toast.makeText(this, R.string.invalid_settings, Toast.LENGTH_LONG).show(); return
        }
        settings.warningDistance = warning
        settings.dangerDistance = danger
        settings.speedLimit = limit
        settings.speedAlertEnabled = binding.speedAlertSwitch.isChecked
        settings.soundEnabled = binding.soundSwitch.isChecked
        settings.vibrationEnabled = binding.vibrationSwitch.isChecked
        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
