package com.example.speedometerkeshab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.speedometerkeshab.data.AccelerometerRepository
import com.example.speedometerkeshab.model.SpeedometerReading // <--- IMPORT NEW MODEL
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.sqrt

/**
 * Concrete implementation of the AccelerometerRepository interface.
 * Handles all interaction with the Android SensorManager and sends data to Firebase Realtime Database.
 */
class AccelerometerRepositoryImpl(context: Context) : AccelerometerRepository, SensorEventListener {

    // --- Firebase Setup ---
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    // Using a structured path: e.g., "readings/session_timestamp"
    private val readingsRef: DatabaseReference = database.getReference("speedometer_readings")
    private val currentSessionRef: DatabaseReference // Reference for the current session data

    init {
        // Initialize the session reference uniquely upon creation
        // (e.g., when the app starts or ViewModel is created)
        currentSessionRef = readingsRef.child("session_${System.currentTimeMillis()}")
    }

    // --- Sensor Setup ---
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isSensorRegistered = false

    // --- Data Processing Constants ---
    private val ALPHA = 0.8f          // Low-pass filter constant for gravity smoothing
    private val SHAKE_THRESHOLD = 0.5f // m/s²: Movement below this is considered noise
    private val SCALING_FACTOR = 10.0f // Factor to make m/s² look like km/h

    // --- Firebase Throttling ---
    // Only send data to Firebase every 500ms
    private val UPLOAD_INTERVAL_MS = 500L
    private var lastUploadTime = 0L

    // --- State Management ---
    private val _currentSpeedMps = MutableLiveData(0f)
    override val currentSpeedMps: LiveData<Float> = _currentSpeedMps

    // --- Internal State for Filtering ---
    private var gravity = floatArrayOf(0f, 0f, 0f)

    // --- Interface Method Implementation ---

    override fun startListening() {
        if (accelerometer == null) return

        // Reset state
        gravity = floatArrayOf(0f, 0f, 0f)
        lastUploadTime = System.currentTimeMillis()

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
        _currentSpeedMps.postValue(0f)
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

        // 4. Update LiveData for UI
        _currentSpeedMps.postValue(finalDisplayValue)

        // 5. Send data to Firebase (Throttled)
        sendDataToFirebase(finalDisplayValue)
    }

    private fun sendDataToFirebase(scaledValue: Float) {
        val currentTime = System.currentTimeMillis()

        // Throttling check: only proceed if the interval has passed
        if (currentTime - lastUploadTime >= UPLOAD_INTERVAL_MS) {

            // 1. Create the data object using the model class
            val reading = SpeedometerReading(
                timestamp = currentTime,
                scaledSpeed = scaledValue
            )

            // 2. Use push() to create a unique, sequential key under the current session.
            currentSessionRef.push().setValue(reading)
                .addOnSuccessListener {
                    // Data successfully saved
                }
                .addOnFailureListener { e ->
                    // Handle write failure (e.g., log error)
                }

            // 3. Update the last upload time
            lastUploadTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}