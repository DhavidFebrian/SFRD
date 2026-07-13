package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val no: Int = 0,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceModel: String = android.os.Build.MODEL ?: "Unknown",
    val synced: Boolean = false
) : Serializable
