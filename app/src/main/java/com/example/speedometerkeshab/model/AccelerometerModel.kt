package com.example.speedometerkeshab.model

data class AccelerometerModel (
    // Field for UI state (must have default value for Firebase)
    val speedMps: Float = 0f,

    // Field for UI state (must have default value for Firebase)
    val isRunning: Boolean = false,

    // NEW FIELD: Required for Firebase to log the time of the reading
    val timestamp: Long = 0
)