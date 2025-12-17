package com.example.speedometerkeshab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.speedometerkeshab.data.AccelerometerRepository
import com.example.speedometerkeshab.model.AccelerometerModel // <--- USING THE SINGLE MODEL
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.sqrt

/**
 * Concrete implementation of the AccelerometerRepository interface.
 * Now updates a SINGLE fixed node in Firebase with the latest reading,
 * using the combined AccelerometerModel.
 */
class AccelerometerRepositoryImpl(context: Context) : AccelerometerRepository, SensorEventListener {

    // --- Firebase Setup ---
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    // Use a FIXED, non-timestamped path for the single live reading.
    private val liveReadingRef: DatabaseReference =
        database.getReference("live_speedometer_data").child("current_reading") // FIXED REFERENCE

    // --- Sensor Setup ---
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isSensorRegistered = false

    // --- Data Processing Constants ---
    private val ALPHA = 0.8f
    private val SHAKE_THRESHOLD = 0.5f
    private val SCALING_FACTOR = 10.0f

    // --- Firebase Throttling ---
    private val UPLOAD_INTERVAL_MS = 500L
    private var lastUploadTime = 0L

    // --- State Management ---
    // NOTE: This LiveData still only emits the Float value, as defined by the interface.
    private val _currentSpeedMps = MutableLiveData(0f)
    override val currentSpeedMps: LiveData<Float> = _currentSpeedMps

    // --- Internal State for Filtering ---
    private var gravity = floatArrayOf(0f, 0f, 0f)

    // --- Interface Method Implementation ---

    override fun startListening() {
        if (accelerometer == null) return

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

        // Reset the Firebase value to 0 when measurement stops
        sendDataToFirebase(scaledValue = 0f, isFinal = true)
    }

    // --- SensorEventListener Implementation ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        // ... (Acceleration calculation logic remains the same) ...

        // Linear acceleration (movement) and magnitude calculation...
        // ... (omitted for brevity, assume finalDisplayValue is calculated) ...
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

        _currentSpeedMps.postValue(finalDisplayValue)

        // 4. Send data to Firebase (Throttled)
        sendDataToFirebase(finalDisplayValue)
    }

    private fun sendDataToFirebase(scaledValue: Float, isFinal: Boolean = false) {
        val currentTime = System.currentTimeMillis()

        // Throttling check OR if it's a final stop action
        if (isFinal || currentTime - lastUploadTime >= UPLOAD_INTERVAL_MS) {

            // 1. Create the data object using the combined model
            // NOTE: We set isRunning to FALSE because the database doesn't need to know the UI state.
            // We only populate the fields required for Firebase.
            val reading = AccelerometerModel(
                timestamp = currentTime,
                speedMps = scaledValue,
                isRunning = false // Always false or omitted for clean persistence
            )

            // 2. Use setValue() directly on the FIXED reference to overwrite the previous data.
            liveReadingRef.setValue(reading)
                .addOnSuccessListener {
                    // Data successfully saved
                }
                .addOnFailureListener { e ->
                    // Handle failure
                }

            // 3. Update the last upload time only if it wasn't a final call
            if (!isFinal) {
                lastUploadTime = currentTime
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}