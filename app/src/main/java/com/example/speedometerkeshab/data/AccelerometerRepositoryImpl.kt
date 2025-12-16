package com.example.speedometerkeshab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.speedometerkeshab.data.AccelerometerRepository
import kotlin.math.sqrt

/**
 * Concrete implementation of the AccelerometerRepository interface.
 * Handles all interaction with the Android SensorManager to calculate
 * scaled movement magnitude.
 */
class AccelerometerRepositoryImpl(context: Context) : AccelerometerRepository, SensorEventListener {

    // --- Sensor Setup ---
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isSensorRegistered = false

    // --- Data Processing Constants ---
    private val ALPHA = 0.8f          // Low-pass filter constant for gravity smoothing
    private val SHAKE_THRESHOLD = 0.5f // m/s²: Movement below this is considered noise
    private val SCALING_FACTOR = 10.0f // Factor to make m/s² look like km/h

    // --- State Management ---
    private val _currentSpeedMps = MutableLiveData(0f)

    // Implementation of the interface property
    override val currentSpeedMps: LiveData<Float> = _currentSpeedMps

    // --- Internal State for Filtering ---
    private var gravity = floatArrayOf(0f, 0f, 0f)

    // --- Interface Method Implementation ---

    override fun startListening() {
        if (accelerometer == null) return

        // Reset the gravity history to [0, 0, 0] when starting
        gravity = floatArrayOf(0f, 0f, 0f)

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )
        isSensorRegistered = true
    }

    override fun stopListening() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
        _currentSpeedMps.postValue(0f) // Reset the displayed value
    }

    // --- SensorEventListener Implementation ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // 1. Separate Gravity from Linear Acceleration (Low-Pass Filter)
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * ax
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * ay
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * az

        // Linear acceleration (movement)
        val linearAccelerationX = ax - gravity[0]
        val linearAccelerationY = ay - gravity[1]
        val linearAccelerationZ = az - gravity[2]

        // 2. Calculate the total magnitude of the movement (m/s²)
        val movementMagnitude = sqrt(
            linearAccelerationX * linearAccelerationX +
                    linearAccelerationY * linearAccelerationY +
                    linearAccelerationZ * linearAccelerationZ
        )

        // 3. Apply Threshold and Scale for UI
        val finalDisplayValue: Float = when {
            movementMagnitude < SHAKE_THRESHOLD -> 0.0f
            else -> movementMagnitude * SCALING_FACTOR
        }

        // 4. Update LiveData
        _currentSpeedMps.postValue(finalDisplayValue)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}