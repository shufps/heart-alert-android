package com.nikashitsa.polar_alert_android.lib

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepository @Inject constructor() {
    private val _bpm = MutableStateFlow(-1)
    val bpm: StateFlow<Int> = _bpm

    private val _state = MutableStateFlow(TrackingState.GOOD)
    val state: StateFlow<TrackingState> = _state

    var isActive: Boolean = false

    fun update(bpm: Int, state: TrackingState) {
        _bpm.value = bpm
        _state.value = state
    }

    fun reset() {
        _bpm.value = -1
        _state.value = TrackingState.GOOD
    }
}
