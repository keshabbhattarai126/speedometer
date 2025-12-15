package com.example.speedometerkeshab

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import kotlin.math.sqrt

// Data class to hold the current UI state
// We'll keep the name 'speedMps' for consistency, but remember it's a scaled magnitude (not true speed)
data class AccelerometerState(
    val speedMps: Float = 0f,
    val isRunning: Boolean = false
)

class AccelerometerViewModel(application: Application) :
    AndroidViewModel(application), SensorEventListener {

    // --- Sensor Setup ---
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isSensorRegistered = false

    // --- State Management ---
    private val _state = mutableStateOf(AccelerometerState())
    val state: State<AccelerometerState> = _state

    // --- 🚨 CRITICAL NEW VARIABLES FOR FILTERING 🚨 ---
    private var gravity = floatArrayOf(0f, 0f, 0f)
    private val ALPHA = 0.8f          // Low-pass filter constant for gravity smoothing
    private val SHAKE_THRESHOLD = 0.5f // m/s²: Movement below this is considered noise/stillness
    private val SCALING_FACTOR = 10.0f // Factor to make m/s² look like km/h (adjust as needed)

    /**
     * Start listening to the accelerometer and reset gravity history.
     */
    fun startMeasurement() {
        if (accelerometer == null) return

        // Reset the gravity history to [0, 0, 0] when starting
        gravity = floatArrayOf(0f, 0f, 0f)
        _state.value = AccelerometerState(isRunning = true, speedMps = 0f)

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        isSensorRegistered = true
    }

    /**
     * Stop listening to the accelerometer and reset the display.
     */
    fun stopMeasurement() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
        _state.value = _state.value.copy(isRunning = false, speedMps = 0f)
    }

    // --- 🔑 THE FIX: Dynamic Sensor Processing 🔑 ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // 1. Separate Gravity from Linear Acceleration (Low-Pass Filter)
        // This calculates the smooth, slow-changing vector (gravity)
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * ax
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * ay
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * az

        // Linear acceleration (acceleration due to movement, gravity-compensated)
        val linearAccelerationX = ax - gravity[0]
        val linearAccelerationY = ay - gravity[1]
        val linearAccelerationZ = az - gravity[2]

        // 2. Calculate the total magnitude of the movement (m/s²)
        // This is the instantaneous force of the motion.
        val movementMagnitude = sqrt(
            linearAccelerationX * linearAccelerationX +
                    linearAccelerationY * linearAccelerationY +
                    linearAccelerationZ * linearAccelerationZ
        )

        // 3. Apply Threshold and Scale for UI
        val finalDisplayValue: Float

        // If movement is below the threshold, assume it's noise/stillness (set to 0)
        if (movementMagnitude < SHAKE_THRESHOLD) {
            finalDisplayValue = 0.0f
        } else {
            // Scale the magnitude to look like a KM/H reading
            finalDisplayValue = movementMagnitude * SCALING_FACTOR
        }

        // 4. Update UI state
        _state.value = _state.value.copy(speedMps = finalDisplayValue)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Required method, but usually empty for accelerometers
    }

    override fun onCleared() {
        super.onCleared()
        stopMeasurement() // Clean up listener when ViewModel is destroyed
    }
}