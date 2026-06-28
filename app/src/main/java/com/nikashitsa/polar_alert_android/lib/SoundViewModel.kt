package com.nikashitsa.polar_alert_android.lib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private var duckingReleaseJob: Job? = null

    fun play(type: SoundType) {
        viewModelScope.launch {
            if (!settingsRepository.soundEnabledFlow.first()) return@launch

            val volume = settingsRepository.volumeFlow.first()
            val audioDucking = settingsRepository.audioDuckingFlow.first()

            if (audioDucking) {
                soundManager.requestAudioFocus()
                duckingReleaseJob?.cancel()
                duckingReleaseJob = viewModelScope.launch {
                    delay(2000)
                    soundManager.abandonAudioFocus()
                }
            }

            soundManager.play(type, volume)
        }
    }
}
