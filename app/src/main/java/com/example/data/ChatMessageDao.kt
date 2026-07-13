package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Update
    suspend fun updateMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM chat_messages WHERE synced = 1")
    suspend fun clearSyncedMessages()

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Transaction
    suspend fun updateChatMessagesTransaction(newMessages: List<ChatMessage>, unsyncedMessages: List<ChatMessage>) {
        clearSyncedMessages()
        if (newMessages.isNotEmpty()) {
            insertMessages(newMessages)
        }
        if (unsyncedMessages.isNotEmpty()) {
            val existingKeys = newMessages.map { "${it.senderName.trim()}_${it.message.trim()}_${it.timestamp}" }.toSet()
            val filteredUnsynced = unsyncedMessages.filter {
                val key = "${it.senderName.trim()}_${it.message.trim()}_${it.timestamp}"
                !existingKeys.contains(key)
            }
            if (filteredUnsynced.isNotEmpty()) {
                insertMessages(filteredUnsynced)
            }
        }
    }
}
