package com.meettrack.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val meetingId = intent.getLongExtra(NotificationScheduler.EXTRA_MEETING_ID, -1L)
        val clientName = intent.getStringExtra(NotificationScheduler.EXTRA_CLIENT_NAME) ?: ""
        val clientPhone = intent.getStringExtra(NotificationScheduler.EXTRA_CLIENT_PHONE) ?: ""
        val meetingTitle = intent.getStringExtra(NotificationScheduler.EXTRA_MEETING_TITLE) ?: ""
        val meetingDateTime = intent.getStringExtra(NotificationScheduler.EXTRA_MEETING_DATETIME) ?: ""
        val meetingLocation = intent.getStringExtra(NotificationScheduler.EXTRA_MEETING_LOCATION) ?: ""
        val minutesBefore = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTES_BEFORE, 30)

        if (meetingId != -1L) {
            notificationScheduler.showImmediateNotification(
                meetingId = meetingId,
                clientName = clientName,
                clientPhone = clientPhone,
                meetingTitle = meetingTitle,
                meetingDateTime = meetingDateTime,
                meetingLocation = meetingLocation,
                minutesBefore = minutesBefore
            )
        }
    }
}
