package com.safegrap.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.safegrap.app.alerts.AlertController
import com.safegrap.app.alerts.AlertState
import com.safegrap.app.camera.CameraFrameAnalyzer
import com.safegrap.app.camera.FrameQualityAnalyzer
import com.safegrap.app.databinding.ActivityMainBinding
import com.safegrap.app.databinding.DialogCalibrationBinding
import com.safegrap.app.detection.VehicleDetection
import com.safegrap.app.detection.VehicleDetector
import com.safegrap.app.distance.CollisionRiskEvaluator
import com.safegrap.app.distance.DistanceEstimator
import com.safegrap.app.distance.LeadVehicleMovementDetector
import com.safegrap.app.settings.AppSettings
import com.safegrap.app.settings.SettingsActivity
import com.safegrap.app.speed.SpeedMonitor
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: AppSettings
    private lateinit var alerts: AlertController
    private lateinit var distanceEstimator: DistanceEstimator
    private lateinit var speedMonitor: SpeedMonitor
    private val collision = CollisionRiskEvaluator()
    private val movement = LeadVehicleMovementDetector()
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: VehicleDetector? = null
    private var lastDetection: VehicleDetection? = null
    private var currentSpeed: Float? = null
    private var distanceState = AlertState.NO_VEHICLE
    private var paused = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else showPermissionMessage()
    }
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) speedMonitor.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        settings = AppSettings(this)
        alerts = AlertController(this, settings)
        distanceEstimator = DistanceEstimator(settings.calibrationFactor)
        speedMonitor = SpeedMonitor(this) { speed -> runOnUiThread { updateSpeed(speed) } }
        binding.settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        binding.soundButton.setOnClickListener {
            settings.soundEnabled = !settings.soundEnabled
            updateSoundButton()
        }
        binding.pauseButton.setOnClickListener { togglePause() }
        binding.calibrateButton.setOnClickListener { showCalibration() }
        updateSoundButton()
        if (hasCameraPermission()) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
        requestLocationIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (::settings.isInitialized) {
            distanceEstimator.reset(settings.calibrationFactor)
            updateSoundButton()
        }
    }

    private fun startCamera() {
        binding.cameraMessageText.setText(R.string.starting_camera)
        binding.cameraMessageText.visibility = View.VISIBLE
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                detector?.close()
                detector = VehicleDetector(this)
                val preview = Preview.Builder().build().also { it.surfaceProvider = binding.previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis.setAnalyzer(cameraExecutor, CameraFrameAnalyzer(detector!!, FrameQualityAnalyzer()) { result ->
                    val detection = result.detection
                    val distance = detection?.let { distanceEstimator.estimate(it) }
                    val state = when {
                        result.invalidCamera -> AlertState.CAMERA_INVALID
                        distance == null -> AlertState.NO_VEHICLE
                        collision.update(distance) -> AlertState.COLLISION
                        movement.update(distance) -> AlertState.VEHICLE_MOVED
                        distance < settings.dangerDistance -> AlertState.DANGER
                        distance < settings.warningDistance -> AlertState.WARNING
                        else -> AlertState.SAFE
                    }
                    if (distance == null) { collision.reset(); movement.reset() }
                    runOnUiThread {
                        lastDetection = detection
                        distanceState = state
                        binding.overlayView.update(detection, distance, state)
                        binding.cameraMessageText.visibility = if (result.invalidCamera) View.VISIBLE else View.GONE
                        if (result.invalidCamera) binding.cameraMessageText.setText(R.string.camera_invalid_detail)
                        renderState(effectiveState())
                    }
                })
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                binding.cameraMessageText.visibility = View.GONE
            } catch (error: Exception) {
                binding.cameraMessageText.text = error.localizedMessage ?: getString(R.string.camera_invalid_detail)
                binding.cameraMessageText.visibility = View.VISIBLE
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateSpeed(speed: Float?) {
        currentSpeed = speed
        binding.speedText.text = speed?.let { getString(R.string.speed_format, it.roundToInt()) } ?: getString(R.string.speed_unavailable)
        val speeding = speed != null && settings.speedAlertEnabled && speed > settings.speedLimit + 3
        binding.speedText.setTextColor(if (speeding) Color.rgb(255, 190, 70) else Color.WHITE)
        renderState(effectiveState())
    }

    private fun effectiveState(): AlertState {
        if (distanceState in setOf(AlertState.COLLISION, AlertState.CAMERA_INVALID, AlertState.DANGER)) return distanceState
        return if (currentSpeed != null && settings.speedAlertEnabled && currentSpeed!! > settings.speedLimit + 3) AlertState.SPEEDING else distanceState
    }

    private fun renderState(state: AlertState) {
        val text = when (state) {
            AlertState.NO_VEHICLE -> R.string.no_vehicle to R.string.no_vehicle_detail
            AlertState.SAFE -> R.string.safe_distance to R.string.safe_detail
            AlertState.WARNING -> R.string.warning_distance to R.string.warning_detail
            AlertState.DANGER -> R.string.danger_distance to R.string.danger_detail
            AlertState.COLLISION -> R.string.collision_risk to R.string.collision_detail
            AlertState.VEHICLE_MOVED -> R.string.vehicle_moved to R.string.vehicle_moved_detail
            AlertState.CAMERA_INVALID -> R.string.camera_invalid to R.string.camera_invalid_detail
            AlertState.SPEEDING -> R.string.speeding to R.string.speeding_detail
        }
        binding.statusTitle.setText(text.first)
        if (state == AlertState.SPEEDING) binding.statusDetail.text = getString(text.second, settings.speedLimit) else binding.statusDetail.setText(text.second)
        val colour = when (state) {
            AlertState.COLLISION, AlertState.DANGER -> Color.rgb(232, 76, 79)
            AlertState.WARNING, AlertState.SPEEDING -> Color.rgb(217, 137, 22)
            else -> Color.rgb(17, 24, 32)
        }
        binding.statusTitle.setTextColor(colour)
        alerts.notify(state)
    }

    private fun togglePause() {
        paused = !paused
        binding.pauseButton.setText(if (paused) R.string.resume else R.string.pause)
        if (paused) { cameraProvider?.unbindAll(); binding.cameraMessageText.visibility = View.VISIBLE; binding.cameraMessageText.setText(R.string.pause) }
        else startCamera()
    }

    private fun showCalibration() {
        val selected = lastDetection ?: run { Toast.makeText(this, R.string.calibration_need_vehicle, Toast.LENGTH_SHORT).show(); return }
        val dialogBinding = DialogCalibrationBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this).setTitle(R.string.calibration_title).setMessage(R.string.calibration_message)
            .setView(dialogBinding.root).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val actual = dialogBinding.calibrationDistanceInput.text?.toString()?.toFloatOrNull()
                if (actual == null || actual !in 1f..100f) return@setOnClickListener
                settings.calibrationFactor = distanceEstimator.calibrate(selected, actual)
                settings.calibrated = true
                Toast.makeText(this, R.string.calibration_saved, Toast.LENGTH_SHORT).show(); dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) speedMonitor.start()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    private fun showPermissionMessage() {
        binding.cameraMessageText.setText(R.string.camera_permission_detail); binding.cameraMessageText.visibility = View.VISIBLE
        binding.cameraMessageText.setOnClickListener { cameraPermission.launch(Manifest.permission.CAMERA) }
        binding.statusTitle.setText(R.string.camera_permission_title)
    }
    private fun updateSoundButton() {
        binding.soundButton.contentDescription = getString(if (settings.soundEnabled) R.string.sound_on else R.string.sound_off)
        binding.soundButton.alpha = if (settings.soundEnabled) 1f else 0.45f
    }

    override fun onDestroy() {
        speedMonitor.stop(); detector?.close(); alerts.release(); cameraExecutor.shutdown(); super.onDestroy()
    }
}
