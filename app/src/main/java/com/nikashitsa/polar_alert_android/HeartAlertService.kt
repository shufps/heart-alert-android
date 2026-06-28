package com.nikashitsa.polar_alert_android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nikashitsa.polar_alert_android.lib.SettingsRepository
import com.nikashitsa.polar_alert_android.lib.SoundManager
import com.nikashitsa.polar_alert_android.lib.TrackingRepository
import com.nikashitsa.polar_alert_android.lib.TrackingState
import com.nikashitsa.polar_alert_android.lib.VibrationManager
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarHrData
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HeartAlertService : Service() {

    @Inject lateinit var api: PolarBleApi
    @Inject lateinit var soundManager: SoundManager
    @Inject lateinit var vibrationManager: VibrationManager
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var trackingRepository: TrackingRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var hrDisposable: Disposable? = null
    private var lastTriggerTime = 0L
    private val throttleInterval = 690L
    private var duckingReleaseJob: Job? = null

    companion object {
        private const val TAG = "HeartAlertService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "heart_alert"
        const val ACTION_START = "com.nikashitsa.ACTION_START"
        const val ACTION_STOP = "com.nikashitsa.ACTION_STOP"
        const val EXTRA_ADDRESS = "address"

        fun startIntent(context: Context, address: String) =
            Intent(context, HeartAlertService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ADDRESS, address)
            }

        fun stopIntent(context: Context) =
            Intent(context, HeartAlertService::class.java).apply {
                action = ACTION_STOP
            }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return START_NOT_STICKY
                startForeground(NOTIFICATION_ID, buildNotification("Monitoring..."))
                startStream(address)
            }
            ACTION_STOP -> {
                trackingRepository.isActive = false
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startStream(address: String) {
        if (hrDisposable?.isDisposed == false) return
        hrDisposable = api.startHrStreaming(address)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { hrData: PolarHrData ->
                    for (sample in hrData.samples) onHr(sample.hr)
                },
                { error -> Log.e(TAG, "HR stream error: $error") }
            )
    }

    private fun onHr(hr: Int) {
        serviceScope.launch {
            val hrMax = settingsRepository.hrMaxFlow.first()
            val newState = if (hr > hrMax) TrackingState.HIGH else TrackingState.GOOD
            trackingRepository.update(hr, newState)
            updateNotification(
                if (newState == TrackingState.HIGH) "$hr BPM — Too high!" else "$hr BPM"
            )

            val now = System.currentTimeMillis()
            if (now - lastTriggerTime > throttleInterval && newState != TrackingState.GOOD) {
                lastTriggerTime = now
                triggerAlerts(newState)
            }
        }
    }

    private suspend fun triggerAlerts(state: TrackingState) {
        if (settingsRepository.soundEnabledFlow.first()) {
            state.sound?.let { soundType ->
                val volume = settingsRepository.volumeFlow.first()
                if (settingsRepository.audioDuckingFlow.first()) {
                    soundManager.requestAudioFocus()
                    duckingReleaseJob?.cancel()
                    duckingReleaseJob = serviceScope.launch {
                        delay(2000)
                        soundManager.abandonAudioFocus()
                    }
                }
                soundManager.play(soundType, volume)
            }
        }
        if (settingsRepository.vibrateFlow.first()) {
            state.vibration?.let { vibrationManager.vibrate(it) }
        }
    }

    override fun onDestroy() {
        trackingRepository.isActive = false
        hrDisposable?.dispose()
        serviceScope.cancel()
        trackingRepository.reset()
        soundManager.abandonAudioFocus()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Heart Alert", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Heart rate monitoring" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            stopIntent(this),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Heart Alert")
            .setContentText(text)
            .setSmallIcon(R.drawable.heart)
            .setContentIntent(openPi)
            .setOngoing(true)
            .addAction(R.drawable.heart, "Stop", stopPi)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }
}
