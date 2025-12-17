package com.example.speedometerkeshab

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.speedometerkeshab.data.AccelerometerRepository
import com.example.speedometerkeshab.model.AccelerometerModel
import com.google.firebase.database.*
import kotlin.math.sqrt

class AccelerometerRepositoryImpl(context: Context) : AccelerometerRepository, SensorEventListener {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val liveReadingRef: DatabaseReference =
        database.getReference("live_speedometer_data").child("current_reading")

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isSensorRegistered = false

    private val ALPHA = 0.8f
    private val SHAKE_THRESHOLD = 0.5f
    private val SCALING_FACTOR = 10.0f
    private val UPLOAD_INTERVAL_MS = 500L
    private var lastUploadTime = 0L

    private val _currentSpeedMps = MutableLiveData(0f)
    override val currentSpeedMps: LiveData<Float> = _currentSpeedMps

    // NEW: LiveData and Listener for Retrieval
    private val _firebaseData = MutableLiveData<AccelerometerModel>()
    override val firebaseData: LiveData<AccelerometerModel> = _firebaseData
    private var firebaseListener: ValueEventListener? = null

    private var gravity = floatArrayOf(0f, 0f, 0f)

    override fun startSyncingFromFirebase() {
        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.getValue(AccelerometerModel::class.java)
                data?.let { _firebaseData.postValue(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        liveReadingRef.addValueEventListener(firebaseListener!!)
    }

    override fun stopSyncingFromFirebase() {
        firebaseListener?.let { liveReadingRef.removeEventListener(it) }
    }

    override fun startListening() {
        if (accelerometer == null) return
        gravity = floatArrayOf(0f, 0f, 0f)
        lastUploadTime = System.currentTimeMillis()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        isSensorRegistered = true
    }

    override fun stopListening() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this)
            isSensorRegistered = false
        }
        _currentSpeedMps.postValue(0f)
        sendDataToFirebase(scaledValue = 0f, isFinal = true)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * ax
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * ay
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * az

        val movementMagnitude = sqrt(
            Math.pow((ax - gravity[0]).toDouble(), 2.0) +
                    Math.pow((ay - gravity[1]).toDouble(), 2.0) +
                    Math.pow((az - gravity[2]).toDouble(), 2.0)
        ).toFloat()

        val finalDisplayValue = if (movementMagnitude < SHAKE_THRESHOLD) 0f else movementMagnitude * SCALING_FACTOR
        _currentSpeedMps.postValue(finalDisplayValue)
        sendDataToFirebase(finalDisplayValue)
    }

    private fun sendDataToFirebase(scaledValue: Float, isFinal: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (isFinal || currentTime - lastUploadTime >= UPLOAD_INTERVAL_MS) {
            val reading = AccelerometerModel(timestamp = currentTime, speedMps = scaledValue)
            liveReadingRef.setValue(reading)
            if (!isFinal) lastUploadTime = currentTime
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}