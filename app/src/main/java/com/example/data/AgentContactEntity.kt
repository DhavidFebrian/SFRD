package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "agent_contacts")
data class AgentContactEntity(
    @PrimaryKey val nameKey: String, // e.g. "donny", lowercased
    val displayName: String,         // e.g. "Donny"
    val phone: String,
    val email: String,
    val instagram: String,
    val avatarUrl: String = ""       // URL or local URI of the photo
) : Serializable
