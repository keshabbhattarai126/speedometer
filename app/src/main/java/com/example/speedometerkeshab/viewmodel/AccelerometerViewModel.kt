package com.example.speedometerkeshab

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import com.example.speedometerkeshab.data.AccelerometerRepository
import com.example.speedometerkeshab.model.AccelerometerModel

class AccelerometerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AccelerometerRepository = AccelerometerRepositoryImpl(application.applicationContext)

    private val _state = mutableStateOf(AccelerometerModel())
    val state: State<AccelerometerModel> = _state

    private val speedObserver = Observer<Float> { newSpeed ->
        _state.value = _state.value.copy(speedMps = newSpeed)
    }

    init {
        repository.currentSpeedMps.observeForever(speedObserver)
    }

    fun startMeasurement() {
        _state.value = _state.value.copy(isRunning = true)
        repository.startListening()
    }

    fun stopMeasurement() {
        repository.stopListening()
        _state.value = _state.value.copy(isRunning = false, speedMps = 0f)
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
        repository.currentSpeedMps.removeObserver(speedObserver)
    }
}