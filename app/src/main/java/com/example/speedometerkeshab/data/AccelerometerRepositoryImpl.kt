package com.example.speedometerkeshab

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.speedometerkeshab.data.AccelerometerRepository
import com.example.speedometerkeshab.model.AccelerometerModel
import com.google.android.gms.location.*
import com.google.firebase.database.*

class AccelerometerRepositoryImpl(context: Context) : AccelerometerRepository {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val liveReadingRef = database.getReference("live_speedometer_data").child("current_reading")
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _currentSpeedMps = MutableLiveData(0f)
    override val currentSpeedMps: LiveData<Float> = _currentSpeedMps

    private val _firebaseData = MutableLiveData<AccelerometerModel>()
    override val firebaseData: LiveData<AccelerometerModel> = _firebaseData

    private var isRunningInternal = false
    private val UPLOAD_INTERVAL_MS = 1000L
    private var lastUploadTime = 0L

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
        .setMinUpdateIntervalMillis(500L)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val speedKmh = location.speed * 3.6f // Actual GPS speed conversion
            _currentSpeedMps.postValue(speedKmh)
            sendDataToFirebase(speedKmh)
        }
    }

    @SuppressLint("MissingPermission")
    override fun startListening() {
        isRunningInternal = true
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun stopListening() {
        isRunningInternal = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _currentSpeedMps.postValue(0f)
        sendDataToFirebase(0f, isFinal = true)
    }

    private fun sendDataToFirebase(speed: Float, isFinal: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (isFinal || currentTime - lastUploadTime >= UPLOAD_INTERVAL_MS) {
            val reading = AccelerometerModel(
                timestamp = currentTime,
                speedMps = speed,
                isRunning = isRunningInternal // Fixes the 'running:false' issue
            )
            liveReadingRef.setValue(reading)
            lastUploadTime = currentTime
        }
    }

    override fun startSyncingFromFirebase() {
        liveReadingRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(AccelerometerModel::class.java)?.let { _firebaseData.postValue(it) }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun stopSyncingFromFirebase() { /* Remove listener logic here */ }
}