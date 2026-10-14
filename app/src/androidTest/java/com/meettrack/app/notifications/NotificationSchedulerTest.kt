package com.meettrack.app.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meettrack.app.data.model.CORRUPT_DATE_SENTINEL
import com.meettrack.app.data.model.Client
import com.meettrack.app.data.model.Meeting
import com.meettrack.app.data.model.MeetingWithClient
import com.meettrack.app.data.preferences.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Objects

@RunWith(AndroidJUnit4::class)
class NotificationSchedulerTest {

    @Test
    fun corruptDateDoesNotScheduleReminder() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val meetingId = System.nanoTime()
        val minutesBefore = 30
        val scheduler = NotificationScheduler(context, SettingsRepository(context))
        val meeting = Meeting(
            id = meetingId,
            clientId = 1,
            title = "Corrupt date",
            dateTime = CORRUPT_DATE_SENTINEL,
            reminderIntervals = "[$minutesBefore]"
        )

        scheduler.scheduleReminders(
            MeetingWithClient(meeting, Client(id = 1, fullName = "Client", phoneNumber = ""))
        )

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            Objects.hash(meetingId, minutesBefore),
            Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        assertNull(pendingIntent)
    }
}
