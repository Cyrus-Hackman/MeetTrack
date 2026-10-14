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
import com.meettrack.app.MainActivity
import com.meettrack.app.R
import com.meettrack.app.data.preferences.SettingsRepository
import com.meettrack.app.data.model.parseReminderIntervals
import com.meettrack.app.data.model.MeetingWithClient
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.util.toEpochMillisOrNull
import kotlinx.coroutines.flow.first
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.util.Objects
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ReminderRescheduler {
    companion object {
        const val CHANNEL_ID = "meettrack_reminders"
        const val CHANNEL_NAME = "Meeting Reminders"
        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
        private const val TAG = "NotificationScheduler"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    suspend fun scheduleReminders(meetingWithClient: MeetingWithClient) {
        val meeting = meetingWithClient.meeting

        // Cancel existing reminders first
        cancelReminders(meeting.id, parseReminderIntervals(meeting.reminderIntervals))

        val meetingEpoch = meeting.dateTime.toEpochMillisOrNull()
        if (meetingEpoch == null) {
            Log.w(
                TAG,
                "Meeting ${meeting.id} has an invalid or out-of-range date; reminders were not scheduled"
            )
            return
        }

        if (!settingsRepository.settings.first().notificationsEnabled ||
            meeting.status.name == "CANCELLED"
        ) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            Log.w(TAG, "Exact alarm permission is not granted; reminders were not scheduled")
            return
        }

        val intervals = parseReminderIntervals(meeting.reminderIntervals)
        intervals.forEach { minutesBefore ->
            val triggerTime = meetingEpoch - (minutesBefore * 60 * 1000L)
            if (triggerTime > System.currentTimeMillis()) {
                scheduleAlarm(
                    meetingId = meeting.id,
                    minutesBefore = minutesBefore,
                    triggerAtMillis = triggerTime
                )
            }
        }
    }

    override suspend fun rescheduleUpcoming(meetings: List<MeetingWithClient>) {
        val now = LocalDateTime.now()
        meetings.asSequence()
            .filter {
                it.meeting.status in setOf(MeetingStatus.UPCOMING, MeetingStatus.RESCHEDULED) &&
                    it.meeting.dateTime.isAfter(now)
            }
            .forEach { scheduleReminders(it) }
    }

    private fun scheduleAlarm(
        meetingId: Long,
        minutesBefore: Int,
        triggerAtMillis: Long
    ) {
        val requestCode = getRequestCode(meetingId, minutesBefore)
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEETING_ID, meetingId)
            putExtra(EXTRA_MINUTES_BEFORE, minutesBefore)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
            Log.d(TAG, "Scheduled alarm for meeting $meetingId, $minutesBefore min before")
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm", e)
        }
    }

    fun cancelReminders(meetingId: Long, intervals: List<Int>) {
        intervals.distinct().forEach { minutesBefore ->
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MEETING_ID, meetingId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, Objects.hash(meetingId, "tap"), tapIntent,
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
            Objects.hash(meetingId, "view"),
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
                Objects.hash(meetingId, "call"),
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
                Objects.hash(meetingId, "navigate"),
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationBuilder.addAction(
                R.drawable.ic_notification,
                "Navigate",
                mapPendingIntent
            )
        }

        val notifId = Objects.hash(meetingId, minutesBefore, "notification")
        notificationManager.notify(notifId, notificationBuilder.build())
    }

    private fun getRequestCode(meetingId: Long, minutesBefore: Int): Int =
        Objects.hash(meetingId, minutesBefore)
}
