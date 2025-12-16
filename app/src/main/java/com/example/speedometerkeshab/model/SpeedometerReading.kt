package com.example.speedometerkeshab.model

// Data class that defines the structure for data saved in the
// Firebase Realtime Database.
// Note: All properties must have default values (e.g., = 0) for Firebase
// to correctly serialize and deserialize the data.
data class SpeedometerReading(
    val timestamp: Long = 0,
    val scaledSpeed: Float = 0f
)