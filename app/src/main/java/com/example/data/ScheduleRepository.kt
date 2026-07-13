package com.example.data

import com.example.network.SheetSchedule
import com.example.network.SheetsApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val editFotoTaskDao: EditFotoTaskDao,
    private val apiService: SheetsApiService,
    private val preferenceManager: PreferenceManager
) {
    private val syncMutex = Mutex()
    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    val allEditFotoTasks: Flow<List<EditFotoTask>> = editFotoTaskDao.getAllEditFotoTasks()

    private fun getAppsScriptUrlForMonth(monthName: String): String {
        val baseUrl = preferenceManager.appsScriptUrl
        if (baseUrl.isBlank()) return ""
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val encodedSheet = try {
            java.net.URLEncoder.encode(monthName, "UTF-8")
        } catch (e: Exception) {
            "Juni%202026"
        }
        return "$baseUrl${separator}sheetName=$encodedSheet"
    }

    private fun getAppsScriptUrl(): String {
        return getAppsScriptUrlForMonth(preferenceManager.selectedMonth)
    }

    private fun getAppsScriptUrlForDate(dateStr: String): String {
        return getAppsScriptUrlForMonth(getSheetNameForDate(dateStr))
    }

    private fun getSheetNameForDate(dateStr: String): String {
        val fallback = preferenceManager.selectedMonth
        if (dateStr.length < 7) return fallback
        val yearPart = dateStr.substring(0, 4) // "2026"
        val monthPart = dateStr.substring(5, 7) // "06"
        
        val monthName = when (monthPart) {
            "01" -> "Januari"
            "02" -> "Februari"
            "03" -> "Maret"
            "04" -> "April"
            "05" -> "Mei"
            "06" -> "Juni"
            "07" -> "Juli"
            "08" -> "Agustus"
            "09" -> "September"
            "10" -> "Oktober"
            "11" -> "November"
            "12" -> "Desember"
            else -> ""
        }
        if (monthName.isBlank()) return fallback
        return "$monthName $yearPart"
    }

    // Seeds beautiful mock data if database is totally empty, so they see a gorgeous dashboard on first launch
    suspend fun seedMockDataIfEmpty() {
        // Hapus data mock bawaan jika ada dari peluncuran aplikasi sebelumnya
        val mockIds = listOf("L-0912", "L-1102", "L-2241", "L-1133", "L-3412", "L-2591")
        scheduleDao.deleteSchedulesByIds(mockIds)
        preferenceManager.isFirstLaunch = false
    }

    // Sync from Google Sheets API with robust filtering and backward compatibility across all months
    suspend fun syncFromGoogleSheets(): Result<Unit> = syncMutex.withLock {
        val monthsToSync = listOf("Juni 2026", "Juli 2026")
        val schedulesList = mutableListOf<Schedule>()
        val editFotoTaskList = mutableListOf<EditFotoTask>()
        
        var anySuccess = false
        var lastError: Exception? = null

        coroutineScope {
            val scope = this
            val deferreds = monthsToSync.map { month ->
                val url = getAppsScriptUrlForMonth(month)
                scope.async {
                    if (url.isBlank()) return@async null
                    try {
                        val responseBody = apiService.getSchedules(url)
                        val jsonString = responseBody.string().trim()
                        Pair(month, jsonString)
                    } catch (e: Exception) {
                        lastError = e
                        null
                    }
                }
            }

            val results = deferreds.mapNotNull { it.await() }

            val moshi = com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()

            for ((month, jsonString) in results) {
                try {
                    if (jsonString.startsWith("{")) {
                        // Combined response format
                        val adapter = moshi.adapter(com.example.network.CombinedSheetResponse::class.java)
                        val response = adapter.fromJson(jsonString)
                        if (response != null) {
                            response.schedules.forEach {
                                schedulesList.add(mapSheetScheduleToSchedule(it, month))
                            }
                            response.editFotoTasks.forEach {
                                editFotoTaskList.add(mapSheetEditFotoToEntity(it, month))
                            }
                        }
                    } else if (jsonString.startsWith("[")) {
                        // Legacy response format (only list of schedules)
                        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.network.SheetSchedule::class.java)
                        val adapter = moshi.adapter<List<com.example.network.SheetSchedule>>(type)
                        val list = adapter.fromJson(jsonString)
                        list?.forEach {
                            schedulesList.add(mapSheetScheduleToSchedule(it, month))
                        }
                    }
                    anySuccess = true
                } catch (e: Exception) {
                    lastError = e
                }
            }
        }

        if (!anySuccess && lastError != null) {
            return Result.failure(lastError!!)
        }

        // Sync Schedules in transaction
        val unsyncedSchedules = scheduleDao.getAllSchedules().first().filter { !it.synced }
        scheduleDao.updateSchedulesTransaction(schedulesList, unsyncedSchedules)

        // Sync Edit Foto Tasks in transaction
        val unsyncedTasks = editFotoTaskDao.getAllEditFotoTasks().first().filter { !it.synced }
        editFotoTaskDao.updateEditFotoTasksTransaction(editFotoTaskList, unsyncedTasks)

        return Result.success(Unit)
    }

    private fun mapSheetScheduleToSchedule(it: com.example.network.SheetSchedule, defaultMonth: String = ""): Schedule {
        val finalIdListing = it.idListing.trim()
        val finalNamaMe = it.namaMe.trim()
        val finalLokasi = it.lokasi.trim()
        val finalStaff = it.staff.trim()
        val finalTanggal = normalizeDate(it.tanggal)
        val finalJam = it.jam.trim()
        val finalType = it.type.trim()
        val finalStatus = it.status.trim()
        val finalSheetName = if (it.sheetName.isNotBlank()) it.sheetName.trim() else defaultMonth

        return Schedule(
            no = it.no,
            idListing = finalIdListing,
            namaMe = finalNamaMe,
            lokasi = finalLokasi,
            tanggal = finalTanggal,
            jam = finalJam,
            staff = finalStaff,
            type = finalType,
            status = finalStatus,
            synced = true,
            sheetName = finalSheetName
        )
    }

    private fun mapSheetEditFotoToEntity(it: com.example.network.SheetEditFotoTask, monthName: String = ""): EditFotoTask {
        return EditFotoTask(
            no = it.no,
            idListing = it.idListing.trim(),
            namaMe = it.namaMe.trim(),
            postingIg = it.postingIg,
            jadwalPosting = it.jadwalPosting.trim(),
            editNotes = it.editNotes.trim(),
            done = it.done,
            judul = it.judul.trim(),
            source = it.source.trim(),
            synced = true,
            sheetName = monthName
        )
    }

    private fun parseTimeFromOldString(raw: String): String {
        if (raw.isBlank()) return ""
        try {
            val parts = raw.split(" ")
            for (p in parts) {
                if (p.contains(":")) {
                    val subParts = p.split(":")
                    if (subParts.size >= 2) {
                        return "${subParts[0]}:${subParts[1]}"
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return raw.trim()
    }

    // Insert schedule (saves locally, then attempts remote sync if configured)
    suspend fun addSchedule(schedule: Schedule): Result<Unit> = syncMutex.withLock {
        val finalSchedule = if (schedule.sheetName.isBlank()) {
            schedule.copy(sheetName = getSheetNameForDate(schedule.tanggal))
        } else {
            schedule
        }
        // 1. Insert local first for instant UI response
        val insertedId = scheduleDao.insertSchedule(finalSchedule)
        val insertedSchedule = finalSchedule.copy(id = insertedId.toInt())

        val url = getAppsScriptUrlForDate(finalSchedule.tanggal)
        if (url.isBlank()) {
            // No sync but saved locally, return warning-as-success
            return Result.success(Unit)
        }

        // 2. Perform remote posting to Sheets API
        return try {
            val sheetModel = SheetSchedule(
                idListing = finalSchedule.idListing,
                namaMe = finalSchedule.namaMe,
                lokasi = finalSchedule.lokasi,
                tanggal = finalSchedule.tanggal,
                jam = finalSchedule.jam,
                staff = finalSchedule.staff,
                type = finalSchedule.type,
                status = finalSchedule.status,
                sheetName = getSheetNameForDate(finalSchedule.tanggal)
            )
            val result = apiService.addSchedule(url, sheetModel)
            if (result.status.lowercase() == "success" || result.status.lowercase() == "ok" || result.message.lowercase() == "success") {
                // Update local status as synced and update 'no'
                val remoteNo = if (result.row != null && result.row > 4) result.row - 4 else 0
                scheduleDao.updateSchedule(insertedSchedule.copy(synced = true, no = remoteNo))
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal mengirim data ke Google Sheets" }))
            }
        } catch (e: Exception) {
            // Keep local as unsynced so they can re-try later
            Result.failure(e)
        }
    }

    // Update existing schedule locally and remotely on Google Sheets
    suspend fun updateSchedule(schedule: Schedule, originalSchedule: Schedule): Result<Unit> = syncMutex.withLock {
        val finalSchedule = if (schedule.sheetName.isBlank()) {
            schedule.copy(sheetName = getSheetNameForDate(schedule.tanggal))
        } else {
            schedule
        }
        // 1. Update locally
        scheduleDao.updateSchedule(finalSchedule)

        val url = getAppsScriptUrlForDate(finalSchedule.tanggal)
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Sync edit to Google Sheets
        return try {
            val sheetModel = SheetSchedule(
                no = finalSchedule.no,
                idListing = finalSchedule.idListing,
                namaMe = finalSchedule.namaMe,
                lokasi = finalSchedule.lokasi,
                tanggal = finalSchedule.tanggal,
                jam = finalSchedule.jam,
                staff = finalSchedule.staff,
                type = finalSchedule.type,
                status = finalSchedule.status,
                action = "edit",
                originalIdListing = originalSchedule.idListing,
                originalNamaMe = originalSchedule.namaMe,
                originalTanggal = originalSchedule.tanggal,
                originalJam = originalSchedule.jam,
                sheetName = getSheetNameForDate(finalSchedule.tanggal)
            )
            val result = apiService.addSchedule(url, sheetModel)
            if (result.status.lowercase() == "success" || result.status.lowercase() == "ok" || result.message.lowercase() == "success") {
                scheduleDao.updateSchedule(finalSchedule.copy(synced = true))
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal memperbarui data di Google Sheets" }))
            }
        } catch (e: Exception) {
            // Mark as unsynced if failed
            scheduleDao.updateSchedule(finalSchedule.copy(synced = false))
            Result.failure(e)
        }
    }

    // Update EditFotoTask locally and remotely
    suspend fun updateEditFotoTask(task: EditFotoTask): Result<Unit> = syncMutex.withLock {
        // 1. Save locally first
        editFotoTaskDao.updateEditFotoTask(task)

        val url = getAppsScriptUrlForDate(task.jadwalPosting)
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Sync to Sheets remotely
        return try {
            val request = com.example.network.SheetEditFotoPostRequest(
                action = "edit_foto",
                no = task.no,
                idListing = task.idListing,
                namaMe = task.namaMe,
                postingIg = task.postingIg,
                jadwalPosting = normalizeDateToDMY(task.jadwalPosting),
                editNotes = task.editNotes,
                done = task.done,
                judul = task.judul,
                source = task.source,
                sheetName = getSheetNameForDate(task.jadwalPosting)
            )
            val result = apiService.updateEditFotoTask(url, request)
            if (result.status.lowercase() == "success" || result.status.lowercase() == "ok" || result.message.lowercase() == "success") {
                editFotoTaskDao.updateEditFotoTask(task.copy(synced = true))
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal memperbarui Task di Google Sheets" }))
            }
        } catch (e: Exception) {
            editFotoTaskDao.updateEditFotoTask(task.copy(synced = false))
            Result.failure(e)
        }
    }

    // Resync pending schedules
    suspend fun syncPendingSchedules(): Result<Int> = syncMutex.withLock {
        val baseUrl = preferenceManager.appsScriptUrl
        if (baseUrl.isBlank()) {
            return Result.failure(Exception("Apps Script URL belum diatur"))
        }

        val all = scheduleDao.getAllSchedules().first()
        val pending = all.filter { !it.synced }
        var successCount = 0

        for (item in pending) {
            try {
                val itemUrl = getAppsScriptUrlForDate(item.tanggal)
                val sheetModel = SheetSchedule(
                    no = item.no,
                    idListing = item.idListing,
                    namaMe = item.namaMe,
                    lokasi = item.lokasi,
                    tanggal = item.tanggal,
                    jam = item.jam,
                    staff = item.staff,
                    type = item.type,
                    status = item.status,
                    action = if (item.no > 0) "edit" else "add",
                    originalIdListing = item.idListing,
                    originalNamaMe = item.namaMe,
                    originalTanggal = item.tanggal,
                    originalJam = item.jam,
                    sheetName = getSheetNameForDate(item.tanggal)
                )
                val result = apiService.addSchedule(itemUrl, sheetModel)
                val statusLow = result.status.lowercase()
                val msgLow = result.message.lowercase()
                if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                    val remoteNo = if (result.row != null && result.row > 4) result.row - 4 else item.no
                    scheduleDao.updateSchedule(item.copy(synced = true, no = remoteNo))
                    successCount++
                }
            } catch (e: Exception) {
                // Ignore and continue
            }
        }

        // Also resync pending edit foto tasks
        val pendingTasks = editFotoTaskDao.getAllEditFotoTasks().first().filter { !it.synced }
        for (item in pendingTasks) {
            try {
                val itemUrl = getAppsScriptUrlForDate(item.jadwalPosting)
                val request = com.example.network.SheetEditFotoPostRequest(
                    action = "edit_foto",
                    no = item.no,
                    idListing = item.idListing,
                    namaMe = item.namaMe,
                    postingIg = item.postingIg,
                    jadwalPosting = normalizeDateToDMY(item.jadwalPosting),
                    editNotes = item.editNotes,
                    done = item.done,
                    judul = item.judul,
                    source = item.source,
                    sheetName = getSheetNameForDate(item.jadwalPosting)
                )
                val result = apiService.updateEditFotoTask(itemUrl, request)
                val statusLow = result.status.lowercase()
                val msgLow = result.message.lowercase()
                if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                    editFotoTaskDao.updateEditFotoTask(item.copy(synced = true))
                    successCount++
                }
            } catch (e: Exception) {
                // Ignore and continue
            }
        }

        return Result.success(successCount)
    }

    // Clear all tables locally
    suspend fun clearLocal() {
        preferenceManager.isFirstLaunch = false
        scheduleDao.clearSchedules()
        editFotoTaskDao.clearEditFotoTasks()
    }

    // Delete a specific schedule (locally and remotely if synced)
    suspend fun deleteSchedule(schedule: Schedule): Result<Unit> = syncMutex.withLock {
        // 1. Delete locally first
        scheduleDao.deleteSchedule(schedule)

        val url = getAppsScriptUrlForDate(schedule.tanggal)
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Clear from Google Sheets remotely
        return try {
            val sheetModel = SheetSchedule(
                idListing = schedule.idListing,
                namaMe = schedule.namaMe,
                lokasi = schedule.lokasi,
                tanggal = schedule.tanggal,
                jam = schedule.jam,
                staff = schedule.staff,
                type = schedule.type,
                status = schedule.status,
                action = "delete",
                sheetName = getSheetNameForDate(schedule.tanggal)
            )
            val result = apiService.addSchedule(url, sheetModel)
            val statusLow = result.status.lowercase()
            val msgLow = result.message.lowercase()
            if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal menghapus data di Google Sheets secara online" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete a specific schedule row via deleteRow action in Google Sheets
    suspend fun deleteRow(schedule: Schedule): Result<Unit> = syncMutex.withLock {
        // 1. Delete locally first
        scheduleDao.deleteSchedule(schedule)

        val url = getAppsScriptUrlForDate(schedule.tanggal)
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Clear from Google Sheets remotely by triggering delete action
        return try {
            val sheetModel = SheetSchedule(
                no = schedule.no,
                idListing = schedule.idListing,
                namaMe = schedule.namaMe,
                lokasi = schedule.lokasi,
                tanggal = schedule.tanggal,
                jam = schedule.jam,
                staff = schedule.staff,
                type = schedule.type,
                status = schedule.status,
                action = "delete",
                sheetName = getSheetNameForDate(schedule.tanggal)
            )
            val result = apiService.addSchedule(url, sheetModel)
            val statusLow = result.status.lowercase()
            val msgLow = result.message.lowercase()
            if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal menghapus baris di Google Sheets secara online" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Delete a specific EditFotoTask (locally and remotely)
    suspend fun deleteEditFotoTask(task: EditFotoTask): Result<Unit> = syncMutex.withLock {
        // 1. Delete locally first
        editFotoTaskDao.deleteEditFotoTask(task)

        val url = getAppsScriptUrlForDate(task.jadwalPosting)
        if (url.isBlank()) {
            return Result.success(Unit)
        }

        // 2. Clear from Google Sheets remotely
        return try {
            val request = com.example.network.SheetEditFotoPostRequest(
                action = "delete_foto",
                no = task.no,
                idListing = task.idListing,
                namaMe = task.namaMe,
                postingIg = task.postingIg,
                jadwalPosting = task.jadwalPosting,
                editNotes = task.editNotes,
                done = task.done,
                judul = task.judul,
                source = task.source,
                sheetName = getSheetNameForDate(task.jadwalPosting)
            )
            val result = apiService.updateEditFotoTask(url, request)
            val statusLow = result.status.lowercase()
            val msgLow = result.message.lowercase()
            if (statusLow == "success" || statusLow == "ok" || msgLow == "success" || msgLow == "ok") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(result.message.ifBlank { "Gagal menghapus task Edit Foto di Google Sheets secara online" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

fun normalizeDate(dateStr: String): String {
    val sanitized = dateStr.replace('\u00A0', ' ').replace('\u2007', ' ').replace('\u202F', ' ').trim()
    if (sanitized.isEmpty()) return ""
    
    // 1. Remove leading day name and standard prefix words
    var cleaned = sanitized.replace(Regex("^(Senin|Selasa|Rabu|Kamis|Jum'at|Jumat|Sabtu|Minggu|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|Tanggal|Hari)[,\\s:]*", RegexOption.IGNORE_CASE), "").trim()
    
    // Strip any remaining leading non-digit characters except maybe if it's a letter (dates always start with digit unless it's day name which we already removed)
    cleaned = cleaned.replace(Regex("^[,\\s:-]+"), "").trim()
    
    // Normalize any spaces around slashes or dashes (e.g. "25 / 06 / 2026" -> "25/06/2026")
    cleaned = cleaned.replace(Regex("\\s*[/-]\\s*"), "/")
    
    // 2. Check if starts with 4-digit year (yyyy-MM-dd)
    val ymdMatch = Regex("^(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})").find(cleaned)
    if (ymdMatch != null) {
        try {
            val y = ymdMatch.groupValues[1]
            val m = String.format(java.util.Locale.US, "%02d", ymdMatch.groupValues[2].toInt())
            val d = String.format(java.util.Locale.US, "%02d", ymdMatch.groupValues[3].toInt())
            return "$y-$m-$d"
        } catch (e: Exception) {}
    }

    // 3. Try parsing dd/MM/yyyy with 4-digit year anywhere in the string
    val dmy4Match = Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})").find(cleaned)
    if (dmy4Match != null) {
        try {
            val d = String.format(java.util.Locale.US, "%02d", dmy4Match.groupValues[1].toInt())
            val m = String.format(java.util.Locale.US, "%02d", dmy4Match.groupValues[2].toInt())
            val y = dmy4Match.groupValues[3]
            return "$y-$m-$d"
        } catch (e: Exception) {}
    }

    // 4. Try parsing dd/MM/yy with 2-digit year anywhere in the string
    val dmy2Match = Regex("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2})").find(cleaned)
    if (dmy2Match != null) {
        try {
            val d = String.format(java.util.Locale.US, "%02d", dmy2Match.groupValues[1].toInt())
            val m = String.format(java.util.Locale.US, "%02d", dmy2Match.groupValues[2].toInt())
            var y = dmy2Match.groupValues[3].toInt()
            y += if (y < 50) 2000 else 1900
            return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m.toInt(), d.toInt())
        } catch (e: Exception) {}
    }

    // 5. Try parsing written date with 4-digit year (e.g. "25 Juni 2026", "25-Jun-2026")
    val written4Match = Regex("(\\d{1,2})[\\s-/]+([a-zA-Z'’]+)[\\s-/]+(\\d{4})").find(cleaned)
    if (written4Match != null) {
        try {
            val d = String.format(java.util.Locale.US, "%02d", written4Match.groupValues[1].toInt())
            val monthStr = written4Match.groupValues[2].lowercase()
            val y = written4Match.groupValues[3]
            val m = when {
                monthStr.startsWith("jan") -> "01"
                monthStr.startsWith("feb") -> "02"
                monthStr.startsWith("mar") -> "03"
                monthStr.startsWith("apr") -> "04"
                monthStr.startsWith("mei") || monthStr.startsWith("may") -> "05"
                monthStr.startsWith("jun") -> "06"
                monthStr.startsWith("jul") -> "07"
                monthStr.startsWith("agu") || monthStr.startsWith("aug") -> "08"
                monthStr.startsWith("sep") -> "09"
                monthStr.startsWith("okt") || monthStr.startsWith("oct") -> "10"
                monthStr.startsWith("nov") -> "11"
                monthStr.startsWith("des") || monthStr.startsWith("dec") -> "12"
                else -> "00"
            }
            if (m != "00") {
                return "$y-$m-$d"
            }
        } catch (e: Exception) {}
    }

    // 6. Try parsing written date with 2-digit year (e.g. "25 Juni 26", "25-Jun-26")
    val written2Match = Regex("(\\d{1,2})[\\s-/]+([a-zA-Z'’]+)[\\s-/]+(\\d{2})").find(cleaned)
    if (written2Match != null) {
        try {
            val d = String.format(java.util.Locale.US, "%02d", written2Match.groupValues[1].toInt())
            val monthStr = written2Match.groupValues[2].lowercase()
            var y = written2Match.groupValues[3].toInt()
            y += if (y < 50) 2000 else 1900
            val m = when {
                monthStr.startsWith("jan") -> "01"
                monthStr.startsWith("feb") -> "02"
                monthStr.startsWith("mar") -> "03"
                monthStr.startsWith("apr") -> "04"
                monthStr.startsWith("mei") || monthStr.startsWith("may") -> "05"
                monthStr.startsWith("jun") -> "06"
                monthStr.startsWith("jul") -> "07"
                monthStr.startsWith("agu") || monthStr.startsWith("aug") -> "08"
                monthStr.startsWith("sep") -> "09"
                monthStr.startsWith("okt") || monthStr.startsWith("oct") -> "10"
                monthStr.startsWith("nov") -> "11"
                monthStr.startsWith("des") || monthStr.startsWith("dec") -> "12"
                else -> "00"
            }
            if (m != "00") {
                return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m.toInt(), d.toInt())
            }
        } catch (e: Exception) {}
    }

    // 7. Try parsing written day-month without year (e.g. "25 Juni" or "25-Jun") - default to current year 2026
    val dayMonthWrittenMatch = Regex("(\\d{1,2})[\\s-/]+([a-zA-Z'’]+)").find(cleaned)
    if (dayMonthWrittenMatch != null) {
        try {
            val d = String.format(java.util.Locale.US, "%02d", dayMonthWrittenMatch.groupValues[1].toInt())
            val monthStr = dayMonthWrittenMatch.groupValues[2].lowercase()
            val y = 2026
            val m = when {
                monthStr.startsWith("jan") -> "01"
                monthStr.startsWith("feb") -> "02"
                monthStr.startsWith("mar") -> "03"
                monthStr.startsWith("apr") -> "04"
                monthStr.startsWith("mei") || monthStr.startsWith("may") -> "05"
                monthStr.startsWith("jun") -> "06"
                monthStr.startsWith("jul") -> "07"
                monthStr.startsWith("agu") || monthStr.startsWith("aug") -> "08"
                monthStr.startsWith("sep") -> "09"
                monthStr.startsWith("okt") || monthStr.startsWith("oct") -> "10"
                monthStr.startsWith("nov") -> "11"
                monthStr.startsWith("des") || monthStr.startsWith("dec") -> "12"
                else -> "00"
            }
            if (m != "00") {
                return "$y-$m-$d"
            }
        } catch (e: Exception) {}
    }

    // 8. Try parsing slash/dash day-month without year (e.g. "25/06") - default to current year 2026
    val dayMonthSlashMatch = Regex("(\\d{1,2})[/-](\\d{1,2})").find(cleaned)
    if (dayMonthSlashMatch != null) {
        try {
            val d = String.format(java.util.Locale.US, "%02d", dayMonthSlashMatch.groupValues[1].toInt())
            val m = String.format(java.util.Locale.US, "%02d", dayMonthSlashMatch.groupValues[2].toInt())
            val y = 2026
            return "$y-$m-$d"
        } catch (e: Exception) {}
    }

    // 9. Standard SimpleDateFormat fallback parser for other formats
    val formats = listOf(
        Pair("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US),
        Pair("yyyy-MM-dd HH:mm:ss", java.util.Locale.US),
        Pair("dd/MM/yyyy", java.util.Locale.US),
        Pair("d/M/yyyy", java.util.Locale.US),
        Pair("dd-MM-yyyy", java.util.Locale.US),
        Pair("d-M-yyyy", java.util.Locale.US),
        Pair("dd MMMM yyyy", java.util.Locale("id", "ID")),
        Pair("d MMMM yyyy", java.util.Locale("id", "ID")),
        Pair("dd MMM yyyy", java.util.Locale("id", "ID")),
        Pair("d MMM yyyy", java.util.Locale("id", "ID")),
        Pair("dd MMMM yyyy", java.util.Locale.US),
        Pair("d MMMM yyyy", java.util.Locale.US),
        Pair("dd MMM yyyy", java.util.Locale.US),
        Pair("d MMM yyyy", java.util.Locale.US),
        Pair("dd-MMM-yyyy", java.util.Locale.US),
        Pair("yyyy/MM/dd", java.util.Locale.US),
        Pair("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.US)
    )
    for (f in formats) {
        try {
            val sdf = java.text.SimpleDateFormat(f.first, f.second)
            val parsed = sdf.parse(cleaned)
            if (parsed != null) {
                return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(parsed)
            }
        } catch (e: Exception) {}
    }

    return sanitized
}

fun normalizeDateToDMY(dateStr: String): String {
    val normalizedYMD = normalizeDate(dateStr)
    if (normalizedYMD.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
        val parts = normalizedYMD.split("-")
        return "${parts[2]}/${parts[1]}/${parts[0]}"
    }
    return normalizedYMD
}
