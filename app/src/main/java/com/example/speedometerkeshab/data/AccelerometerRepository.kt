package com.example.speedometerkeshab.data

import androidx.lifecycle.LiveData
import com.example.speedometerkeshab.model.AccelerometerModel

interface AccelerometerRepository {
    val currentSpeedMps: LiveData<Float>

    // NEW: LiveData to observe data coming FROM Firebase
    val firebaseData: LiveData<AccelerometerModel>

    fun startListening()
    fun stopListening()

    // NEW: Methods to handle Firebase data sync
    fun startSyncingFromFirebase()
    fun stopSyncingFromFirebase()
}