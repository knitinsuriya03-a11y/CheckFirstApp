package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.LiveVoiceStatus
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.InlineErrorBanner
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.PinReminderCard
import com.example.ui.components.SmallTag
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.HeroCopyLight
import com.example.ui.theme.LocalCheckFirstColors
import com.example.ui.theme.PromiseCheckTint
import com.example.viewmodel.AiAssistantViewModel
import com.example.viewmodel.LiveVoiceMessageKey

@Composable
fun LiveVoiceScreen(
    tamil: Boolean,
    viewModel: AiAssistantViewModel,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val context = LocalContext.current
    val state by viewModel.voiceState.collectAsStateWithLifecycle()

    val effectiveTamil = tamil || s.isTamil
    var permissionDenied by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permissionDenied = false
            viewModel.startLiveVoiceSession(effectiveTamil)
        } else {
            permissionDenied = true
        }
    }

    // Stop live voice session cleanly if user leaves this screen or revokes microphone permission while backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasMic = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasMic) {
                    permissionDenied = false
                } else if (viewModel.voiceState.value.status == LiveVoiceStatus.ACTIVE ||
                    viewModel.voiceState.value.status == LiveVoiceStatus.CONNECTING
                ) {
                    permissionDenied = true
                    viewModel.stopLiveVoiceSession()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (viewModel.voiceState.value.status == LiveVoiceStatus.ACTIVE ||
                viewModel.voiceState.value.status == LiveVoiceStatus.CONNECTING
            ) {
                viewModel.stopLiveVoiceSession()
            }
        }
    }

    fun onStartClicked() {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            permissionDenied = false
            viewModel.startLiveVoiceSession(effectiveTamil)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val localizedStatusText = when (state.statusKey) {
        LiveVoiceMessageKey.IDLE -> s.voiceStatusIdle
        LiveVoiceMessageKey.CONNECTING -> s.voiceStatusConnecting
        LiveVoiceMessageKey.ACTIVE -> s.voiceStatusActive
        LiveVoiceMessageKey.ENDED -> s.voiceStatusEnded
        LiveVoiceMessageKey.TIMEOUT -> s.voiceStatusTimeout
        LiveVoiceMessageKey.MIC_DENIED -> s.voiceStatusMicDenied
        LiveVoiceMessageKey.UNAVAILABLE -> s.voiceStatusUnavailable
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.voiceEyebrow,
            title = s.voiceTitle,
            subtitle = s.voiceSubtitle,
        )

        // Signature Cosmic Horizon Voice Console Card
        CosmicHorizonCard(contentPadding = PaddingValues(24.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SmallTag(
                    text = when (state.status) {
                        LiveVoiceStatus.IDLE -> s.voiceTagReady
                        LiveVoiceStatus.CONNECTING -> s.voiceTagConnecting
                        LiveVoiceStatus.ACTIVE -> s.voiceTagActive
                        LiveVoiceStatus.ERROR -> s.voiceTagStopped
                    },
                )

                // Luminous Acoustic Pulse Ring
                Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                    if (state.status == LiveVoiceStatus.ACTIVE) {
                        val pulseTransition = rememberInfiniteTransition(label = "voicePulse")
                        val pulseScale by pulseTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 900),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "pulseScale",
                        )
                        Box(
                            modifier = Modifier
                                .size(126.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(c.accent.copy(alpha = 0.28f)),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(
                                when (state.status) {
                                    LiveVoiceStatus.ACTIVE -> c.accent
                                    LiveVoiceStatus.CONNECTING -> c.accentTint
                                    else -> c.surfaceRaised
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (state.status) {
                            LiveVoiceStatus.CONNECTING -> CircularProgressIndicator(
                                color = PromiseCheckTint,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp),
                            )

                            LiveVoiceStatus.ACTIVE -> Icon(
                                Icons.Filled.GraphicEq,
                                contentDescription = s.voiceTagActive,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp),
                            )

                            else -> Icon(
                                Icons.Filled.Mic,
                                contentDescription = s.voiceTagReady,
                                tint = c.accentLight,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                }

                Text(
                    text = localizedStatusText,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.navy,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = s.voiceDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = HeroCopyLight,
                    textAlign = TextAlign.Center,
                )

                if (state.status == LiveVoiceStatus.ACTIVE || state.status == LiveVoiceStatus.CONNECTING) {
                    Button(
                        onClick = { viewModel.stopLiveVoiceSession() },
                        modifier = Modifier.fillMaxWidth().testTag("live_voice_end_button"),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.voiceEndBtn, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = { onStartClicked() },
                        modifier = Modifier.fillMaxWidth().testTag("live_voice_start_button"),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.voiceStartBtn, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (permissionDenied) {
            InlineErrorBanner(s.voiceMicPermissionError)
        }

        if (!state.errorMessage.isNullOrBlank()) {
            InlineErrorBanner(state.errorMessage!!)
        }

        CheckFirstCard {
            Text(s.voiceSituationsTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                s.voiceSituations.forEach { situation ->
                    Text(situation, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
            }
        }

        PinReminderCard(
            title = s.voicePrivacyTitle,
            body = s.voicePrivacyBody,
        )

        Spacer(Modifier.height(16.dp))
    }
}
