package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.Prefs
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.components.InlineErrorBanner
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.SmallTag
import com.example.ui.components.checkFirstFilterChipColors
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.HeroCopyLight
import com.example.ui.theme.LocalCheckFirstColors
import com.example.ui.theme.PromiseCheckTint
import com.google.firebase.auth.FirebaseUser

@Composable
fun SettingsScreen(
    prefs: Prefs,
    currentUser: FirebaseUser?,
    syncError: String?,
    onUpdatePrefs: ((Prefs) -> Prefs) -> Unit,
    onClearHistory: () -> Unit,
    onSignInSuccess: () -> Unit,
    onSignInError: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageTitle(
            eyebrow = s.setEyebrow,
            title = s.setTitle,
            subtitle = s.setSubtitle,
        )

        // Language & Accessibility
        CheckFirstCard {
            Text(s.setLangAccessTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.setLangPrefTitle, style = MaterialTheme.typography.titleSmall, color = c.navy)
                    Text(s.setLangPrefSub, style = MaterialTheme.typography.bodySmall, color = c.muted)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !prefs.tamil,
                        onClick = { onUpdatePrefs { it.copy(tamil = false) } },
                        label = { Text("English") },
                        shape = PillShape,
                        colors = checkFirstFilterChipColors(),
                        modifier = Modifier.testTag("settings_lang_en"),
                    )
                    FilterChip(
                        selected = prefs.tamil,
                        onClick = { onUpdatePrefs { it.copy(tamil = true) } },
                        label = { Text("தமிழ்") },
                        shape = PillShape,
                        colors = checkFirstFilterChipColors(),
                        modifier = Modifier.testTag("settings_lang_ta"),
                    )
                }
            }
            SettingSwitchRow(
                title = s.setSimpleModeTitle,
                description = s.setSimpleModeSub,
                checked = prefs.simpleMode,
                onCheckedChange = { v -> onUpdatePrefs { it.copy(simpleMode = v) } },
                testTag = "settings_simple_mode_switch",
            )
            SettingSwitchRow(
                title = s.setLargeTextTitle,
                description = s.setLargeTextSub,
                checked = prefs.largeText,
                onCheckedChange = { v -> onUpdatePrefs { it.copy(largeText = v) } },
                testTag = "settings_large_text_switch",
            )
        }

        // Notifications
        CheckFirstCard {
            Text(s.setNotificationsTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(6.dp))
            SettingSwitchRow(
                title = s.setSafetyTipsTitle,
                description = s.setSafetyTipsSub,
                checked = prefs.safetyTips,
                onCheckedChange = { v -> onUpdatePrefs { it.copy(safetyTips = v) } },
                testTag = "settings_safety_tips_switch",
            )
            SettingSwitchRow(
                title = s.setAlertsTitle,
                description = s.setAlertsSub,
                checked = prefs.completionAlerts,
                onCheckedChange = { v -> onUpdatePrefs { it.copy(completionAlerts = v) } },
                testTag = "settings_completion_alerts_switch",
            )
        }

        // Account & Cloud Sync
        CheckFirstCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.setAccountTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Text(
                        text = if (currentUser != null) s.setAccountSubSignedIn else s.setAccountSubSignedOut,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.muted,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (currentUser != null) {
                Surface(
                    color = c.surfaceGlow,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, c.accentBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.CloudDone, contentDescription = null, tint = c.clear, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.displayName?.takeIf { it.isNotBlank() } ?: s.setGoogleAccountFallback,
                                style = MaterialTheme.typography.titleSmall,
                                color = c.navy,
                            )
                            Text(
                                text = currentUser.email.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.muted,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("settings_sign_out_button"),
                    shape = PillShape,
                    border = BorderStroke(1.dp, c.pillSecondaryBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = c.pillSecondary,
                        contentColor = c.navy,
                    ),
                ) {
                    Text(s.menuSignOut)
                }
            } else {
                GoogleSignInActionButton(
                    onAuthSuccess = onSignInSuccess,
                    onAuthError = onSignInError,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!syncError.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                InlineErrorBanner(syncError)
            }
        }

        // Privacy Preferences
        CheckFirstCard {
            Text(s.setPrivacyTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(6.dp))
            SettingSwitchRow(
                title = if (currentUser != null) s.setHistoryTitleSignedIn else s.setHistoryTitleLocal,
                description = s.setHistorySub,
                checked = prefs.saveHistory,
                onCheckedChange = { v -> onUpdatePrefs { it.copy(saveHistory = v) } },
                testTag = "settings_save_history_switch",
            )
            if (prefs.saveHistory) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = c.surfaceGlow,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, c.accentBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.clear, modifier = Modifier.size(18.dp))
                        Text(
                            text = if (currentUser != null) s.setHistoryEnabledSynced else s.setHistoryEnabledLocal,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.ink,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = onClearHistory,
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            modifier = Modifier.testTag("settings_clear_history_button"),
                        ) {
                            Text(s.scanClearBtn, color = c.accentLight, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Privacy Promise Cosmic Horizon Card
        CosmicHorizonCard {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(c.accentTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = PromiseCheckTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(s.setPromiseTitle, style = MaterialTheme.typography.titleLarge, color = c.navy)
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.setPromiseBody,
                style = MaterialTheme.typography.bodyMedium,
                color = HeroCopyLight,
            )
        }

        // What gets processed?
        CheckFirstCard {
            Text(s.setProcessedTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(8.dp))
            Text(
                text = s.setProcessedBody1,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.setProcessedBody2,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
            )
            Spacer(Modifier.height(12.dp))
            s.setProcessedBullets.forEach { line ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = c.clear, modifier = Modifier.size(16.dp))
                    Text(line, style = MaterialTheme.typography.bodySmall, color = c.ink)
                }
            }
        }

        // About
        CheckFirstCard {
            Text(s.setAboutTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(6.dp))
            Text(
                text = s.setAboutBody,
                style = MaterialTheme.typography.bodySmall,
                color = c.muted,
            )
            Spacer(Modifier.height(10.dp))
            SmallTag(s.setAboutTag)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    val c = LocalCheckFirstColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = c.track)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = c.navy)
                Spacer(Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = c.muted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = c.accent,
                    uncheckedThumbColor = c.muted,
                    uncheckedTrackColor = c.surfaceRaised,
                    uncheckedBorderColor = c.line,
                ),
            )
        }
    }
}
