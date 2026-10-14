package com.meettrack.app.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meettrack.app.data.db.MeetTrackDatabase
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingStatus
import com.meettrack.app.data.repository.ClientRepository
import com.meettrack.app.data.repository.MeetingRepository
import com.meettrack.app.data.preferences.SettingsRepository
import com.meettrack.app.notifications.NotificationScheduler
import com.meettrack.app.notifications.ReminderRescheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class BackupManagerImportTest {

    @Test
    fun importingExistingMeetingIdsReschedulesTheirReminders() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(
            context,
            MeetTrackDatabase::class.java
        ).allowMainThreadQueries().build()

        try {
            val meetingRepository = MeetingRepository(database.meetingDao())
            val notificationScheduler = NotificationScheduler(
                context,
                SettingsRepository(context)
            )
            val clientRepository = ClientRepository(
                database.clientDao(),
                meetingRepository,
                notificationScheduler
            )
            val client = Client(fullName = "Existing Client", phoneNumber = "555-0100")
            val clientId = database.clientDao().insertClient(client)
            val meeting = Meeting(
                clientId = clientId,
                title = "Existing meeting",
                dateTime = LocalDateTime.now().plusDays(1),
                reminderIntervals = "[30]"
            )
            val meetingId = database.meetingDao().insertMeeting(meeting)
            val importedMeeting = meeting.copy(
                id = meetingId,
                title = "Restored meeting",
                status = MeetingStatus.RESCHEDULED
            )
            val recordingRescheduler = RecordingRescheduler()
            val backupManager = BackupManager(
                database,
                clientRepository,
                meetingRepository,
                recordingRescheduler
            )
            val backupJson = BackupPayloadJson.encode(
                BackupPayload(
                    clients = listOf(client.copy(id = clientId)),
                    meetings = listOf(importedMeeting)
                )
            )

            val result = backupManager.importFromJson(backupJson)

            assertTrue(result.isSuccess)
            assertEquals(listOf(meetingId), recordingRescheduler.rescheduledMeetingIds)
            assertEquals("Restored meeting", meetingRepository.getMeetingById(meetingId)?.title)
        } finally {
            database.close()
        }
    }

    private class RecordingRescheduler : ReminderRescheduler {
        val rescheduledMeetingIds = mutableListOf<Long>()

        override suspend fun rescheduleUpcoming(
            meetings: List<com.meettrack.app.data.model.MeetingWithClient>
        ) {
            rescheduledMeetingIds += meetings.map { it.meeting.id }
        }
    }
}
