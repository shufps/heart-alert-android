package com.nikashitsa.polar_alert_android.ui.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.play.core.review.ReviewManagerFactory
import com.nikashitsa.polar_alert_android.R
import com.nikashitsa.polar_alert_android.lib.BluetoothViewModel
import com.nikashitsa.polar_alert_android.lib.DeviceConnectionState
import com.nikashitsa.polar_alert_android.lib.HrFeature
import com.nikashitsa.polar_alert_android.lib.SettingsDefaults
import com.nikashitsa.polar_alert_android.lib.SettingsViewModel
import com.nikashitsa.polar_alert_android.lib.SoundType
import com.nikashitsa.polar_alert_android.lib.SoundViewModel
import com.nikashitsa.polar_alert_android.lib.TrackingState
import com.nikashitsa.polar_alert_android.lib.VibrationType
import com.nikashitsa.polar_alert_android.lib.VibrationViewModel
import com.nikashitsa.polar_alert_android.ui.components.AppButton
import com.nikashitsa.polar_alert_android.ui.theme.Colors
import com.nikashitsa.polar_alert_android.ui.theme.Fonts
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme
import kotlinx.coroutines.delay
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    bluetooth: BluetoothViewModel = hiltViewModel(),
    settings: SettingsViewModel = hiltViewModel(),
    sound: SoundViewModel = hiltViewModel(),
    vibration: VibrationViewModel = hiltViewModel(),
    onSettings: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val deviceConnectionState = bluetooth.deviceConnectionState.collectAsState()
    val hrFeature = bluetooth.hrFeature.collectAsState()
    val hrMax by settings.hrMax.collectAsState()

    BackHandler {
        onBack()
    }

    TrackingScreenContent(
        deviceConnectionState = deviceConnectionState.value,
        hrFeature = hrFeature.value,
        hrStreamStart = bluetooth::hrStreamStart,
        hrStreamStop = bluetooth::hrStreamStop,
        playSound = sound::play,
        hrMax = hrMax,
        setHrMax = settings::setHrMax,
        vibrate = vibration::vibrate,
        onSettings = onSettings,
        onStop = { bluetooth.disconnect(); onBack() },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreenContent(
    deviceConnectionState: DeviceConnectionState,
    hrFeature: HrFeature = HrFeature(),
    hrStreamStart: (String, (Int) -> Unit) -> Unit = { _, _ -> },
    hrStreamStop: () -> Unit = {},
    playSound: (SoundType) -> Unit = {},
    hrMax: Int = SettingsDefaults.HR_MAX,
    setHrMax: (Int) -> Unit = {},
    vibrate: (VibrationType) -> Unit = {},
    initialBpm: Int = -1,
    onSettings: () -> Unit = {},
    onStop: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var localHrMax by remember(hrMax) { mutableIntStateOf(hrMax) }
    var isPaused by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                requestAppReview(context, activity)
                onStop()
            }) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = Colors.White.copy(alpha = 0.5f),
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Colors.White.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!isPaused) {
            BpmView(
                deviceConnectionState = deviceConnectionState,
                hrFeature = hrFeature,
                hrStreamStart = hrStreamStart,
                hrStreamStop = hrStreamStop,
                playSound = playSound,
                hrMax = localHrMax,
                setHrMax = {
                    localHrMax = it
                    setHrMax(it)
                },
                vibrate = vibrate,
                initialBpm = initialBpm,
            )
        } else {
            Text("Paused", style = Fonts.textLg, color = Colors.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            HrMaxDial(hrMax = localHrMax, onHrMaxChange = { localHrMax = it; setHrMax(it) })
        }

        Spacer(modifier = Modifier.weight(1f))

        AppButton(if (isPaused) "Resume" else "Pause") {
            isPaused = !isPaused
        }
    }
}

private fun requestAppReview(context: Context, activity: Activity?) {
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful && activity != null) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        }
    }
}

@Composable
fun BpmView(
    deviceConnectionState: DeviceConnectionState,
    hrFeature: HrFeature,
    hrStreamStart: (String, (Int) -> Unit) -> Unit = { _, _ -> },
    hrStreamStop: () -> Unit = {},
    playSound: (SoundType) -> Unit = {},
    hrMax: Int = SettingsDefaults.HR_MAX,
    setHrMax: (Int) -> Unit = {},
    vibrate: (VibrationType) -> Unit = {},
    initialBpm: Int = -1,
) {
    DisposableEffect(Unit) {
        onDispose { hrStreamStop() }
    }
    var state by rememberSaveable { mutableStateOf(TrackingState.GOOD) }
    var bpm by rememberSaveable { mutableIntStateOf(initialBpm) }
    var prevConnectionState by rememberSaveable(
        stateSaver = DeviceConnectionState.Saver
    ) { mutableStateOf(DeviceConnectionState.Connected()) }
    var lastTriggerTime by rememberSaveable { mutableStateOf<Date?>(null) }
    val throttleInterval = 690
    val hrMaxState = rememberUpdatedState(hrMax)

    when (val connectionState = deviceConnectionState) {
        is DeviceConnectionState.Disconnected -> {
            Text("Disconnected", style = Fonts.textLg)
            PlaySoundRepeatedly(playSound, SoundType.DISCONNECTED) {
                prevConnectionState = DeviceConnectionState.Disconnected()
            }
        }
        is DeviceConnectionState.Connecting -> {
            Text("Reconnecting...", style = Fonts.textLg)
            PlaySoundRepeatedly(playSound, SoundType.RECONNECTING) {
                prevConnectionState = DeviceConnectionState.Disconnected()
            }
        }
        is DeviceConnectionState.Connected -> {
            if (hrFeature.isSupported) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Current BPM display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.height(80.dp),
                    ) {
                        val bpmLabel = if (bpm > -1) "$bpm" else "--"
                        Text(
                            text = bpmLabel,
                            style = Fonts.text2XlBold,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.offset(y = (-12).dp),
                            color = state.heartBeatColor,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HeartIcon(state)
                            Text(
                                text = "BPM",
                                style = Fonts.textLg,
                                color = state.heartBeatColor,
                            )
                            LaunchedEffect(Unit) {
                                if (prevConnectionState is DeviceConnectionState.Disconnected) {
                                    playSound(SoundType.CONNECTED)
                                }

                                hrStreamStart(connectionState.address) { hr ->
                                    bpm = hr
                                    val prevState = state
                                    state = if (bpm > hrMaxState.value) TrackingState.HIGH else TrackingState.GOOD

                                    val now = Date()
                                    if (lastTriggerTime == null ||
                                        now.time - lastTriggerTime!!.time > throttleInterval
                                    ) {
                                        lastTriggerTime = now
                                        state.sound?.let { sound ->
                                            playSound(sound)
                                        }
                                        state.vibration?.let { vib ->
                                            vibrate(vib)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = state.heartBeatDescription,
                        style = Fonts.textLg,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HrMaxDial(
                        hrMax = hrMax,
                        onHrMaxChange = setHrMax,
                        isAlarming = state == TrackingState.HIGH,
                    )
                }
            } else {
                Text("Reconnecting...", style = Fonts.textLg)
            }
        }
    }
}

@Composable
fun HrMaxDial(
    hrMax: Int,
    onHrMaxChange: (Int) -> Unit,
    isAlarming: Boolean = false,
) {
    val minVal = 80
    val maxVal = 190
    val startAngle = 135f
    val sweepTotal = 270f
    val arcColor = if (isAlarming) Colors.Red else Colors.White
    val strokeWidth = 18.dp
    val dialSize = 220.dp
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val hrMaxState = rememberUpdatedState(hrMax)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "MAX BPM",
            style = Fonts.textMd,
            color = Colors.White.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(4.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(dialSize)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount.x - dragAmount.y
                            val delta = (dragAccumulator / 4f).toInt()
                            if (delta != 0) {
                                dragAccumulator -= delta * 4f
                                onHrMaxChange((hrMaxState.value + delta).coerceIn(minVal, maxVal))
                            }
                        }
                    }
            ) {
                val strokePx = strokeWidth.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val arcOffset = Offset(inset, inset)

                // Background arc
                drawArc(
                    color = Colors.Gray,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
                // Filled arc
                val fraction = (hrMaxState.value - minVal).toFloat() / (maxVal - minVal)
                drawArc(
                    color = arcColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * fraction,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "$hrMax",
                style = Fonts.text2XlBold,
                color = arcColor,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledIconButton(
                onClick = { onHrMaxChange((hrMax - 1).coerceIn(minVal, maxVal)) },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Colors.Gray,
                    contentColor = Colors.White,
                ),
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Decrease max HR",
                    modifier = Modifier.size(32.dp),
                )
            }
            FilledIconButton(
                onClick = { onHrMaxChange((hrMax + 1).coerceIn(minVal, maxVal)) },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Colors.Gray,
                    contentColor = Colors.White,
                ),
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Increase max HR",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
fun PlaySoundRepeatedly(playSound: (SoundType) -> Unit = {}, soundType: SoundType, onStart: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        onStart()
        while (true) {
            playSound(soundType)
            delay(5000)
        }
    }
}

@Composable
fun HeartIcon(state: TrackingState) {
    key(state.heartBeatDuration) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween((state.heartBeatDuration * 1000).toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        Image(
            painter = painterResource(id = R.drawable.heart),
            contentDescription = "Heart",
            modifier = Modifier
                .height(32.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
    }
}

@Preview
@Composable
fun TrackingScreenConnectedPreview() {
    HeartAlertTheme {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Connected(),
            hrFeature = HrFeature(true),
            initialBpm = 117,
        )
    }
}

@Preview
@Composable
fun TrackingScreenDisconnectedPreview() {
    HeartAlertTheme {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Disconnected()
        )
    }
}

@Preview
@Composable
fun TrackingScreenConnectingPreview() {
    HeartAlertTheme {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Connecting()
        )
    }
}
