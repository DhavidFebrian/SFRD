package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EditFotoTaskDao {
    @Query("SELECT * FROM edit_foto_tasks ORDER BY no ASC")
    fun getAllEditFotoTasks(): Flow<List<EditFotoTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditFotoTasks(tasks: List<EditFotoTask>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEditFotoTask(task: EditFotoTask): Long

    @Query("DELETE FROM edit_foto_tasks")
    suspend fun clearEditFotoTasks()

    @Query("SELECT * FROM edit_foto_tasks")
    suspend fun getEditFotoTasksList(): List<EditFotoTask>

    @Transaction
    suspend fun updateEditFotoTasksTransaction(newTasks: List<EditFotoTask>, unsyncedTasks: List<EditFotoTask>) {
        val existingTasks = getEditFotoTasksList()
        val existingMap = existingTasks.associateBy {
            if (it.no > 0) {
                "sheet_no_${it.no}"
            } else {
                "local_${it.idListing.trim().lowercase()}_${it.namaMe.trim().lowercase()}"
            }
        }

        val updatedNewTasks = newTasks.map { task ->
            val key = if (task.no > 0) {
                "sheet_no_${task.no}"
            } else {
                "local_${task.idListing.trim().lowercase()}_${task.namaMe.trim().lowercase()}"
            }
            val existing = existingMap[key]
            if (existing != null) {
                task.copy(id = existing.id, createdAt = existing.createdAt)
            } else {
                task
            }
        }

        val filteredUnsynced = if (unsyncedTasks.isNotEmpty()) {
            val newUniqueKeys = updatedNewTasks.map {
                if (it.no > 0) {
                    "sheet_no_${it.no}"
                } else {
                    "local_${it.idListing.trim().lowercase()}_${it.namaMe.trim().lowercase()}"
                }
            }.toSet()
            unsyncedTasks.filter {
                val key = if (it.no > 0) {
                    "sheet_no_${it.no}"
                } else {
                    "local_${it.idListing.trim().lowercase()}_${it.namaMe.trim().lowercase()}"
                }
                !newUniqueKeys.contains(key)
            }
        } else {
            emptyList()
        }

        val finalTasksToInsert = updatedNewTasks + filteredUnsynced
        val idsToKeep = finalTasksToInsert.map { it.id }.toSet()
        val fetchedMonths = newTasks.map { it.sheetName }.filter { it.isNotBlank() }.distinct().toSet()
        val idsToDelete = existingTasks.filter { existing ->
            !idsToKeep.contains(existing.id) && (fetchedMonths.isEmpty() || fetchedMonths.contains(existing.sheetName))
        }.map { it.id }

        if (idsToDelete.isNotEmpty()) {
            deleteEditFotoTasksByIdsInternal(idsToDelete)
        }
        if (finalTasksToInsert.isNotEmpty()) {
            insertEditFotoTasks(finalTasksToInsert)
        }
    }

    @Query("DELETE FROM edit_foto_tasks WHERE id IN (:ids)")
    suspend fun deleteEditFotoTasksByIdsInternal(ids: List<Int>)

    @Update
    suspend fun updateEditFotoTask(task: EditFotoTask)

    @Delete
    suspend fun deleteEditFotoTask(task: EditFotoTask)
}
