package com.example.speedometerkeshab.data

import androidx.lifecycle.LiveData

interface AccelerometerRepository {
    /**
     * LiveData emitting the current scaled acceleration value (Float).
     */
    val currentSpeedMps: LiveData<Float>

    /**
     * Initiates the process of listening to the sensor data.
     */
    fun startListening()

    /**
     * Stops the sensor data collection and resets the displayed value.
     */
    fun stopListening()
}