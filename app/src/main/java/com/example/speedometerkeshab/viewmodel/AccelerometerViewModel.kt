package com.example.speedometerkeshab

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import com.example.speedometerkeshab.data.AccelerometerRepository
import com.example.speedometerkeshab.model.AccelerometerModel

/**
 * ViewModel responsible for managing the UI state and orchestrating data flow
 * between the View (Composable) and the Repository.
 *
 * Extends AndroidViewModel to access the Application context without a custom factory.
 */
class AccelerometerViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Instantiate the Repository (using the concrete implementation via the interface)
    private val repository: AccelerometerRepository =
        AccelerometerRepositoryImpl(application.applicationContext)

    // 2. State Management for Compose UI
    private val _state = mutableStateOf(AccelerometerModel())
    val state: State<AccelerometerModel> = _state

    // 3. LiveData Observer to bridge Repository data (LiveData<Float>) to ViewModel state (State<AccelerometerState>)
    private val speedObserver = Observer<Float> { newSpeed ->
        // Update the Compose state whenever the Repository's LiveData changes
        _state.value = _state.value.copy(speedMps = newSpeed)
    }

    init {
        // Start observing the Repository's data source immediately
        repository.currentSpeedMps.observeForever(speedObserver)
    }

    /**
     * Start the measurement process by calling the Repository.
     */
    fun startMeasurement() {
        _state.value = _state.value.copy(isRunning = true)
        repository.startListening()
    }

    /**
     * Stop the measurement process by calling the Repository.
     */
    fun stopMeasurement() {
        repository.stopListening()
        // The observer handles the speed resetting to 0f, we just update the flag.
        _state.value = _state.value.copy(isRunning = false, speedMps = 0f)
    }

    /**
     * Clean up the listener and observer when the ViewModel is destroyed to prevent leaks.
     */
    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
        // CRITICAL: Always remove observers in onCleared()
        repository.currentSpeedMps.removeObserver(speedObserver)
    }
}