package com.example.ui.screens

import com.example.data.Schedule
import java.text.SimpleDateFormat
import java.util.*

object WhatsAppFormatter {

    fun getIndonesianTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    fun getHonorificForMe(name: String): String {
        val clean = name.lowercase().trim()
        
        // Direct checks if the name already includes honorifics
        if (clean.startsWith("pak ") || clean.startsWith("pak/")) return "Pak"
        if (clean.startsWith("bu ") || clean.startsWith("bu/") || clean.startsWith("ibu ")) return "Bu"
        
        // Exact lists provided by the user
        val maleNames = listOf(
            "donny", "agung", "bayu", "ilham", "iskandar", "remmy", "sam", "santiaji", 
            "vincent", "yayan", "haryadi", "rony", "dasep", "dedie", "dhenis", 
            "zulkifli", "muljadi", "andika", "briand"
        )
        val femaleNames = listOf(
            "dian", "dini", "dutta", "ifa", "ike", "imelda", "resmi", "indah", 
            "ruby", "aii dyana", "ayu", "mari", "amelia", "hilda", "desy", 
            "rika", "meisi", "yuma"
        )
        
        for (m in maleNames) {
            if (clean.contains(m)) return "Pak"
        }
        for (f in femaleNames) {
            if (clean.contains(f)) return "Bu"
        }
        
        // Standard female name keyword matching for extra coverage
        val standardFemaleKeywords = listOf("sri", "siti", "dewi", "putri", "fitri", "maria", "rani", "lia", "eka", "linda", "kartika", "nur", "sarah")
        if (standardFemaleKeywords.any { clean.contains(it) }) {
            return "Bu"
        }
        
        return "Pak/Bu" // Default fallback if not found
    }

    fun getHonorificLongForMe(name: String): String {
        val honorific = getHonorificForMe(name)
        return if (honorific == "Bu") "Ibu" else "Pak"
    }

    fun isScheduleDone(schedule: Schedule): Boolean {
        val status = schedule.status.lowercase().trim()
        val type = schedule.type.lowercase().trim()
        return status == "done" || status == "selesai" || type.startsWith("done")
    }

    fun generateWhatsAppFollowUpMessage(context: android.content.Context, schedule: Schedule, targetMeName: String): String {
        val prefs = com.example.data.PreferenceManager(context)
        val customTemplate = if (isScheduleDone(schedule)) prefs.formatDone else prefs.formatNotDone
        
        if (customTemplate.isNotBlank()) {
            return substituteTags(customTemplate, schedule, targetMeName)
        }

        val cleanId = schedule.idListing.trim()
        val formattedDate = formatIndonesianDate(schedule.tanggal)
        val formattedTime = formatTwelveHourTime(schedule.jam)
        
        val cleanName = targetMeName.trim()
        val hasHonorific = cleanName.lowercase().startsWith("pak ") || 
                           cleanName.lowercase().startsWith("pak/") || 
                           cleanName.lowercase().startsWith("bu ") || 
                           cleanName.lowercase().startsWith("bu/") || 
                           cleanName.lowercase().startsWith("ibu ")
        
        val honorific = getHonorificForMe(targetMeName)
        val greetingName = if (targetMeName.isNotBlank()) {
            if (hasHonorific) {
                cleanName
            } else {
                if (honorific != "Pak/Bu") "$honorific $cleanName" else "Pak/Bu $cleanName"
            }
        } else {
            "ME Ray White"
        }

        return if (isScheduleDone(schedule)) {
            // Already Done format with dynamic Pak/Bu prefix and automated message indicator
            """
Halo $greetingName.
Foto listing ${schedule.idListing.ifBlank { "(Manual Input)" }} sudah kita update ya $honorific.

Berikut info lengkap jadwal kegiatan untuk properti Anda:
📌 *ID Listing*: ${schedule.idListing.ifBlank { "(Manual Input)" }}
🎬 *Tipe*: ${schedule.type}
📅 *Jadwal*: $formattedDate pada $formattedTime
📍 *Lokasi*: ${schedule.lokasi.ifBlank { "-" }}
🏃 *Staff*: ${schedule.staff}
⚡ *Status*: ${schedule.status}

Detail lengkap properti website Ray White Cipete:
🔗 https://raywhitecipete.net/ListingView/Detail/$cleanId

Terima kasih.
            """.trimIndent()
        } else {
            // Pending / Not Done format requested by the user
            val greeting = getIndonesianTimeGreeting()
            val honorificLong = if (honorific == "Bu") "Ibu" else "Pak"
            val displayMeName = targetMeName.ifBlank { "ME Ray White" }
            
            """
$greeting $honorific $displayMeName.

Listing $honorificLong $displayMeName berikut kira-kira bisa kita ambil foto ulang kapan? agar kita dapat input ke dalam jadwal.

🔗 https://raywhitecipete.net/ListingView/Detail/$cleanId

Terima kasih.
            """.trimIndent()
        }
    }

    private fun substituteTags(template: String, schedule: Schedule, targetMeName: String): String {
        val cleanId = schedule.idListing.trim()
        val formattedDate = formatIndonesianDate(schedule.tanggal)
        val formattedTime = formatTwelveHourTime(schedule.jam)
        
        val greeting = getIndonesianTimeGreeting()
        val cleanName = targetMeName.trim()
        val hasHonorific = cleanName.lowercase().startsWith("pak ") || 
                           cleanName.lowercase().startsWith("pak/") || 
                           cleanName.lowercase().startsWith("bu ") || 
                           cleanName.lowercase().startsWith("bu/") || 
                           cleanName.lowercase().startsWith("ibu ")
        
        val honorific = getHonorificForMe(targetMeName)
        val displayHonorificName = if (targetMeName.isNotBlank()) {
            if (hasHonorific) {
                cleanName
            } else {
                if (honorific != "Pak/Bu") "$honorific $cleanName" else "Pak/Bu $cleanName"
            }
        } else {
            "ME Ray White"
        }
        val honorificLong = if (honorific == "Bu") "Ibu" else "Pak"
        val displayMeName = targetMeName.ifBlank { "ME Ray White" }
        
        return template
            .replace("{greeting}", greeting)
            .replace("{honorific_name}", displayHonorificName)
            .replace("{me_name}", displayMeName)
            .replace("{id_listing}", cleanId.ifBlank { "(Manual Input)" })
            .replace("{type}", schedule.type)
            .replace("{lokasi}", schedule.lokasi.ifBlank { "-" })
            .replace("{jadwal_tanggal}", formattedDate)
            .replace("{jadwal_jam}", formattedTime)
            .replace("{staff}", schedule.staff)
            .replace("{status}", schedule.status)
            .replace("{website_link}", "https://raywhitecipete.net/ListingView/Detail/$cleanId")
    }

    private fun formatIndonesianDate(rawDate: String): String {
        val dateStr = rawDate.trim()
        val inputFormats = listOf("yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy/MM/dd")
        var parsedDate: Date? = null
        
        for (fmt in inputFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                parsedDate = sdf.parse(dateStr)
                if (parsedDate != null) break
            } catch (_: Exception) {}
        }
        
        if (parsedDate == null) return dateStr
        
        try {
            val sdfIndo = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
            return sdfIndo.format(parsedDate)
        } catch (e: Exception) {
            return dateStr
        }
    }

    private fun formatTwelveHourTime(rawTime: String): String {
        val timeStr = rawTime.trim()
        val timeParts = timeStr.split(":")
        if (timeParts.size >= 2) {
            try {
                val hour = timeParts[0].toInt()
                val min = timeParts[1].toInt()
                val period = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, min, period)
            } catch (_: Exception) {}
        }
        return timeStr
    }
}
