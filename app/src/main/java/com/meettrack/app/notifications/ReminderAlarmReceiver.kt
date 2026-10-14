package com.meettrack.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.meettrack.app.data.db.MeetTrackDatabase
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.data.preferences.SettingsRepository
import com.meettrack.app.di.ApplicationScope
import com.meettrack.app.util.formatMeetingDateTime
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationScheduler: NotificationScheduler

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var database: MeetTrackDatabase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val meetingId = intent.getLongExtra(NotificationScheduler.EXTRA_MEETING_ID, -1L)
        val minutesBefore = intent.getIntExtra(NotificationScheduler.EXTRA_MINUTES_BEFORE, 30)

        if (meetingId != -1L) {
            val pendingResult = goAsync()
            applicationScope.launch {
                try {
                    if (settingsRepository.settings.first().notificationsEnabled) {
                        val meetingWithClient =
                            database.meetingDao().getMeetingWithClientByIdDirect(meetingId)
                                ?: return@launch
                        if (meetingWithClient.meeting.status in setOf(
                                MeetingStatus.CANCELLED,
                                MeetingStatus.COMPLETED
                            )
                        ) return@launch

                        val meeting = meetingWithClient.meeting
                        val client = meetingWithClient.client
                        val location = meeting.location.ifBlank {
                            client.formattedAddress.ifBlank { client.address }
                        }
                        notificationScheduler.showImmediateNotification(
                            meetingId = meetingId,
                            clientName = client.fullName,
                            clientPhone = client.phoneNumber,
                            meetingTitle = meeting.title,
                            meetingDateTime = meeting.dateTime.formatMeetingDateTime(
                                DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
                            ),
                            meetingLocation = location,
                            minutesBefore = minutesBefore
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ReminderAlarmReceiver", "Failed to process meeting reminder", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
