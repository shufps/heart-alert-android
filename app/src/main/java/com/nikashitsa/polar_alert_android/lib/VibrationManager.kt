package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class VibrationType {
    LOW, HIGH
}

@Singleton
class VibrationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun vibrate(type: VibrationType) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // timings: [delay, on, off, on, ...], amplitudes: [0, strength, 0, strength, ...]
        val effect = when (type) {
            VibrationType.LOW -> VibrationEffect.createWaveform(
                longArrayOf(0, 200, 100, 200),
                intArrayOf(0, 255, 0, 255),
                -1
            )
            VibrationType.HIGH -> VibrationEffect.createWaveform(
                longArrayOf(0, 300, 100, 300, 100, 300),
                intArrayOf(0, 255, 0, 255, 0, 255),
                -1
            )
        }
        vibrator.vibrate(effect)
    }
}
