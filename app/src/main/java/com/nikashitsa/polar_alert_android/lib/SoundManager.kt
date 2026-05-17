package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool
import com.nikashitsa.polar_alert_android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundType(val resId: Int) {
    HIGH_BEEP(R.raw.high_beep),
    LOW_BEEP(R.raw.low_beep),
    CONNECTED(R.raw.connected),
    DISCONNECTED(R.raw.disconnected),
    RECONNECTING(R.raw.reconnecting),
    GOOD(R.raw.good),
    TOO_HIGH(R.raw.too_high),
    TOO_LOW(R.raw.too_low),
}

@Singleton
class SoundManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // USAGE_MEDIA follows the active audio route exclusively (BT headphones or speaker, not both)
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setWillPauseWhenDucked(false)
        .build()
    private var focusHeld = false

    init {
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        for (type in SoundType.entries) {
            soundMap[type] = soundPool.load(context, type.resId, 1)
        }
    }

    fun play(type: SoundType, volume: Int) {
        soundMap[type]?.let { soundId ->
            val volumeFloat = volume / 100f
            soundPool.play(soundId, volumeFloat, volumeFloat, 1, 0, 1f)
        }
    }

    fun requestAudioFocus() {
        if (!focusHeld) {
            audioManager.requestAudioFocus(audioFocusRequest)
            focusHeld = true
        }
    }

    fun abandonAudioFocus() {
        if (focusHeld) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            focusHeld = false
        }
    }

    fun release() {
        abandonAudioFocus()
        soundPool.release()
    }
}
