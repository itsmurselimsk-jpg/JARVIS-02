package com.blurr.voice.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val role: String, // "user" or "model"
    val content: String,
    val languageTag: String = "en",
    val isActionable: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
