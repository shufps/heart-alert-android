package com.nikashitsa.polar_alert_android.lib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
): ViewModel() {

    private val _volume = MutableStateFlow<Int>(SettingsDefaults.VOLUME)
    val volume: StateFlow<Int> = _volume
    private val _hrMax = MutableStateFlow<Int>(SettingsDefaults.HR_MAX)
    val hrMax: StateFlow<Int> = _hrMax
    private val _vibrate = MutableStateFlow<Boolean>(SettingsDefaults.VIBRATE)
    val vibrate: StateFlow<Boolean> = _vibrate
    private val _soundEnabled = MutableStateFlow<Boolean>(SettingsDefaults.SOUND_ENABLED)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled
    private val _audioDucking = MutableStateFlow<Boolean>(SettingsDefaults.AUDIO_DUCKING)
    val audioDucking: StateFlow<Boolean> = _audioDucking

    init {
        observe(repository.volumeFlow, _volume)
        observe(repository.hrMaxFlow, _hrMax)
        observe(repository.vibrateFlow, _vibrate)
        observe(repository.soundEnabledFlow, _soundEnabled)
        observe(repository.audioDuckingFlow, _audioDucking)
    }

    fun setVolume(value: Int) = update { repository.setVolume(value) }
    fun setHrMax(value: Int) = update { repository.setHrMax(value) }
    fun setVibrate(value: Boolean) = update { repository.setVibrate(value) }
    fun setSoundEnabled(value: Boolean) = update { repository.setSoundEnabled(value) }
    fun setAudioDucking(value: Boolean) = update { repository.setAudioDucking(value) }

    private fun <T> observe(flow: Flow<T>, state: MutableStateFlow<T>) {
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
    }
    private fun update(action: suspend () -> Unit) {
        viewModelScope.launch { action() }
    }
}