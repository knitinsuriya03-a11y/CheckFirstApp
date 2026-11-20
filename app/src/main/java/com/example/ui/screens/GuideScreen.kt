package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CheckFirstCard
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.PageTitle
import com.example.ui.components.PillShape
import com.example.ui.components.SectionHeading
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors

@Composable
fun GuideScreen(
    onOpenEmergency: () -> Unit,
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
            eyebrow = s.guideEyebrow,
            title = s.guideTitle,
            subtitle = s.guideSubtitle,
        )

        CheckFirstCard {
            s.guideSteps.forEachIndexed { index, step ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(c.accentTint),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "0${index + 1}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = c.accentLight,
                            )
                        }
                        if (index < s.guideSteps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .width(2.dp)
                                    .height(42.dp)
                                    .background(c.accentBorder),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f).padding(bottom = if (index < s.guideSteps.lastIndex) 16.dp else 0.dp)) {
                        Text(step.title, style = MaterialTheme.typography.titleLarge, color = c.navy)
                        Spacer(Modifier.height(3.dp))
                        Text(step.body, style = MaterialTheme.typography.bodyMedium, color = c.ink)
                        Spacer(Modifier.height(3.dp))
                        Text(step.detail, style = MaterialTheme.typography.bodySmall, color = c.subtle)
                    }
                }
            }
        }

        CheckFirstCard(containerColor = c.surfaceGlow, borderColor = c.accentBorder) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.accentTint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.guideVerifyTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = s.guideVerifyBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.ink,
                    )
                }
            }
        }

        SectionHeading(title = s.guideScamsTitle, note = s.guideScamsNote)

        s.guideScams.forEach { (title, desc) ->
            CheckFirstCard(contentPadding = PaddingValues(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.cautionBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = c.cautionIcon, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = c.navy)
                        Spacer(Modifier.height(3.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall, color = c.muted)
                    }
                }
            }
        }

        CosmicHorizonCard(contentPadding = PaddingValues(20.dp)) {
            Text(s.guideAlreadySentTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.guideAlreadySentBody,
                style = MaterialTheme.typography.bodySmall,
                color = c.ink,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onOpenEmergency,
                modifier = Modifier.fillMaxWidth().testTag("guide_open_emergency_button"),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(s.guideOpenEmergencyBtn, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
