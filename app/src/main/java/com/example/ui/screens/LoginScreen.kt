package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.components.InlineErrorBanner
import com.example.ui.components.PillShape
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.HeroCopyLight
import com.example.ui.theme.HeroEyebrowLight
import com.example.ui.theme.LocalCheckFirstColors
import com.google.firebase.auth.FirebaseUser

/**
 * Mandatory startup authentication gate so CheckFirst always opens on the login screen.
 * Uses real Google Sign-In via Jetpack Credential Manager (GetSignInWithGoogleOption).
 */
@Composable
fun LoginScreen(
    currentUser: FirebaseUser?,
    tamil: Boolean,
    onToggleLanguage: () -> Unit,
    onSignInSuccess: () -> Unit,
    onContinueSignedInUser: () -> Unit,
    onSwitchAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    var authError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Brand & Language Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(c.accentTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = c.accentGlow,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Column {
                        Text("CheckFirst", style = MaterialTheme.typography.titleLarge, color = c.navy)
                        Text(s.brandTagline, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }

                Surface(
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { onToggleLanguage() }
                        .testTag("login_language_toggle"),
                    color = if (tamil) c.accentTint else c.surfaceRaised,
                    shape = PillShape,
                    border = BorderStroke(1.dp, if (tamil) c.accentBorder else c.line),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Translate,
                            contentDescription = s.toggleLangDesc,
                            tint = if (tamil) c.accentLight else c.muted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (tamil) "தமிழ்" else "EN",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (tamil) c.accentLight else c.navy,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Hero Cosmic Horizon Login Card
            CosmicHorizonCard(contentPadding = PaddingValues(24.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(c.accentTint),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = c.accentLight,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = s.loginEyebrow,
                        style = MaterialTheme.typography.labelMedium,
                        color = HeroEyebrowLight,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = s.loginTitle,
                        style = MaterialTheme.typography.displaySmall,
                        color = c.navy,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = s.loginSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HeroCopyLight,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))

                    if (currentUser != null) {
                        // Returning signed-in Google account — confirm entry or switch account
                        val displayName = currentUser.displayName?.takeIf { it.isNotBlank() }
                            ?: currentUser.email?.takeIf { it.isNotBlank() }
                            ?: s.setGoogleAccountFallback
                        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "G"

                        Surface(
                            color = c.surfaceRaised,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, c.accentBorder),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(c.accentTint),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = c.accentLight,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.loginSignedInAs, style = MaterialTheme.typography.labelSmall, color = c.accentLight)
                                    Text(displayName, style = MaterialTheme.typography.titleSmall, color = c.navy)
                                    if (!currentUser.email.isNullOrBlank() && currentUser.email != displayName) {
                                        Text(currentUser.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = c.muted)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                authError = null
                                onContinueSignedInUser()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_continue_button"),
                            shape = PillShape,
                            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 15.dp, horizontal = 18.dp),
                        ) {
                            Text(
                                text = "${s.loginContinueAsPrefix} $displayName",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                authError = null
                                onSwitchAccount()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_switch_account_button"),
                            shape = PillShape,
                            border = BorderStroke(1.dp, c.pillSecondaryBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = c.pillSecondary,
                                contentColor = c.navy,
                            ),
                            contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                        ) {
                            Text(s.loginSwitchAccount, style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        GoogleSignInActionButton(
                            onAuthSuccess = {
                                authError = null
                                onSignInSuccess()
                            },
                            onAuthError = { err ->
                                authError = err
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = s.loginButtonGoogle,
                            testTag = "login_google_sign_in_button",
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = s.loginRequiredNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.subtle,
                            textAlign = TextAlign.Center,
                        )
                    }

                    if (!authError.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        InlineErrorBanner(authError!!)
                    }
                }
            }

            // Privacy & Safety Guarantees Card
            CheckFirstCard {
                Text(s.loginWhyTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                Spacer(Modifier.height(10.dp))
                listOf(s.loginBenefit1, s.loginBenefit2, s.loginBenefit3).forEach { line ->
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = c.clear,
                            modifier = Modifier.padding(top = 2.dp).size(17.dp),
                        )
                        Text(line, style = MaterialTheme.typography.bodySmall, color = c.ink)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(s.loginPrivacyNote, style = MaterialTheme.typography.bodySmall, color = c.subtle)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
