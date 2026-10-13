package com.meettrack.app.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.meettrack.app.MainActivity
import com.meettrack.app.R
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingWithClient
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "meettrack_reminders"
        const val CHANNEL_NAME = "Meeting Reminders"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_CLIENT_NAME = "client_name"
        const val EXTRA_CLIENT_PHONE = "client_phone"
        const val EXTRA_MEETING_TITLE = "meeting_title"
        const val EXTRA_MEETING_DATETIME = "meeting_datetime"
        const val EXTRA_MEETING_LOCATION = "meeting_location"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
        private const val TAG = "NotificationScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val gson = Gson()

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for upcoming client meetings"
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleReminders(meetingWithClient: MeetingWithClient) {
        val meeting = meetingWithClient.meeting
        val client = meetingWithClient.client

        // Cancel existing reminders first
        cancelReminders(meeting.id)

        if (meeting.status.name == "CANCELLED") return

        val intervals = parseIntervals(meeting.reminderIntervals)
        val meetingEpoch = meeting.dateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val effectiveLocation = meeting.location.ifBlank {
            client.formattedAddress.ifBlank { client.address }
        }

        intervals.forEach { minutesBefore ->
            val triggerTime = meetingEpoch - (minutesBefore * 60 * 1000L)
            if (triggerTime > System.currentTimeMillis()) {
                scheduleAlarm(
                    meetingId = meeting.id,
                    minutesBefore = minutesBefore,
                    triggerAtMillis = triggerTime,
                    clientName = client.fullName,
                    clientPhone = client.phoneNumber,
                    meetingTitle = meeting.title,
                    meetingDateTime = meeting.dateTime,
                    meetingLocation = effectiveLocation
                )
            }
        }
    }

    private fun scheduleAlarm(
        meetingId: Long,
        minutesBefore: Int,
        triggerAtMillis: Long,
        clientName: String,
        clientPhone: String,
        meetingTitle: String,
        meetingDateTime: LocalDateTime,
        meetingLocation: String
    ) {
        val requestCode = getRequestCode(meetingId, minutesBefore)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEETING_ID, meetingId)
            putExtra(EXTRA_CLIENT_NAME, clientName)
            putExtra(EXTRA_CLIENT_PHONE, clientPhone)
            putExtra(EXTRA_MEETING_TITLE, meetingTitle)
            putExtra(EXTRA_MEETING_DATETIME, meetingDateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")))
            putExtra(EXTRA_MEETING_LOCATION, meetingLocation)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for meeting $meetingId, $minutesBefore min before")
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm: ${e.message}")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminders(meetingId: Long) {
        val allPossibleIntervals = listOf(1, 5, 10, 15, 30, 60, 120, 1440)
        allPossibleIntervals.forEach { minutesBefore ->
            val requestCode = getRequestCode(meetingId, minutesBefore)
            val intent = Intent(context, ReminderAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    fun showImmediateNotification(
        meetingId: Long,
        clientName: String,
        clientPhone: String,
        meetingTitle: String,
        meetingDateTime: String,
        meetingLocation: String,
        minutesBefore: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val timeText = when {
            minutesBefore == 0 -> "NOW"
            minutesBefore < 60 -> "in $minutesBefore minutes"
            minutesBefore == 60 -> "in 1 hour"
            minutesBefore < 1440 -> "in ${minutesBefore / 60} hours"
            else -> "tomorrow"
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_MEETING_ID, meetingId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, meetingId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("📅 Meeting $timeText")
            .setContentText("$meetingTitle with $clientName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        buildString {
                            append("$meetingTitle with $clientName\n")
                            append("📅 $meetingDateTime\n")
                            if (meetingLocation.isNotBlank()) append("📍 $meetingLocation")
                        }
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setVibrate(longArrayOf(0, 300, 100, 300))

        // Action button 1: "View Details"
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            (meetingId * 1000 + 1).toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.addAction(
            R.drawable.ic_notification,
            "View Details",
            viewPendingIntent
        )

        // Action button 2: "Call Client" (if client has phone number)
        if (clientPhone.isNotBlank()) {
            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clientPhone")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                context,
                (meetingId * 1000 + 2).toInt(),
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                R.drawable.ic_notification,
                "Call Client",
                callPendingIntent
            )
        }

        // Action button 3: "Navigate" (if meeting or client location exists)
        if (meetingLocation.isNotBlank()) {
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(meetingLocation)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val mapPendingIntent = PendingIntent.getActivity(
                context,
                (meetingId * 1000 + 3).toInt(),
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                R.drawable.ic_notification,
                "Navigate",
                mapPendingIntent
            )
        }

        val notifId = (meetingId * 1000 + minutesBefore).toInt()
        notificationManager.notify(notifId, notificationBuilder.build())
    }

    private fun parseIntervals(json: String): List<Int> {
        return try {
            val type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getRequestCode(meetingId: Long, minutesBefore: Int): Int =
        (meetingId * 10000 + minutesBefore).toInt()
}
