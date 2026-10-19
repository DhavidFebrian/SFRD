package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScheduleDaoSyncTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ScheduleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.scheduleDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `successful Foto Ulang sync removes stale remote rows and keeps unsynced local row`() = runBlocking {
        val cachedRows = (1..48).map { number -> fotoUlang(number, synced = true) }
        dao.insertSchedules(cachedRows)

        val pendingLocal = fotoUlang(999, synced = false).copy(idListing = "LOCAL-PENDING")
        dao.insertSchedule(pendingLocal)
        val unsynced = dao.getSchedulesList().filter { !it.synced }

        val currentRemoteRows = (1..37).map { number -> fotoUlang(number, synced = true) }
        dao.updateSchedulesTransaction(
            newSchedules = currentRemoteRows,
            unsyncedSchedules = unsynced,
            fetchedSheetNames = setOf("Foto Ulang")
        )

        val result = dao.getSchedulesList()
        assertEquals(38, result.size)
        assertEquals(37, result.count { it.synced && it.sheetName == "Foto Ulang" })
        assertTrue(result.any { it.idListing == "LOCAL-PENDING" && !it.synced })
        assertTrue(result.none { it.synced && it.no in 38..48 })
    }

    private fun fotoUlang(number: Int, synced: Boolean) = Schedule(
        no = number,
        idListing = number.toString(),
        namaMe = "ME $number",
        lokasi = "Lokasi $number",
        tanggal = "",
        jam = "",
        staff = "",
        type = "Foto Ulang",
        status = "Pending",
        synced = synced,
        sheetName = "Foto Ulang"
    )
}
