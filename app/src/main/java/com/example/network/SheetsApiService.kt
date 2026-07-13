package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class SheetSchedule(
    @Json(name = "no") val no: Int = 0,
    @Json(name = "idListing") val idListing: String = "",
    @Json(name = "namaMe") val namaMe: String = "",
    @Json(name = "lokasi") val lokasi: String = "",
    @Json(name = "tanggal") val tanggal: String = "",
    @Json(name = "jam") val jam: String = "",
    @Json(name = "staff") val staff: String = "",
    @Json(name = "type") val type: String = "Foto",
    @Json(name = "status") val status: String = "Pending",
    @Json(name = "action") val action: String = "add",
    @Json(name = "originalIdListing") val originalIdListing: String = "",
    @Json(name = "originalNamaMe") val originalNamaMe: String = "",
    @Json(name = "originalTanggal") val originalTanggal: String = "",
    @Json(name = "originalJam") val originalJam: String = "",
    @Json(name = "sheetName") val sheetName: String = ""
)

@JsonClass(generateAdapter = true)
data class SheetEditFotoTask(
    @Json(name = "no") val no: Int = 0,
    @Json(name = "idListing") val idListing: String = "",
    @Json(name = "namaMe") val namaMe: String = "",
    @Json(name = "postingIg") val postingIg: Boolean = false,
    @Json(name = "jadwalPosting") val jadwalPosting: String = "",
    @Json(name = "editNotes") val editNotes: String = "",
    @Json(name = "done") val done: Boolean = false,
    @Json(name = "judul") val judul: String = "",
    @Json(name = "source") val source: String = ""
)

@JsonClass(generateAdapter = true)
data class CombinedSheetResponse(
    @Json(name = "schedules") val schedules: List<SheetSchedule> = emptyList(),
    @Json(name = "editFotoTasks") val editFotoTasks: List<SheetEditFotoTask> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SheetEditFotoPostRequest(
    @Json(name = "action") val action: String = "edit_foto",
    @Json(name = "no") val no: Int,
    @Json(name = "idListing") val idListing: String,
    @Json(name = "namaMe") val namaMe: String,
    @Json(name = "postingIg") val postingIg: Boolean,
    @Json(name = "jadwalPosting") val jadwalPosting: String,
    @Json(name = "editNotes") val editNotes: String,
    @Json(name = "done") val done: Boolean,
    @Json(name = "judul") val judul: String,
    @Json(name = "source") val source: String,
    @Json(name = "sheetName") val sheetName: String = ""
)

@JsonClass(generateAdapter = true)
data class GeneralResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "row") val row: Int? = null
)

interface SheetsApiService {
    @GET
    suspend fun getSchedules(@Url url: String): ResponseBody

    @POST
    suspend fun addSchedule(@Url url: String, @Body schedule: SheetSchedule): GeneralResponse

    @POST
    suspend fun updateEditFotoTask(@Url url: String, @Body request: SheetEditFotoPostRequest): GeneralResponse

    @GET
    suspend fun getMeetingListings(@Url url: String): MeetingListingsResponse

    @POST
    suspend fun addMeetingListing(@Url url: String, @Body request: AddMeetingListingRequest): GeneralResponse

    @POST
    suspend fun updateMeetingIgPost(@Url url: String, @Body request: UpdateMeetingIgPostRequest): GeneralResponse

    @POST
    suspend fun updateMeetingDetails(@Url url: String, @Body request: UpdateMeetingDetailsRequest): GeneralResponse

    @POST
    suspend fun updateMeetingSchedule(@Url url: String, @Body request: UpdateMeetingScheduleRequest): GeneralResponse

    @GET
    suspend fun getAbsensiMeeting(@Url url: String): AbsensiResponse

    @POST
    suspend fun updateAbsensiMeeting(@Url url: String, @Body request: UpdateAbsensiRequest): UpdateAbsensiResponse

    @GET
    suspend fun getYearlyIgPostingHistory(@Url url: String): YearlyIgHistoryResponse
}

@JsonClass(generateAdapter = true)
data class YearlyIgHistoryResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "history") val history: Map<String, List<String>> = emptyMap()
)


@JsonClass(generateAdapter = true)
data class AbsensiDate(
    @Json(name = "colIndex") val colIndex: Int,
    @Json(name = "label") val label: String
)

@JsonClass(generateAdapter = true)
data class AbsensiMarketing(
    @Json(name = "row") val row: Int,
    @Json(name = "name") val name: String,
    @Json(name = "attendance") val attendance: List<Boolean> = emptyList(),
    @Json(name = "totalHadirBulan") val totalHadirBulan: Int = 0
)

@JsonClass(generateAdapter = true)
data class AbsensiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "monthIndex") val monthIndex: Int = 0,
    @Json(name = "dates") val dates: List<AbsensiDate> = emptyList(),
    @Json(name = "marketingList") val marketingList: List<AbsensiMarketing> = emptyList(),
    @Json(name = "dateTotals") val dateTotals: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateAbsensiRequest(
    @Json(name = "action") val action: String = "update_absensi_meeting",
    @Json(name = "row") val row: Int,
    @Json(name = "col") val col: Int,
    @Json(name = "present") val present: Boolean
)

@JsonClass(generateAdapter = true)
data class UpdateAbsensiResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "newRowTotal") val newRowTotal: Any? = null,
    @Json(name = "newColTotal") val newColTotal: Any? = null
)

@JsonClass(generateAdapter = true)
data class UpdateMeetingDetailsRequest(
    @Json(name = "action") val action: String = "update_weekly_meeting_details",
    @Json(name = "sheetName") val sheetName: String,
    @Json(name = "date") val date: String,
    @Json(name = "row") val row: Int,
    @Json(name = "colIndex") val colIndex: Int,
    @Json(name = "idListing") val idListing: String,
    @Json(name = "namaMe") val namaMe: String,
    @Json(name = "keterangan") val keterangan: String,
    @Json(name = "catatan") val catatan: String
)

@JsonClass(generateAdapter = true)
data class AddMeetingListingRequest(
    @Json(name = "action") val action: String = "add_weekly_meeting_listing",
    @Json(name = "sheetName") val sheetName: String,
    @Json(name = "date") val date: String,
    @Json(name = "idListing") val idListing: String,
    @Json(name = "namaMe") val namaMe: String,
    @Json(name = "keterangan") val keterangan: String,
    @Json(name = "catatan") val catatan: String
)

@JsonClass(generateAdapter = true)
data class UpdateMeetingIgPostRequest(
    @Json(name = "action") val action: String = "update_weekly_meeting_posting_ig",
    @Json(name = "sheetName") val sheetName: String,
    @Json(name = "date") val date: String,
    @Json(name = "row") val row: Int,
    @Json(name = "colIndex") val colIndex: Int,
    @Json(name = "postingIg") val postingIg: Boolean
)

@JsonClass(generateAdapter = true)
data class UpdateMeetingScheduleRequest(
    @Json(name = "action") val action: String = "update_weekly_meeting_schedule",
    @Json(name = "sheetName") val sheetName: String,
    @Json(name = "date") val date: String,
    @Json(name = "row") val row: Int,
    @Json(name = "colIndex") val colIndex: Int,
    @Json(name = "jadwalPosting") val jadwalPosting: String
)

@JsonClass(generateAdapter = true)
data class MeetingListing(
    @Json(name = "no") val no: Int = 0,
    @Json(name = "date") val date: String = "",
    @Json(name = "colIndex") val colIndex: Int = 0,
    @Json(name = "idListing") val idListing: String = "",
    @Json(name = "keterangan") val keterangan: String = "",
    @Json(name = "postingIg") val postingIg: String = "",
    @Json(name = "jadwalPosting") val jadwalPosting: String = "",
    @Json(name = "namaMe") val namaMe: String = "",
    @Json(name = "catatan") val catatan: String = ""
)

@JsonClass(generateAdapter = true)
data class MeetingListingsResponse(
    @Json(name = "status") val status: String = "",
    @Json(name = "sheetName") val sheetName: String = "",
    @Json(name = "date") val date: String = "",
    @Json(name = "listings") val listings: List<MeetingListing> = emptyList(),
    @Json(name = "message") val message: String? = null
)

