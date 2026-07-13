package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.PreferenceManager
import com.example.network.SheetsApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class BackgroundSyncService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private lateinit var db: AppDatabase
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var apiService: SheetsApiService

    override fun onCreate() {
        super.onCreate()
        Log.d("BackgroundSyncService", "Service created")
        db = AppDatabase.getDatabase(this)
        preferenceManager = PreferenceManager(this)
        
        // Setup API service
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        apiService = Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SheetsApiService::class.java)

        // Start background sync loop
        startSyncLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BackgroundSyncService", "Service started command")
        // Always return START_STICKY to restart service if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startSyncLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val url = preferenceManager.appsScriptUrl
                    if (url.isNotBlank()) {
                        Log.d("BackgroundSyncService", "Running silent background sync...")
                        
                        // 1. Sync schedules for all months
                        try {
                            val months = listOf("Juni 2026", "Juli 2026")
                            val parsedSchedules = mutableListOf<com.example.data.Schedule>()
                            val parsedEditFotoTasks = mutableListOf<com.example.data.EditFotoTask>()
                            val moshiObj = Moshi.Builder()
                                .addLast(KotlinJsonAdapterFactory())
                                .build()
 
                            for (monthName in months) {
                                try {
                                    val separator = if (url.contains("?")) "&" else "?"
                                    val encodedMonth = java.net.URLEncoder.encode(monthName, "UTF-8")
                                    val schedulesUrl = "$url${separator}sheetName=$encodedMonth"
                                    val responseBody = apiService.getSchedules(schedulesUrl)
                                    val jsonString = responseBody.string().trim()
 
                                    if (jsonString.startsWith("{")) {
                                        val adapter = moshiObj.adapter(com.example.network.CombinedSheetResponse::class.java)
                                        val response = adapter.fromJson(jsonString)
                                        if (response != null) {
                                            response.schedules.forEach {
                                                parsedSchedules.add(mapSheetScheduleToSchedule(it, monthName))
                                            }
                                            response.editFotoTasks.forEach {
                                                parsedEditFotoTasks.add(mapSheetEditFotoToEntity(it, monthName))
                                            }
                                        }
                                    } else if (jsonString.startsWith("[")) {
                                        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.network.SheetSchedule::class.java)
                                        val adapter = moshiObj.adapter<List<com.example.network.SheetSchedule>>(type)
                                        val list = adapter.fromJson(jsonString)
                                        list?.forEach {
                                            parsedSchedules.add(mapSheetScheduleToSchedule(it, monthName))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("BackgroundSyncService", "Failed to sync month $monthName", e)
                                }
                            }
 
                            if (parsedSchedules.isNotEmpty()) {
                                val actualSchedules = parsedSchedules.filter { it.lokasi.isNotBlank() && it.lokasi != "CHAT_MESSAGE" }
                                if (actualSchedules.isNotEmpty()) {
                                    val unsyncedSchedules = db.scheduleDao().getSchedulesList().filter { !it.synced }
                                    db.scheduleDao().updateSchedulesTransaction(actualSchedules, unsyncedSchedules)
                                    
                                    val finalSchedules = db.scheduleDao().getSchedulesList()
                                    com.example.receiver.AlarmReceiver.syncAllAlarms(applicationContext, finalSchedules)
                                }
                             }
                             if (parsedEditFotoTasks.isNotEmpty()) {
                                 val unsyncedTasks = db.editFotoTaskDao().getEditFotoTasksList().filter { !it.synced }
                                 db.editFotoTaskDao().updateEditFotoTasksTransaction(parsedEditFotoTasks, unsyncedTasks)
                             }
                        } catch (e: Exception) {
                            Log.e("BackgroundSyncService", "Schedule sync failed", e)
                        }

                        // 2. Sync chats & trigger notifications
                        try {
                            val separator = if (url.contains("?")) "&" else "?"
                            val chatUrl = "$url${separator}sheetName=Chat"
                            
                            val responseBody = apiService.getSchedules(chatUrl)
                            val jsonString = responseBody.string().trim()
                            
                            val moshiObj = Moshi.Builder()
                                .addLast(KotlinJsonAdapterFactory())
                                .build()
                            
                            val chatList = mutableListOf<ChatMessage>()
                            var isFallback = false
                            
                            if (jsonString.startsWith("{")) {
                                val adapter = moshiObj.adapter(com.example.network.CombinedSheetResponse::class.java)
                                val response = adapter.fromJson(jsonString)
                                if (response != null) {
                                    val schedules = response.schedules
                                    if (schedules.any { it.lokasi.isNotBlank() && it.lokasi != "CHAT_MESSAGE" }) {
                                        isFallback = true
                                    } else {
                                        schedules.forEach {
                                            chatList.add(mapSheetScheduleToChatMessage(it))
                                        }
                                    }
                                }
                            } else if (jsonString.startsWith("[")) {
                                val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.network.SheetSchedule::class.java)
                                val adapter = moshiObj.adapter<List<com.example.network.SheetSchedule>>(type)
                                val list = adapter.fromJson(jsonString)
                                if (list != null) {
                                    if (list.any { it.lokasi.isNotBlank() && it.lokasi != "CHAT_MESSAGE" }) {
                                        isFallback = true
                                    } else {
                                        list.forEach {
                                            chatList.add(mapSheetScheduleToChatMessage(it))
                                        }
                                    }
                                }
                            }

                            if (!isFallback && chatList.isNotEmpty()) {
                                val senderPrefs = getSharedPreferences("chat_prefs", Context.MODE_PRIVATE)
                                val currentSender = senderPrefs.getString("sender_role_override", null) ?: "Raffa"
                                
                                val existingMessages = db.chatMessageDao().getAllMessages().first()
                                val existingKeys = existingMessages.map { "${it.senderName.trim()}_${it.message.trim()}_${it.timestamp}" }.toSet()
                                
                                val unsynced = existingMessages.filter { !it.synced }
                                db.chatMessageDao().updateChatMessagesTransaction(chatList, unsynced)
                                
                                val lastSeen = preferenceManager.lastSeenChatTimestamp
                                val newMessages = chatList.filter { msg ->
                                    val key = "${msg.senderName.trim()}_${msg.message.trim()}_${msg.timestamp}"
                                    !existingKeys.contains(key) && 
                                    msg.senderName != currentSender && 
                                    !msg.message.startsWith("READ_RECEIPT_FOR_") &&
                                    msg.timestamp > lastSeen
                                }

                                if (newMessages.isNotEmpty()) {
                                    newMessages.sortedBy { it.timestamp }.forEach { msg ->
                                        showChatNotification(msg)
                                    }
                                    val maxTs = newMessages.maxOfOrNull { it.timestamp } ?: lastSeen
                                    preferenceManager.lastSeenChatTimestamp = maxTs
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BackgroundSyncService", "Chat sync failed", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BackgroundSyncService", "Error in sync loop", e)
                }
                delay(10000)
            }
        }
    }

    private fun showChatNotification(message: ChatMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_messages_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Pesan Chat Baru",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk pesan obrolan tim baru"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to_chat", true)
        }
        val pFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, message.id, clickIntent, pFlags)

        val cleanMsg = message.message
        val title = "Pesan baru dari ${message.senderName}"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(cleanMsg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cleanMsg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(message.id.takeIf { it != 0 } ?: System.currentTimeMillis().toInt(), notification)
    }

    private fun mapSheetScheduleToChatMessage(it: com.example.network.SheetSchedule): ChatMessage {
        val messageText = it.idListing.trim()
        val sender = it.namaMe.trim()
        val device = it.staff.trim().ifBlank { "Unknown" }
        
        val timestamp = it.type.toLongOrNull() ?: try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            val parsedDate = sdf.parse("${it.tanggal.trim()} ${it.jam.trim()}")
            parsedDate?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        return ChatMessage(
            no = it.no,
            senderName = sender,
            message = messageText,
            timestamp = timestamp,
            deviceModel = device,
            synced = true
        )
    }

    private fun mapSheetScheduleToSchedule(it: com.example.network.SheetSchedule, defaultMonth: String = ""): com.example.data.Schedule {
        val finalIdListing = it.idListing.trim()
        val finalNamaMe = it.namaMe.trim()
        val finalLokasi = it.lokasi.trim()
        val finalStaff = it.staff.trim()
        val finalTanggal = com.example.data.normalizeDate(it.tanggal)
        val finalJam = it.jam.trim()
        val finalType = it.type.trim()
        val finalStatus = it.status.trim()
        val finalSheetName = if (it.sheetName.isNotBlank()) it.sheetName.trim() else defaultMonth

        return com.example.data.Schedule(
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

    private fun mapSheetEditFotoToEntity(it: com.example.network.SheetEditFotoTask, monthName: String = ""): com.example.data.EditFotoTask {
        return com.example.data.EditFotoTask(
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BackgroundSyncService", "Service destroyed")
        serviceJob.cancel()
    }
}
