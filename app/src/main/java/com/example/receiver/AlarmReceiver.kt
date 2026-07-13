package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Schedule
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("AlarmReceiver", "Device rebooted, rescheduling alarms from local database.")
            val pendingResult = goAsync()
            val applicationContext = context.applicationContext
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val schedules = db.scheduleDao().getSchedulesList()
                    syncAllAlarms(applicationContext, schedules)
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to reschedule alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val scheduleId = intent.getIntExtra("schedule_id", -1)
        val idListing = intent.getStringExtra("id_listing") ?: ""
        val namaMe = intent.getStringExtra("nama_me") ?: ""
        val lokasi = intent.getStringExtra("lokasi") ?: ""
        val jam = intent.getStringExtra("jam") ?: ""
        val alarmType = intent.getIntExtra("alarm_type", 1) // 1 = 2h, 2 = 1h, 3 = 30m

        if (scheduleId == -1) return

        showNotification(context, scheduleId, idListing, namaMe, lokasi, jam, alarmType)
    }

    private fun showNotification(
        context: Context,
        scheduleId: Int,
        idListing: String,
        namaMe: String,
        lokasi: String,
        jam: String,
        alarmType: Int
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "schedule_alarms_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jadwal Foto Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat sesi foto properti"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when notification clicked
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, scheduleId * 10 + alarmType, clickIntent, flags)

        val title = when (alarmType) {
            1 -> "Pengingat 2 Jam - Sesi Foto"
            2 -> "Pengingat 1 Jam - Sesi Foto"
            else -> "⚠️ Pengingat 30 Menit - Sesi Foto"
        }

        val leadTimeWord = when (alarmType) {
            1 -> "2 jam"
            2 -> "1 jam"
            else -> "30 menit"
        }

        val body = "Sesi Foto ID: $idListing dengan ME $namaMe di $lokasi akan dimulai dalam $leadTimeWord (pukul $jam). Bersiaplah!"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        // Notification unique ID per schedule + alarm sub-type
        val notificationId = scheduleId * 10 + alarmType
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val TAG = "AlarmScheduler"

        fun parseScheduleDateTime(rawDate: String, rawTime: String): Date? {
            val dateStr = rawDate.trim()
            var timeStr = rawTime.trim()
            if (timeStr.length == 5) {
                timeStr += ":00"
            } else if (timeStr.length > 8) {
                timeStr = timeStr.substring(0, 8)
            }
            
            val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm")
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                    val date = sdf.parse("$dateStr $timeStr")
                    if (date != null) return date
                } catch (_: Exception) {}
            }
            return null
        }

        private fun isAlarmAlreadyScheduled(context: Context, scheduleId: Int, alarmType: Int, triggerTimeMs: Long): Boolean {
            val prefs = context.getSharedPreferences("scheduled_alarms_tracker", Context.MODE_PRIVATE)
            val key = "alarm_${scheduleId}_${alarmType}"
            val lastTriggerTime = prefs.getLong(key, -1L)
            if (lastTriggerTime != triggerTimeMs) {
                return false
            }
            
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = scheduleId * 10 + alarmType
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            return pendingIntent != null
        }
        
        private fun markAlarmAsScheduled(context: Context, scheduleId: Int, alarmType: Int, triggerTimeMs: Long) {
            val prefs = context.getSharedPreferences("scheduled_alarms_tracker", Context.MODE_PRIVATE)
            prefs.edit().putLong("alarm_${scheduleId}_${alarmType}", triggerTimeMs).apply()
        }
        
        private fun clearAlarmTracker(context: Context, scheduleId: Int, alarmType: Int) {
            val prefs = context.getSharedPreferences("scheduled_alarms_tracker", Context.MODE_PRIVATE)
            prefs.edit().remove("alarm_${scheduleId}_${alarmType}").apply()
        }

        fun scheduleAlarmsForSchedule(context: Context, schedule: Schedule) {
            val isDone = schedule.status.lowercase().trim() == "done" ||
                         schedule.status.lowercase().trim() == "selesai" ||
                         schedule.type.lowercase().trim().startsWith("done")

            if (isDone) {
                cancelAlarmsForSchedule(context, schedule)
                return
            }

            val targetDateTime = parseScheduleDateTime(schedule.tanggal, schedule.jam) ?: return
            val currentTime = System.currentTimeMillis()

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // 1. Alarm 2 Jam (120 min) sebelum
            val time2h = targetDateTime.time - (120 * 60 * 1000)
            if (time2h > currentTime) {
                if (!isAlarmAlreadyScheduled(context, schedule.id, 1, time2h)) {
                    setAlarm(context, alarmManager, schedule, 1, time2h)
                    markAlarmAsScheduled(context, schedule.id, 1, time2h)
                }
            } else {
                cancelSingleAlarm(context, schedule.id, 1)
            }

            // 2. Alarm 1 Jam (60 min) sebelum
            val time1h = targetDateTime.time - (60 * 60 * 1000)
            if (time1h > currentTime) {
                if (!isAlarmAlreadyScheduled(context, schedule.id, 2, time1h)) {
                    setAlarm(context, alarmManager, schedule, 2, time1h)
                    markAlarmAsScheduled(context, schedule.id, 2, time1h)
                }
            } else {
                cancelSingleAlarm(context, schedule.id, 2)
            }

            // 3. Alarm 30 Menit (30 min) sebelum
            val time30m = targetDateTime.time - (30 * 60 * 1000)
            if (time30m > currentTime) {
                if (!isAlarmAlreadyScheduled(context, schedule.id, 3, time30m)) {
                    setAlarm(context, alarmManager, schedule, 3, time30m)
                    markAlarmAsScheduled(context, schedule.id, 3, time30m)
                }
            } else {
                cancelSingleAlarm(context, schedule.id, 3)
            }
        }

        private fun setAlarm(
            context: Context,
            alarmManager: AlarmManager,
            schedule: Schedule,
            alarmType: Int,
            triggerTimeMs: Long
        ) {
            try {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("schedule_id", schedule.id)
                    putExtra("id_listing", schedule.idListing)
                    putExtra("nama_me", schedule.namaMe)
                    putExtra("lokasi", schedule.lokasi)
                    putExtra("jam", schedule.jam)
                    putExtra("alarm_type", alarmType)
                }

                val requestCode = schedule.id * 10 + alarmType
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                            // Safe fallback for exact alarms if permission not granted
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                        }
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                    }
                    Log.d(TAG, "Scheduled alarm type $alarmType for schedule ${schedule.id} at ${Date(triggerTimeMs)}")
                } catch (e: Exception) {
                    try {
                        // Fallback to inexact set
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                        Log.w(TAG, "Failed setExact, used inexact fallback: ${e.message}")
                    } catch (inner: Exception) {
                        Log.e(TAG, "Failed set inexact fallback: ${inner.message}", inner)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed overall setAlarm: ${e.message}", e)
            }
        }

        fun cancelAlarmsForSchedule(context: Context, schedule: Schedule) {
            cancelSingleAlarm(context, schedule.id, 1)
            cancelSingleAlarm(context, schedule.id, 2)
            cancelSingleAlarm(context, schedule.id, 3)
        }

        private fun cancelSingleAlarm(context: Context, scheduleId: Int, alarmType: Int) {
            try {
                val intent = Intent(context, AlarmReceiver::class.java)
                val requestCode = scheduleId * 10 + alarmType
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_NO_CREATE
                }
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
                if (pendingIntent != null) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    alarmManager?.cancel(pendingIntent)
                    pendingIntent.cancel()
                    Log.d(TAG, "Cancelled alarm type $alarmType for schedule $scheduleId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel single alarm for schedule $scheduleId: ${e.message}")
            } finally {
                clearAlarmTracker(context, scheduleId, alarmType)
            }
        }

        fun syncAllAlarms(context: Context, schedules: List<Schedule>) {
            try {
                // One-time sweep to clear any orphaned leaked alarms from older versions (v2.2 or below)
                val prefs = context.getSharedPreferences("scheduled_alarms_tracker", Context.MODE_PRIVATE)
                val sweepDone = prefs.getBoolean("leaked_alarms_sweep_done_v2", false)
                if (!sweepDone) {
                    Log.d(TAG, "Running one-time sweep to cancel legacy leaked alarms...")
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (alarmManager != null) {
                        for (id in 1..2000) {
                            for (type in 1..3) {
                                val intent = Intent(context, AlarmReceiver::class.java)
                                val requestCode = id * 10 + type
                                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                                } else {
                                    PendingIntent.FLAG_NO_CREATE
                                }
                                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
                                if (pendingIntent != null) {
                                    alarmManager.cancel(pendingIntent)
                                    pendingIntent.cancel()
                                }
                            }
                        }
                    }
                    prefs.edit().putBoolean("leaked_alarms_sweep_done_v2", true).apply()
                    Log.d(TAG, "Legacy leaked alarms sweep completed successfully.")
                }

                val currentTime = System.currentTimeMillis()
                
                // 1. Separate schedules into toSchedule and toCancel
                val (upcomingSchedules, otherSchedules) = schedules.partition { schedule ->
                    val isDone = schedule.status.lowercase().trim() == "done" ||
                                 schedule.status.lowercase().trim() == "selesai" ||
                                 schedule.type.lowercase().trim().startsWith("done")
                    
                    if (isDone) {
                        false
                    } else {
                        val targetDateTime = parseScheduleDateTime(schedule.tanggal, schedule.jam)
                        if (targetDateTime == null) {
                            false
                        } else {
                            // Keep if any of the three alarms is in the future.
                            // The latest alarm is 30 mins (1800000 ms) before the target.
                            val latestAlarmTime = targetDateTime.time - (30 * 60 * 1000)
                            latestAlarmTime > currentTime
                        }
                    }
                }
                
                // 2. Sort upcoming schedules by datetime ascending, and take the top 25 closest ones
                val sortedUpcoming = upcomingSchedules.sortedBy { schedule ->
                    parseScheduleDateTime(schedule.tanggal, schedule.jam)?.time ?: Long.MAX_VALUE
                }
                
                val toSchedule = sortedUpcoming.take(25)
                val toCancelUpcoming = sortedUpcoming.drop(25)
                
                // 3. For schedules we want to schedule
                toSchedule.forEach { schedule ->
                    try {
                        scheduleAlarmsForSchedule(context, schedule)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to schedule alarms for schedule ${schedule.id}: ${e.message}", e)
                    }
                }
                
                // 4. Cancel alarms for everything else
                val toCancelTotal = otherSchedules + toCancelUpcoming
                toCancelTotal.forEach { schedule ->
                    try {
                        cancelAlarmsForSchedule(context, schedule)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to cancel alarms for schedule ${schedule.id}: ${e.message}", e)
                    }
                }

                // 5. Clean up orphan alarms (schedules that have been deleted from the database)
                val activeScheduleIds = schedules.map { it.id }.toSet()
                val trackerPrefs = context.getSharedPreferences("scheduled_alarms_tracker", Context.MODE_PRIVATE)
                val allSavedKeys = trackerPrefs.all.keys.toList()
                allSavedKeys.forEach { key ->
                    if (key.startsWith("alarm_")) {
                        val parts = key.split("_")
                        if (parts.size == 3) {
                            val scheduleId = parts[1].toIntOrNull()
                            val alarmType = parts[2].toIntOrNull()
                            if (scheduleId != null && alarmType != null) {
                                if (!activeScheduleIds.contains(scheduleId)) {
                                    cancelSingleAlarm(context, scheduleId, alarmType)
                                }
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Synced alarms. Scheduled: ${toSchedule.size}, Cancelled/Inactive: ${toCancelTotal.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync all alarms: ${e.message}", e)
            }
        }
    }
}
