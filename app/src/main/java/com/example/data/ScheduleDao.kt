package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY tanggal DESC, jam DESC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<Schedule>)

    @Query("DELETE FROM schedules")
    suspend fun clearSchedules()

    @Query("SELECT * FROM schedules")
    suspend fun getSchedulesList(): List<Schedule>

    @Transaction
    suspend fun updateSchedulesTransaction(newSchedules: List<Schedule>, unsyncedSchedules: List<Schedule>) {
        val existingSchedules = getSchedulesList()
        val existingMap = existingSchedules.associateBy {
            if (it.no > 0 && it.sheetName.isNotBlank()) {
                "sheet_${it.sheetName.trim().lowercase()}_no_${it.no}"
            } else {
                "local_${it.idListing.trim().lowercase()}_${it.namaMe.trim().lowercase()}_${it.tanggal.trim().lowercase()}_${it.jam.trim().lowercase()}"
            }
        }

        val updatedNewSchedules = newSchedules.map { schedule ->
            val key = if (schedule.no > 0 && schedule.sheetName.isNotBlank()) {
                "sheet_${schedule.sheetName.trim().lowercase()}_no_${schedule.no}"
            } else {
                "local_${schedule.idListing.trim().lowercase()}_${schedule.namaMe.trim().lowercase()}_${schedule.tanggal.trim().lowercase()}_${schedule.jam.trim().lowercase()}"
            }
            val existing = existingMap[key]
            if (existing != null) {
                schedule.copy(id = existing.id, createdAt = existing.createdAt)
            } else {
                schedule
            }
        }

        val filteredUnsynced = if (unsyncedSchedules.isNotEmpty()) {
            val newUniqueKeys = updatedNewSchedules.map {
                if (it.no > 0 && it.sheetName.isNotBlank()) {
                    "sheet_${it.sheetName.trim().lowercase()}_no_${it.no}"
                } else {
                    "local_${it.idListing.trim().lowercase()}_${it.namaMe.trim().lowercase()}_${it.tanggal.trim().lowercase()}_${it.jam.trim().lowercase()}"
                }
            }.toSet()
            unsyncedSchedules.filter {
                val key = if (it.no > 0 && it.sheetName.isNotBlank()) {
                    "sheet_${it.sheetName.trim().lowercase()}_no_${it.no}"
                } else {
                    "local_${it.idListing.trim().lowercase()}_${it.namaMe.trim().lowercase()}_${it.tanggal.trim().lowercase()}_${it.jam.trim().lowercase()}"
                }
                !newUniqueKeys.contains(key)
            }
        } else {
            emptyList()
        }

        val finalSchedulesToInsertWithoutNonAktif = updatedNewSchedules + filteredUnsynced
        val insertedIds = finalSchedulesToInsertWithoutNonAktif.map { it.id }.toSet()
        val nonAktifSchedulesToKeep = existingSchedules.filter {
            val isNonAktif = it.idListing.isNotBlank() &&
                             it.namaMe.isNotBlank() &&
                             it.lokasi.isNotBlank() &&
                             it.tanggal.isBlank() &&
                             it.jam.isBlank()
            isNonAktif && !insertedIds.contains(it.id)
        }
        val finalSchedulesToInsert = finalSchedulesToInsertWithoutNonAktif + nonAktifSchedulesToKeep
        val idsToKeep = finalSchedulesToInsert.map { it.id }.toSet()
        val fetchedMonths = newSchedules.map { it.sheetName }.filter { it.isNotBlank() }.distinct().toSet()
        val idsToDelete = existingSchedules.filter { existing ->
            !idsToKeep.contains(existing.id) && (fetchedMonths.isEmpty() || fetchedMonths.contains(existing.sheetName))
        }.map { it.id }

        // Optimize: skip database write entirely if the new state matches the existing state exactly
        val existingById = existingSchedules.associateBy { it.id }
        val isIdentical = existingSchedules.size == finalSchedulesToInsert.size &&
                          idsToDelete.isEmpty() &&
                          finalSchedulesToInsert.all { newSched ->
                              val oldSched = existingById[newSched.id]
                              oldSched != null && oldSched == newSched
                          }

        if (isIdentical) {
            return
        }

        if (idsToDelete.isNotEmpty()) {
            deleteSchedulesByIdsInternal(idsToDelete)
        }
        if (finalSchedulesToInsert.isNotEmpty()) {
            insertSchedules(finalSchedulesToInsert)
        }
    }

    @Query("DELETE FROM schedules WHERE id IN (:ids)")
    suspend fun deleteSchedulesByIdsInternal(ids: List<Int>)

    @Delete
    suspend fun deleteSchedule(schedule: Schedule)

    @Query("DELETE FROM schedules WHERE idListing IN (:ids)")
    suspend fun deleteSchedulesByIds(ids: List<String>)

    @Update
    suspend fun updateSchedule(schedule: Schedule)
}
