package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("jadwal_foto_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_APPS_SCRIPT_URL = "apps_script_url"
        private const val KEY_SPREADSHEET_ID = "spreadsheet_id"
        private const val KEY_WEEKLY_MEETING_URL = "weekly_meeting_url"
        
        // DEFAULT_URL: Paste your Google Apps Script URL here so new devices connect auto-magically!
        const val DEFAULT_APPS_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbytrM7-rYQ_EjK9pzHJn4GvFL8j9ypajc6-BzzAqbCCbawXoXf9Gi9E0ECPNmjVsXIH/exec"
        const val DEFAULT_WEEKLY_MEETING_URL = "https://script.google.com/macros/s/AKfycbytrM7-rYQ_EjK9pzHJn4GvFL8j9ypajc6-BzzAqbCCbawXoXf9Gi9E0ECPNmjVsXIH/exec"
    }

    var appsScriptUrl: String
        get() {
            val url = prefs.getString(KEY_APPS_SCRIPT_URL, DEFAULT_APPS_SCRIPT_URL)
            val oldUrls = setOf(
                "https://script.google.com/macros/s/AKfycbwrCQKzNu047yMnQQEhkty1rvCsujrAHtyZGrczgB5-awZAh0R03XnMBIxqA1sHXwgL/exec",
                "https://script.google.com/macros/s/AKfycbwimHayI2ub4x6xabamVvCribr97G3CUIJgEAlFF0MVqOVSHrsni6Zvs6A5MUY2UQMh/exec",
                "https://script.google.com/macros/s/AKfycbziazZC-ynPJ5jDNGPGzdkEKd2Cjh3fmmsaskuLf6ZexclD8bGUJnfDcTge5F0n9GSc/exec",
                "https://script.google.com/macros/s/AKfycbwi54YGMF62kRa5S3TqDe_4SlNSRxrxNXCPFt62kIz5-ZxuJ_W5Pw7c6L6_9kIg0Ca2/exec",
                "https://script.google.com/macros/s/AKfycbzHgc3R_K2UF_mB9ATwSNN9D-SlRZ5NAng4PHB4-bNlpw-vHNzi-DgZdN0p1dL6P_PO/exec",
                "https://script.google.com/macros/s/AKfycbxlNROkH7DZed2uQ_jF3QdN5ALgKet0lA0aLkvNO_rnn0CiaIBf0FuXhDbB-qgmwp7H/exec",
                "https://script.google.com/macros/s/AKfycby0Vlz5juUk15I8YCBGJa6CB9q9rAOO88IpH427RsYj3kcus5ZJEEai8PGexaDLhLBQ/exec",
                "https://script.google.com/macros/s/AKfycbxtlkyXUYx0N4UGxUP7zVRi1_Ms5c-vvOuC1nbPiCbkQQplyW1wi-gp6ma5s2ArdrC/exec",
                "https://script.google.com/macros/s/AKfycby4gcJoh0pg2PSkYDNoKmtuNKJtRBKSltv5M4wZoT9332lXXakt9oB3sY2KNYop6xGw/exec",
                "https://script.google.com/macros/s/AKfycbwKbDoQ2C3kvmUezrEzORRvRgK1icHaBP5qOs8qV5YFMYMbz7VNPZs-_vbs_RiwIzzp/exec",
                "https://script.google.com/macros/s/AKfycbxEt7iMirBKBzgBmtBIsLkrnKB2-O7V6C3ioNdZxcdD4dS3tyakeooyAMtgjqRo4OFq/exec",
                "https://script.google.com/macros/s/AKfycbwbdpjURjH8ikhYiLV_x_vEZE3LIJvrUAgK_0IRhDPike9H98U4fd3VB9In0j4UPyMX/exec",
                "https://script.google.com/macros/s/AKfycbxznuaPNN9ft2HqnIYbsJDobcFGxKUpIVIt12I772Zzcu0UGjTMKgoJ4DZHKAvUu9sD/exec",
                "https://script.google.com/macros/s/AKfycbz1gQG__mJEU8r_G336z4zQiETADLTKBaoGzSLNuVwqIrYeHdtIyFcYPj8CDidI9wM/exec",
                "https://script.google.com/macros/s/AKfycbyY815X1GY3u3PhIZItPji9vIutXMxOyWIfFFp3WOIbcqWyJwVPvumkpHLKnvnCXCDB/exec",
                "https://script.google.com/macros/s/AKfycbxxdpyWR9Uo5853VwFXewN4PpPrsQBBUsSoPuiEAckFhYMFHPqo-vH1SjZeovVoY0Bv/exec",
                "https://script.google.com/macros/s/AKfycbzpZRIxNKOG6mFTdALHotY8kt6MqoOJH9ECGx_3duP682u7WGm7UAfsQAfLkz0Xmmjv/exec",
                "https://script.google.com/macros/s/AKfycbx040AthTH2TpkDIORIPg6Vi_9K1FrJSg2qgQvFsQguKgxYPuARxUU5Wf7n0OOCvm5k/exec",
                "https://script.google.com/macros/s/AKfycbwuQr6LM8mEZo3I19WuxUVwJqzhMTKKlL22xxN_0MSerBEMSABKhOm2ZVqddItk-TXj/exec",
                "https://script.google.com/macros/s/AKfycbySAF6KfUkbU29NBYF847gZwPTWkBARS2Z1QR1Od93MhO2zVzgqcBqYRijrCd1FZEe9/exec",
                "https://script.google.com/macros/s/AKfycbywA2HD3yfOn8NtmLFQNN_KHIhxDbvpIIzIopG8X-0zmn1OGGYbEuV1RqEltd9z2h1g/exec",
                "https://script.google.com/macros/s/AKfycbxW6HWBMwGlLY8PZVqWJn6rKrWmIJO4LLUYh2tz0mZ7rZv_BI2cfdADYkVTqT5QDOjT/exec"
            )
            return if (url.isNullOrBlank() || oldUrls.contains(url)) DEFAULT_APPS_SCRIPT_URL else url
        }
        set(value) = prefs.edit().putString(KEY_APPS_SCRIPT_URL, value.trim()).apply()

    var weeklyMeetingUrl: String
        get() {
            val url = prefs.getString(KEY_WEEKLY_MEETING_URL, DEFAULT_WEEKLY_MEETING_URL) ?: DEFAULT_WEEKLY_MEETING_URL
            val oldWeeklyUrls = setOf(
                "https://script.google.com/macros/s/AKfycbnjoXMwvPMSLKdrcE7Pcvzj0O_8WRZZFNmetZ_a1KmVi9ahwAaINQ9aVZFVLMToX9g/exec",
                "https://script.google.com/macros/s/AKfycbyRNk1K74ju5jFMqlR-eVebsGWgsQ73Mv666pgDF4F80DVRBk4I9BHRGYfao-QvwVZ6Rw/exec",
                "https://script.google.com/macros/s/AKfycbywA2HD3yfOn8NtmLFQNN_KHIhxDbvpIIzIopG8X-0zmn1OGGYbEuV1RqEltd9z2h1g/exec",
                "https://script.google.com/macros/s/AKfycbySAF6KfUkbU29NBYF847gZwPTWkBARS2Z1QR1Od93MhO2zVzgqcBqYRijrCd1FZEe9/exec",
                "https://script.google.com/macros/s/AKfycbwuQr6LM8mEZo3I19WuxUVwJqzhMTKKlL22xxN_0MSerBEMSABKhOm2ZVqddItk-TXj/exec",
                "https://script.google.com/macros/s/AKfycbxW6HWBMwGlLY8PZVqWJn6rKrWmIJO4LLUYh2tz0mZ7rZv_BI2cfdADYkVTqT5QDOjT/exec"
            )
            return if (url.isNullOrBlank() || oldWeeklyUrls.contains(url)) DEFAULT_WEEKLY_MEETING_URL else url
        }
        set(value) = prefs.edit().putString(KEY_WEEKLY_MEETING_URL, value.trim()).apply()

    var spreadsheetId: String
        get() = prefs.getString(KEY_SPREADSHEET_ID, "1L0ajG9dAmhisDQ7ADil5KKNRkzgXp_jdCi41-6mPkIw") ?: "1L0ajG9dAmhisDQ7ADil5KKNRkzgXp_jdCi41-6mPkIw"
        set(value) = prefs.edit().putString(KEY_SPREADSHEET_ID, value.trim()).apply()

    // selectedMonth always returns current month dynamically - never rely on saved pref
    val selectedMonth: String
        get() {
            return try {
                val cal = java.util.Calendar.getInstance()
                val monthFormat = java.text.SimpleDateFormat("MMMM", java.util.Locale("id", "ID"))
                val mName = monthFormat.format(cal.time)
                val capitalized = mName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                "$capitalized 2026"
            } catch (e: Exception) {
                "Juli 2026"
            }
        }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit().putBoolean("is_first_launch", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("is_dark_theme", true)
        set(value) = prefs.edit().putBoolean("is_dark_theme", value).apply()

    var selectedThemeStyle: String
        get() = prefs.getString("selected_theme_style", "COSMIC_SLATE") ?: "COSMIC_SLATE"
        set(value) = prefs.edit().putString("selected_theme_style", value).apply()

    var formatDone: String
        get() = prefs.getString("format_done", "") ?: ""
        set(value) = prefs.edit().putString("format_done", value).apply()

    var formatNotDone: String
        get() = prefs.getString("format_not_done", "") ?: ""
        set(value) = prefs.edit().putString("format_not_done", value).apply()

    var lastSeenChatTimestamp: Long
        get() = prefs.getLong("last_seen_chat_timestamp", 0L)
        set(value) = prefs.edit().putLong("last_seen_chat_timestamp", value).apply()

    fun getListingImage(id: String): String? = prefs.getString("img_$id", null)
    fun saveListingImage(id: String, url: String) = prefs.edit().putString("img_$id", url).apply()

    fun getListingTitle(id: String): String? = prefs.getString("title_$id", null)
    fun saveListingTitle(id: String, title: String) = prefs.edit().putString("title_$id", title).apply()

    fun getListingDesc(id: String): String? = prefs.getString("desc_$id", null)
    fun saveListingDesc(id: String, desc: String) = prefs.edit().putString("desc_$id", desc).apply()

    fun getListingPrice(id: String): String? = prefs.getString("price_$id", null)
    fun saveListingPrice(id: String, price: String) = prefs.edit().putString("price_$id", price).apply()

    fun getListingSold(id: String): Boolean = prefs.getBoolean("sold_$id", false)
    fun saveListingSold(id: String, sold: Boolean) = prefs.edit().putBoolean("sold_$id", sold).apply()

    fun getListingGallery(id: String): List<String>? {
        val str = prefs.getString("gallery_$id", null) ?: return null
        if (str.isBlank()) return emptyList()
        return str.split("|||")
    }
    fun saveListingGallery(id: String, urls: List<String>) {
        prefs.edit().putString("gallery_$id", urls.joinToString("|||")).apply()
    }

    fun getAgentInfo(id: String): String? = prefs.getString("agent_$id", null)
    fun saveAgentInfo(id: String, name: String, waUrl: String, avatarUrl: String) {
        prefs.edit().putString("agent_$id", "$name|||$waUrl|||$avatarUrl").apply()
    }
}
