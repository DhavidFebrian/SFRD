package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentContactDao {
    @Query("SELECT * FROM agent_contacts ORDER BY nameKey ASC")
    fun getAllAgentContactsFlow(): Flow<List<AgentContactEntity>>

    @Query("SELECT * FROM agent_contacts ORDER BY nameKey ASC")
    suspend fun getAllAgentContacts(): List<AgentContactEntity>

    @Query("SELECT * FROM agent_contacts WHERE nameKey = :nameKey LIMIT 1")
    suspend fun getAgentContactByName(nameKey: String): AgentContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgentContact(contact: AgentContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAgentContacts(contacts: List<AgentContactEntity>)

    @Update
    suspend fun updateAgentContact(contact: AgentContactEntity)

    @Query("DELETE FROM agent_contacts WHERE nameKey = :nameKey")
    suspend fun deleteAgentContact(nameKey: String)

    @Query("DELETE FROM agent_contacts")
    suspend fun deleteAllAgentContacts()
}
