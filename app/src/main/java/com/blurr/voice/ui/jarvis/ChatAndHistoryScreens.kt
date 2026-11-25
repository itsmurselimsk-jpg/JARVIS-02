package com.blurr.voice.ui.jarvis

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blurr.voice.MemoriesActivity
import com.blurr.voice.TaskLogDetailsActivity
import com.blurr.voice.data.ChatMessageEntity
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCrimson
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisGlassSurface
import com.blurr.voice.ui.theme.JarvisObsidian
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextMuted
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: JarvisViewModel,
    onRequestMicPermission: () -> Unit,
    initialPrompt: String? = null
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            viewModel.sendChatMessage(initialPrompt)
        }
    }

    LaunchedEffect(messages.size, streamingReply) {
        if (messages.isNotEmpty() || streamingReply.isNotEmpty()) {
            listState.animateScrollToItem((messages.size + if (streamingReply.isNotEmpty()) 1 else 0).coerceAtLeast(1) - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            JarvisStatusPill(
                text = if (isGenerating) "STREAMING • GEMINI 3.8 FLASH" else "GEMINI 3.8 FLASH READY",
                color = if (isGenerating) JarvisElectricBlue else JarvisCyan,
                pulsing = isGenerating
            )
            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Clear Chat History",
                        tint = JarvisTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty() && streamingReply.isEmpty()) {
                item {
                    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Neural Chat Online",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask anything in English, Hindi (हिन्दी), or Hinglish. Actionable requests can be executed directly on your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JarvisTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val starters = listOf(
                            "Summarize today's focus plan",
                            "मुझे हिन्दी में एक मोटिवेशनल कोट सुनाओ",
                            "Open YouTube"
                        )
                        starters.forEach { starter ->
                            AssistChip(
                                onClick = { viewModel.sendChatMessage(starter) },
                                label = { Text(starter) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = JarvisGlassElevated,
                                    labelColor = JarvisTextPrimary
                                ),
                                border = BorderStroke(1.dp, JarvisOutline)
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val precedingUserMsg = remember(messages, msg.id) {
                    val idx = messages.indexOfFirst { it.id == msg.id }
                    if (idx > 0 && messages[idx - 1].role == "user") messages[idx - 1].content else null
                }
                ChatBubble(
                    message = msg,
                    onExecute = precedingUserMsg?.let { cmd ->
                        { viewModel.runAutomationTask(cmd) }
                    }
                )
            }

            if (streamingReply.isNotEmpty()) {
                item(key = "streaming_bubble") {
                    ChatBubble(
                        message = ChatMessageEntity(
                            sessionId = JarvisViewModel.CHAT_SESSION_ID,
                            role = "model",
                            content = "$streamingReply ▍"
                        ),
                        onExecute = null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Composer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask JARVIS (English / हिन्दी)...",
                        color = JarvisTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = JarvisGlassSurface,
                    unfocusedContainerColor = JarvisGlassSurface,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisOutline,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field")
            )

            IconButton(
                onClick = {
                    if (telemetry.hasMicPermission) {
                        viewModel.navigateTo(JarvisScreen.VOICE)
                        viewModel.startInAppVoiceTurn()
                    } else {
                        onRequestMicPermission()
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(JarvisGlassElevated)
                    .border(1.dp, JarvisOutline, CircleShape)
                    .testTag("chat_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Input",
                    tint = JarvisCyan
                )
            }

            IconButton(
                onClick = {
                    val toSend = inputText
                    inputText = ""
                    viewModel.sendChatMessage(toSend)
                },
                enabled = inputText.isNotBlank() && !isGenerating,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isGenerating) JarvisElectricBlue else JarvisGlassElevated
                    )
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = if (inputText.isNotBlank() && !isGenerating) JarvisBlack else JarvisTextMuted
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessageEntity,
    onExecute: (() -> Unit)?
) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) JarvisElectricBlue.copy(alpha = 0.24f) else JarvisGlassSurface)
                .border(
                    width = 1.dp,
                    color = if (isUser) JarvisElectricBlue else JarvisOutline,
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (isUser) "YOU" else "JARVIS",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) JarvisElectricBlue else JarvisCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextPrimary
            )
            if (!isUser && message.isActionable && onExecute != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = onExecute,
                    label = { Text("Execute on Phone") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = JarvisCyan
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = JarvisObsidian,
                        labelColor = JarvisCyan
                    ),
                    border = BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.6f))
                )
            }
        }
    }
}

@Composable
fun ConversationHistoryScreen(
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val taskLogs by viewModel.taskLogs.collectAsStateWithLifecycle()
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Filter chats, logs, or memories...", color = JarvisTextMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JarvisGlassSurface,
                unfocusedContainerColor = JarvisGlassSurface,
                focusedBorderColor = JarvisCyan,
                unfocusedBorderColor = JarvisOutline,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = JarvisObsidian,
            contentColor = JarvisCyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = JarvisCyan
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chats (${chatMessages.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tasks (${taskLogs.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Memory (${memories.size})") })
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    val filtered = chatMessages.reversed().filter {
                        query.isBlank() || it.content.contains(query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyArchiveCard("No chat transcripts stored yet. Start a conversation in AI Chat or Voice Assistant.")
                        }
                    } else {
                        items(filtered, key = { it.id }) { msg ->
                            JarvisGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (msg.role == "user") "USER" else "JARVIS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (msg.role == "user") JarvisElectricBlue else JarvisCyan
                                    )
                                    Text(
                                        text = dateFormat.format(Date(msg.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisTextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = JarvisTextPrimary
                                )
                            }
                        }
                    }
                }

                1 -> {
                    val filtered = taskLogs.filter {
                        query.isBlank() || it.input.contains(query, ignoreCase = true) || it.output.contains(query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyArchiveCard("No automation task logs recorded yet.")
                        }
                    } else {
                        items(filtered, key = { it.uid }) { log ->
                            JarvisGlassCard(
                                onClick = {
                                    val intent = Intent(context, TaskLogDetailsActivity::class.java).apply {
                                        putExtra("log_uid", log.uid)
                                    }
                                    context.startActivity(intent)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "AUTOMATION TASK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisCyan
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = JarvisTextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = log.input,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = JarvisTextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = log.output,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = JarvisTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        JarvisGlowButton(
                            text = "Manage Memory Vault",
                            outlined = true,
                            onClick = {
                                context.startActivity(Intent(context, MemoriesActivity::class.java))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    val filtered = memories.filter {
                        query.isBlank() || it.originalText.contains(query, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            EmptyArchiveCard("No local Room memories stored yet.")
                        }
                    } else {
                        items(filtered, key = { it.id }) { mem ->
                            JarvisGlassCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mem.originalText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = JarvisTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dateFormat.format(Date(mem.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = JarvisTextMuted
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMemory(mem.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete Memory",
                                            tint = JarvisCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArchiveCard(message: String) {
    JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = JarvisTextSecondary
        )
    }
}
