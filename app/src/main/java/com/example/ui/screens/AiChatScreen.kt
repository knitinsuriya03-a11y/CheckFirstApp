package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ChatMessage
import com.example.ui.components.CosmicHorizonCard
import com.example.ui.components.InlineErrorBanner
import com.example.ui.components.InputShape
import com.example.ui.components.PillShape
import com.example.ui.components.PrivacyChip
import com.example.ui.components.SmallTag
import com.example.ui.components.checkFirstTextFieldColors
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalCheckFirstColors
import com.example.viewmodel.AiAssistantViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    tamil: Boolean,
    viewModel: AiAssistantViewModel,
    modifier: Modifier = Modifier,
) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    val state by viewModel.chatState.collectAsStateWithLifecycle()
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val effectiveTamil = tamil || s.isTamil

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Header strip
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(s.chatEyebrow, style = MaterialTheme.typography.labelMedium, color = c.accentLight)
                Text(
                    text = s.chatTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.navy,
                )
            }
            if (state.usedFallbackModel) {
                SmallTag(
                    text = "${s.aiInsightAutoSwitchedPrefix} ${state.activeModelName}",
                    modifier = Modifier.testTag("chat_fallback_model_badge"),
                )
            }
            if (state.messages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.resetChat() },
                    modifier = Modifier.testTag("chat_reset_button"),
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = s.chatResetDesc, tint = c.muted)
                }
            }
        }

        // Scrollable chat thread
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (state.messages.isEmpty()) {
                item {
                    CosmicHorizonCard(contentPadding = PaddingValues(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.accentTint),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = c.accentLight, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(s.chatHeroTitle, style = MaterialTheme.typography.titleMedium, color = c.navy)
                                Text(
                                    text = "${s.chatHeroModelPrefix} ${state.activeModelName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.muted,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = s.chatHeroBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                        Spacer(Modifier.height(12.dp))
                        PrivacyChip(s.chatPrivacyChip)
                    }
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(s.chatTryAsking, style = MaterialTheme.typography.labelLarge, color = c.navy)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.chatStarterPrompts.forEachIndexed { idx, prompt ->
                            SuggestionChip(
                                onClick = { viewModel.sendChatMessage(prompt, effectiveTamil) },
                                label = { Text(prompt, style = MaterialTheme.typography.bodySmall) },
                                shape = PillShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = c.surfaceRaised,
                                    labelColor = c.ink,
                                ),
                                border = BorderStroke(1.dp, c.line),
                                modifier = Modifier.testTag("chat_starter_$idx"),
                            )
                        }
                    }
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }
        }

        if (!state.errorMessage.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            InlineErrorBanner(state.errorMessage!!)
        }

        Spacer(Modifier.height(8.dp))

        // Composer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(s.chatPlaceholder) },
                maxLines = 4,
                colors = checkFirstTextFieldColors(),
                modifier = Modifier.weight(1f).testTag("chat_message_input"),
                shape = InputShape,
            )
            Surface(
                shape = CircleShape,
                color = if (input.isNotBlank() && !state.isSending) c.accent else c.surfaceRaised,
                border = BorderStroke(1.dp, if (input.isNotBlank() && !state.isSending) c.accentGlow else c.line),
                modifier = Modifier.size(48.dp),
            ) {
                IconButton(
                    onClick = {
                        val toSend = input.trim()
                        if (toSend.isNotEmpty()) {
                            input = ""
                            viewModel.sendChatMessage(toSend, effectiveTamil)
                        }
                    },
                    enabled = input.isNotBlank() && !state.isSending,
                    modifier = Modifier.testTag("chat_send_button"),
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.accentLight, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = s.chatSendDesc,
                            tint = if (input.isNotBlank()) Color.White else c.muted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val c = LocalCheckFirstColors.current
    val s = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            color = if (message.isUser) c.accent else c.surfaceRaised,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp,
            ),
            border = if (message.isUser) null else BorderStroke(1.dp, c.line),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(
                    text = message.text.ifBlank { s.chatThinking },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) Color.White else c.ink,
                )
                if (message.isStreaming && message.text.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(s.chatStreaming, style = MaterialTheme.typography.labelSmall, color = c.accentLight)
                }
            }
        }
    }
}
